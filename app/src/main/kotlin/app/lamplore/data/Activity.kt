package app.lamplore.data

import android.content.Context
import app.lamplore.App
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/** Words that describe the recording, not the book: left out of "your taste". */
private val noiseTags = setOf(
    "librivox", "audiobook", "audiobooks", "audio book", "public domain", "books", "book", "fiction", "english", "free", "audio",
    "spoken word", "recording", "literature", "stories", "short stories", "novel", "novels", "classic", "classics", "podcast", "podcasts",
)

/** A tag worth counting: short, readable, not recording jargon. */
fun cleanTag(t: String): String? {
    val s = t.trim().lowercase()
    if (s.length < 3 || s.length > 28 || s in noiseTags || s.startsWith("librivox") || s.any { it.isDigit() }) return null
    return s.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

/**
 * The permanent journal: seconds listened per day, per topic, per genre tag, per author and per title,
 * and per hour of the day. Everything stays on the phone. Written every ten seconds of listening.
 * Hiding things in History never changes it; [clearAll] is the only way it is erased.
 */
object Activity {
    private val prefs get() = App.context.getSharedPreferences("murmur_activity", Context.MODE_PRIVATE)

    /** date -> key -> seconds */
    private val topics = HashMap<String, HashMap<String, Int>>()
    private val tags = HashMap<String, HashMap<String, Int>>()
    private val authors = HashMap<String, HashMap<String, Int>>()

    /** date -> story id -> (title, topic, seconds) */
    private val stories = HashMap<String, HashMap<String, Triple<String, String, Int>>>()

    /** seconds listened in each hour of the day, all time */
    private val hours = IntArray(24)

    private var loaded = false

    private fun readMap(json: String?): HashMap<String, HashMap<String, Int>> {
        val out = HashMap<String, HashMap<String, Int>>()
        runCatching {
            val t = JSONObject(json ?: "{}")
            t.keys().forEach { d ->
                val o = t.getJSONObject(d)
                out[d] = HashMap<String, Int>().also { m -> o.keys().forEach { k -> m[k] = o.getInt(k) } }
            }
        }
        return out
    }

    private fun writeMap(m: Map<String, Map<String, Int>>): String {
        val t = JSONObject()
        m.forEach { (d, inner) -> t.put(d, JSONObject().also { o -> inner.forEach { (k, v) -> o.put(k, v) } }) }
        return t.toString()
    }

    private fun load() {
        if (loaded) return
        loaded = true
        topics.putAll(readMap(prefs.getString("topics", null)))
        tags.putAll(readMap(prefs.getString("tags", null)))
        authors.putAll(readMap(prefs.getString("authors", null)))
        runCatching {
            val s = JSONObject(prefs.getString("stories", "{}") ?: "{}")
            s.keys().forEach { d ->
                val o = s.getJSONObject(d)
                stories[d] = HashMap<String, Triple<String, String, Int>>().also { m ->
                    o.keys().forEach { id -> o.getJSONArray(id).let { a -> m[id] = Triple(a.getString(0), a.getString(1), a.getInt(2)) } }
                }
            }
        }
        runCatching {
            val a = org.json.JSONArray(prefs.getString("hours", "[]") ?: "[]")
            for (i in 0 until minOf(24, a.length())) hours[i] = a.getInt(i)
        }
    }

    fun record(story: Story, seconds: Int) {
        if (App.incognito) return
        load()
        val d = LocalDate.now().toString()
        val topic = topicOf(story)
        topics.getOrPut(d) { HashMap() }.merge(topic, seconds, Int::plus)
        stories.getOrPut(d) { HashMap() }.merge(story.id, Triple(story.title, topic, seconds)) { a, _ -> Triple(a.first, a.second, a.third + seconds) }
        if (story.author.isNotBlank()) authors.getOrPut(d) { HashMap() }.merge(story.author.replace(", retold", ""), seconds, Int::plus)
        (story.tags + if (story.isStream) emptyList() else story.moods.toList()).mapNotNull { cleanTag(it) }.distinct().forEach { t -> tags.getOrPut(d) { HashMap() }.merge(t, seconds, Int::plus) }
        hours[LocalTime.now().hour] += seconds
        save()
    }

    fun topicOf(s: Story): String = s.category

    private fun save() {
        // keep about two years
        val cutoff = LocalDate.now().minusDays(730).toString()
        listOf(topics, tags, authors).forEach { m -> m.keys.removeAll { it < cutoff } }
        stories.keys.removeAll { it < cutoff }
        val s = JSONObject()
        stories.forEach { (d, m) ->
            s.put(d, JSONObject().also { o -> m.forEach { (id, v) -> o.put(id, org.json.JSONArray().put(v.first).put(v.second).put(v.third)) } })
        }
        prefs.edit()
            .putString("topics", writeMap(topics)).putString("tags", writeMap(tags)).putString("authors", writeMap(authors))
            .putString("stories", s.toString()).putString("hours", org.json.JSONArray(hours.toList()).toString())
            .apply()
    }

    fun clearAll() {
        topics.clear(); tags.clear(); authors.clear(); stories.clear(); hours.fill(0)
        loaded = true
        prefs.edit().clear().apply()
    }

    private fun from(days: Int): String = LocalDate.now().minusDays(days - 1L).toString()

    /** Totals per key over a window of [days] days ending today, the same-length window before it, or all time (days = 0). */
    private fun sum(m: Map<String, Map<String, Int>>, days: Int, previous: Boolean = false): List<Pair<String, Int>> {
        val today = LocalDate.now()
        val lo: String
        val hi: String
        when {
            days <= 0 -> { lo = ""; hi = "9999" }
            !previous -> { lo = today.minusDays(days - 1L).toString(); hi = "9999" }
            else -> { lo = today.minusDays(2L * days - 1).toString(); hi = today.minusDays(days - 1L).toString() }
        }
        val acc = HashMap<String, Int>()
        m.forEach { (d, inner) -> if (d >= lo && d < hi) inner.forEach { (k, v) -> acc.merge(k, v, Int::plus) } }
        return acc.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    /** Topics listened to in the last [days] days (0 = ever), most time first, as (topic, seconds). */
    fun byTopic(days: Int): List<Pair<String, Int>> { load(); return sum(topics, days) }

    /** Genre tags and authors in the last [days] days, the previous [days] days ([previous] = true), or ever (days = 0). */
    fun byTag(days: Int, previous: Boolean = false): List<Pair<String, Int>> { load(); return sum(tags, days, previous) }
    fun byAuthor(days: Int): List<Pair<String, Int>> { load(); return sum(authors, days) }

    /** Titles by time listened in the last [days] days (0 = ever): (id, title, topic, seconds). */
    fun topTitles(days: Int): List<Triple<String, String, Int>> {
        load()
        val lo = if (days <= 0) "" else from(days)
        val acc = HashMap<String, Pair<String, Int>>()
        stories.forEach { (d, m) -> if (d >= lo) m.forEach { (id, v) -> acc.merge(id, v.first to v.third) { a, b -> a.first to a.second + b.second } } }
        return acc.entries.sortedByDescending { it.value.second }.map { Triple(it.key, it.value.first, it.value.second) }
    }

    /** The most one title was listened to in a single day (the "binge"), per title. */
    fun biggestDay(): List<Triple<String, String, Int>> {
        load()
        val best = HashMap<String, Pair<String, Int>>()
        stories.values.forEach { m -> m.forEach { (id, v) -> if (v.third > (best[id]?.second ?: 0)) best[id] = v.first to v.third } }
        return best.entries.sortedByDescending { it.value.second }.map { Triple(it.key, it.value.first, it.value.second) }
    }

    /** Seconds listened in each hour of the day, ever. */
    fun hourSeconds(): IntArray { load(); return hours.copyOf() }

    /** Average minutes listened on each weekday (Monday first) over the last [weeks] weeks. */
    fun weekdayMinutes(weeks: Int = 12): List<Int> {
        val total = IntArray(7)
        var d = LocalDate.now()
        for (i in 0 until weeks * 7) {
            total[d.dayOfWeek.value - 1] += seconds(d)
            d = d.minusDays(1)
        }
        return total.map { it / 60 / weeks }
    }

    /** Seconds listened on a given day: the larger of the all-time day total and what topics recorded. */
    fun seconds(d: LocalDate): Int {
        load()
        return maxOf(App.secondsOn(d), topics[d.toString()]?.values?.sum() ?: 0)
    }

    fun totalSeconds(): Int {
        load()
        return App.allSeconds()
    }

    /** Longest run of days with a minute or more of listening. */
    fun bestStreak(): Int {
        val start = App.firstListenDate() ?: return 0
        var best = 0
        var run = 0
        var d = start
        val today = LocalDate.now()
        while (!d.isAfter(today)) {
            if (seconds(d) >= 60) { run++; if (run > best) best = run } else run = 0
            d = d.plusDays(1)
        }
        return best
    }
}
