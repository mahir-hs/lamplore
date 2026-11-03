package app.murmur.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random

/**
 * Plays the generated background sound on its own thread. Changing the target sound crossfades
 * (about 3 seconds), so scene changes in a story blend instead of cutting.
 */
class Ambience {
    @Volatile private var kind = AmbienceKind.NONE
    @Volatile private var volume = 0.35f
    @Volatile private var running = false
    private var thread: Thread? = null

    private class Layer(val gen: SoundGen, var gain: Float)

    fun update(kind: AmbienceKind, volume: Float, run: Boolean) {
        this.kind = kind
        this.volume = volume
        running = run
        if (running) ensureThread()
    }

    @Synchronized
    private fun ensureThread() {
        if (thread?.isAlive == true) return
        thread = Thread { loop() }.apply {
            isDaemon = true
            start()
        }
    }

    private fun loop() {
        val rate = Generators.RATE
        val buf = ShortArray(2048)
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuf, buf.size * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val rnd = Random()
        val layers = LinkedHashMap<AmbienceKind, Layer>()
        val fadeInc = 1f / (rate * 3f)
        var curVol = 0f

        track.play()
        while (true) {
            if (!running) {
                track.pause()
                track.flush()
                var waited = 0
                while (!running) {
                    try { Thread.sleep(200) } catch (_: InterruptedException) { }
                    waited += 200
                    if (waited > 120_000) {
                        track.release()
                        return
                    }
                }
                curVol = 0f
                track.play()
            }

            val tgt = kind
            val vol = if (tgt == AmbienceKind.NONE) 0f else volume
            if (tgt != AmbienceKind.NONE && !layers.containsKey(tgt)) {
                layers[tgt] = Layer(Generators.create(tgt, rnd), 0f)
            }
            val active = layers.entries.toList()
            for (i in buf.indices) {
                curVol += (vol - curVol) * 0.0004f
                var s = 0f
                for (e in active) {
                    val l = e.value
                    l.gain = if (e.key == tgt) minOf(1f, l.gain + fadeInc) else maxOf(0f, l.gain - fadeInc)
                    if (l.gain > 0f) s += l.gen.next() * l.gain
                }
                buf[i] = (s.coerceIn(-1f, 1f) * curVol * Short.MAX_VALUE * 0.6f).toInt().toShort()
            }
            layers.entries.removeAll { it.key != tgt && it.value.gain <= 0f }
            track.write(buf, 0, buf.size)
        }
    }
}
