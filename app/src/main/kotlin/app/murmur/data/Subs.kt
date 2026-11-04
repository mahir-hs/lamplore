package app.murmur.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.murmur.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A show you follow. [known] holds the episodes already seen, so only new ones count as updates. */
data class Sub(val item: Item, val autoDownload: Boolean, val known: Set<String>, val lastCheck: Long)

/** One new episode found by an update check. */
data class UpdateEntry(val story: Story, val showKey: String, val showTitle: String, val foundAt: Long, val read: Boolean)

/**
 * Subscriptions and the Updates feed (like aniyomi's library updates). A background job checks each
 * followed show's own feed every few hours, lists new episodes, can download them for you and posts one
 * quiet notification. Shows that cannot be read are listed with a Retry.
 */
object Subs {
    val shows = mutableStateListOf<Sub>()
    val updates = mutableStateListOf<UpdateEntry>()
    val errors = mutableStateMapOf<String, String>()
    var checking by mutableStateOf(false)
        private set
    var lastRun by mutableStateOf(0L)
        private set

    /** Hours between background checks; 0 = off. */
    var intervalHours by mutableStateOf(12)
        private set

    private val prefs get() = App.context.getSharedPreferences("murmur_subs", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val a = JSONArray(prefs.getString("subs", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val k = o.getJSONArray("known")
                shows.add(
                    Sub(
                        Item(o.getString("key"), o.getString("title"), o.optString("author"), o.optString("art"), o.getString("ref"), true),
                        o.optBoolean("auto"), (0 until k.length()).map { k.getString(it) }.toSet(), o.optLong("last"),
                    ),
                )
            }
        }
        runCatching {
            val a = JSONArray(prefs.getString("updates", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                updates.add(UpdateEntry(Listen.storyFromJson(o.getJSONObject("story")), o.getString("show"), o.getString("showTitle"), o.getLong("at"), o.optBoolean("read")))
            }
        }
        intervalHours = prefs.getInt("interval", 12)
        lastRun = prefs.getLong("lastRun", 0L)
    }

    private fun save() {
        val a = JSONArray()
        shows.forEach {
            a.put(
                JSONObject().put("key", it.item.key).put("title", it.item.title).put("author", it.item.author).put("art", it.item.art)
                    .put("ref", it.item.ref).put("auto", it.autoDownload).put("known", JSONArray(it.known.toList().takeLast(400))).put("last", it.lastCheck),
            )
        }
        val u = JSONArray()
        updates.take(150).forEach {
            u.put(JSONObject().put("story", Listen.storyToJson(it.story)).put("show", it.showKey).put("showTitle", it.showTitle).put("at", it.foundAt).put("read", it.read))
        }
        prefs.edit().putString("subs", a.toString()).putString("updates", u.toString()).putInt("interval", intervalHours).putLong("lastRun", lastRun).apply()
    }

    fun isSubscribed(key: String) = shows.any { it.item.key == key }

    suspend fun subscribe(item: Item) {
        if (isSubscribed(item.key)) return
        // the episodes that exist now are "known", so only future ones show up as updates
        val eps = Listen.episodes(item, 30)
        shows.add(0, Sub(item, false, eps.map { it.id }.toSet(), System.currentTimeMillis()))
        save()
        schedule(App.context)
    }

    fun unsubscribe(key: String) {
        shows.removeAll { it.item.key == key }
        updates.removeAll { it.showKey == key }
        errors.remove(key)
        save()
    }

    fun setAutoDownload(key: String, on: Boolean) {
        val i = shows.indexOfFirst { it.item.key == key }
        if (i >= 0) { shows[i] = shows[i].copy(autoDownload = on); save() }
    }

    fun unread(): Int = updates.count { !it.read }

    fun markAllRead() {
        updates.indices.forEach { updates[it] = updates[it].copy(read = true) }
        save()
    }

    fun markRead(storyId: String) {
        val i = updates.indexOfFirst { it.story.id == storyId && !it.read }
        if (i >= 0) { updates[i] = updates[i].copy(read = true); save() }
    }

    fun clearUpdates() { updates.clear(); save() }

    fun setInterval(ctx: Context, hours: Int) {
        intervalHours = hours
        save()
        schedule(ctx)
    }

    // ---- checking -------------------------------------------------------------------------------------

    /** Look for new episodes in every followed show. Returns how many were found. */
    suspend fun checkAll(ctx: Context, notify: Boolean = true): Int {
        if (checking || (shows.isEmpty() && Searches.saved.isEmpty())) return 0
        checking = true
        var found = 0
        val fresh = mutableListOf<UpdateEntry>()
        try {
            for (sub in shows.toList()) {
                val eps = withContext(Dispatchers.IO) { Listen.episodes(sub.item, 30) }
                if (eps.isEmpty()) {
                    errors[sub.item.key] = "Could not read this show's feed. Check your connection."
                    continue
                }
                errors.remove(sub.item.key)
                val new = eps.filter { it.id !in sub.known }
                val i = shows.indexOfFirst { it.item.key == sub.item.key }
                if (i >= 0) shows[i] = shows[i].copy(known = sub.known + eps.map { it.id }, lastCheck = System.currentTimeMillis())
                if (new.isEmpty()) continue
                found += new.size
                new.forEach { fresh += UpdateEntry(it, sub.item.key, sub.item.title, System.currentTimeMillis(), false) }
                if (sub.autoDownload) withContext(Dispatchers.Main) { new.take(3).forEach { Downloads.start(it) } }
            }
            updates.addAll(0, fresh)
            lastRun = System.currentTimeMillis()
            save()
        } finally {
            checking = false
        }
        if (notify && fresh.isNotEmpty()) notifyNew(ctx, fresh)
        // saved searches: anything new on LibriVox that matches?
        val hits = runCatching { Searches.checkSaved() }.getOrDefault(emptyList())
        if (notify && hits.isNotEmpty()) notifyOther(ctx, "New audiobooks", "New results for " + hits.take(3).joinToString(", ") { "\"$it\"" })
        return found
    }

    private fun notifyOther(ctx: Context, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("updates", "New episodes", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, app.murmur.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(ctx, "updates") else android.app.Notification.Builder(ctx)
        runCatching { nm.notify(4109, b.setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build()) }
    }

    private fun notifyNew(ctx: Context, fresh: List<UpdateEntry>) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("updates", "New episodes", NotificationManager.IMPORTANCE_LOW))
        val shows = fresh.map { it.showTitle }.distinct()
        val text = "${fresh.size} new from " + shows.take(3).joinToString(", ") + if (shows.size > 3) " and ${shows.size - 3} more" else ""
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, app.murmur.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(ctx, "updates") else android.app.Notification.Builder(ctx)
        runCatching {
            nm.notify(
                4108,
                b.setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle("New episodes").setContentText(text)
                    .setContentIntent(open).setAutoCancel(true).build(),
            )
        }
    }

    // ---- scheduling ------------------------------------------------------------------------------------

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (intervalHours <= 0 || (shows.isEmpty() && Searches.saved.isEmpty())) { wm.cancelUniqueWork("murmur-updates"); return }
        val req = PeriodicWorkRequestBuilder<UpdateWorker>(intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork("murmur-updates", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

/** The background job: one update check. */
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { Subs.checkAll(applicationContext, notify = true) }
        return Result.success()
    }
}
