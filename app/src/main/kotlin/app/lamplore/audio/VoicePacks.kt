package app.lamplore.audio

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
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

enum class PackKind { PIPER, KOKORO }

data class VoicePack(
    val id: String,
    val title: String,
    val desc: String,
    val archive: String,
    val sizeMb: Int,
    val kind: PackKind,
    val speakers: List<String>,
)

object VoicePacks {
    private const val BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"

    val all: List<VoicePack> = listOf(
        VoicePack(
            id = "kokoro-int8-en-v0_19",
            title = "Kokoro",
            desc = "Most natural voices. 11 voices, US and UK, male and female. Needs a head start before a story " +
                "plays, since it generates slower than real time on most phones.",
            archive = "kokoro-int8-en-v0_19",
            sizeMb = 99,
            kind = PackKind.KOKORO,
            speakers = listOf(
                "Default (US female)", "Bella (US female)", "Nicole (US female)", "Sarah (US female)",
                "Sky (US female)", "Adam (US male)", "Michael (US male)", "Emma (UK female)",
                "Isabella (UK female)", "George (UK male)", "Lewis (UK male)",
            ),
        ),
        VoicePack(
            id = "kokoro-multi-lang-v1_0",
            title = "Kokoro v1.0 (large, experimental)",
            desc = "The most voices: 28 English voices, US and UK, with lighter and deeper ones for characters. " +
                "Big and slow on a phone, so stories need a long head start.",
            archive = "kokoro-multi-lang-v1_0",
            sizeMb = 330,
            kind = PackKind.KOKORO,
            // ids 0-27 of the 53 speakers are the English ones; the rest are Chinese and are left out
            speakers = listOf(
                "Alloy (US female)", "Aoede (US female)", "Bella (US female)", "Heart (US female)",
                "Jessica (US female)", "Kore (US female)", "Nicole (US female)", "Nova (US female)",
                "River (US female)", "Sarah (US female)", "Sky (US female)",
                "Adam (US male)", "Echo (US male)", "Eric (US male)", "Fenrir (US male)", "Liam (US male)",
                "Michael (US male)", "Onyx (US male)", "Puck (US male)", "Santa (US male)",
                "Alice (UK female)", "Emma (UK female)", "Isabella (UK female)", "Lily (UK female)",
                "Daniel (UK male)", "Fable (UK male)", "George (UK male)", "Lewis (UK male)",
            ),
        ),
        piper("vits-piper-en_US-lessac-medium-int8", "Lessac", "US female, clear narrator voice.", "Lessac (US female)"),
        piper("vits-piper-en_US-ryan-medium-int8", "Ryan", "US male, warm and steady.", "Ryan (US male)"),
        piper("vits-piper-en_US-amy-medium-int8", "Amy", "US female, soft and gentle.", "Amy (US female)"),
        piper("vits-piper-en_US-joe-medium-int8", "Joe", "US male, deeper tone.", "Joe (US male)"),
        piper("vits-piper-en_GB-alan-medium-int8", "Alan", "UK male.", "Alan (UK male)"),
        piper("vits-piper-en_GB-jenny_dioco-medium-int8", "Jenny", "UK female.", "Jenny (UK female)"),
    )

    private fun piper(id: String, title: String, desc: String, speaker: String) = VoicePack(
        id = id,
        title = "Piper - $title",
        desc = "$desc Fast and light.",
        archive = id,
        sizeMb = 20,
        kind = PackKind.PIPER,
        speakers = listOf(speaker),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun url(p: VoicePack) = "$BASE${p.archive}.tar.bz2"
}

/** Downloads, unpacks and tracks voice packs on the phone. */
object PackManager {
    private lateinit var ctx: Context
    val installed = mutableStateListOf<String>()
    /** 0..1 while downloading, 2 while unpacking. */
    val progress = mutableStateMapOf<String, Float>()
    val errors = mutableStateMapOf<String, String>()
    private val jobs = mutableMapOf<String, Job>()

    fun init(c: Context) {
        ctx = c.applicationContext
        refresh()
    }

    fun dir(p: VoicePack) = File(ctx.filesDir, "voices/${p.id}")

    fun refresh() {
        installed.clear()
        installed.addAll(VoicePacks.all.filter { File(dir(it), ".ok").exists() }.map { it.id })
    }

    fun diskBytes(): Long =
        File(ctx.filesDir, "voices").walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun download(p: VoicePack) {
        if (jobs[p.id]?.isActive == true) return
        errors.remove(p.id)
        progress[p.id] = 0f
        jobs[p.id] = App.scope.launch {
            try {
                withContext(Dispatchers.IO) { fetchAndUnpack(p) }
                refresh()
                if (!App.voiceName.startsWith("sherpa:")) {
                    App.voiceName = "sherpa:${p.id}:0"
                    App.saveSettings()
                }
            } catch (e: CancellationException) {
                dir(p).deleteRecursively()
                throw e
            } catch (e: Exception) {
                dir(p).deleteRecursively()
                errors[p.id] = e.message ?: "Download failed"
            } finally {
                progress.remove(p.id)
            }
        }
    }

    fun cancel(p: VoicePack) {
        jobs[p.id]?.cancel()
    }

    fun delete(p: VoicePack) {
        PlayerHub.sherpa.release(p.id)
        dir(p).deleteRecursively()
        if (App.voiceName.startsWith("sherpa:${p.id}:")) {
            App.voiceName = ""
            App.saveSettings()
        }
        refresh()
    }

    private suspend fun fetchAndUnpack(p: VoicePack) {
        val tmpDir = File(ctx.filesDir, "voices/tmp").apply { mkdirs() }
        val tmp = File(tmpDir, "${p.id}.tar.bz2")
        val conn = (URL(VoicePacks.url(p)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            check(conn.responseCode == 200) { "Download failed (HTTP ${conn.responseCode})" }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: (p.sizeMb * 1024L * 1024L)
            conn.inputStream.use { input ->
                tmp.outputStream().buffered().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        progress[p.id] = (read.toFloat() / total).coerceIn(0f, 0.99f)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        progress[p.id] = 2f
        val target = dir(p)
        target.deleteRecursively()
        target.mkdirs()
        val root = target.canonicalPath + File.separator
        TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(tmp), 1 shl 16))).use { tar ->
            var e = tar.nextEntry
            while (e != null) {
                currentCoroutineContext().ensureActive()
                val rel = e.name.substringAfter('/', "")
                if (rel.isNotEmpty()) {
                    val f = File(target, rel)
                    require(f.canonicalPath.startsWith(root)) { "Bad archive entry" }
                    if (e.isDirectory) f.mkdirs() else {
                        f.parentFile?.mkdirs()
                        f.outputStream().buffered().use { tar.copyTo(it) }
                    }
                }
                e = tar.nextEntry
            }
        }
        tmp.delete()
        check(target.listFiles { f -> f.extension == "onnx" }?.isNotEmpty() == true) { "Voice files missing after unpacking" }
        File(target, ".ok").writeText("ok")
    }
}
