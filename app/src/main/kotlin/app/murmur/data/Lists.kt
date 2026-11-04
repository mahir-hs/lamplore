package app.murmur.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.murmur.App
import org.json.JSONArray
import org.json.JSONObject

/** A saved way of looking at My list: what to show, in what order, grouped how. */
data class SavedView(val name: String, val filter: String, val sort: String, val group: String, val list: String)

/**
 * Your own lists (Sleep, Commute...) over the books and shows you have starred, plus saved views of My
 * list. Adding something to a list also stars it, so it always has a place in My list.
 */
object Lists {
    val names = mutableStateListOf<String>()
    private val members = mutableStateMapOf<String, List<String>>()
    val views = mutableStateListOf<SavedView>()

    private val prefs get() = App.context.getSharedPreferences("murmur_extra", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val o = JSONObject(prefs.getString("lists", "{}") ?: "{}")
            o.keys().forEach { k -> names.add(k); members[k] = o.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) } } }
        }
        runCatching {
            val a = JSONArray(prefs.getString("views", "[]") ?: "[]")
            for (i in 0 until a.length()) a.getJSONObject(i).let { views.add(SavedView(it.getString("n"), it.getString("f"), it.getString("s"), it.getString("g"), it.optString("l"))) }
        }
    }

    private fun save() {
        val o = JSONObject()
        names.forEach { o.put(it, JSONArray(members[it] ?: emptyList<String>())) }
        val a = JSONArray()
        views.forEach { a.put(JSONObject().put("n", it.name).put("f", it.filter).put("s", it.sort).put("g", it.group).put("l", it.list)) }
        prefs.edit().putString("lists", o.toString()).putString("views", a.toString()).apply()
    }

    fun create(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || n in names) return false
        names.add(n)
        members[n] = emptyList()
        save()
        return true
    }

    fun delete(name: String) { names.remove(name); members.remove(name); views.removeAll { it.list == name }; save() }

    fun keys(name: String): List<String> = members[name] ?: emptyList()
    fun isIn(name: String, key: String) = key in keys(name)

    fun toggle(name: String, item: Item) {
        val cur = keys(name)
        if (item.key in cur) members[name] = cur - item.key
        else {
            members[name] = listOf(item.key) + cur
            if (!Listen.isStarred(item.key)) Listen.toggleStar(item)
        }
        save()
    }

    /** Called when an item is removed from My list altogether: it leaves every list. */
    fun forget(key: String) {
        var changed = false
        names.toList().forEach { n -> if (key in keys(n)) { members[n] = keys(n) - key; changed = true } }
        if (changed) save()
    }

    fun saveView(v: SavedView) { views.removeAll { it.name == v.name }; views.add(v); save() }
    fun deleteView(name: String) { views.removeAll { it.name == name }; save() }
}
