@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.data.Downloads
import app.lamplore.data.History
import app.lamplore.data.HistoryItem
import app.lamplore.data.Item
import app.lamplore.data.Listen
import app.lamplore.data.Status
import app.lamplore.data.Story
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** History as its own tab. */
@Composable
fun HistoryScreen(onPlay: (Story) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("History", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
        HistoryPage(onPlay)
        Spacer(Modifier.height(28.dp))
    }
}

/** Find the story behind a history entry: a built-in story, something played or downloaded, or a book to fetch again. */
private suspend fun resolve(h: HistoryItem): Story? {
    return Listen.find(h.id, h.title, h.author, h.art)
}

private fun whenText(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val d = z.toLocalDate()
    val today = LocalDate.now()
    val time = z.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (d) {
        today -> "Today $time"
        today.minusDays(1) -> "Yesterday $time"
        else -> z.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

/**
 * Everything you have listened to, newest first, with how far you got. Remove and Clear only hide entries
 * (Restore brings them back); your Activity and the "Heard" marks are not affected.
 */
@Composable
fun ColumnScope.HistoryPage(onPlay: (Story) -> Unit) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf<Status?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var missing by remember { mutableStateOf("") }
    val all = History.visible()
    val list = all.filter { filter == null || History.status(it) == filter }

    Quiet(
        if (App.incognito) "Incognito is on, so nothing new is being added." else "Remove only hides a title here. Your activity stays.",
        Modifier.padding(top = 4.dp),
    )
    FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
        Pill("All", filter == null) { filter = null }
        Status.entries.forEach { s -> Pill(s.label, filter == s) { filter = if (filter == s) null else s } }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (all.isNotEmpty()) {
            Text(
                if (confirmClear) "Tap again to hide all" else "Clear history",
                style = MaterialTheme.typography.labelLarge, color = if (confirmClear) Ink.ember else Ink.dusk,
                modifier = Modifier.bounceClick("Clear history") { if (confirmClear) { History.hideAll(); confirmClear = false } else confirmClear = true }.padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }
        val hidden = History.hiddenCount()
        if (hidden > 0) {
            Text(
                "Restore hidden ($hidden)", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
                modifier = Modifier.bounceClick("Restore hidden history") { History.restoreAll() }.padding(vertical = 12.dp, horizontal = 12.dp),
            )
        }
    }
    if (missing.isNotBlank()) Quiet(missing, Modifier.padding(bottom = 6.dp))

    if (list.isEmpty()) {
        Quiet(if (all.isEmpty()) "Nothing here yet. What you listen to will be listed with how far you got." else "Nothing with this status.", Modifier.padding(top = 8.dp))
    }
    list.forEach { h ->
        key(h.id) {
            HistoryRow(h, onOpen = {
                missing = ""
                scope.launch {
                    val s = resolve(h)
                    if (s != null) onPlay(s) else missing = "\"${h.title}\" could not be opened. Check your connection."
                }
            })
        }
    }
}

@Composable
private fun HistoryRow(h: HistoryItem, onOpen: () -> Unit) {
    val status = History.status(h)
    val f = App.progressFraction(h.id)
    var menu by remember { mutableStateOf(false) }
    val story = App.stories.firstOrNull { it.id == h.id }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).bounceClick("Continue ${h.title}", onOpen), verticalAlignment = Alignment.CenterVertically) {
            if (story != null) Cover(story, Modifier.width(48.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
            else ItemArt(Item(h.id, h.title, h.author, h.art, "", false), Modifier.width(48.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(h.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Quiet("${h.topic}  ·  ${whenText(h.lastAt)}  ·  ${fmtTime(h.seconds)}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        status.label, style = MaterialTheme.typography.labelSmall,
                        color = when (status) { Status.FINISHED -> Ink.moss; Status.DROPPED -> Ink.dusk; else -> Ink.ember },
                    )
                    if (h.days > 1) Text("  ·  heard on ${h.days} days", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
                }
                if (status == Status.IN_PROGRESS && f > 0f) ThinBar(f, Modifier.padding(top = 4.dp))
            }
        }
        Box {
            Box(Modifier.bounceClick("More options for ${h.title}") { menu = true }.padding(10.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Ink.dusk)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.lampHigh) {
                DropdownMenuItem(text = { Text("Remove from history") }, onClick = { menu = false; History.hide(h.id) })
                DropdownMenuItem(
                    text = { Text(if (h.dropped) "Track again" else "Drop (stop tracking)") },
                    onClick = { menu = false; History.setDropped(h.id, !h.dropped) },
                )
                DropdownMenuItem(
                    text = { Text(if (h.id in App.completed) "Mark as not finished" else "Mark as finished") },
                    onClick = { menu = false; if (h.id in App.completed) App.unmarkCompleted(h.id) else App.markCompleted(h.id) },
                )
            }
        }
    }
}
