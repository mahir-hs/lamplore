package app.murmur.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import app.murmur.App
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Where a title stands: partway, done, or put aside. */
enum class Status(val label: String) { IN_PROGRESS("In progress"), FINISHED("Finished"), DROPPED("Dropped") }

/** One title you have listened to. Removing it from History only hides it; the journal keeps the time. */
data class HistoryItem(
    val id: String,
    val title: String,
    val author: String,
    val topic: String,
    val art: String,
    val tags: List<String>,
    val firstAt: Long,
    val lastAt: Long,
    val seconds: Int,
    /** Number of different days this was listened on (more than one means re-listened). */
    val days: Int,
    val hidden: Boolean,
    val dropped: Boolean,
)

/**
 * The history list, one entry per title. It follows aniyomi's rule: Remove and Clear only hide an entry
 * (Restore brings it back), and "seen" marks and your Activity do not depend on what is hidden. Only
 * [deleteEverything] really erases. Nothing is recorded while incognito is on.
 */
object History {
    val items = mutableStateListOf<HistoryItem>()

    private val prefs get() = App.context.getSharedPreferences("murmur_history", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val a = JSONArray(prefs.getString("items", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val t = o.optJSONArray("tags")
                items.add(
                    HistoryItem(
                        o.getString("id"), o.getString("title"), o.optString("author"), o.optString("topic"), o.optString("art"),
                        if (t == null) emptyList() else (0 until t.length()).map { t.getString(it) },
                        o.getLong("first"), o.getLong("last"), o.getInt("sec"), o.optInt("days", 1), o.optBoolean("hidden"), o.optBoolean("dropped"),
                    ),
                )
            }
        }
    }

    private fun save() {
        val a = JSONArray()
        items.forEach {
            a.put(
                JSONObject().put("id", it.id).put("title", it.title).put("author", it.author).put("topic", it.topic).put("art", it.art)
                    .put("tags", JSONArray(it.tags)).put("first", it.firstAt).put("last", it.lastAt).put("sec", it.seconds)
                    .put("days", it.days).put("hidden", it.hidden).put("dropped", it.dropped),
            )
        }
        prefs.edit().putString("items", a.toString()).apply()
    }

    private fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Add [seconds] of listening to a title, creating its entry the first time. Does nothing while incognito. */
    fun record(story: Story, seconds: Int) {
        if (App.incognito) return
        val now = System.currentTimeMillis()
        val i = items.indexOfFirst { it.id == story.id }
        if (i < 0) {
            items.add(
                0,
                HistoryItem(story.id, story.title, story.author, Activity.topicOf(story), story.art, story.tags, now, now, seconds, 1, hidden = false, dropped = false),
            )
        } else {
            val old = items[i]
            val newDay = day(old.lastAt) != day(now)
            // listening again brings a hidden entry back to the top
            items.removeAt(i)
            items.add(
                0,
                old.copy(
                    lastAt = now, seconds = old.seconds + seconds, days = old.days + if (newDay) 1 else 0, hidden = false,
                    tags = if (story.tags.isNotEmpty()) story.tags else old.tags, art = story.art.ifBlank { old.art },
                ),
            )
        }
        // persisting every ten seconds is cheap (a short JSON string)
        save()
    }

    fun status(h: HistoryItem): Status = when {
        h.dropped -> Status.DROPPED
        h.id in App.completed -> Status.FINISHED
        else -> Status.IN_PROGRESS
    }

    /** Has this title ever been listened to, even if its history entry is hidden? */
    fun seen(id: String): Boolean = !App.incognito && items.any { it.id == id }

    fun visible(): List<HistoryItem> = items.filter { !it.hidden }.sortedByDescending { it.lastAt }
    fun hiddenCount(): Int = items.count { it.hidden }

    private fun update(id: String, f: (HistoryItem) -> HistoryItem) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) { items[i] = f(items[i]); save() }
    }

    fun hide(id: String) = update(id) { it.copy(hidden = true) }
    fun hideAll() { items.indices.forEach { items[it] = items[it].copy(hidden = true) }; save() }
    fun restoreAll() { items.indices.forEach { items[it] = items[it].copy(hidden = false) }; save() }
    fun setDropped(id: String, dropped: Boolean) = update(id) { it.copy(dropped = dropped) }

    /** The only action that really erases: history, activity and seen marks. */
    fun deleteEverything() {
        items.clear()
        prefs.edit().clear().apply()
        Activity.clearAll()
    }
}
