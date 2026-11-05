package app.lamplore.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.lamplore.App
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two looping layers under the voice: an ambience recording and a music bed. Changing a layer
 * fades the old sound out, swaps the file, and fades the new one in, so scene changes blend.
 * Neither layer takes audio focus; the voice player owns it.
 */
class SoundBed(private val ctx: Context) {
    private val ambience = Layer()
    private val music = Layer()

    fun update(kind: AmbienceKind, mood: MusicMood, ambVol: Float, musicVol: Float, run: Boolean) {
        ambience.set(kind.file, if (run) ambVol else 0f)
        music.set(mood.file, if (run) musicVol else 0f)
    }

    fun release() {
        ambience.release()
        music.release()
    }

    private inner class Layer {
        private var player: ExoPlayer? = null
        private var file: String? = null
        private var job: Job? = null

        private fun player(): ExoPlayer = player ?: ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                false, // never steal focus from the voice
            )
            .build()
            .also {
                it.repeatMode = Player.REPEAT_MODE_ONE
                it.volume = 0f
                player = it
            }

        fun set(newFile: String?, target: Float) {
            val p = player()
            job?.cancel()
            job = App.scope.launch {
                if (newFile != file) {
                    ramp(p, 0f, 1200)
                    p.stop()
                    p.clearMediaItems()
                    file = newFile
                    if (newFile != null) {
                        p.setMediaItem(MediaItem.fromUri("asset:///sounds/$newFile.opus"))
                        p.prepare()
                    }
                }
                if (file == null) return@launch
                if (target > 0f) {
                    if (!p.isPlaying) p.play()
                    ramp(p, target, 1800)
                } else {
                    ramp(p, 0f, 900)
                    p.pause()
                }
            }
        }

        private suspend fun ramp(p: ExoPlayer, to: Float, ms: Long) {
            val from = p.volume
            if (from == to) return
            val steps = (ms / 40).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                p.volume = from + (to - from) * i / steps
                delay(40)
            }
        }

        fun release() {
            job?.cancel()
            player?.release()
            player = null
            file = null
        }
    }
}
