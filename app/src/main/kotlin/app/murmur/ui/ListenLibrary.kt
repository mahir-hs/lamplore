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
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import app.murmur.data.History
import app.murmur.data.Lists
import app.murmur.data.SavedView
import app.murmur.data.Subs
import kotlinx.coroutines.launch
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

/** How My list is filtered, sorted and grouped. */
class MyListState {
    var list by mutableStateOf<String?>(null)
    var filter by mutableStateOf("all")
    var sort by mutableStateOf("added")
    var group by mutableStateOf("none")
}

private val filterLabels = listOf("all" to "Everything", "books" to "Audiobooks", "shows" to "Shows", "unfinished" to "Unfinished", "downloaded" to "Downloaded")
private val sortLabels = listOf("added" to "Recently added", "title" to "Title A to Z", "author" to "Author A to Z", "played" to "Last listened")
private val groupLabels = listOf("none" to "No grouping", "author" to "By author", "list" to "By list")

/** A small text button that opens a menu of choices. */
@Composable
private fun Menu(label: String, choices: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            "$label  ▾", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
            modifier = Modifier.bounceClick("$label menu") { open = true }.padding(horizontal = 6.dp, vertical = 10.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Ink.lampHigh) {
            choices.forEach { (t, f) -> DropdownMenuItem(text = { Text(t) }, onClick = { open = false; f() }) }
        }
    }
}

/** Ask for one line of text. */
@Composable
private fun NameDialog(title: String, hint: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text(hint) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onDone(text.trim()) }) { Text("Save", color = Ink.ember) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Ink.dusk) } },
        containerColor = Ink.lamp,
    )
}

/** Starred books and shows in your own lists, the shows you follow, and what you are partway through or finished. */
fun LazyListScope.myListContent(st: MyListState, onOpenItem: (Item) -> Unit, onPlay: (Story) -> Unit) {
    item {
        var newList by remember { mutableStateOf(false) }
        var saveView by remember { mutableStateOf(false) }
        Column(Modifier.padding(horizontal = 22.dp)) {
            FlowRow(horizontalArrangement = spacedRow) {
                Pill("Everything", st.list == null) { st.list = null }
                Lists.names.forEach { n -> Pill(n, st.list == n) { st.list = if (st.list == n) null else n } }
                Pill("+ New list", false) { newList = true }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Menu(filterLabels.first { it.first == st.filter }.second, filterLabels.map { (k, l) -> l to { st.filter = k } })
                Menu(sortLabels.first { it.first == st.sort }.second, sortLabels.map { (k, l) -> l to { st.sort = k } })
                Menu(groupLabels.first { it.first == st.group }.second, groupLabels.map { (k, l) -> l to { st.group = k } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Menu(
                    "Views",
                    Lists.views.map { v -> v.name to { st.list = v.list.ifBlank { null }; st.filter = v.filter; st.sort = v.sort; st.group = v.group } } +
                        listOf("Save this view..." to { saveView = true }),
                )
                if (Lists.views.isNotEmpty()) Menu("Delete view", Lists.views.map { v -> v.name to { Lists.deleteView(v.name) } })
                val sel = st.list
                if (sel != null) Text(
                    "Delete list \"$sel\"", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                    modifier = Modifier.bounceClick("Delete list $sel") { Lists.delete(sel); st.list = null }.padding(8.dp),
                )
            }
        }
        if (newList) NameDialog("New list", "For example: Sleep", { Lists.create(it); st.list = it; newList = false }, { newList = false })
        if (saveView) NameDialog("Save this view", "Name", {
            Lists.saveView(SavedView(it, st.filter, st.sort, st.group, st.list ?: ""))
            saveView = false
        }, { saveView = false })
    }

    val base = st.list?.let { l -> Listen.starred.filter { it.key in Lists.keys(l) } } ?: Listen.starred.toList()
    val filtered = base.filter {
        when (st.filter) {
            "books" -> !it.isShow
            "shows" -> it.isShow
            "unfinished" -> it.key !in App.completed
            "downloaded" -> Downloads.isSaved(it.key)
            else -> true
        }
    }
    val sorted = when (st.sort) {
        "title" -> filtered.sortedBy { it.title.lowercase() }
        "author" -> filtered.sortedBy { it.author.lowercase() }
        "played" -> filtered.sortedByDescending { i -> History.items.firstOrNull { it.id == i.key }?.lastAt ?: 0L }
        else -> filtered
    }
    if (sorted.isEmpty()) {
        item {
            Quiet(
                if (Listen.starred.isEmpty()) "Nothing here yet. Open any audiobook or show and tap \"Add to my list\"." else "Nothing matches this view.",
                Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    } else {
        val groups: List<Pair<String?, List<Item>>> = when (st.group) {
            "author" -> sorted.groupBy { it.author.ifBlank { "Unknown" } }.toSortedMap(compareBy { it.lowercase() }).map { (k, v) -> k to v }
            "list" -> sorted.groupBy { i -> Lists.names.firstOrNull { Lists.isIn(it, i.key) } ?: "Not in a list" }.toSortedMap(compareBy { it.lowercase() }).map { (k, v) -> k to v }
            else -> listOf(null to sorted)
        }
        groups.forEach { (heading, rows) ->
            if (heading != null) item(key = "gh_$heading") { Heading(heading, Modifier.padding(horizontal = 20.dp)) }
            items(rows, key = { "st_${heading}_${it.key}" }) { it0 ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ItemRow(it0) { onOpenItem(it0) } }
                    Text(
                        "Remove", style = MaterialTheme.typography.labelMedium, color = Ink.dusk,
                        modifier = Modifier.bounceClick("Remove ${it0.title} from my list") { Listen.toggleStar(it0) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    if (Subs.shows.isNotEmpty() && st.list == null) {
        item { Heading("Following", Modifier.padding(horizontal = 20.dp)) }
        items(Subs.shows.toList(), key = { "fo_" + it.item.key }) { sub ->
            Box(Modifier.fillMaxWidth()) { ItemRow(sub.item) { onOpenItem(sub.item) } }
        }
    }
    val going = Listen.recent.filter { App.progressFraction(it.id) > 0f && it.id !in App.completed }
    if (going.isNotEmpty() && st.list == null) {
        item { Heading("In progress", Modifier.padding(horizontal = 20.dp)) }
        items(going, key = { "ip_" + it.id }) { RecentRow(it) { onPlay(it) } }
    }
    val done = Listen.recent.filter { it.id in App.completed }
    if (done.isNotEmpty() && st.list == null) {
        item { Heading("Finished", Modifier.padding(horizontal = 20.dp)) }
        items(done, key = { "fin_" + it.id }) { RecentRow(it) { onPlay(it) } }
    }
}

// ---- Updates -----------------------------------------------------------------------------------------

/** New episodes of the shows you follow, with a check button, errors and the shows themselves. */
fun LazyListScope.updatesContent(onOpenItem: (Item) -> Unit, onPlay: (Story) -> Unit) {
    item {
        val scope = rememberCoroutineScope()
        val ctx = androidx.compose.ui.platform.LocalContext.current
        var msg by remember { mutableStateOf("") }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
            if (Subs.shows.isEmpty()) {
                Quiet("Follow a show (open it and tap Follow) and new episodes appear here, checked in the background.")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Quiet(
                            "Following ${Subs.shows.size} show${if (Subs.shows.size == 1) "" else "s"}. " +
                                if (Subs.lastRun > 0) "Last checked ${java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(Subs.lastRun))}." else "Not checked yet.",
                        )
                    }
                    GhostButton(if (Subs.checking) "Checking..." else "Check now", {
                        if (!Subs.checking) scope.launch { val n = Subs.checkAll(ctx, notify = false); msg = if (n == 0) "No new episodes." else "$n new." }
                    })
                }
                if (msg.isNotBlank()) Quiet(msg, Modifier.padding(top = 4.dp))
            }
        }
    }
    if (Subs.errors.isNotEmpty()) {
        item { Heading("Could not update", Modifier.padding(horizontal = 20.dp)) }
        items(Subs.errors.keys.toList(), key = { "er_$it" }) { key ->
            val sub = Subs.shows.firstOrNull { it.item.key == key }
            if (sub != null) Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sub.item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Quiet(Subs.errors[key] ?: "")
                }
                val scope = rememberCoroutineScope()
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Text("Retry", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Retry ${sub.item.title}") { scope.launch { Subs.checkAll(ctx, notify = false) } }.padding(12.dp))
            }
        }
    }
    val ups = Subs.updates.toList()
    if (ups.isNotEmpty()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Heading("New episodes", Modifier.weight(1f))
                if (Subs.unread() > 0) Text("Mark all read", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Mark all read") { Subs.markAllRead() }.padding(8.dp))
            }
        }
        items(ups, key = { "up_" + it.story.id }) { u ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp)) {
                Column(Modifier.fillMaxWidth().bounceClick("Play ${u.story.title}") { onPlay(u.story) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!u.read) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Ink.ember))
                        if (!u.read) Spacer(Modifier.width(8.dp))
                        Text(u.story.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Quiet(u.showTitle + (u.story.tracks.firstOrNull()?.seconds?.let { if (it > 0) "  ·  ${fmtLen(it)}" else "" } ?: ""))
                }
                DownloadControl(u.story, compact = true)
            }
        }
        item { Text("Clear list", style = MaterialTheme.typography.labelLarge, color = Ink.dusk, modifier = Modifier.bounceClick("Clear updates") { Subs.clearUpdates() }.padding(horizontal = 22.dp, vertical = 12.dp)) }
    } else if (Subs.shows.isNotEmpty()) {
        item { Quiet("No new episodes yet.", Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) }
    }
    if (Subs.shows.isNotEmpty()) {
        item { Heading("Shows you follow", Modifier.padding(horizontal = 20.dp)) }
        items(Subs.shows.toList(), key = { "fs_" + it.item.key }) { sub ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ItemRow(sub.item) { onOpenItem(sub.item) } }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                    Text(if (sub.autoDownload) "Auto-download on" else "Auto-download off", style = MaterialTheme.typography.labelSmall, color = if (sub.autoDownload) Ink.moss else Ink.dusk,
                        modifier = Modifier.bounceClick("Toggle auto-download for ${sub.item.title}") { Subs.setAutoDownload(sub.item.key, !sub.autoDownload) }.padding(6.dp))
                    Text("Unfollow", style = MaterialTheme.typography.labelSmall, color = Ink.dusk,
                        modifier = Modifier.bounceClick("Unfollow ${sub.item.title}") { Subs.unsubscribe(sub.item.key) }.padding(6.dp))
                }
            }
        }
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
    if (Downloads.queued.isNotEmpty()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Heading("Downloading", Modifier.weight(1f))
                val allPaused = Downloads.queued.all { Downloads.isPaused(it) }
                Text(
                    if (allPaused) "Resume all" else "Pause all", style = MaterialTheme.typography.labelLarge, color = Ink.ember,
                    modifier = Modifier.bounceClick(if (allPaused) "Resume all downloads" else "Pause all downloads") { if (allPaused) Downloads.resumeAll() else Downloads.pauseAll() }.padding(8.dp),
                )
            }
            if (Downloads.waitingWifi) Quiet("Waiting for Wi-Fi. You can change this in You > Downloads and updates.", Modifier.padding(horizontal = 22.dp))
        }
        items(Downloads.queued.toList(), key = { "q_" + it }) { id ->
            val s = Downloads.storyOf(id)
            if (s != null) {
                val p = Downloads.progress[id] ?: 0f
                val paused = Downloads.isPaused(id)
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val ch = Downloads.activeChapter[id]
                    Quiet(
                        if (paused) "Paused at ${(p * 100).toInt()}%"
                        else "${(p * 100).toInt()}%" + (if (ch != null && s.tracks.size > 1) "  ·  chapter ${ch + 1}" else "") + Downloads.speedText(id).let { if (it.isBlank()) "" else "  ·  $it" },
                    )
                    ThinBar(p, Modifier.padding(top = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (paused) "Resume" else "Pause", style = MaterialTheme.typography.labelMedium, color = Ink.ember,
                            modifier = Modifier.bounceClick(if (paused) "Resume ${s.title}" else "Pause ${s.title}") { if (paused) Downloads.resume(id) else Downloads.pause(id) }.padding(end = 14.dp, top = 10.dp, bottom = 10.dp))
                        Text("Up", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Move ${s.title} up") { Downloads.move(id, -1) }.padding(end = 14.dp, top = 10.dp, bottom = 10.dp))
                        Text("Down", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Move ${s.title} down") { Downloads.move(id, 1) }.padding(end = 14.dp, top = 10.dp, bottom = 10.dp))
                        Spacer(Modifier.weight(1f))
                        Text("Cancel", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Cancel download of ${s.title}") { Downloads.cancel(id) }.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
    if (Downloads.failed.isNotEmpty()) {
        item { Heading("Stopped", Modifier.padding(horizontal = 20.dp)) }
        items(Downloads.failed.keys.toList(), key = { "fl_$it" }) { id ->
            val s = Downloads.failed[id]
            if (s != null) Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Quiet(Downloads.errors[id] ?: "Stopped.")
                }
                Text("Retry", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Retry ${s.title}") { Downloads.retry(id) }.padding(12.dp))
                Text("Dismiss", style = MaterialTheme.typography.labelLarge, color = Ink.dusk, modifier = Modifier.bounceClick("Dismiss ${s.title}") { Downloads.dismissFailed(id) }.padding(8.dp))
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
                val scope = rememberCoroutineScope()
                var listsMenu by remember { mutableStateOf(false) }
                var newList by remember { mutableStateOf(false) }
                FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
                    Pill(if (starred) "In my list" else "Add to my list", starred) { Listen.toggleStar(item) }
                    Box {
                        Pill("Lists", Lists.names.any { Lists.isIn(it, item.key) }) { listsMenu = true }
                        DropdownMenu(expanded = listsMenu, onDismissRequest = { listsMenu = false }, containerColor = Ink.lampHigh) {
                            Lists.names.forEach { n -> DropdownMenuItem(text = { Text((if (Lists.isIn(n, item.key)) "✓  " else "     ") + n) }, onClick = { Lists.toggle(n, item) }) }
                            DropdownMenuItem(text = { Text("New list...") }, onClick = { listsMenu = false; newList = true })
                        }
                    }
                    if (item.isShow) {
                        val following = Subs.isSubscribed(item.key)
                        Pill(if (following) "Following" else "Follow", following) { if (following) Subs.unsubscribe(item.key) else scope.launch { Subs.subscribe(item) } }
                        if (following) {
                            val auto = Subs.shows.firstOrNull { it.item.key == item.key }?.autoDownload == true
                            Pill("Auto-download", auto) { Subs.setAutoDownload(item.key, !auto) }
                        }
                    }
                    val b = book
                    if (b != null) {
                        val fin = b.id in App.completed
                        Pill(if (fin) "Finished" else "Mark finished", fin) { if (fin) App.unmarkCompleted(b.id) else App.markCompleted(b.id) }
                    }
                }
                if (newList) NameDialog("New list", "For example: Sleep", { Lists.create(it); Lists.toggle(it, item); newList = false }, { newList = false })
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
