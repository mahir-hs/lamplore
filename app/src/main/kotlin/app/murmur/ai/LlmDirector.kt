@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package app.murmur.ai

import android.content.Context
import android.util.Log
import app.murmur.data.Story
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * The AI Director's language model. For now this only measures: it asks the model to direct a real story
 * (a sound scene, a music mood and a pace for every paragraph, as JSON) and records how long that takes.
 * The result tells us, per phone, whether the AI Director is practical before it is wired into playback.
 */
object LlmDirector {
    private const val TAG = "Murmur"

    val scenes = listOf("rain", "storm", "wind", "ocean", "stream", "forest", "night", "fire", "desert", "cave", "tense", "calm", "none")
    val moods = listOf("calm", "sleep", "hopeful", "adventure", "tension", "sad", "warm")

    private val system = """
        You direct audio stories for a calm listening app. The user gives a story split into numbered paragraphs.
        For each paragraph choose: s, the background sound that fits the place or weather (${scenes.joinToString(", ")});
        m, the music mood (${moods.joinToString(", ")}); and pace, from 0.8 (slow and calm) to 1.15 (quick and urgent).
        Answer with JSON only, one entry per paragraph in order.
    """.trimIndent()

    /** The answer must have exactly one entry per paragraph, or the model may return an empty list. */
    private fun schemaFor(n: Int) = (
        """{"type":"object","properties":{"p":{"type":"array","minItems":$n,"maxItems":$n,"items":{"type":"object","properties":{""" +
            """"s":{"type":"string","enum":[${scenes.joinToString(",") { "\"$it\"" }}]},""" +
            """"m":{"type":"string","enum":[${moods.joinToString(",") { "\"$it\"" }}]},""" +
            """"pace":{"type":"number"}},"required":["s","m","pace"]}}},"required":["p"]}"""
        )

    /** What one test run measured. Times are seconds. */
    data class Run(
        val backend: String,
        val ok: Boolean,
        val loadSec: Double = 0.0,
        val firstTokenSec: Double = 0.0,
        val totalSec: Double = 0.0,
        val decodeTokensPerSec: Double = 0.0,
        val prefillTokensPerSec: Double = 0.0,
        val outputTokens: Int = 0,
        val paragraphsAsked: Int = 0,
        val paragraphsAnswered: Int = 0,
        val validJson: Boolean = false,
        val error: String? = null,
        val sample: String = "",
    ) {
        /** One plain sentence for the screen. */
        fun line(): String = if (!ok) {
            "$backend: did not work. ${error ?: ""}".trim()
        } else {
            "$backend: loads in ${fmt(loadSec)} s, then directs the story in ${fmt(totalSec)} s. " +
                (if (validJson) "The answer had the right format for $paragraphsAnswered of $paragraphsAsked paragraphs. Experimental: its choices are not good enough yet, so stories still use the built-in Director."
                else "The answer was not valid, so the AI Director would fall back to its rules.")
        }

        private fun fmt(d: Double) = "%.1f".format(d)
    }

    /** True if the native library can load on this phone (64-bit only). */
    fun available(): Boolean = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    private fun paragraphsOf(story: Story, maxWords: Int = 700): List<String> {
        val out = mutableListOf<String>()
        var words = 0
        for (p in story.body.replace("\r", "").split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }) {
            val n = p.split(Regex("\\s+")).size
            if (words + n > maxWords && out.isNotEmpty()) break
            out += p
            words += n
        }
        return out
    }

    /** Runs the model once over `story` on the chosen backend and measures it. Never throws. */
    suspend fun measure(ctx: Context, model: File, story: Story, gpu: Boolean): Run = withContext(Dispatchers.Default) {
        val name = if (gpu) "Graphics chip" else "Processor"
        val paras = paragraphsOf(story)
        val prompt = buildString {
            append("Story: ").append(story.title).append("\n")
            paras.forEachIndexed { i, p -> append("[").append(i).append("] ").append(p).append("\n") }
        }
        val t0 = System.nanoTime()
        try {
            val config = EngineConfig(
                modelPath = model.absolutePath,
                backend = if (gpu) Backend.GPU() else Backend.CPU(),
                maxNumTokens = 4096,
                cacheDir = ctx.cacheDir.absolutePath,
            )
            Engine(config).use { engine ->
                engine.initialize()
                val loadSec = (System.nanoTime() - t0) / 1e9
                val conversation = engine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(system),
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0),
                        enableResponseFormat = true,
                        maxOutputToken = 60 + paras.size * 40,
                        thinkingConfig = ThinkingConfig(false),
                    ),
                )
                conversation.use { c ->
                    val t1 = System.nanoTime()
                    val reply = c.sendMessage(prompt, responseFormat = ResponseFormat.json(schemaFor(paras.size)), thinkingConfig = ThinkingConfig(false))
                    val totalSec = (System.nanoTime() - t1) / 1e9
                    val text = reply.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
                    val info = runCatching { c.getBenchmarkInfo() }.getOrNull() // only filled when the engine was built with benchmark settings
                    var answered = 0
                    val valid = runCatching { answered = JSONObject(text).getJSONArray("p").length() }.isSuccess && answered == paras.size
                    Log.i(TAG, "LLM $name: load ${"%.1f".format(loadSec)}s total ${"%.1f".format(totalSec)}s\n$text")
                    Run(
                        backend = name, ok = true, loadSec = loadSec,
                        firstTokenSec = info?.timeToFirstTokenInSecond ?: 0.0, totalSec = totalSec,
                        decodeTokensPerSec = info?.lastDecodeTokensPerSecond ?: 0.0, prefillTokensPerSec = info?.lastPrefillTokensPerSecond ?: 0.0,
                        outputTokens = info?.lastDecodeTokenCount ?: 0, paragraphsAsked = paras.size, paragraphsAnswered = answered,
                        validJson = valid, sample = text.take(240),
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "LLM $name failed", t)
            Run(backend = name, ok = false, error = (t.message ?: t.javaClass.simpleName).take(160))
        }
    }
}
