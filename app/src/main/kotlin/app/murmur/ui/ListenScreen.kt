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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import app.murmur.data.History
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.PlayerHub
import app.murmur.data.Categories
import app.murmur.data.Downloads
import app.murmur.data.Item
import app.murmur.data.Listen
import app.murmur.data.Story
import kotlinx.coroutines.delay

private enum class Kind { BOOKS, PODCASTS, MYLIST, DOWNLOADS }

/** A shelf: a short live preview for the row, and pages for the "See all" grid. */
private class ShelfDef(val title: String, val preview: suspend () -> List<Item>, val page: suspend (Int) -> List<Item>)

private const val L = Listen.LIBRIVOX
private fun archiveShelf(title: String, query: String) =
    ShelfDef(title, { Listen.archive(query) }, { p -> Listen.archive(query, rows = 36, page = p) })

private fun books(title: String, subject: String) = archiveShelf(title, "$L AND subject:\"$subject\"")
private fun chart(title: String, genre: Int) =
    ShelfDef(title, { Listen.topShows(genre, 25) }, { p -> if (p == 1) Listen.topShows(genre, 100) else emptyList() })
private fun searchShelf(title: String, term: String) =
    ShelfDef(title, { Listen.searchShows(term, 25) }, { p -> if (p == 1) Listen.searchShows(term, 100) else emptyList() })

/** Audiobook shelves: each is a live query, most popular first. */
private val bookShelves = listOf(
    archiveShelf("Most popular", L),
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
    archiveShelf("Old-time radio", Listen.RADIO),
)

private val showShelves = listOf(
    searchShelf("Sleep and bedtime", "sleep stories"),
    chart("Fiction and stories", 1483),
    chart("Kids and family", 1305),
    chart("Mental health", 1517),
    searchShelf("Meditation and calm", "meditation calm"),
    chart("Health and fitness", 1512),
    chart("Education", 1304),
    chart("History", 1487),
    chart("Science", 1533),
    chart("Society and culture", 1324),
    chart("Comedy", 1303),
)

/**
 * Free recorded audio, streamed or saved for offline: audiobooks (LibriVox, old-time radio) and
 * podcasts. Browse by shelf (tap a shelf's name for everything in it), search, or open a title.
 */
@Composable
fun ListenScreen(onOpen: (Story) -> Unit) {
    var kind by remember { mutableStateOf(Kind.BOOKS) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Item?>(null) }
    var seeAll by remember { mutableStateOf<ShelfDef?>(null) }
    BackHandler(enabled = detail != null || seeAll != null) { if (detail != null) detail = null else seeAll = null }

    val d = detail
    if (d != null) {
        Detail(d, onOpen, onBack = { detail = null })
        return
    }
    val all = seeAll
    if (all != null) {
        SeeAll(all, onOpen = { detail = it }, onBack = { seeAll = null })
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
                Quiet("Free audiobooks and podcasts. Stream them, or save them for offline.", Modifier.padding(top = 4.dp))
                if (App.incognito) Text("Incognito: nothing you listen to is recorded.", style = MaterialTheme.typography.labelMedium, color = Ink.ember, modifier = Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 10.dp)) {
                    Pill("Audiobooks", kind == Kind.BOOKS) { kind = Kind.BOOKS }
                    Pill("Podcasts", kind == Kind.PODCASTS) { kind = Kind.PODCASTS }
                    Pill("My list", kind == Kind.MYLIST) { kind = Kind.MYLIST }
                    Pill("Downloads", kind == Kind.DOWNLOADS) { kind = Kind.DOWNLOADS }
                }
                if (kind == Kind.BOOKS || kind == Kind.PODCASTS) OutlinedTextField(
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
        if (kind == Kind.MYLIST) {
            myListContent(onOpenItem = { detail = it }, onPlay = onOpen)
        } else if (kind == Kind.DOWNLOADS) {
            downloadsContent(onOpenItem = { detail = it }, onPlay = onOpen)
        } else if (q.length >= 2) {
            val r = results
            when {
                r == null -> item { Quiet("Looking...", Modifier.padding(22.dp)) }
                r.isEmpty() -> item { Quiet("Nothing found for \"$q\". Check your connection or try another word.", Modifier.padding(22.dp)) }
                else -> items(r, key = { it.key }) { ItemRow(it) { detail = it } }
            }
        } else {
            val mine = Listen.recent.filter { (kind == Kind.PODCASTS) == (it.category == Categories.PODCASTS) }.take(5)
            if (mine.isNotEmpty()) {
                item { Heading("Continue listening", Modifier.padding(horizontal = 20.dp)) }
                items(mine, key = { "r_" + it.id }) { s -> RecentRow(s) { onOpen(s) } }
            }
            val shelves = if (kind == Kind.BOOKS) bookShelves else showShelves
            items(shelves, key = { kind.name + it.title }) { def -> ShelfRow(def, onAll = { seeAll = def }) { detail = it } }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

// ---- shelves and "see all" ---------------------------------------------------------------------------

@Composable
private fun ShelfRow(def: ShelfDef, onAll: () -> Unit, onOpen: (Item) -> Unit) {
    val items by produceState<List<Item>?>(null, def) { value = def.preview() }
    val list = items
    Row(
        Modifier.fillMaxWidth().bounceClick("See all in ${def.title}", onAll).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Heading(def.title, Modifier.weight(1f))
        Text("See all", style = MaterialTheme.typography.labelLarge, color = Ink.ember)
    }
    when {
        list == null -> Quiet("Loading...", Modifier.padding(horizontal = 20.dp).height(160.dp))
        list.isEmpty() -> Quiet("Could not load this shelf. Check your connection.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        else -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.key }) { ItemTile(it, Modifier.width(124.dp)) { onOpen(it) } }
        }
    }
}

/** Everything in one shelf as a grid, loading more as you scroll. */
@Composable
private fun SeeAll(def: ShelfDef, onOpen: (Item) -> Unit, onBack: () -> Unit) {
    val items = remember { mutableStateListOf<Item>() }
    var next by remember { mutableIntStateOf(1) }
    var done by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val grid = rememberLazyGridState()

    LaunchedEffect(next) {
        loading = true
        val r = def.page(next)
        val have = items.map { it.key }.toHashSet()
        items.addAll(r.filter { it.key !in have })
        if (r.isEmpty()) done = true
        loading = false
    }
    LaunchedEffect(grid) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (!done && !loading && items.isNotEmpty() && last >= items.size - 5) next += 1
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = grid,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                GhostButton("Back", onBack, Modifier.padding(top = 10.dp))
                Text(def.title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 14.dp))
                Quiet(if (items.isEmpty() && loading) "Loading..." else "${items.size}${if (done) "" else "+"} titles", Modifier.padding(bottom = 6.dp))
            }
        }
        items(items, key = { it.key }) { ItemTile(it, Modifier.fillMaxWidth()) { onOpen(it) } }
        if (loading && items.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Quiet("Loading more...", Modifier.padding(8.dp)) }
        if (!loading && items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Quiet("Nothing loaded. Check your connection.", Modifier.padding(8.dp)) }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ItemTile(item: Item, modifier: Modifier, onClick: () -> Unit) {
    val seen = History.seen(item.key)
    Column(modifier.alpha(if (seen) 0.6f else 1f).bounceClick("Open ${item.title}", onClick)) {
        ItemArt(item, Modifier.fillMaxWidth().aspectRatio(0.8f), seen = seen)
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        if (item.author.isNotBlank()) Text(item.author, style = MaterialTheme.typography.labelSmall, color = Ink.dusk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Cover on the same paper mat as the story covers; a plain tile with the initial while it loads. */
@Composable
fun ItemArt(item: Item, modifier: Modifier, corner: Dp = 10.dp, mat: Dp = 4.dp, seen: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(Ink.paper).padding(mat)) {
        RemoteImage(item.art, Modifier.fillMaxSize().clip(RoundedCornerShape((corner - mat).coerceAtLeast(2.dp)))) {
            Box(Modifier.fillMaxSize().background(Ink.lampHigh), contentAlignment = Alignment.Center) {
                Text(item.title.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = Ink.dusk)
            }
        }
        if (seen) {
            Text(
                "Heard", style = MaterialTheme.typography.labelSmall, color = Ink.moth,
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp).background(Ink.night.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun ItemRow(item: Item, onClick: () -> Unit) {
    val seen = History.seen(item.key)
    Row(
        Modifier.fillMaxWidth().alpha(if (seen) 0.6f else 1f).bounceClick("Open ${item.title}", onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemArt(item, Modifier.width(56.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp, seen = seen)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.author.isNotBlank()) Quiet(item.author)
        }
    }
}

/** A thin progress bar: how far through, or how much downloaded. */
@Composable
fun ThinBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Ink.lampHigh)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).background(Ink.aurora))
    }
}

@Composable
fun RecentRow(s: Story, onClick: () -> Unit) {
    val f = App.progressFraction(s.id)
    Row(
        Modifier.fillMaxWidth().bounceClick("Continue ${s.title}", onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(s, Modifier.width(48.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Quiet(s.author + if (s.id in App.completed) "  ·  Finished" else if (f > 0f) "  ·  ${(f * 100).toInt()}%" else "")
            if (f > 0f && s.id !in App.completed) ThinBar(f, Modifier.padding(top = 6.dp))
        }
    }
}

// ---- download control -----------------------------------------------------------------------------------

/** Download / progress / saved, for a whole book or one episode. */
@Composable
fun DownloadControl(story: Story, compact: Boolean = false) {
    val p = Downloads.progress[story.id]
    val saved = Downloads.count(story) >= story.tracks.size
    val err = Downloads.errors[story.id]
    when {
        p != null -> Column(if (compact) Modifier.width(150.dp) else Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Downloading ${(p * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Ink.ember, modifier = Modifier.weight(1f))
                Text("Cancel", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Cancel download") { Downloads.cancel(story.id) }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp))
            }
            ThinBar(p)
            val sp = Downloads.speedText(story.id)
            if (sp.isNotBlank()) Text(sp, style = MaterialTheme.typography.labelSmall, color = Ink.dusk, modifier = Modifier.padding(top = 2.dp))
        }
        saved -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Saved offline", style = MaterialTheme.typography.labelMedium, color = Ink.moss)
            Text("Remove", style = MaterialTheme.typography.labelMedium, color = Ink.dusk, modifier = Modifier.bounceClick("Remove download") { Downloads.remove(story.id) }.padding(12.dp))
        }
        else -> Column {
            if (compact) {
                Text("Download", style = MaterialTheme.typography.labelLarge, color = Ink.ember, modifier = Modifier.bounceClick("Download ${story.title}") { Downloads.start(story) }.padding(vertical = 10.dp))
            } else {
                GhostButton("Download for offline", { Downloads.start(story) }, Modifier.fillMaxWidth())
            }
            if (err != null && !compact) Quiet(err, Modifier.padding(top = 4.dp))
        }
    }
}
