package app.murmur.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One JSON file with everything that is yours: settings, stars, progress, saved stories, notes, history,
 * the activity journal and the list of downloads (the audio files themselves are not included). Restore
 * replaces what is on the phone with what is in the file, so restoring twice changes nothing and never
 * counts a listen twice. Automatic backups go to the app's folder and the newest five are kept.
 */
object Backup {
    /** The SharedPreferences files that make up the backup. */
    private val files = listOf("murmur", "murmur_listen", "murmur_activity", "murmur_history", "murmur_downloads", "murmur_subs", "murmur_extra")

    private const val VERSION = 1

    fun toJson(ctx: Context): String {
        val root = JSONObject().put("app", "murmur").put("version", VERSION).put("createdAt", System.currentTimeMillis())
        val all = JSONObject()
        for (name in files) {
            val p = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
            val o = JSONObject()
            for ((k, v) in p.all) {
                when (v) {
                    is String -> o.put(k, JSONObject().put("t", "s").put("v", v))
                    is Int -> o.put(k, JSONObject().put("t", "i").put("v", v))
                    is Long -> o.put(k, JSONObject().put("t", "l").put("v", v))
                    is Float -> o.put(k, JSONObject().put("t", "f").put("v", v.toDouble()))
                    is Boolean -> o.put(k, JSONObject().put("t", "b").put("v", v))
                    is Set<*> -> o.put(k, JSONObject().put("t", "ss").put("v", JSONArray(v.map { it.toString() })))
                }
            }
            all.put(name, o)
        }
        return root.put("prefs", all).toString()
    }

    /** Replace the phone's data with the backup. Returns false (and changes nothing) if the file is not a Murmur backup. */
    fun restore(ctx: Context, json: String): Boolean {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return false
        if (root.optString("app") != "murmur") return false
        val all = root.optJSONObject("prefs") ?: return false
        for (name in files) {
            val o = all.optJSONObject(name) ?: continue
            val e = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            for (k in o.keys()) {
                val x = o.getJSONObject(k)
                when (x.getString("t")) {
                    "s" -> e.putString(k, x.getString("v"))
                    "i" -> e.putInt(k, x.getInt("v"))
                    "l" -> e.putLong(k, x.getLong("v"))
                    "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                    "b" -> e.putBoolean(k, x.getBoolean("v"))
                    "ss" -> e.putStringSet(k, x.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
                }
            }
            e.commit()
        }
        return true
    }

    fun fileName(): String = "murmur-backup-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".json"

    // ---- automatic backups ---------------------------------------------------------------------------

    fun dir(ctx: Context): File = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "backups").apply { mkdirs() }

    fun list(ctx: Context): List<File> = dir(ctx).listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()

    private fun settings(ctx: Context) = ctx.getSharedPreferences("murmur_extra", Context.MODE_PRIVATE)

    /** 0 = off, otherwise every this many days. */
    var intervalDays: Int = 0
        private set

    fun loadSettings(ctx: Context) { intervalDays = settings(ctx).getInt("backup_days", 0) }

    fun setInterval(ctx: Context, days: Int) {
        intervalDays = days
        settings(ctx).edit().putInt("backup_days", days).apply()
    }

    fun lastAutoAt(ctx: Context): Long = settings(ctx).getLong("backup_last", 0L)

    /** Write a backup now into the app folder and trim old ones. */
    fun writeAuto(ctx: Context): File {
        val f = File(dir(ctx), fileName())
        f.writeText(toJson(ctx))
        settings(ctx).edit().putLong("backup_last", System.currentTimeMillis()).apply()
        list(ctx).drop(5).forEach { it.delete() }
        return f
    }

    /** Call at app start: makes a backup when the chosen interval has passed. */
    fun autoIfDue(ctx: Context) {
        loadSettings(ctx)
        if (intervalDays <= 0) return
        val due = lastAutoAt(ctx) + intervalDays * 24L * 3600_000L
        if (System.currentTimeMillis() >= due) runCatching { writeAuto(ctx) }
    }
}
