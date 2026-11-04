package app.murmur.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import app.murmur.App
import app.murmur.audio.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentSkipListSet

/**
 * Offline copies of audiobook chapters and podcast episodes: a whole book, a few chapters, or one.
 * Files live in the app's private storage (`files/downloads/<id>/<chapter>.mp3`), are resumed if
 * interrupted, and are used by the player instead of the internet whenever they exist. Requests are
 * queued and run one chapter at a time; a foreground service keeps them going when the app is closed.
 * The speed and time-left readout uses a sliding window like aniyomi's RateTracker.
 */
object Downloads {
    /** What one story has been asked to download. */
    private class Req(val story: Story) {
        val wanted = ConcurrentSkipListSet<Int>()
        val done = ConcurrentSkipListSet<Int>()
        val rate = Rate()
        var bytes = 0L
    }

    /** 0..1 over the chapters asked for, per story being downloaded. */
    val progress = mutableStateMapOf<String, Float>()

    /** Current speed in bytes per second, and estimated seconds left, per story. */
    val speed = mutableStateMapOf<String, Long>()
    val eta = mutableStateMapOf<String, Int>()

    /** The chapter being fetched now, and how far through it (0..1), per story. */
    val activeChapter = mutableStateMapOf<String, Int>()
    val chapterFraction = mutableStateMapOf<String, Float>()

    /** Story ids waiting or downloading, in order. */
    val queued = mutableStateListOf<String>()

    /** Stories with at least one chapter on the phone, newest first. */
    val saved = mutableStateListOf<Story>()

    /** Last error per story. */
    val errors = mutableStateMapOf<String, String>()

    /** Bumped whenever a file appears or disappears, so lists re-check the disk. */
    var rev by mutableIntStateOf(0)
        private set

    private val reqs = LinkedHashMap<String, Req>()
    private var worker: Job? = null
    private val prefs get() = App.context.getSharedPreferences("murmur_downloads", Context.MODE_PRIVATE)
    private fun root() = File(App.context.filesDir, "downloads")

    fun init() {
        runCatching {
            Listen.decode(prefs.getString("saved", "[]") ?: "[]").filter { dir(it.id).exists() }.forEach { saved.add(it) }
        }
    }

    // ---- files ------------------------------------------------------------------------------------

    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun dir(id: String) = File(root(), safe(id))
    private fun ext(url: String) = url.substringBefore('?').substringAfterLast('.', "mp3").lowercase().takeIf { it.length in 2..4 } ?: "mp3"
    private fun target(id: String, i: Int, url: String) = File(dir(id), "$i.${ext(url)}")

    /** The saved file for one chapter, or null when it has to be streamed. */
    fun local(id: String, i: Int, url: String): File? = target(id, i, url).takeIf { it.exists() && it.length() > 0 }

    /** Is this chapter on the phone? Reads [rev], so a composable calling it refreshes when files change. */
    fun has(id: String, i: Int, url: String): Boolean {
        rev
        return local(id, i, url) != null
    }

    fun count(story: Story): Int = story.tracks.indices.count { has(story.id, it, story.tracks[it].url) }

    /** The story behind a queued download (it may not be in [saved] yet). */
    fun storyOf(id: String): Story? = reqs[id]?.story ?: saved.firstOrNull { it.id == id }

    fun isSaved(id: String) = saved.any { it.id == id }
    fun isBusy(id: String) = queued.contains(id)

    fun bytes(): Long = root().walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }.sumOf { it.length() }
    fun bytes(id: String): Long = dir(id).walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }.sumOf { it.length() }

    // ---- requests -----------------------------------------------------------------------------------

    /** Download [chapters] of [story] (all of it when null). Chapters already on the phone are skipped. */
    fun start(story: Story, chapters: Collection<Int>? = null) {
        if (!story.isStream) return
        val todo = (chapters ?: story.tracks.indices.toList()).filter { it in story.tracks.indices && !has(story.id, it, story.tracks[it].url) }
        if (todo.isEmpty()) return
        errors.remove(story.id)
        val r = reqs.getOrPut(story.id) {
            queued.add(story.id)
            progress[story.id] = 0f
            Req(story)
        }
        r.wanted.addAll(todo)
        DownloadService.sync(App.context)
        if (worker?.isActive != true) worker = App.scope.launch { runQueue() }
    }

    /** Stop downloading a story. What is already on the phone stays. */
    fun cancel(id: String) {
        val r = reqs.remove(id) ?: return
        finish(r)
    }

    fun removeChapter(story: Story, i: Int) {
        val f = target(story.id, i, story.tracks[i].url)
        f.delete()
        File(f.path + ".part").delete()
        rev++
        if (count(story) == 0) forget(story.id)
    }

    /** Delete everything saved for a story. */
    fun remove(id: String) {
        reqs.remove(id)?.let { finish(it) }
        dir(id).deleteRecursively()
        forget(id)
        rev++
    }

    fun removeAll() {
        queued.toList().forEach { cancel(it) }
        saved.toList().forEach { remove(it.id) }
        root().deleteRecursively()
        rev++
    }

    private fun forget(id: String) {
        saved.removeAll { it.id == id }
        persist()
    }

    private fun persist() {
        prefs.edit().putString("saved", Listen.encode(saved)).apply()
    }

    // ---- the worker ---------------------------------------------------------------------------------

    private suspend fun runQueue() {
        while (true) {
            val r = reqs.values.firstOrNull() ?: break
            val next = r.wanted.firstOrNull { it !in r.done }
            if (next == null) { reqs.remove(r.story.id); finish(r); continue }
            activeChapter[r.story.id] = next
            chapterFraction[r.story.id] = 0f
            val ok = withContext(Dispatchers.IO) { runCatching { fetch(r, next) }.getOrDefault(false) }
            if (reqs[r.story.id] !== r) continue // cancelled while downloading
            if (!ok) {
                errors[r.story.id] = "Download stopped. Check your connection and try again."
                reqs.remove(r.story.id)
                finish(r)
                continue
            }
            r.done.add(next)
            // keep the story listed as soon as one chapter is on the phone
            if (saved.none { it.id == r.story.id }) { saved.add(0, r.story); persist() }
            rev++
        }
    }

    private fun finish(r: Req) {
        reqs.remove(r.story.id)
        queued.remove(r.story.id)
        progress.remove(r.story.id)
        speed.remove(r.story.id)
        eta.remove(r.story.id)
        activeChapter.remove(r.story.id)
        chapterFraction.remove(r.story.id)
        if (r.done.isNotEmpty() && saved.none { it.id == r.story.id }) { saved.add(0, r.story); persist() }
        rev++
        DownloadService.sync(App.context)
    }

    private fun weight(s: Story, i: Int) = s.tracks[i].seconds.coerceAtLeast(1).toFloat()

    /** One chapter, with retries: archive.org mirrors answer with a transient 500 now and then. */
    private suspend fun fetch(r: Req, i: Int): Boolean {
        val s = r.story
        val t = s.tracks[i]
        dir(s.id).mkdirs()
        val out = target(s.id, i, t.url)
        if (out.exists() && out.length() > 0) return true
        val part = File(out.path + ".part")
        var attempt = 0
        while (true) {
            try {
                var have = if (part.exists()) part.length() else 0L
                val c = Listen.open(t.url, have)
                if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
                if (have > 0 && c.responseCode != 206) { have = 0L; part.delete() } // server ignored the range: start over
                val total = (c.contentLengthLong.takeIf { it > 0 } ?: 0L) + have
                val bytesPerSecOfAudio = if (total > 0 && t.seconds > 0) total.toDouble() / t.seconds else 8000.0
                c.inputStream.use { ins ->
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(have)
                        val buf = ByteArray(64 * 1024)
                        var done = have
                        var last = 0L
                        while (true) {
                            if (!kotlin.coroutines.coroutineContext.isActive || reqs[s.id] !== r) return false
                            val n = ins.read(buf)
                            if (n < 0) break
                            raf.write(buf, 0, n)
                            done += n
                            r.bytes += n
                            val now = System.nanoTime()
                            if (now - last > 250_000_000L) {
                                last = now
                                val within = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                                chapterFraction[s.id] = within
                                val wanted = r.wanted.toList()
                                val all = wanted.sumOf { weight(s, it).toDouble() }.toFloat()
                                val finished = r.done.sumOf { weight(s, it).toDouble() }.toFloat()
                                progress[s.id] = ((finished + weight(s, i) * within) / all).coerceIn(0f, 1f)
                                val perSec = r.rate.add(r.bytes)
                                if (perSec != null) {
                                    speed[s.id] = perSec
                                    val leftAudioSec = wanted.filter { it !in r.done && it != i }.sumOf { s.tracks[it].seconds.coerceAtLeast(1) } +
                                        s.tracks[i].seconds.coerceAtLeast(1) * (1f - within)
                                    if (perSec > 0) eta[s.id] = (leftAudioSec * bytesPerSecOfAudio / perSec).toInt()
                                }
                            }
                        }
                    }
                }
                return part.renameTo(out)
            } catch (e: IOException) {
                if (reqs[s.id] !== r) return false
                attempt++
                if (attempt > 6) return false
                delay(1500L * attempt)
            }
        }
    }

    // ---- text -----------------------------------------------------------------------------------------

    fun speedText(id: String): String {
        val bps = speed[id] ?: return ""
        val sp = when {
            bps >= 1_000_000 -> "%.1f MB/s".format(bps / 1_000_000.0)
            bps >= 1_000 -> "${bps / 1000} KB/s"
            else -> "$bps B/s"
        }
        val e = eta[id] ?: return sp
        val left = when {
            e < 60 -> "$e s left"
            e < 3600 -> "${e / 60} min left"
            else -> "${e / 3600} h ${e % 3600 / 60} min left"
        }
        return "$sp  ·  $left"
    }

    /** One line for the notification. */
    fun summary(): String {
        val id = queued.firstOrNull() ?: return ""
        val title = reqs[id]?.story?.title ?: ""
        return listOf(title, speedText(id)).filter { it.isNotBlank() }.joinToString("  ·  ")
    }

    /** Smoothed bytes per second over a short sliding window (the same idea as aniyomi's RateTracker). */
    private class Rate(private val windowMs: Long = 4_000L) {
        private class Sample(val t: Long, val total: Long)

        private val samples = ArrayDeque<Sample>()
        var perSecond: Long? = null
            private set

        fun add(total: Long): Long? {
            val now = System.currentTimeMillis()
            val last = samples.peekLast()
            if (last != null && total < last.total) { samples.clear(); perSecond = null }
            samples.addLast(Sample(now, total))
            while (samples.size > 2 && now - samples.peekFirst()!!.t > windowMs) samples.removeFirst()
            val first = samples.peekFirst()!!
            val el = now - first.t
            if (el >= 500) perSecond = (total - first.total) * 1000 / el
            return perSecond
        }
    }
}
