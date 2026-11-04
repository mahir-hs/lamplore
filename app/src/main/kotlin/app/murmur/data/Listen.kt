package app.murmur.data

import android.content.Context
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import app.murmur.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A podcast show found by search; its episodes are fetched from the feed when opened. */
data class Show(val name: String, val author: String, val feedUrl: String, val art: String)

/**
 * Free recorded audio that is streamed, never copied or re-hosted:
 *  - LibriVox: public-domain audiobooks read by volunteers (official JSON API, no key).
 *  - Podcasts: found with Apple's free iTunes Search API (no key); episodes come from the show's own
 *    RSS feed, the same way every podcast app works.
 * Anything played is remembered (title, links, progress) so it can be resumed from the Listen tab.
 */
object Listen {
    private const val UA = "Murmur/0.4 (+https://github.com/mahir-hs/murmur)"

    /** Recently played streams, newest first. Saved so they can be resumed later. */
    val recent = mutableStateListOf<Story>()

    private val prefs get() = App.context.getSharedPreferences("murmur_listen", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val a = JSONArray(prefs.getString("recent", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val t = o.getJSONArray("tracks")
                recent.add(
                    stream(
                        o.getString("id"), o.getString("title"), o.getString("author"), o.getString("category"),
                        o.optString("summary"),
                        (0 until t.length()).map { k -> t.getJSONObject(k).let { Track(it.getString("t"), it.getString("u"), it.getInt("s")) } },
                    ),
                )
            }
        }
    }

    /** Remember a stream that was just started (moves it to the top). */
    fun remember(s: Story) {
        if (!s.isStream) return
        recent.removeAll { it.id == s.id }
        recent.add(0, s)
        while (recent.size > 30) recent.removeAt(recent.size - 1)
        persist()
    }

    private fun persist() {
        val a = JSONArray()
        recent.forEach { r ->
            a.put(
                JSONObject().put("id", r.id).put("title", r.title).put("author", r.author)
                    .put("category", r.category).put("summary", r.summary)
                    .put("tracks", JSONArray().also { ta -> r.tracks.forEach { ta.put(JSONObject().put("t", it.title).put("u", it.url).put("s", it.seconds)) } }),
            )
        }
        prefs.edit().putString("recent", a.toString()).apply()
    }

    fun forget(id: String) {
        recent.removeAll { it.id == id }
        persist()
    }

    private fun stream(id: String, title: String, author: String, category: String, summary: String, tracks: List<Track>) = Story(
        id = id, title = title, author = author, category = category, moods = setOf("calm"),
        summary = summary, body = "", rights = if (category == Categories.AUDIOBOOKS) "Public domain, read by LibriVox volunteers" else "Streamed from the publisher's feed",
        tracks = tracks,
    )

    // ---- LibriVox ------------------------------------------------------------------------------

    /** Audiobooks whose title or author starts with [query] (the LibriVox API matches from the start). */
    suspend fun audiobooks(query: String, limit: Int = 30): List<Story> = withContext(Dispatchers.IO) {
        val q = enc("^" + query.trim())
        val seen = LinkedHashMap<String, Story>()
        for (field in listOf("title", "author")) {
            runCatching {
                val json = JSONObject(get("https://librivox.org/api/feed/audiobooks/?format=json&extended=1&limit=$limit&$field=$q"))
                val books = json.optJSONArray("books") ?: return@runCatching
                for (i in 0 until books.length()) book(books.getJSONObject(i))?.let { seen.putIfAbsent(it.id, it) }
            }
        }
        seen.values.toList()
    }

    private fun book(o: JSONObject): Story? {
        val secs = o.optJSONArray("sections") ?: return null
        val tracks = (0 until secs.length()).mapNotNull {
            val s = secs.getJSONObject(it)
            val url = s.optString("listen_url")
            if (url.isBlank()) null else Track(s.optString("title").ifBlank { "Part ${it + 1}" }, url.replace("http://", "https://"), s.optString("playtime").toIntOrNull() ?: 0)
        }
        if (tracks.isEmpty()) return null
        val au = o.optJSONArray("authors")?.let { a ->
            (0 until a.length()).joinToString(", ") { a.getJSONObject(it).let { x -> "${x.optString("first_name")} ${x.optString("last_name")}".trim() } }
        }.orEmpty().ifBlank { "Unknown" }
        val desc = o.optString("description").replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
        return stream("lv_${o.getString("id")}", o.getString("title").trim(), au, Categories.AUDIOBOOKS, desc.take(160), tracks)
    }

    // ---- Podcasts ------------------------------------------------------------------------------

    suspend fun shows(query: String, limit: Int = 25): List<Show> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(get("https://itunes.apple.com/search?media=podcast&limit=$limit&term=${enc(query.trim())}"))
            val r = json.optJSONArray("results") ?: return@runCatching emptyList()
            (0 until r.length()).mapNotNull {
                val o = r.getJSONObject(it)
                val feed = o.optString("feedUrl")
                if (feed.isBlank()) null else Show(o.optString("collectionName"), o.optString("artistName"), feed, o.optString("artworkUrl100"))
            }
        }.getOrDefault(emptyList())
    }

    /** Latest episodes of a show, read from its RSS feed. */
    suspend fun episodes(show: Show, max: Int = 40): List<Story> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open(show.feedUrl)
            conn.inputStream.use { ins ->
                val p = Xml.newPullParser()
                p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                p.setInput(ins, null)
                val out = mutableListOf<Story>()
                var inItem = false
                var title = ""; var url = ""; var dur = 0; var desc = ""
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT && out.size < max) {
                    when (ev) {
                        XmlPullParser.START_TAG -> when (p.name) {
                            "item" -> { inItem = true; title = ""; url = ""; dur = 0; desc = "" }
                            "title" -> if (inItem) title = p.nextText().trim()
                            "enclosure" -> if (inItem) url = p.getAttributeValue(null, "url").orEmpty()
                            "itunes:duration" -> if (inItem) dur = duration(p.nextText())
                            "description" -> if (inItem) desc = p.nextText().replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
                        }
                        XmlPullParser.END_TAG -> if (p.name == "item") {
                            inItem = false
                            if (url.isNotBlank() && title.isNotBlank()) {
                                out += stream("pc_${(show.feedUrl + url).hashCode().toUInt()}", title, show.name, Categories.PODCASTS, desc.take(160), listOf(Track(title, url.replace("http://", "https://"), dur)))
                            }
                        }
                    }
                    ev = p.next()
                }
                out
            }
        }.getOrDefault(emptyList())
    }

    /** "1:02:03", "12:30" or "754" to seconds. */
    private fun duration(s: String): Int {
        val parts = s.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    // ---- http ----------------------------------------------------------------------------------

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun open(url: String): HttpURLConnection {
        var u = url
        repeat(5) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", UA)
            val code = c.responseCode
            if (code in 300..399) {
                val loc = c.getHeaderField("Location") ?: return c
                u = URL(URL(u), loc).toString() // also follows http -> https, which HttpURLConnection will not do itself
                c.disconnect()
            } else return c
        }
        return URL(u).openConnection() as HttpURLConnection
    }

    private fun get(url: String): String = open(url).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
}
