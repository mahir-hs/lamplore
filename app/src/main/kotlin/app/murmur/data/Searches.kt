package app.murmur.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.murmur.App
import org.json.JSONArray
import org.json.JSONObject

/** One search across everything: your own titles, audiobooks and shows, with exact matches first. */
data class Merged(val mine: List<Item>, val books: List<Item>, val shows: List<Item>) {
    val isEmpty get() = mine.isEmpty() && books.isEmpty() && shows.isEmpty()
}

/**
 * Search that remembers (the idea behind aniyomi's saved searches): recent searches, searches you saved,
 * and a background check that tells you when a saved search has new results.
 */
object Searches {
    val recent = mutableStateListOf<String>()
    val saved = mutableStateListOf<String>()

    /** Number of new results found for a saved search since you last opened it. */
    val fresh = mutableStateMapOf<String, Int>()

    private val knownKeys = HashMap<String, Set<String>>()
    private val prefs get() = App.context.getSharedPreferences("murmur_extra", Context.MODE_PRIVATE)

    fun init() {
        runCatching { JSONArray(prefs.getString("recent_searches", "[]") ?: "[]").let { a -> (0 until a.length()).forEach { recent.add(a.getString(it)) } } }
        runCatching { JSONArray(prefs.getString("saved_searches", "[]") ?: "[]").let { a -> (0 until a.length()).forEach { saved.add(a.getString(it)) } } }
        runCatching {
            val o = JSONObject(prefs.getString("saved_known", "{}") ?: "{}")
            o.keys().forEach { k -> knownKeys[k] = o.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } }
        }
        runCatching {
            val o = JSONObject(prefs.getString("saved_fresh", "{}") ?: "{}")
            o.keys().forEach { k -> fresh[k] = o.getInt(k) }
        }
    }

    private fun save() {
        val known = JSONObject()
        knownKeys.forEach { (k, v) -> known.put(k, JSONArray(v.toList().take(200))) }
        val f = JSONObject()
        fresh.forEach { (k, v) -> f.put(k, v) }
        prefs.edit().putString("recent_searches", JSONArray(recent.toList()).toString()).putString("saved_searches", JSONArray(saved.toList()).toString())
            .putString("saved_known", known.toString()).putString("saved_fresh", f.toString()).apply()
    }

    /** Remember a search that found something (not while incognito). */
    fun addRecent(q: String) {
        val s = q.trim()
        if (s.length < 2 || App.incognito) return
        recent.remove(s)
        recent.add(0, s)
        while (recent.size > 12) recent.removeAt(recent.size - 1)
        save()
    }

    fun clearRecent() { recent.clear(); save() }

    fun isSaved(q: String) = saved.any { it.equals(q.trim(), true) }

    fun toggleSaved(q: String) {
        val s = q.trim()
        if (isSaved(s)) { saved.removeAll { it.equals(s, true) }; knownKeys.remove(s); fresh.remove(s) } else if (s.length >= 2) saved.add(0, s)
        save()
        Subs.schedule(App.context)
    }

    /** The user opened a saved search: its new results are now seen. */
    suspend fun opened(q: String, results: List<Item>) {
        if (!isSaved(q)) return
        knownKeys[q] = results.map { it.key }.toSet() + (knownKeys[q] ?: emptySet())
        fresh.remove(q)
        save()
    }

    /** Titles worth suggesting while typing: from your own lists and history. */
    fun suggest(q: String): List<String> {
        val s = q.trim()
        if (s.length < 2) return emptyList()
        val pool = Listen.starred.map { it.title } + Listen.starred.map { it.author } + Downloads.saved.map { it.title } +
            History.items.map { it.title } + History.items.map { it.author } + recent + saved
        return pool.filter { it.isNotBlank() && it.contains(s, true) && !it.equals(s, true) }.distinct().take(5)
    }

    /** Background check: how many new audiobooks match each saved search? Returns the searches that found something new. */
    suspend fun checkSaved(): List<String> {
        val found = mutableListOf<String>()
        for (q in saved.toList()) {
            val keys = Listen.searchBooks(q).map { it.key }.toSet()
            if (keys.isEmpty()) continue
            val known = knownKeys[q]
            if (known == null) { knownKeys[q] = keys; continue }
            val new = keys - known
            if (new.isNotEmpty()) { fresh[q] = (fresh[q] ?: 0) + new.size; knownKeys[q] = known + keys; found += q }
        }
        save()
        return found
    }
}
