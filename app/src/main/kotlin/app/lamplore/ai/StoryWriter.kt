@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package app.lamplore.ai

import android.content.Context
import app.lamplore.App
import app.lamplore.data.Secret
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Writes a bedtime story from an idea. Two engines: the user's own API key (a strong model, needs internet,
 * one request per story) or a small language model on the phone (offline, much weaker). Nothing is sent
 * anywhere unless the user picks the key engine and presses Write.
 */
object StoryWriter {
    enum class Source { CLOUD, LOCAL }

    data class Request(val idea: String, val forName: String, val children: Boolean, val minutes: Int)
    data class Draft(val title: String, val body: String)

    const val DEFAULT_MODEL = "claude-haiku-5-5"
    private const val KEY = "anthropic_key"
    private val prefs get() = App.context.getSharedPreferences("lamplore_writer", Context.MODE_PRIVATE)

    var source: Source
        get() = runCatching { Source.valueOf(prefs.getString("engine", "CLOUD")!!) }.getOrDefault(Source.CLOUD)
        set(v) { prefs.edit().putString("engine", v.name).apply() }
    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) { prefs.edit().putString("model", v.trim().ifBlank { DEFAULT_MODEL }).apply() }
    var apiKey: String
        get() = Secret.get(KEY)
        set(v) = Secret.put(KEY, v.trim())
    val hasKey get() = Secret.has(KEY)

    private fun system(r: Request) = buildString {
        append("You write bedtime stories for a calm listening app that reads them aloud. ")
        append("Write one complete story in plain prose: no headings, no lists, no markdown, no sound effects in brackets. ")
        append("Short sentences and gentle rhythm, easy to follow by ear. A soft, safe world: no violence, no fear that is not resolved, no villains who win. ")
        append("Begin slowly, let the story drift, and end with the main character settling down to sleep so the listener can too. ")
        append(if (r.children) "The listener is a young child. Use simple words and friendly animals or objects. " else "The listener is an adult winding down. A quiet, dreamlike tone suits. ")
        if (r.forName.isNotBlank()) append("The main character is called ${r.forName.trim()}; use the name naturally, never as a list of options. ")
        append("Aim for about ${r.minutes * 140} words. Reply with the title on the first line as \"Title: ...\", a blank line, then the story.")
    }

    /** Never throws; a failure comes back as a plain sentence. */
    suspend fun write(r: Request): Result<Draft> = withContext(Dispatchers.IO) {
        runCatching { parse(if (source == Source.CLOUD) cloud(r) else local(r)) }
    }

    private fun cloud(r: Request): String {
        val key = apiKey
        if (key.isBlank()) error("Add your API key first.")
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", r.minutes * 140 * 2 + 300)
            .put("system", system(r))
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Story idea: ${r.idea.trim()}")))
        val c = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("x-api-key", key)
            setRequestProperty("anthropic-version", "2023-06-01")
            setRequestProperty("content-type", "application/json")
        }
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrDefault("")
            error(
                when (code) {
                    401, 403 -> "The API key was refused. Check it in the settings below."
                    404 -> "The model \"$model\" was not found. Check the model name below."
                    429 -> "Too many requests, or the account is out of credit. $msg".trim()
                    else -> "The service answered $code. $msg".trim()
                },
            )
        }
        val parts = JSONObject(text).getJSONArray("content")
        return (0 until parts.length()).map { parts.getJSONObject(it) }.filter { it.optString("type") == "text" }.joinToString("") { it.getString("text") }
    }

    private fun local(r: Request): String {
        val m = listOf(AiModels.LARGE, AiModels.SMALL).firstOrNull { AiModels.isInstalled(it) }
            ?: error("Download a model on this phone first (see below).")
        val config = EngineConfig(
            modelPath = AiModels.file(m).absolutePath,
            backend = Backend.CPU(),
            maxNumTokens = 4096,
            cacheDir = App.context.cacheDir.absolutePath,
        )
        return Engine(config).use { engine ->
            engine.initialize()
            engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(system(r)),
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8, seed = System.nanoTime().toInt()),
                    maxOutputToken = (r.minutes * 140 * 1.6).toInt() + 200,
                    thinkingConfig = ThinkingConfig(false),
                ),
            ).use { c ->
                val reply = c.sendMessage("Story idea: ${r.idea.trim()}", thinkingConfig = ThinkingConfig(false))
                reply.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
            }
        }
    }

    /** "Title: X" on the first line, then the story. Falls back to a title made from the idea. */
    internal fun parse(raw: String): Draft {
        val text = raw.replace(Regex("(?s)<think>.*?</think>"), "").replace("\r", "").trim()
        if (text.length < 80) error("The story came back too short. Try again.")
        val lines = text.lines()
        val first = lines.first().trim()
        val hasTitle = first.startsWith("Title:", ignoreCase = true)
        val title = (if (hasTitle) first.substringAfter(":") else first.take(60)).trim().trim('"', '*', '#', ' ').ifBlank { "A bedtime story" }
        val body = (if (hasTitle) lines.drop(1) else lines).joinToString("\n").trim().replace(Regex("\\*+"), "").replace(Regex("(?m)^#+\\s*"), "")
        return Draft(title, body)
    }
}
