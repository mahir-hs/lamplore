@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.PlayerHub
import app.murmur.data.Downloads
import app.murmur.data.Item
import app.murmur.data.Listen
import app.murmur.data.Story

private fun mb(bytes: Long) = "%.0f MB".format(bytes / 1_048_576.0)

// ---- My list -----------------------------------------------------------------------------------------

/** Starred books and shows, what you are partway through, and what you have finished. */
fun LazyListScope.myListContent(onOpenItem: (Item) -> Unit, onPlay: (Story) -> Unit) {
    item {
        Quiet("Star an audiobook or a show to keep it here.", Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
    }
    if (Listen.starred.isNotEmpty()) {
        item { Heading("Starred", Modifier.padding(horizontal = 20.dp)) }
        items(Listen.starred.toList(), key = { "st_" + it.key }) { it0 ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ItemRow(it0) { onOpenItem(it0) } }
                Text(
                    "Remove", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                    modifier = Modifier.bounceClick("Remove ${it0.title} from my list") { Listen.toggleStar(it0) }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
    val going = Listen.recent.filter { App.progressFraction(it.id) > 0f && it.id !in App.completed }
    if (going.isNotEmpty()) {
        item { Heading("In progress", Modifier.padding(horizontal = 20.dp)) }
        items(going, key = { "ip_" + it.id }) { RecentRow(it) { onPlay(it) } }
    }
    val done = Listen.recent.filter { it.id in App.completed }
    if (done.isNotEmpty()) {
        item { Heading("Finished", Modifier.padding(horizontal = 20.dp)) }
        items(done, key = { "fin_" + it.id }) { RecentRow(it) { onPlay(it) } }
    }
    if (Listen.starred.isEmpty() && going.isEmpty() && done.isEmpty()) {
        item { Quiet("Nothing here yet. Open any audiobook or show and tap \"Add to my list\".", Modifier.padding(22.dp)) }
    }
}

// ---- Downloads ---------------------------------------------------------------------------------------

/** What is downloading now (with speed and time left), and everything saved on this phone. */
fun LazyListScope.downloadsContent(onOpenItem: (Item) -> Unit, onPlay: (Story) -> Unit) {
    item {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
            Quiet(
                if (Downloads.saved.isEmpty() && Downloads.queued.isEmpty()) "Nothing saved yet. Open a book and tap Download, or the arrow next to a chapter."
                else "${Downloads.saved.size} saved  ·  ${mb(Downloads.bytes().also { Downloads.rev })} on this phone. These play without internet.",
            )
        }
    }
    val queue = Downloads.queued.toList()
    if (queue.isNotEmpty()) {
        item { Heading("Downloading", Modifier.padding(horizontal = 20.dp)) }
        items(queue, key = { "q_" + it }) { id ->
            val s = Downloads.storyOf(id)
            if (s != null) {
                val p = Downloads.progress[id] ?: 0f
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val ch = Downloads.activeChapter[id]
                            Quiet("${(p * 100).toInt()}%" + (if (ch != null && s.tracks.size > 1) "  ·  chapter ${ch + 1}" else "") + Downloads.speedText(id).let { if (it.isBlank()) "" else "  ·  $it" })
                        }
                        Text(
                            "Cancel", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                            modifier = Modifier.bounceClick("Cancel download of ${s.title}") { Downloads.cancel(id) }.padding(12.dp),
                        )
                    }
                    ThinBar(p, Modifier.padding(top = 6.dp))
                }
            }
        }
    }
    val saved = Downloads.saved.toList()
    if (saved.isNotEmpty()) {
        item { Heading("On this phone", Modifier.padding(horizontal = 20.dp)) }
        items(saved, key = { "sv_" + it.id }) { s ->
            val n = Downloads.count(s)
            Row(
                Modifier.fillMaxWidth().bounceClick("Open ${s.title}") { if (s.tracks.size > 1) onOpenItem(Listen.itemOf(s)) else onPlay(s) }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(s, Modifier.width(48.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Quiet((if (s.tracks.size > 1) "$n of ${s.tracks.size} chapters  ·  " else "") + mb(Downloads.bytes(s.id)))
                }
                Text(
                    "Remove", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                    modifier = Modifier.bounceClick("Remove downloads of ${s.title}") { Downloads.remove(s.id) }.padding(12.dp),
                )
            }
        }
    }
}

// ---- one audiobook or show ----------------------------------------------------------------------------------

@Composable
fun Detail(item: Item, onOpen: (Story) -> Unit, onBack: () -> Unit) {
    var book by remember { mutableStateOf<Story?>(null) }
    var eps by remember { mutableStateOf<List<Story>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Int>() }
    LaunchedEffect(item) {
        if (item.isShow) eps = Listen.episodes(item) else Listen.book(item).let { book = it; failed = it == null }
    }
    val starred = Listen.isStarred(item.key)

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                GhostButton("Back", onBack, Modifier.padding(top = 18.dp))
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Top) {
                    ItemArt(item, Modifier.width(120.dp).aspectRatio(0.8f))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        if (item.author.isNotBlank()) Quiet(item.author, Modifier.padding(top = 4.dp))
                        val b = book
                        if (b != null) {
                            val secs = b.tracks.sumOf { it.seconds }
                            Quiet(listOf(fmtLen(secs), if (b.tracks.size > 1) "${b.tracks.size} parts" else "").filter { it.isNotBlank() }.joinToString("  ·  "), Modifier.padding(top = 4.dp))
                            val f = App.progressFraction(b.id)
                            if (f > 0f && b.id !in App.completed) {
                                Quiet("${(f * 100).toInt()}% listened", Modifier.padding(top = 8.dp))
                                ThinBar(f, Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
                    Pill(if (starred) "In my list" else "Add to my list", starred) { Listen.toggleStar(item) }
                    val b = book
                    if (b != null) {
                        val fin = b.id in App.completed
                        Pill(if (fin) "Finished" else "Mark finished", fin) { if (fin) App.unmarkCompleted(b.id) else App.markCompleted(b.id) }
                    }
                }
                val b = book
                if (b != null) {
                    val resume = App.hasProgress(b.id)
                    PrimaryButton(if (resume) "Continue" else "Listen", { PlayerHub.playFrom(b, App.savedProgress(b.id)?.first ?: 0); onOpen(b) }, Modifier.fillMaxWidth().padding(top = 10.dp))
                    Box(Modifier.padding(top = 8.dp)) {
                        if (selecting) SelectBar(b, selected) { selecting = false; selected.clear() }
                        else BookDownloadBar(b) { selecting = true }
                    }
                    if (b.summary.isNotBlank()) {
                        Text(
                            b.summary, style = MaterialTheme.typography.bodyMedium, color = Ink.dusk,
                            maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 16.dp).bounceClick("Show more or less") { expanded = !expanded },
                        )
                    }
                    Heading("Chapters", Modifier.padding(top = 12.dp))
                } else if (!item.isShow) {
                    Quiet(if (failed) "This audiobook could not be loaded. Check your connection and try again." else "Loading chapters...", Modifier.padding(top = 20.dp))
                } else if (eps == null) {
                    Quiet("Loading episodes...", Modifier.padding(top = 20.dp))
                } else {
                    Heading("Latest episodes", Modifier.padding(top = 18.dp))
                }
            }
        }
        val b = book
        if (b != null) {
            itemsIndexed(b.tracks) { i, t ->
                Row(
                    Modifier.fillMaxWidth()
                        .bounceClick(if (selecting) "Select ${t.title}" else "Play ${t.title}") {
                            if (selecting) { if (i in selected) selected.remove(i) else selected.add(i) } else { PlayerHub.playFrom(b, i); onOpen(b) }
                        }
                        .padding(start = 22.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selecting) {
                        Checkbox(
                            checked = i in selected, onCheckedChange = { if (it) selected.add(i) else selected.remove(i) },
                            colors = CheckboxDefaults.colors(checkedColor = Ink.ember, uncheckedColor = Ink.dusk, checkmarkColor = Ink.onEmber),
                        )
                    } else {
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.width(32.dp))
                    }
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                    if (t.seconds > 0) Quiet(fmtLen(t.seconds), Modifier.padding(start = 8.dp))
                    if (!selecting) ChapterDl(b, i) else Spacer(Modifier.width(22.dp))
                }
            }
        }
        val e = eps
        if (e != null) {
            if (e.isEmpty()) item { Quiet("No episodes could be loaded from this show's feed.", Modifier.padding(22.dp)) }
            items(e, key = { it.id }) { s ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
                    Column(Modifier.fillMaxWidth().bounceClick("Play ${s.title}") { onOpen(s) }.padding(top = 4.dp)) {
                        Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val m = s.tracks.firstOrNull()?.seconds ?: 0
                        val f = App.progressFraction(s.id)
                        Quiet(listOf(fmtLen(m), if (s.id in App.completed) "Finished" else if (f > 0f) "${(f * 100).toInt()}% listened" else "").filter { it.isNotBlank() }.joinToString("  ·  "))
                        if (s.summary.isNotBlank()) Text(s.summary, style = MaterialTheme.typography.bodyMedium, color = Ink.dusk, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    DownloadControl(s, compact = true)
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

/** The arrow beside a chapter: download it, watch it download, or (when saved) tap to remove it. */
@Composable
private fun ChapterDl(b: Story, i: Int) {
    val has = Downloads.has(b.id, i, b.tracks[i].url)
    val active = Downloads.activeChapter[b.id] == i && Downloads.isBusy(b.id)
    val label = when {
        has -> "Saved offline. Tap to remove chapter ${i + 1}"
        active -> "Downloading chapter ${i + 1}. Tap to cancel"
        else -> "Download chapter ${i + 1}"
    }
    Box(
        Modifier.size(52.dp).bounceClick(label) {
            when {
                has -> Downloads.removeChapter(b, i)
                active -> Downloads.cancel(b.id)
                else -> Downloads.start(b, listOf(i))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        when {
            has -> GlyphIcon(Glyph.CHECK, Ink.moss, 22.dp, label)
            active -> CircularProgressIndicator(
                progress = { Downloads.chapterFraction[b.id] ?: 0f }, modifier = Modifier.size(22.dp),
                color = Ink.ember, trackColor = Ink.lampHigh, strokeWidth = 2.5.dp,
            )
            else -> GlyphIcon(Glyph.DOWNLOAD, if (Downloads.isBusy(b.id)) Ink.line else Ink.dusk, 22.dp, label)
        }
    }
}

/** Whole-book download controls: progress with speed and time left, or Download all / Select chapters. */
@Composable
private fun BookDownloadBar(b: Story, onSelect: () -> Unit) {
    val busy = Downloads.isBusy(b.id)
    val n = Downloads.count(b)
    val p = Downloads.progress[b.id]
    Column(Modifier.fillMaxWidth()) {
        if (busy && p != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Downloading ${(p * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Ink.ember)
                    val sp = Downloads.speedText(b.id)
                    if (sp.isNotBlank()) Text(sp, style = MaterialTheme.typography.labelMedium, color = Ink.dusk)
                }
                Text(
                    "Cancel", style = MaterialTheme.typography.labelLarge, color = Ink.dusk,
                    modifier = Modifier.bounceClick("Cancel download") { Downloads.cancel(b.id) }.padding(12.dp),
                )
            }
            ThinBar(p, Modifier.padding(top = 4.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (n < b.tracks.size) {
                    GhostButton(if (n > 0) "Download rest (${b.tracks.size - n})" else "Download all", { Downloads.start(b) }, Modifier.weight(1f))
                }
                GhostButton("Choose chapters", onSelect, Modifier.weight(1f))
            }
        }
        if (n > 0 && !busy) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$n of ${b.tracks.size} chapters on this phone  ·  ${mb(Downloads.bytes(b.id))}", style = MaterialTheme.typography.labelMedium,
                    color = Ink.moss, modifier = Modifier.weight(1f),
                )
                Text(
                    "Remove", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                    modifier = Modifier.bounceClick("Remove all downloads of this book") { Downloads.remove(b.id) }.padding(12.dp),
                )
            }
        }
        Downloads.errors[b.id]?.let { Quiet(it, Modifier.padding(top = 4.dp)) }
    }
}

/** Pick several chapters, then download (or remove) them together. */
@Composable
private fun SelectBar(b: Story, selected: MutableList<Int>, onDone: () -> Unit) {
    val all = b.tracks.indices.toList()
    Column(Modifier.fillMaxWidth()) {
        Text("${selected.size} of ${b.tracks.size} selected", style = MaterialTheme.typography.labelLarge, color = Ink.ember)
        FlowRow(horizontalArrangement = spacedRow) {
            Pill("All", selected.size == all.size) { selected.clear(); selected.addAll(all) }
            Pill("Next 5", false) { pickNext(b, 5, selected) }
            Pill("Next 10", false) { pickNext(b, 10, selected) }
            Pill("None", selected.isEmpty()) { selected.clear() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            PrimaryButton("Download ${selected.size}", { if (selected.isNotEmpty()) { Downloads.start(b, selected.toList()); onDone() } }, Modifier.weight(1f))
            GhostButton("Cancel", onDone, Modifier.weight(0.6f))
        }
        val have = selected.filter { Downloads.has(b.id, it, b.tracks[it].url) }
        if (have.isNotEmpty()) {
            Text(
                "Remove ${have.size} from this phone", style = MaterialTheme.typography.labelLarge, color = Ink.dusk,
                modifier = Modifier.bounceClick("Remove selected downloads") { have.forEach { Downloads.removeChapter(b, it) }; onDone() }.padding(vertical = 12.dp),
            )
        }
    }
}

/** The next [n] chapters from where you are that are not on the phone yet. */
private fun pickNext(b: Story, n: Int, selected: MutableList<Int>) {
    val from = App.savedProgress(b.id)?.first ?: 0
    val next = (from until b.tracks.size).filter { !Downloads.has(b.id, it, b.tracks[it].url) }.take(n)
    selected.clear()
    selected.addAll(next)
}

fun fmtLen(secs: Int): String = when {
    secs <= 0 -> ""
    secs >= 3600 -> "${secs / 3600} h ${secs % 3600 / 60} min"
    else -> "${maxOf(1, secs / 60)} min"
}
