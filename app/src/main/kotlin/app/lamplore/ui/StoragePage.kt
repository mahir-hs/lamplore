@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.ai.AiModels
import app.lamplore.audio.PackManager
import app.lamplore.data.Crash
import app.lamplore.data.Downloads
import app.lamplore.data.RemoteContent
import java.io.File

private fun size(f: File): Long = if (!f.exists()) 0L else f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
private fun mbText(b: Long) = if (b >= 1_048_576L * 1024) "%.1f GB".format(b / 1_073_741_824.0) else "%.1f MB".format(b / 1_048_576.0)

/** Where the space goes, per title, with sorting and bulk delete. */
@Composable
fun ColumnScope.StorageManagerPage() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var sort by remember { mutableStateOf("size") }
    val picked = remember { mutableStateListOf<String>() }

    val down = Downloads.saved.toList().also { Downloads.rev; tick }.map { it to Downloads.bytes(it.id) }
    val audioDir = File(ctx.filesDir, "audio")
    val stories = (audioDir.listFiles()?.filter { it.isDirectory } ?: emptyList()).map { d ->
        val id = d.name.substringBefore("__")
        (App.stories.firstOrNull { it.id == id }?.title ?: id) to d
    }.map { (t, d) -> Triple(t, d, size(d)) }.sortedByDescending { it.third }
    val covers = size(File(ctx.cacheDir, "img"))
    val pictures = RemoteContent.cacheBytes()

    val total = down.sumOf { it.second } + stories.sumOf { it.third } + covers + pictures + PackManager.diskBytes() + AiModels.diskBytes()
    Quiet("About ${mbText(total)} used by Lamplore on this phone.", Modifier.padding(top = 4.dp))
    Row(Modifier.padding(top = 4.dp)) {
        Column {
            Quiet("Downloads  ${mbText(down.sumOf { it.second })}")
            Quiet("Story narration  ${mbText(stories.sumOf { it.third })}")
            Quiet("Pictures and covers  ${mbText(covers + pictures)}")
            Quiet("Voices  ${mbText(PackManager.diskBytes())}   AI models  ${mbText(AiModels.diskBytes())}")
        }
    }

    Heading("Downloaded audiobooks and episodes")
    if (down.isEmpty()) Quiet("Nothing downloaded.")
    else {
        FlowRow(horizontalArrangement = spacedRow) {
            Pill("Largest first", sort == "size") { sort = "size" }
            Pill("A to Z", sort == "name") { sort = "name" }
            Pill("Newest", sort == "new") { sort = "new" }
        }
        val rows = when (sort) {
            "name" -> down.sortedBy { it.first.title.lowercase() }
            "new" -> down
            else -> down.sortedByDescending { it.second }
        }
        rows.forEach { (s, b) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = s.id in picked, onCheckedChange = { if (it) picked.add(s.id) else picked.remove(s.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Ink.ember, uncheckedColor = Ink.dusk, checkmarkColor = Ink.onEmber),
                )
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Quiet(mbText(b) + if (s.tracks.size > 1) "  ·  ${Downloads.count(s)} of ${s.tracks.size} chapters" else "")
                }
            }
        }
        Row {
            Text(
                "Select all", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
                modifier = Modifier.bounceClick("Select all downloads") { picked.clear(); picked.addAll(rows.map { it.first.id }) }.padding(vertical = 12.dp, horizontal = 4.dp),
            )
            Text(
                "Delete selected (${picked.size})", style = MaterialTheme.typography.labelLarge, color = if (picked.isEmpty()) Ink.line else Ink.ember,
                modifier = Modifier.bounceClick("Delete selected downloads") { picked.toList().forEach { Downloads.remove(it) }; picked.clear(); tick++ }.padding(vertical = 12.dp, horizontal = 16.dp),
            )
        }
    }

    Heading("Story narration")
    Quiet("Made on the phone when you listen. It is rebuilt whenever it is needed, so it is safe to delete.")
    if (stories.isEmpty()) Quiet("Nothing stored.", Modifier.padding(top = 4.dp))
    stories.take(30).forEach { (t, d, b) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(t, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Quiet(mbText(b))
            }
            Text("Delete", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Delete narration of $t") { d.deleteRecursively(); tick++ }.padding(12.dp))
        }
    }
    if (stories.isNotEmpty()) Text(
        "Clear all story narration", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
        modifier = Modifier.bounceClick("Clear all story narration") { App.clearAudio(); tick++ }.padding(vertical = 12.dp),
    )

    Heading("Pictures and covers")
    Quiet("Book illustrations ${mbText(pictures)}, audiobook and show covers ${mbText(covers)}. They download again when needed.")
    Row {
        Text("Clear illustrations", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Clear illustrations") { RemoteContent.clearCache(); tick++ }.padding(vertical = 12.dp, horizontal = 4.dp))
        Text("Clear covers", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Clear covers") { File(ctx.cacheDir, "img").deleteRecursively(); tick++ }.padding(vertical = 12.dp, horizontal = 16.dp))
    }
}

/** What to send along with a bug report, and the last crash if there was one. Nothing leaves the phone by itself. */
@Composable
fun ColumnScope.DiagnosticsPage() {
    val ctx = LocalContext.current
    var crash by remember { mutableStateOf(Crash.last(ctx)) }
    var copied by remember { mutableStateOf("") }
    val stat = runCatching { StatFs(ctx.filesDir.path).let { it.availableBytes to it.totalBytes } }.getOrNull()
    val info = buildString {
        appendLine("Lamplore 0.4")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}")
        if (stat != null) appendLine("Free space: ${mbText(stat.first)} of ${mbText(stat.second)}")
        appendLine("Memory limit: ${(Runtime.getRuntime().maxMemory() / 1_048_576)} MB")
        appendLine("Stories: ${App.stories.size}, downloads: ${Downloads.saved.size}")
    }
    fun copy(text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Lamplore", text))
        copied = "Copied."
    }
    Quiet("Kept on this phone. Copy it to share if something goes wrong.", Modifier.padding(top = 4.dp))
    Heading("This phone")
    Text(info, style = MaterialTheme.typography.bodyMedium, color = Ink.moth, modifier = Modifier.padding(top = 4.dp))
    Text("Copy device info", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Copy device info") { copy(info) }.padding(vertical = 12.dp))

    Heading("Last crash")
    if (crash == null) Quiet("No crash recorded.")
    else {
        Text(crash!!.take(1500), style = MaterialTheme.typography.bodySmall, color = Ink.dusk, modifier = Modifier.padding(top = 4.dp))
        Row {
            Text("Copy report", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Copy crash report") { copy(info + "\n" + crash) }.padding(vertical = 12.dp, horizontal = 4.dp))
            Text("Clear", style = MaterialTheme.typography.labelLarge, color = Ink.dusk, modifier = Modifier.bounceClick("Clear crash report") { Crash.clear(ctx); crash = null }.padding(vertical = 12.dp, horizontal = 16.dp))
        }
    }
    if (copied.isNotBlank()) Quiet(copied)
}
