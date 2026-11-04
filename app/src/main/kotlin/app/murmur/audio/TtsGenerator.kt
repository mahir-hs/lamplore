package app.murmur.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Voice engine boundary. Today this is Android's built-in offline TTS.
 * Piper / Kokoro (sherpa-onnx) can be added later as another implementation of this contract.
 */
interface VoiceEngine {
    suspend fun voices(): List<VoiceInfo>
    suspend fun resolveVoice(preferred: String): String
    /**
     * Renders `text` to a WAV; true on success. Piper/Kokoro (voice names starting "sherpa:") bake `speed` into the
     * speech and add natural pauses, so the player must not stretch their audio again. `tail` is seconds of silence
     * appended after the text. The system engine ignores both (the player stretches its audio instead).
     */
    suspend fun synth(text: String, voiceName: String, out: File, speed: Float = 1f, tail: Float = 0f): Boolean

    /** Load the voice ahead of time so the first chunk's timing isn't skewed by model loading. */
    suspend fun warmUp(voiceName: String) {}
    fun stop()
}

enum class Gender { MALE, FEMALE, UNKNOWN }

data class VoiceInfo(val name: String, val label: String, val offline: Boolean, val gender: Gender = Gender.UNKNOWN) {
    /** Which download pack (or "system") this voice belongs to, so character voices can match the narrator's quality. */
    val family: String
        get() = when {
            !name.startsWith("sherpa:") -> "system"
            name.split(":")[1].startsWith("vits-piper") -> "piper" // all Piper packs count as one family
            else -> name.split(":")[1]
        }
}

fun genderFromLabel(s: String): Gender = when {
    s.contains("female", ignoreCase = true) -> Gender.FEMALE
    s.contains("male", ignoreCase = true) -> Gender.MALE
    else -> Gender.UNKNOWN
}

class TtsGenerator(private val ctx: Context) : VoiceEngine {
    private lateinit var tts: TextToSpeech
    private val ready = CompletableDeferred<Boolean>()
    private val waiting = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            utteranceId?.let { waiting.remove(it)?.resume(true) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceId?.let { waiting.remove(it)?.resume(false) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceId?.let { waiting.remove(it)?.resume(false) }
        }
    }

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.setOnUtteranceProgressListener(listener)
                tts.language = Locale.US
                ready.complete(true)
            } else {
                ready.complete(false)
            }
        }
    }

    private fun englishVoices(): List<Voice> =
        (tts.voices ?: emptySet())
            .filter { it.locale.language == "en" }
            .sortedWith(
                compareBy<Voice> { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
                    .thenBy { it.name },
            )

    override suspend fun voices(): List<VoiceInfo> {
        if (!ready.await()) return emptyList()
        return englishVoices().map {
            val tag = it.name.substringAfter("-x-", it.name).substringBefore("-local")
            VoiceInfo(
                name = it.name,
                label = "${it.locale.displayCountry.ifBlank { it.locale.displayLanguage }} - $tag",
                offline = !it.isNetworkConnectionRequired,
                gender = genderFromLabel(it.name),
            )
        }
    }

    override suspend fun resolveVoice(preferred: String): String {
        if (!ready.await()) return ""
        val all = englishVoices()
        if (preferred.isNotBlank() && all.any { it.name == preferred }) return preferred
        return (all.firstOrNull { it.locale == Locale.US && !it.isNetworkConnectionRequired }
            ?: all.firstOrNull())?.name ?: ""
    }

    override suspend fun synth(text: String, voiceName: String, out: File, speed: Float, tail: Float): Boolean {
        if (!ready.await()) return false
        englishVoices().firstOrNull { it.name == voiceName }?.let { tts.voice = it }
        out.parentFile?.mkdirs()
        val id = UUID.randomUUID().toString()
        return suspendCancellableCoroutine { cont ->
            waiting[id] = cont
            cont.invokeOnCancellation {
                waiting.remove(id)
                tts.stop()
            }
            val r = tts.synthesizeToFile(text, Bundle(), out, id)
            if (r != TextToSpeech.SUCCESS) {
                waiting.remove(id)?.resume(false)
            }
        }
    }

    override fun stop() {
        if (::tts.isInitialized) tts.stop()
    }
}

object TextChunker {
    /**
     * Small first chunk so playback starts quickly, then chunks that roughly double in size up to [max].
     * Growing chunks keep generation ahead of playback.
     */
    fun split(text: String, first: Int = 200, max: Int = 1000): List<String> {
        val sentences = text.replace("\r", "")
            .split(Regex("(?<=[.!?…])\\s+|\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (s in sentences) {
            val cap = minOf(max, first shl minOf(out.size, 6))
            if (sb.isNotEmpty() && sb.length + s.length + 1 > cap) {
                out += sb.toString()
                sb.clear()
            }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(s)
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out
    }
}
