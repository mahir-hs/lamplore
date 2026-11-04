package app.murmur.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One timed line of a transcript. */
class Cue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Podcast transcripts (the `podcast:transcript` tag in a show's feed): WebVTT, SubRip and the
 * podcast-namespace JSON format are read into timed lines, so the player can show what is being said.
 * Transcripts without times cannot be followed along and are ignored.
 */
object Transcripts {
    /** Transcript types that carry times, best first. */
    private val timed = listOf("text/vtt", "application/x-subrip", "application/srt", "text/srt", "application/json")

    /** Pick the best timed transcript from `(url, type)` pairs, as "url|type", or "" if none is usable. */
    fun choose(options: List<Pair<String, String>>): String {
        val best = timed.firstNotNullOfOrNull { t -> options.firstOrNull { it.second.equals(t, true) } } ?: return ""
        return best.first + "|" + best.second
    }

    suspend fun load(spec: String): List<Cue> = withContext(Dispatchers.IO) {
        val url = spec.substringBefore('|')
        val type = spec.substringAfter('|', "").lowercase()
        runCatching {
            val text = Listen.open(url).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            if ("json" in type) json(text) else cues(text)
        }.getOrDefault(emptyList())
    }

    private fun clock(s: String): Long {
        val p = s.trim().replace(',', '.').split(':')
        val sec = p.last().toDoubleOrNull() ?: return 0L
        val min = if (p.size >= 2) p[p.size - 2].toLongOrNull() ?: 0L else 0L
        val hr = if (p.size >= 3) p[p.size - 3].toLongOrNull() ?: 0L else 0L
        return ((hr * 3600 + min * 60) * 1000 + sec * 1000).toLong()
    }

    /** WebVTT and SubRip: blocks separated by blank lines, a "start --> end" line, then the words. */
    private fun cues(text: String): List<Cue> {
        val out = mutableListOf<Cue>()
        for (block in text.replace("\r", "").split("\n\n")) {
            val lines = block.lines()
            val at = lines.indexOfFirst { "-->" in it }
            if (at < 0) continue
            val (a, b) = lines[at].split("-->").let { it[0] to it[1].trim().substringBefore(' ') }
            val words = lines.drop(at + 1).joinToString(" ") { it.trim() }.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
            if (words.isNotEmpty()) out += Cue(clock(a), clock(b), words)
        }
        return out
    }

    /** The podcast-namespace JSON transcript: {"segments":[{"startTime":0,"endTime":4,"body":"..."}]}. */
    private fun json(text: String): List<Cue> {
        val arr = JSONObject(text).optJSONArray("segments") ?: return emptyList()
        return (0 until arr.length()).mapNotNull {
            val o = arr.getJSONObject(it)
            val body = o.optString("body").trim()
            if (body.isEmpty()) null else Cue((o.optDouble("startTime") * 1000).toLong(), (o.optDouble("endTime") * 1000).toLong(), body)
        }
    }

    /** The line being spoken at [posMs], or the last one that started before it. */
    fun at(cues: List<Cue>, posMs: Long): String {
        if (cues.isEmpty()) return ""
        var lo = 0
        var hi = cues.size - 1
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (cues[mid].startMs <= posMs) { best = mid; lo = mid + 1 } else hi = mid - 1
        }
        if (best < 0) return ""
        val c = cues[best]
        // a long silence after a line: show nothing rather than a stale line
        return if (posMs - c.endMs > 8000 && c.endMs > c.startMs) "" else c.text
    }
}
