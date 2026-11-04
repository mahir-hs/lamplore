package app.murmur.data

import android.content.Context
import app.murmur.App
import org.json.JSONObject
import java.time.LocalDate

/** One story's listening on one day, for the history list. */
data class HistoryRow(val date: LocalDate, val id: String, val title: String, val topic: String, val seconds: Int)

/**
 * What you listened to and for how long: seconds per day, per topic (a shelf or kind of audio) and per
 * story. Everything stays on the phone. Written every ten seconds of listening, read by the Activity page.
 */
object Activity {
    private val prefs get() = App.context.getSharedPreferences("murmur_activity", Context.MODE_PRIVATE)

    /** date -> topic -> seconds */
    private val topics = HashMap<String, HashMap<String, Int>>()

    /** date -> story id -> (title, topic, seconds) */
    private val stories = HashMap<String, HashMap<String, Triple<String, String, Int>>>()

    private var loaded = false

    private fun load() {
        if (loaded) return
        loaded = true
        runCatching {
            val t = JSONObject(prefs.getString("topics", "{}") ?: "{}")
            t.keys().forEach { d ->
                val o = t.getJSONObject(d)
                topics[d] = HashMap<String, Int>().also { m -> o.keys().forEach { k -> m[k] = o.getInt(k) } }
            }
            val s = JSONObject(prefs.getString("stories", "{}") ?: "{}")
            s.keys().forEach { d ->
                val o = s.getJSONObject(d)
                stories[d] = HashMap<String, Triple<String, String, Int>>().also { m ->
                    o.keys().forEach { id -> o.getJSONArray(id).let { a -> m[id] = Triple(a.getString(0), a.getString(1), a.getInt(2)) } }
                }
            }
        }
    }

    fun record(story: Story, seconds: Int) {
        load()
        val d = LocalDate.now().toString()
        val topic = topicOf(story)
        topics.getOrPut(d) { HashMap() }.merge(topic, seconds, Int::plus)
        stories.getOrPut(d) { HashMap() }.merge(story.id, Triple(story.title, topic, seconds)) { a, _ -> Triple(a.first, a.second, a.third + seconds) }
        save()
    }

    fun topicOf(s: Story): String = s.category

    private fun save() {
        // keep about a year
        val cutoff = LocalDate.now().minusDays(400).toString()
        topics.keys.removeAll { it < cutoff }
        stories.keys.removeAll { it < cutoff }
        val t = JSONObject()
        topics.forEach { (d, m) -> t.put(d, JSONObject().also { o -> m.forEach { (k, v) -> o.put(k, v) } }) }
        val s = JSONObject()
        stories.forEach { (d, m) ->
            s.put(d, JSONObject().also { o -> m.forEach { (id, v) -> o.put(id, org.json.JSONArray().put(v.first).put(v.second).put(v.third)) } })
        }
        prefs.edit().putString("topics", t.toString()).putString("stories", s.toString()).apply()
    }

    /** Topics listened to in the last [days] days (0 = ever), most time first, as (topic, seconds). */
    fun byTopic(days: Int): List<Pair<String, Int>> {
        load()
        val from = if (days <= 0) "" else LocalDate.now().minusDays(days - 1L).toString()
        val sum = HashMap<String, Int>()
        topics.forEach { (d, m) -> if (d >= from) m.forEach { (k, v) -> sum.merge(k, v, Int::plus) } }
        return sum.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    /** Seconds listened on a given day: the larger of the all-time day total and what topics recorded. */
    fun seconds(d: LocalDate): Int {
        load()
        return maxOf(App.secondsOn(d), topics[d.toString()]?.values?.sum() ?: 0)
    }

    /** Listening history, newest day first, then longest first within a day. */
    fun history(limit: Int = 80): List<HistoryRow> {
        load()
        return stories.entries.sortedByDescending { it.key }.flatMap { (d, m) ->
            m.entries.sortedByDescending { it.value.third }.map { (id, v) -> HistoryRow(LocalDate.parse(d), id, v.first, v.second, v.third) }
        }.take(limit)
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
