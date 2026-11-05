package app.lamplore.data

import android.content.Context
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import app.lamplore.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * One thing to browse: an audiobook / radio item (ref = Internet Archive id) or a podcast show
 * (ref = feed link, isShow = true). Chapters and episodes are fetched only when it is opened.
 */
data class Item(val key: String, val title: String, val author: String, val art: String, val ref: String, val isShow: Boolean)

/**
 * Free recorded audio that is streamed, never copied or re-hosted:
 *  - Audiobooks: LibriVox (public domain, read by volunteers) and old-time radio, both hosted by the
 *    Internet Archive, whose search and file-list APIs need no key.
 *  - Podcasts: Apple's free iTunes charts and Search API (no key); episodes come from the show's own
 *    RSS feed, the same way every podcast app works.
 * Anything played is remembered (title, links, progress) so it can be resumed from the Listen tab.
 */
object Listen {
    private const val UA = "Lamplore/0.5 (+https://github.com/mahir-hs/lamplore)"

    private const val AUDIO = "mediatype:audio AND (language:English OR language:eng)"
    const val LIBRIVOX = "collection:librivoxaudio AND $AUDIO"
    const val RADIO = "collection:oldtimeradio AND mediatype:audio AND -identifier:lumedwards"

    /** Recently played streams, newest first. Saved so they can be resumed later. */
    val recent = mutableStateListOf<Story>()

    private val prefs get() = App.context.getSharedPreferences("murmur_listen", Context.MODE_PRIVATE)
    private val cache = HashMap<String, List<Item>>()
    private val bookCache = HashMap<String, Story>()

    /** Audiobooks and shows the listener has starred: "My list". */
    val starred = mutableStateListOf<Item>()

    fun isStarred(key: String) = starred.any { it.key == key }

    fun toggleStar(item: Item) {
        if (isStarred(item.key)) { starred.removeAll { it.key == item.key }; Lists.forget(item.key) } else starred.add(0, item)
        val a = JSONArray()
        starred.forEach { a.put(JSONObject().put("k", it.key).put("t", it.title).put("a", it.author).put("i", it.art).put("r", it.ref).put("s", it.isShow)) }
        prefs.edit().putString("starred", a.toString()).apply()
    }

    /** A story that was played or downloaded, as something to browse. */
    fun itemOf(s: Story) = Item(s.id, s.title, s.author, s.art, if (s.id.startsWith("ia_")) s.id.removePrefix("ia_") else s.id, false)

    fun init() {
        runCatching { recent.addAll(decode(prefs.getString("recent", "[]") ?: "[]")) }
        runCatching {
            val a = JSONArray(prefs.getString("starred", "[]") ?: "[]")
            for (i in 0 until a.length()) a.getJSONObject(i).let { starred.add(Item(it.getString("k"), it.getString("t"), it.getString("a"), it.getString("i"), it.getString("r"), it.getBoolean("s"))) }
        }
    }

    fun decode(json: String): List<Story> {
        val a = JSONArray(json)
        return (0 until a.length()).map { storyFromJson(a.getJSONObject(it)) }
    }

    fun encode(list: List<Story>): String {
        val a = JSONArray()
        list.forEach { a.put(storyToJson(it)) }
        return a.toString()
    }

    fun storyFromJson(o: JSONObject): Story {
        val t = o.getJSONArray("tracks")
        return stream(
            o.getString("id"), o.getString("title"), o.getString("author"), o.getString("category"), o.optString("summary"),
            (0 until t.length()).map { k -> t.getJSONObject(k).let { Track(it.getString("t"), it.getString("u"), it.getInt("s")) } },
            o.optString("art"),
            o.optJSONArray("tags")?.let { g -> (0 until g.length()).map { g.getString(it) } } ?: emptyList(),
            o.optString("transcript"),
        )
    }

    fun storyToJson(r: Story): JSONObject =
        JSONObject().put("id", r.id).put("title", r.title).put("author", r.author)
            .put("category", r.category).put("summary", r.summary).put("art", r.art).put("tags", JSONArray(r.tags)).put("transcript", r.transcript)
            .put("tracks", JSONArray().also { ta -> r.tracks.forEach { ta.put(JSONObject().put("t", it.title).put("u", it.url).put("s", it.seconds)) } })

    /** Find a story by id: a built-in one, something played or downloaded, or an audiobook to fetch again. */
    suspend fun find(id: String, title: String, author: String, art: String): Story? {
        App.stories.firstOrNull { it.id == id }?.let { return it }
        recent.firstOrNull { it.id == id }?.let { return it }
        Downloads.saved.firstOrNull { it.id == id }?.let { return it }
        if (id.startsWith("ia_")) return book(Item(id, title, author, art, id.removePrefix("ia_"), false))
        return null
    }

    /** Remember a stream that was just started (moves it to the top). */
    fun remember(s: Story) {
        if (!s.isStream || App.incognito) return
        recent.removeAll { it.id == s.id }
        recent.add(0, s)
        while (recent.size > 30) recent.removeAt(recent.size - 1)
        persist()
    }

    fun forget(id: String) {
        recent.removeAll { it.id == id }
        persist()
    }

    private fun persist() {
        prefs.edit().putString("recent", encode(recent)).apply()
    }

    fun stream(id: String, title: String, author: String, category: String, summary: String, tracks: List<Track>, art: String = "", tags: List<String> = emptyList(), transcript: String = "") = Story(
        id = id, title = title, author = author, category = category, moods = setOf("calm"),
        summary = summary, body = "",
        rights = if (category == Categories.PODCASTS) "Streamed from the publisher's feed" else "Free recording hosted by the Internet Archive (LibriVox is public domain)",
        tracks = tracks, art = art, tags = tags, transcript = transcript,
    )

    // ---- Browsing ------------------------------------------------------------------------------

    /** Items from the Internet Archive (it hosts all of LibriVox, about 22,000 free audiobooks), most popular first. */
    suspend fun archive(query: String, rows: Int = 24, page: Int = 1, sort: String = "downloads desc"): List<Item> = cached("a|$query|$rows|$page|$sort") {
        val u = "https://archive.org/advancedsearch.php?q=${enc(query)}&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator" +
            "&sort%5B%5D=${enc(sort)}&rows=$rows&page=$page&output=json"
        val docs = JSONObject(get(u)).getJSONObject("response").getJSONArray("docs")
        (0 until docs.length()).map {
            val d = docs.getJSONObject(it)
            val id = d.getString("identifier")
            val creator = d.optJSONArray("creator")?.optString(0) ?: d.optString("creator")
            Item("ia_$id", d.optString("title").ifBlank { id }.trim(), if (creator == "null") "" else creator, "https://archive.org/services/img/$id", id, false)
        }
    }

    /** Search over audiobooks and old-time radio by title, author or subject. */
    suspend fun searchBooks(text: String): List<Item> {
        val terms = text.replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(5)
        if (terms.isEmpty()) return emptyList()
        val f = terms.joinToString(" AND ") { "(title:$it* OR creator:$it* OR subject:$it*)" }
        return archive("(collection:librivoxaudio OR collection:oldtimeradio) AND $AUDIO AND $f", rows = 40)
    }

    /** One search across your own titles, audiobooks (and old-time radio) and shows, exact matches first. */
    suspend fun searchAll(text: String): Merged = kotlinx.coroutines.coroutineScope {
        val books = async { searchBooks(text) }
        val shows = async { searchShows(text, 15) }
        val q = text.trim()
        val mine = (starred.toList() + Downloads.saved.map { itemOf(it) } + Subs.shows.map { it.item })
            .distinctBy { it.key }.filter { it.title.contains(q, true) || it.author.contains(q, true) }
        fun rank(i: Item) = when {
            i.title.equals(q, true) -> 0
            i.title.startsWith(q, true) -> 1
            i.title.contains(q, true) -> 2
            else -> 3
        }
        Merged(mine.sortedBy(::rank), books.await().sortedBy(::rank), shows.await().sortedBy(::rank))
    }

    /** "More like this": books by the same author or on the same subjects, most popular first. */
    suspend fun similar(item: Item, tags: List<String>): List<Item> {
        val parts = tags.take(2).map { "subject:\"$it\"" } + if (item.author.isNotBlank() && item.author != "Unknown") listOf("creator:\"${item.author.replace("\"", "")}\"") else emptyList()
        if (parts.isEmpty()) return emptyList()
        return archive("$LIBRIVOX AND (${parts.joinToString(" OR ")}) AND -identifier:${item.ref}", rows = 14)
    }

    /** Apple's top podcasts in one genre, with feed links. */
    suspend fun topShows(genre: Int, limit: Int = 25): List<Item> = cached("t|$genre|$limit") {
        val feed = JSONObject(get("https://itunes.apple.com/us/rss/toppodcasts/limit=$limit/genre=$genre/json")).getJSONObject("feed")
        val entries = feed.optJSONArray("entry") ?: return@cached emptyList()
        val ids = (0 until entries.length()).joinToString(",") {
            entries.getJSONObject(it).getJSONObject("id").getJSONObject("attributes").getString("im:id")
        }
        shows(JSONObject(get("https://itunes.apple.com/lookup?entity=podcast&id=$ids")))
    }

    suspend fun searchShows(query: String, limit: Int = 30): List<Item> = cached("s|$query|$limit") {
        shows(JSONObject(get("https://itunes.apple.com/search?media=podcast&limit=$limit&term=${enc(query.trim())}")))
    }

    private fun shows(json: JSONObject): List<Item> {
        val r = json.optJSONArray("results") ?: return emptyList()
        return (0 until r.length()).mapNotNull {
            val o = r.getJSONObject(it)
            val feed = o.optString("feedUrl")
            if (feed.isBlank()) null
            else Item(
                "pc_${feed.hashCode().toUInt()}", o.optString("collectionName"), o.optString("artistName"),
                o.optString("artworkUrl600").ifBlank { o.optString("artworkUrl100") }, feed, true,
            )
        }
    }

    private suspend fun cached(key: String, load: () -> List<Item>): List<Item> = withContext(Dispatchers.IO) {
        synchronized(cache) { cache[key] }?.let { return@withContext it }
        val r = runCatching(load).getOrDefault(emptyList())
        if (r.isNotEmpty()) synchronized(cache) { cache[key] = r }
        r
    }

    // ---- One audiobook -------------------------------------------------------------------------

    /** Full book (all chapters as tracks) from the Internet Archive item's file list. */
    suspend fun book(item: Item): Story? = withContext(Dispatchers.IO) {
        synchronized(bookCache) { bookCache[item.ref] }?.let { return@withContext it }
        // already downloaded or played: no need for the internet to show its chapters
        Downloads.saved.firstOrNull { it.id == item.key }?.let { return@withContext it }
        recent.firstOrNull { it.id == item.key }?.let { return@withContext it }
        runCatching {
            val j = JSONObject(get("https://archive.org/metadata/${item.ref}"))
            val files = j.getJSONArray("files")
            val all = (0 until files.length()).map { files.getJSONObject(it) }
            val pick = listOf("64Kbps MP3", "VBR MP3", "128Kbps MP3", "MP3").firstNotNullOfOrNull { fmt ->
                all.filter { it.optString("format") == fmt }.takeIf { it.isNotEmpty() }
            } ?: all.filter { it.optString("name").endsWith(".mp3", true) }
            val tracks = pick.sortedBy { it.optString("name") }.mapIndexed { i, f ->
                val name = f.getString("name")
                val len = f.optString("length")
                Track(
                    f.optString("title").ifBlank { name.substringBeforeLast('.') }.ifBlank { "Part ${i + 1}" },
                    "https://archive.org/download/${item.ref}/${android.net.Uri.encode(name)}",
                    if (':' in len) duration(len) else len.toDoubleOrNull()?.toInt() ?: 0,
                )
            }
            if (tracks.isEmpty()) return@runCatching null
            val m = j.optJSONObject("metadata")
            val desc = m?.opt("description")?.let { if (it is JSONArray) it.optString(0) else it.toString() }.orEmpty()
                .replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
            val radio = m?.opt("collection").toString().contains("oldtimeradio")
            val subjects = m?.opt("subject").let { sj ->
                when (sj) {
                    is JSONArray -> (0 until sj.length()).map { sj.getString(it) }
                    is String -> sj.split(';', ',')
                    else -> emptyList()
                }
            }
            val tags = subjects.mapNotNull { cleanTag(it) }.distinct().take(8)
            stream(item.key, item.title, item.author.ifBlank { "Unknown" }, if (radio) "Old-time radio" else Categories.AUDIOBOOKS, desc, tracks, item.art, tags)
                .also { synchronized(bookCache) { bookCache[item.ref] = it } }
        }.getOrNull()
    }

    // ---- Podcast episodes ----------------------------------------------------------------------

    /** Latest episodes of a show, read from its RSS feed. */
    suspend fun episodes(show: Item, max: Int = 40): List<Story> = withContext(Dispatchers.IO) {
        runCatching {
            open(show.ref).inputStream.use { ins ->
                val p = Xml.newPullParser()
                p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                p.setInput(ins, null)
                val out = mutableListOf<Story>()
                var inItem = false
                var title = ""; var url = ""; var dur = 0; var desc = ""
                val trans = mutableListOf<Pair<String, String>>()
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT && out.size < max) {
                    when (ev) {
                        XmlPullParser.START_TAG -> when (p.name) {
                            "item" -> { inItem = true; title = ""; url = ""; dur = 0; desc = ""; trans.clear() }
                            "podcast:transcript" -> if (inItem) trans += p.getAttributeValue(null, "url").orEmpty() to p.getAttributeValue(null, "type").orEmpty()
                            "title" -> if (inItem) title = p.nextText().trim()
                            "enclosure" -> if (inItem) url = p.getAttributeValue(null, "url").orEmpty()
                            "itunes:duration" -> if (inItem) dur = duration(p.nextText())
                            "description" -> if (inItem) desc = p.nextText().replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
                        }
                        XmlPullParser.END_TAG -> if (p.name == "item") {
                            inItem = false
                            if (url.isNotBlank() && title.isNotBlank()) {
                                out += stream(
                                    "pe_${(show.ref + url).hashCode().toUInt()}", title, show.title, Categories.PODCASTS, desc.take(160),
                                    listOf(Track(title, url.replace("http://", "https://"), dur)), show.art, emptyList(), Transcripts.choose(trans),
                                )
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

    /** Opens [url], following redirects (also http to https). [from] > 0 asks the server to resume at that byte. */
    fun open(url: String, from: Long = 0L): HttpURLConnection {
        var u = url
        repeat(5) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", UA)
            if (from > 0) c.setRequestProperty("Range", "bytes=$from-")
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
