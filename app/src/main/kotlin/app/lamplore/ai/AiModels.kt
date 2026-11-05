package app.lamplore.ai

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.lamplore.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

data class AiModel(val id: String, val title: String, val fileName: String, val url: String, val sizeMb: Int)

/**
 * The language models the AI Director can use. Both are Apache-2.0 Qwen3 builds in LiteRT-LM format,
 * downloaded once into the app's private storage (never bundled in the APK) and resumable.
 */
object AiModels {
    val SMALL = AiModel(
        "qwen3-0.6b", "Small model (Qwen3 0.6B)", "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm",
        "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm", 343,
    )
    val LARGE = AiModel(
        "qwen3-1.7b", "Large model (Qwen3 1.7B)", "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
        "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm", 977,
    )
    val all = listOf(SMALL, LARGE)

    val installed = mutableStateListOf<String>()
    /** 0..1 while downloading. */
    val progress = mutableStateMapOf<String, Float>()
    val errors = mutableStateMapOf<String, String>()
    private val jobs = mutableMapOf<String, Job>()
    private lateinit var ctx: Context

    fun init(c: Context) {
        ctx = c.applicationContext
        refresh()
    }

    fun file(m: AiModel) = File(File(ctx.filesDir, "ai").apply { mkdirs() }, m.fileName)

    fun refresh() {
        installed.clear()
        installed.addAll(all.filter { file(it).let { f -> f.exists() && f.length() > m(it) } }.map { it.id })
    }

    // a model file is only trusted if it is nearly its expected size
    private fun m(model: AiModel) = model.sizeMb * 1_048_576L * 9 / 10

    fun isInstalled(m: AiModel) = m.id in installed

    fun diskBytes(): Long = File(ctx.filesDir, "ai").walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun delete(model: AiModel) {
        file(model).delete()
        File(file(model).path + ".part").delete()
        refresh()
    }

    fun cancel(model: AiModel) { jobs[model.id]?.cancel() }

    /** Downloads (or resumes) a model. Progress and errors are observable. */
    fun download(model: AiModel) {
        if (jobs[model.id]?.isActive == true) return
        errors.remove(model.id)
        progress[model.id] = 0f
        jobs[model.id] = App.scope.launch {
            try {
                withContext(Dispatchers.IO) { fetch(model) }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors[model.id] = (e.message ?: "Download failed") + ". Check your connection and try again."
            } finally {
                progress.remove(model.id)
            }
        }
    }

    private suspend fun fetch(model: AiModel) {
        val part = File(file(model).path + ".part")
        val have = if (part.exists()) part.length() else 0L
        val conn = (URL(model.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        try {
            conn.connect()
            val resumed = conn.responseCode == 206
            check(conn.responseCode == 200 || resumed) { "Download failed (HTTP ${conn.responseCode})" }
            val start = if (resumed) have else 0L
            val length = conn.contentLengthLong
            val total = if (length > 0) start + length else model.sizeMb * 1_048_576L
            RandomAccessFile(part, "rw").use { out ->
                out.seek(start)
                conn.inputStream.use { input ->
                    val buf = ByteArray(256 * 1024)
                    var read = start
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        progress[model.id] = (read.toFloat() / total).coerceIn(0f, 0.99f)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        check(part.length() > m(model)) { "The download ended early" }
        file(model).delete()
        check(part.renameTo(file(model))) { "Could not save the model" }
    }
}
