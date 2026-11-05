@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.data.Backup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Source {
    data class Doc(val uri: Uri) : Source
    data class Auto(val file: File) : Source
}

/** Save everything that is yours to a file, restore it, or let the app do it on a schedule. */
@Composable
fun ColumnScope.BackupPage() {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Source?>(null) }
    var interval by remember { mutableStateOf(Backup.intervalDays.also { Backup.loadSettings(ctx) }.let { Backup.intervalDays }) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            msg = runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Backup.toJson(ctx).toByteArray()) } }
                .fold({ "Backup saved." }, { "Could not save the backup." })
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pending = Source.Doc(uri) }

    Quiet(
        "Settings, stars, progress, your stories, notes, history and activity go into one file. Downloaded audio is not included, only the list of what you had.",
        Modifier.padding(top = 4.dp),
    )
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        PrimaryButton("Create backup", { create.launch(Backup.fileName()) }, Modifier.weight(1f))
        GhostButton("Restore from file", { open.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, Modifier.weight(1f))
    }

    val p = pending
    if (p != null) {
        Heading("Restore this backup?")
        Quiet("This replaces what is on the phone with the file's contents. Restoring the same file again changes nothing. Lamplore closes afterwards; open it again to see everything.")
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Restore now", {
                val json = runCatching {
                    when (p) {
                        is Source.Doc -> ctx.contentResolver.openInputStream(p.uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                        is Source.Auto -> p.file.readText()
                    }
                }.getOrNull()
                pending = null
                if (json != null && Backup.restore(ctx, json)) {
                    msg = "Restored. Lamplore is closing; open it again."
                    App.scope.launch { delay(1600); android.os.Process.killProcess(android.os.Process.myPid()) }
                } else msg = "That file is not a Lamplore backup."
            }, Modifier.weight(1f))
            GhostButton("Cancel", { pending = null }, Modifier.weight(0.6f))
        }
    }
    if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.padding(top = 10.dp))

    Heading("Automatic backup")
    FlowRow(horizontalArrangement = spacedRow) {
        listOf(0 to "Off", 1 to "Every day", 7 to "Every week").forEach { (d, l) ->
            Pill(l, interval == d) { interval = d; Backup.setInterval(ctx, d) }
        }
    }
    val last = Backup.lastAutoAt(ctx)
    Quiet(
        if (interval == 0) "Off. Turn it on to keep the newest five backups in the app's folder."
        else "Checked when you open Lamplore. " + if (last > 0) "Last one: ${SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(last))}." else "None yet.",
        Modifier.padding(top = 4.dp),
    )
    val files = Backup.list(ctx)
    if (files.isNotEmpty()) {
        Quiet("Saved in ${Backup.dir(ctx).path}", Modifier.padding(top = 4.dp))
        files.forEach { f ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Quiet("${f.length() / 1024} KB")
                }
                Text(
                    "Restore", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
                    modifier = Modifier.bounceClick("Restore ${f.name}") { pending = Source.Auto(f) }.padding(12.dp),
                )
            }
        }
    }
    GhostButton("Back up now to the app's folder", { runCatching { Backup.writeAuto(ctx) }; msg = "Backup written." }, Modifier.fillMaxWidth().padding(top = 12.dp))
}
