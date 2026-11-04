package app.murmur.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.murmur.App
import app.murmur.audio.PlayerHub
import app.murmur.data.SeedStories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The measured side of the phone check: real timings from this phone, shown next to the estimates. */
object PhoneTests {
    /** True while a test runs; only one at a time so the numbers are honest. */
    var busy by mutableStateOf(false)
        private set
    var step by mutableStateOf("")
        private set
    var voiceResult by mutableStateOf("")
        private set
    val aiResults = mutableStateMapOf<String, List<LlmDirector.Run>>()

    fun testVoice(ctx: Context) {
        if (busy) return
        busy = true
        step = "Reading a test sentence"
        App.scope.launch {
            voiceResult = try {
                measureVoice(ctx)
            } catch (t: Throwable) {
                "The test did not finish: ${t.message ?: t.javaClass.simpleName}"
            }
            busy = false
            step = ""
        }
    }

    private suspend fun measureVoice(ctx: Context): String {
        val voice = PlayerHub.engine.resolveVoice(App.voiceName)
        if (!voice.startsWith("sherpa:")) return "Pick a Piper or Kokoro voice under Narrator first, then test it."
        val label = PlayerHub.engine.voices().firstOrNull { it.name == voice }?.label ?: "This voice"
        val file = File(ctx.cacheDir, "speedtest.wav")
        PlayerHub.engine.warmUp(voice) // load the model first so the clock measures reading, not loading
        val t0 = System.nanoTime()
        val ok = PlayerHub.engine.synth(
            "The tortoise kept walking, step by step, while the hare slept soundly in the warm afternoon sun.",
            voice, file, 0.9f, 0f,
        )
        val wall = (System.nanoTime() - t0) / 1e9
        if (!ok) return "$label could not read the test sentence."
        val audio = withContext(Dispatchers.IO) { wavSeconds(file) }
        file.delete()
        if (audio <= 0 || wall <= 0) return "The test gave no result."
        val speed = audio / wall
        val times = "%.1f".format(speed)
        return when {
            speed >= 1.5 -> "$label reads $times times faster than real time. Stories start almost at once."
            speed >= 1.0 -> "$label reads $times times real time. Stories start quickly."
            else -> "$label reads at $times times real time. Stories build a head start before they play."
        }
    }

    /** Runs the AI Director test on the graphics chip and then the processor, and keeps both results. */
    fun testAi(ctx: Context, model: AiModel) {
        if (busy || !AiModels.isInstalled(model)) return
        busy = true
        val story = SeedStories.all.first { it.id == "seed_tortoise_hare" }
        App.scope.launch {
            val file = AiModels.file(model)
            step = "Loading the model on the graphics chip"
            val gpu = LlmDirector.measure(ctx, file, story, gpu = true)
            step = "Loading the model on the processor"
            val cpu = LlmDirector.measure(ctx, file, story, gpu = false)
            aiResults[model.id] = listOf(gpu, cpu)
            busy = false
            step = ""
        }
    }

    private fun wavSeconds(f: File): Double {
        if (f.length() <= 44) return 0.0
        return f.inputStream().use { s ->
            val h = ByteArray(28)
            if (s.read(h) < 28) return 0.0
            val rate = (h[24].toInt() and 0xFF) or ((h[25].toInt() and 0xFF) shl 8) or
                ((h[26].toInt() and 0xFF) shl 16) or ((h[27].toInt() and 0xFF) shl 24)
            if (rate <= 0) 0.0 else (f.length() - 44) / (rate * 2.0)
        }
    }
}
