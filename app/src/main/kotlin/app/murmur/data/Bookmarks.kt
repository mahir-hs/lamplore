package app.murmur.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import app.murmur.App
import org.json.JSONArray
import org.json.JSONObject

/** A saved moment in a story, book or episode, with an optional note (like aniyomi's saved pages). */
data class Bookmark(
    val id: Long,
    val storyId: String,
    val title: String,
    val author: String,
    val art: String,
    /** Which part: the segment of a story or the chapter of a book. */
    val index: Int,
    val posMs: Long,
    /** What was playing: the chapter's name or the first words of the passage. */
    val label: String,
    val note: String,
    val at: Long,
)

object Bookmarks {
    val items = mutableStateListOf<Bookmark>()

    private val prefs get() = App.context.getSharedPreferences("murmur_extra", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val a = JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                items.add(Bookmark(o.getLong("id"), o.getString("sid"), o.getString("t"), o.optString("a"), o.optString("art"), o.getInt("i"), o.getLong("p"), o.optString("l"), o.optString("n"), o.getLong("at")))
            }
        }
    }

    private fun save() {
        val a = JSONArray()
        items.forEach {
            a.put(JSONObject().put("id", it.id).put("sid", it.storyId).put("t", it.title).put("a", it.author).put("art", it.art)
                .put("i", it.index).put("p", it.posMs).put("l", it.label).put("n", it.note).put("at", it.at))
        }
        prefs.edit().putString("bookmarks", a.toString()).apply()
    }

    fun add(story: Story, index: Int, posMs: Long, label: String, note: String) {
        items.add(0, Bookmark(System.currentTimeMillis(), story.id, story.title, story.author, story.art, index, posMs, label, note.trim(), System.currentTimeMillis()))
        save()
    }

    fun setNote(id: Long, note: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) { items[i] = items[i].copy(note = note.trim()); save() }
    }

    fun remove(id: Long) { items.removeAll { it.id == id }; save() }

    fun forStory(storyId: String) = items.filter { it.storyId == storyId }
}
