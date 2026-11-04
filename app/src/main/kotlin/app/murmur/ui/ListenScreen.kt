@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.murmur.audio.PlayerHub
import app.murmur.data.Item
import app.murmur.data.Listen
import app.murmur.data.Story
import kotlinx.coroutines.delay

private enum class Kind { BOOKS, PODCASTS }

private class ShelfDef(val title: String, val load: suspend () -> List<Item>)

private val L = Listen.LIBRIVOX
private fun books(title: String, subject: String) = ShelfDef(title) { Listen.archive("$L AND subject:\"$subject\"") }

/** Audiobook shelves: each is a live query, most popular first. */
private val bookShelves = listOf(
    ShelfDef("Most popular") { Listen.archive(L) },
    books("For children", "children"),
    books("Fairy tales", "fairy tales"),
    books("Fables", "fables"),
    books("Short stories", "short stories"),
    books("Mystery and detective", "detective"),
    books("Adventure", "adventure"),
    books("Science fiction", "science fiction"),
    books("Myths and legends", "mythology"),
    books("Humor", "humor"),
    books("Poetry", "poetry"),
    books("Philosophy and wisdom", "philosophy"),
    books("History", "history"),
    books("Travel and nature", "travel"),
    ShelfDef("Old-time radio") { Listen.archive(Listen.RADIO) },
)

private val showShelves = listOf(
    ShelfDef("Sleep and bedtime") { Listen.searchShows("sleep stories") },
    ShelfDef("Fiction and stories") { Listen.topShows(1483) },
    ShelfDef("Kids and family") { Listen.topShows(1305) },
    ShelfDef("Mental health") { Listen.topShows(1517) },
    ShelfDef("Meditation and calm") { Listen.searchShows("meditation calm") },
    ShelfDef("Health and fitness") { Listen.topShows(1512) },
    ShelfDef("Education") { Listen.topShows(1304) },
    ShelfDef("History") { Listen.topShows(1487) },
    ShelfDef("Science") { Listen.topShows(1533) },
    ShelfDef("Society and culture") { Listen.topShows(1324) },
    ShelfDef("Comedy") { Listen.topShows(1303) },
)

/**
 * Free recorded audio, streamed: audiobooks (LibriVox, old-time radio) and podcasts. Browse by
 * shelf, search, or open a title to see its description and chapters.
 */
@Composable
fun ListenScreen(onOpen: (Story) -> Unit) {
    var kind by remember { mutableStateOf(Kind.BOOKS) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Item?>(null) }
    BackHandler(enabled = detail != null) { detail = null }

    val d = detail
    if (d != null) {
        Detail(d, onOpen, onBack = { detail = null })
        return
    }

    val q = query.trim()
    val results by produceState<List<Item>?>(null, q, kind) {
        value = null
        if (q.length >= 2) {
            delay(450) // wait for the typing to settle
            value = if (kind == Kind.BOOKS) Listen.searchBooks(q) else Listen.searchShows(q)
        }
    }

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Listen", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
                Quiet("Free audiobooks and podcasts, streamed. Nothing to download.", Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 10.dp)) {
                    Pill("Audiobooks", kind == Kind.BOOKS) { kind = Kind.BOOKS }
                    Pill("Podcasts", kind == Kind.PODCASTS) { kind = Kind.PODCASTS }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (kind == Kind.BOOKS) "Title, author or subject" else "Search podcasts") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink.dusk) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
        if (q.length >= 2) {
            val r = results
            when {
                r == null -> item { Quiet("Looking...", Modifier.padding(22.dp)) }
                r.isEmpty() -> item { Quiet("Nothing found for \"$q\". Check your connection or try another word.", Modifier.padding(22.dp)) }
                else -> items(r, key = { it.key }) { ItemRow(it) { detail = it } }
            }
        } else {
            val mine = Listen.recent.filter { (kind == Kind.PODCASTS) == (it.category == app.murmur.data.Categories.PODCASTS) }.take(5)
            if (mine.isNotEmpty()) {
                item { Heading("Continue listening", Modifier.padding(horizontal = 20.dp)) }
                items(mine, key = { "r_" + it.id }) { s -> RecentRow(s) { onOpen(s) } }
            }
            val shelves = if (kind == Kind.BOOKS) bookShelves else showShelves
            items(shelves, key = { kind.name + it.title }) { def -> ShelfRow(def) { detail = it } }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

// ---- shelves -------------------------------------------------------------------------------------

@Composable
private fun ShelfRow(def: ShelfDef, onOpen: (Item) -> Unit) {
    val items by produceState<List<Item>?>(null, def) { value = def.load() }
    val list = items
    Heading(def.title, Modifier.padding(horizontal = 20.dp))
    when {
        list == null -> Quiet("Loading...", Modifier.padding(horizontal = 20.dp).height(160.dp))
        list.isEmpty() -> Quiet("Could not load this shelf. Check your connection.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        else -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.key }) { ItemTile(it, 124.dp) { onOpen(it) } }
        }
    }
}

@Composable
private fun ItemTile(item: Item, width: Dp, onClick: () -> Unit) {
    Column(Modifier.width(width).bounceClick("Open ${item.title}", onClick)) {
        ItemArt(item, Modifier.width(width).aspectRatio(0.8f))
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        if (item.author.isNotBlank()) Text(item.author, style = MaterialTheme.typography.labelSmall, color = Ink.dusk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Cover on the same paper mat as the story covers; a plain tile with the initial while it loads. */
@Composable
private fun ItemArt(item: Item, modifier: Modifier, corner: Dp = 10.dp, mat: Dp = 4.dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(Ink.paper).padding(mat)) {
        RemoteImage(item.art, Modifier.fillMaxSize().clip(RoundedCornerShape((corner - mat).coerceAtLeast(2.dp)))) {
            Box(Modifier.fillMaxSize().background(Ink.lampHigh), contentAlignment = Alignment.Center) {
                Text(item.title.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = Ink.dusk)
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick("Open ${item.title}", onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemArt(item, Modifier.width(56.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.author.isNotBlank()) Quiet(item.author)
        }
    }
}

@Composable
private fun RecentRow(s: Story, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick("Continue ${s.title}", onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(s, Modifier.width(48.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Quiet(s.author, Modifier.padding(top = 0.dp))
        }
        if (s.id in app.murmur.App.completed) Quiet("Finished")
    }
}

// ---- one audiobook or show ---------------------------------------------------------------------------

@Composable
private fun Detail(item: Item, onOpen: (Story) -> Unit, onBack: () -> Unit) {
    var book by remember { mutableStateOf<Story?>(null) }
    var eps by remember { mutableStateOf<List<Story>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(item) {
        if (item.isShow) eps = Listen.episodes(item) else Listen.book(item).let { book = it; failed = it == null }
    }
    var expanded by remember { mutableStateOf(false) }

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
                            val len = if (secs <= 0) "" else if (secs >= 3600) "${secs / 3600} h ${secs % 3600 / 60} min" else "${secs / 60} min"
                            Quiet(listOf(len, if (b.tracks.size > 1) "${b.tracks.size} parts" else "").filter { it.isNotBlank() }.joinToString("  ·  "), Modifier.padding(top = 4.dp))
                        }
                    }
                }
                val b = book
                if (b != null) {
                    val resume = app.murmur.App.hasProgress(b.id)
                    PrimaryButton(if (resume) "Continue" else "Listen", { PlayerHub.playFrom(b, app.murmur.App.savedProgress(b.id)?.first ?: 0); onOpen(b) }, Modifier.fillMaxWidth().padding(top = 18.dp))
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
                    Modifier.fillMaxWidth().bounceClick("Play ${t.title}") { PlayerHub.playFrom(b, i); onOpen(b) }.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.width(32.dp))
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (t.seconds > 0) Quiet(if (t.seconds >= 3600) "${t.seconds / 3600} h ${t.seconds % 3600 / 60} min" else "${maxOf(1, t.seconds / 60)} min")
                }
            }
        }
        val e = eps
        if (e != null) {
            if (e.isEmpty()) item { Quiet("No episodes could be loaded from this show's feed.", Modifier.padding(22.dp)) }
            items(e, key = { it.id }) { s ->
                Column(Modifier.fillMaxWidth().bounceClick("Play ${s.title}") { onOpen(s) }.padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val m = s.tracks.firstOrNull()?.seconds ?: 0
                    if (m > 0) Quiet(if (m >= 3600) "${m / 3600} h ${m % 3600 / 60} min" else "${maxOf(1, m / 60)} min")
                    if (s.summary.isNotBlank()) Text(s.summary, style = MaterialTheme.typography.bodyMedium, color = Ink.dusk, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}
