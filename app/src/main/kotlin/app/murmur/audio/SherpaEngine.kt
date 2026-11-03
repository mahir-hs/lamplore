package app.murmur.audio

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Piper and Kokoro voices running fully on the phone via sherpa-onnx.
 * Voice names look like "sherpa:<packId>:<speakerId>".
 */
class SherpaEngine : VoiceEngine {
    private val mutex = Mutex()
    /** Loaded engines, least recently used first. A story switches voices often, so keep a few resident. */
    private val engines = LinkedHashMap<String, OfflineTts>()
    private val maxResident = 3

    override suspend fun voices(): List<VoiceInfo> =
        PackManager.installed.mapNotNull { VoicePacks.byId(it) }.flatMap { p ->
            p.speakers.mapIndexed { i, s ->
                VoiceInfo(
                    name = "sherpa:${p.id}:$i",
                    label = if (p.kind == PackKind.KOKORO) "$s - Kokoro" else s,
                    offline = true,
                    gender = genderFromLabel(s),
                )
            }
        }

    override suspend fun resolveVoice(preferred: String): String {
        if (!preferred.startsWith("sherpa:")) return ""
        val id = preferred.split(":").getOrNull(1) ?: return ""
        return if (id in PackManager.installed) preferred else ""
    }

    override suspend fun synth(text: String, voiceName: String, out: File): Boolean {
        val parts = voiceName.split(":")
        val pack = parts.getOrNull(1)?.let { VoicePacks.byId(it) } ?: return false
        val sid = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val engine = ensure(pack)
                    out.parentFile?.mkdirs()
                    val audio = engine.generate(text, sid, 1.0f)
                    audio.samples.isNotEmpty() && audio.save(out.absolutePath)
                } catch (e: Throwable) {
                    Log.e("Murmur", "Sherpa synth failed for ${pack.id}", e)
                    false
                }
            }
        }
    }

    override suspend fun warmUp(voiceName: String) {
        val pack = voiceName.split(":").getOrNull(1)?.let { VoicePacks.byId(it) } ?: return
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try { ensure(pack) } catch (e: Throwable) { Log.e("Murmur", "Warm-up failed for ${pack.id}", e) }
            }
        }
    }

    override fun stop() {}

    fun release(packId: String) {
        engines.remove(packId)?.free()
    }

    private fun ensure(pack: VoicePack): OfflineTts {
        engines.remove(pack.id)?.let { engines[pack.id] = it; return it } // move to most-recent
        while (engines.size >= maxResident) {
            val eldest = engines.keys.first()
            engines.remove(eldest)?.free()
        }

        val dir = PackManager.dir(pack)
        val onnx = dir.listFiles { f -> f.extension == "onnx" }?.firstOrNull()?.absolutePath
            ?: error("No .onnx model in ${dir.absolutePath}")
        val tokens = File(dir, "tokens.txt").absolutePath
        val espeak = File(dir, "espeak-ng-data").absolutePath
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

        val model = when (pack.kind) {
            PackKind.PIPER -> OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(model = onnx, tokens = tokens, dataDir = espeak),
                numThreads = threads,
                debug = false,
                provider = "cpu",
            )
            PackKind.KOKORO -> OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = onnx,
                    voices = File(dir, "voices.bin").absolutePath,
                    tokens = tokens,
                    dataDir = espeak,
                ),
                numThreads = threads,
                debug = false,
                provider = "cpu",
            )
        }
        val created = OfflineTts(config = OfflineTtsConfig(model = model))
        engines[pack.id] = created
        return created
    }
}

/** Routes each voice to the right engine: downloaded Piper/Kokoro packs, or the phone's built-in TTS. */
class CompositeEngine(private val system: VoiceEngine, private val sherpa: VoiceEngine) : VoiceEngine {
    override suspend fun voices(): List<VoiceInfo> = sherpa.voices() + system.voices()

    override suspend fun resolveVoice(preferred: String): String {
        if (preferred.startsWith("sherpa:")) {
            val r = sherpa.resolveVoice(preferred)
            if (r.isNotBlank()) return r
            return system.resolveVoice("")
        }
        return system.resolveVoice(preferred)
    }

    override suspend fun synth(text: String, voiceName: String, out: File): Boolean =
        if (voiceName.startsWith("sherpa:")) sherpa.synth(text, voiceName, out) else system.synth(text, voiceName, out)

    override suspend fun warmUp(voiceName: String) {
        if (voiceName.startsWith("sherpa:")) sherpa.warmUp(voiceName)
    }

    override fun stop() {
        sherpa.stop()
        system.stop()
    }
}
