package app.lamplore.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Natural pacing for narration. Reading a whole paragraph in one go gives flat, rushed speech, so text is cut
 * into sentences (and long sentences at commas), each is spoken on its own, and real silence goes between them:
 * short after a comma, longer after a full stop, longest at the end of a paragraph.
 */
object SpeechPacing {
    data class Piece(val text: String, val pause: Float)

    private val sentenceEnd = Regex("(?<=[.!?…])[\"”’')\\]]*\\s+")
    private val clauseBreak = Regex("(?<=[,;:—–])\\s+")
    private const val LONG = 190

    fun split(text: String): List<Piece> {
        val out = ArrayList<Piece>()
        for (sentence in text.replace("\r", "").split(sentenceEnd).map { it.trim() }.filter { it.isNotEmpty() }) {
            if (sentence.length <= LONG) {
                out += Piece(sentence, pauseFor(sentence))
                continue
            }
            // long sentence: breathe at clause breaks, keeping each piece reasonably long
            val sb = StringBuilder()
            val clauses = sentence.split(clauseBreak)
            for ((i, c) in clauses.withIndex()) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(c)
                val last = i == clauses.lastIndex
                if (last || sb.length >= 70) {
                    out += Piece(sb.toString(), if (last) pauseFor(sentence) else pauseFor(c))
                    sb.clear()
                }
            }
        }
        return out
    }

    /** Seconds of silence after a piece, judged by how it ends. */
    private fun pauseFor(s: String): Float {
        val t = s.trimEnd('"', '”', '’', '\'', ')', ']', ' ')
        return when {
            t.endsWith("…") || t.endsWith("...") -> 0.6f
            t.endsWith("?") -> 0.45f
            t.endsWith("!") -> 0.4f
            t.endsWith(".") -> 0.4f
            t.endsWith(":") -> 0.35f
            t.endsWith(";") || t.endsWith("—") || t.endsWith("–") -> 0.3f
            t.endsWith(",") -> 0.2f
            else -> 0.3f
        }
    }
}

object WavWriter {
    /** 16-bit mono PCM WAV with a plain 44-byte header (what PlayerHub's duration reader expects). */
    fun write(out: File, samples: FloatArray, sampleRate: Int) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.capacity()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.capacity())
        }
        RandomAccessFile(out, "rw").use { f ->
            f.setLength(0)
            f.write(header.array())
            f.write(data.array())
        }
    }
}
