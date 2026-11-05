package app.lamplore.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lamplore.App
import app.lamplore.audio.AmbienceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Books and illustrations that live online, not in the APK. The catalog (`catalog.json`) holds story
 * text and plate credits (tiny); plate images are downloaded one at a time, the first time a story is
 * shown, and kept in the app's cache. If the phone is offline, the last catalog is used and covers
 * fall back to the drawn motif.
 *
 * Hosted as GitHub Release assets under the tag `content`, so adding a book needs no app update.
 */
object RemoteContent {
    const val BASE = "https://github.com/mahir-hs/lamplore/releases/download/content/"

    var status by mutableStateOf("")
        private set

    private val catalogFile get() = File(App.context.filesDir, "catalog.json")
    private val plateDir get() = File(App.context.filesDir, "plates").apply { mkdirs() }

    /** Loads the saved catalog right away, then tries the network. Safe to call more than once. */
    fun refresh() {
        dropStaleAudioOnce()
        App.scope.launch {
            val cached = withContext(Dispatchers.IO) { runCatching { parse(catalogFile.readText()) }.getOrNull() }
            if (cached != null) apply(cached)
            status = "Checking for new books..."
            val fresh = withContext(Dispatchers.IO) {
                runCatching {
                    val text = get(BASE + "catalog.json")
                    val list = parse(text)
                    catalogFile.writeText(text)
                    list
                }.getOrNull()
            }
            if (fresh != null) apply(fresh)
            status = when {
                fresh != null -> "${fresh.size} online stories"
                cached != null -> "Offline. Showing ${cached.size} saved online stories"
                else -> "No online library yet"
            }
        }
    }

    private fun apply(list: List<Story>) {
        App.stories.removeAll { it.id.startsWith("bk_") }
        val have = App.stories.map { it.id }.toSet()
        App.stories.addAll(list.filter { it.id !in have })
    }

    /** Local file for an online plate, downloading it if needed. Null when offline and not yet cached. */
    suspend fun plateFile(name: String): File? = withContext(Dispatchers.IO) {
        val f = File(plateDir, name)
        if (f.exists() && f.length() > 0) return@withContext f
        runCatching {
            val tmp = File(plateDir, "$name.part")
            val conn = open(BASE + name)
            conn.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            conn.disconnect()
            check(tmp.length() > 0 && tmp.renameTo(f))
            f
        }.getOrNull()
    }

    fun cacheBytes(): Long = plateDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearCache() {
        plateDir.listFiles()?.forEach { it.delete() }
    }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = true
        check(c.responseCode == 200) { "HTTP ${c.responseCode}" }
        return c
    }

    private fun get(url: String): String {
        val c = open(url)
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun parse(text: String): List<Story> {
        val root = JSONObject(text)
        val arr = root.getJSONArray("stories")
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { story(arr.getJSONObject(i)) }.getOrNull()
        }
    }

    // Windows-1252 shows bytes 0x80..0x9F as these characters (the rest of 0x80..0xFF equals Latin-1)
    private const val CP1252_HIGH = "\u20ac\u0081\u201a\u0192\u201e\u2026\u2020\u2021\u02c6\u2030\u0160\u2039\u0152\u008d\u017d\u008f" +
        "\u0090\u2018\u2019\u201c\u201d\u2022\u2013\u2014\u02dc\u2122\u0161\u203a\u0153\u009d\u017e\u0178"

    /**
     * The first online books were saved with UTF-8 dashes and quotes read as Windows-1252, so "—" arrived
     * as "â€”". Turn the bytes back into text; anything that does not look like that is left alone.
     */
    fun repair(s: String): String {
        if (!s.contains("\u00e2\u20ac")) return s
        val bytes = ByteArray(s.length)
        for ((i, c) in s.withIndex()) {
            val b = when {
                c.code < 0x80 || c.code in 0xa0..0xff -> c.code
                CP1252_HIGH.indexOf(c) >= 0 -> 0x80 + CP1252_HIGH.indexOf(c)
                else -> return s
            }
            bytes[i] = b.toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    /** Narration made from the damaged text is stale: drop it once so it is read again from the repaired text. */
    private fun dropStaleAudioOnce() {
        val prefs = App.context.getSharedPreferences("murmur", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("fix_text_v1", false)) return
        File(App.context.filesDir, "audio").listFiles()?.filter { it.name.startsWith("bk_") }?.forEach { it.deleteRecursively() }
        prefs.edit().putBoolean("fix_text_v1", true).apply()
    }

    private fun story(o: JSONObject): Story {
        val credit = o.optString("credit")
        val plates = o.optJSONArray("plates")?.let { a ->
            (0 until a.length()).map { k ->
                val p = a.getJSONObject(k)
                val c = p.optJSONArray("crop")
                Plate(
                    asset = p.getString("file"),
                    credit = p.optString("credit").ifBlank { credit },
                    l = c?.optDouble(0, 0.0)?.toFloat() ?: 0f,
                    t = c?.optDouble(1, 0.0)?.toFloat() ?: 0f,
                    r = c?.optDouble(2, 1.0)?.toFloat() ?: 1f,
                    b = c?.optDouble(3, 1.0)?.toFloat() ?: 1f,
                )
            }
        } ?: emptyList()
        val moods = o.optJSONArray("moods")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: setOf("calm")
        return Story(
            id = "bk_" + o.getString("id"),
            title = repair(o.getString("title")),
            author = repair(o.getString("author")),
            category = o.optString("category").ifBlank { Categories.FABLES },
            moods = moods,
            summary = repair(o.optString("summary")),
            body = repair(o.getString("body")),
            rights = o.optString("rights").ifBlank { Rights.PUBLIC_DOMAIN },
            motif = runCatching { Motif.valueOf(o.getString("motif")) }.getOrNull(),
            scene = runCatching { AmbienceKind.valueOf(o.getString("scene")) }.getOrNull(),
            plates = plates,
        )
    }
}
