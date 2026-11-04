package app.murmur.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.murmur.App
import app.murmur.audio.AmbienceKind
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
    const val BASE = "https://github.com/mahir-hs/murmur/releases/download/content/"

    var status by mutableStateOf("")
        private set

    private val catalogFile get() = File(App.context.filesDir, "catalog.json")
    private val plateDir get() = File(App.context.filesDir, "plates").apply { mkdirs() }

    /** Loads the saved catalog right away, then tries the network. Safe to call more than once. */
    fun refresh() {
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
            title = o.getString("title"),
            author = o.getString("author"),
            category = o.optString("category").ifBlank { Categories.FABLES },
            moods = moods,
            summary = o.optString("summary"),
            body = o.getString("body"),
            rights = o.optString("rights").ifBlank { Rights.PUBLIC_DOMAIN },
            motif = runCatching { Motif.valueOf(o.getString("motif")) }.getOrNull(),
            scene = runCatching { AmbienceKind.valueOf(o.getString("scene")) }.getOrNull(),
            plates = plates,
        )
    }
}
