@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lamplore.audio.PlayerHub
import app.lamplore.data.Bookmark
import app.lamplore.data.Bookmarks
import app.lamplore.data.Listen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A small dialog to write or edit a note on a bookmark. */
@Composable
fun NoteDialog(title: String, initial: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("A note (optional)") }, minLines = 2) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save", color = Ink.ember) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Ink.dusk) } },
        containerColor = Ink.lamp,
    )
}

private fun stamp(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))

/** Every saved moment, grouped by title. Tap one to jump straight back to it. */
@Composable
fun ColumnScope.BookmarksPage(onShowPlayer: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Bookmark?>(null) }
    var missing by remember { mutableStateOf("") }

    Quiet("Save a moment from the player's menu: Bookmark this moment.", Modifier.padding(top = 4.dp))
    if (missing.isNotBlank()) Quiet(missing, Modifier.padding(top = 6.dp))
    if (Bookmarks.items.isEmpty()) {
        Quiet("Nothing saved yet.", Modifier.padding(top = 12.dp))
        return
    }
    val groups = Bookmarks.items.groupBy { it.storyId }
    groups.forEach { (_, list) ->
        Heading(list.first().title)
        list.forEach { b ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(
                    Modifier.fillMaxWidth().bounceClick("Jump to ${b.label.ifBlank { b.title }}") {
                        missing = ""
                        scope.launch {
                            val s = Listen.find(b.storyId, b.title, b.author, b.art)
                            if (s != null) { PlayerHub.jumpTo(s, b.index, b.posMs); onShowPlayer() }
                            else missing = "\"${b.title}\" could not be opened. Check your connection."
                        }
                    },
                ) {
                    Text(
                        b.label.ifBlank { fmtClock(b.posMs) }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (b.note.isNotBlank()) Text(b.note, style = MaterialTheme.typography.bodyMedium, color = Ink.moth, modifier = Modifier.padding(top = 2.dp))
                    Quiet("${stamp(b.at)}  ·  at ${fmtClock(b.posMs)}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Note", style = MaterialTheme.typography.labelMedium, color = Ink.ember, modifier = Modifier.bounceClick("Edit note") { editing = b }.padding(end = 16.dp, top = 8.dp, bottom = 8.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Delete bookmark") { Bookmarks.remove(b.id) }.padding(vertical = 8.dp))
                }
            }
        }
    }
    editing?.let { b -> NoteDialog("Note", b.note, { Bookmarks.setNote(b.id, it); editing = null }, { editing = null }) }
}

fun fmtClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60) else "%d:%02d".format(total / 60, total % 60)
}
