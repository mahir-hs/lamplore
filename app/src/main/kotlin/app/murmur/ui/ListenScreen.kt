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

enum class ListenKind { BOOKS, PODCASTS, UPDATES, MYLIST, DOWNLOADS }

/** A shelf: a short live preview for the row, and pages for the "See all" grid. */
class ShelfDef(val title: String, val preview: suspend () -> List<Item>, val page: suspend (Int) -> List<Item>)

private const val L = Listen.LIBRIVOX
private fun archiveShelf(title: String, query: String, sort: String = "downloads desc", unseenOnly: Boolean = false) =
    ShelfDef(
        title,
        { Listen.archive(query, sort = sort).let { l -> if (unseenOnly) l.filter { !History.seen(it.key) } else l } },
        { p -> Listen.archive(query, rows = 36, page = p, sort = sort) },
    )

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

/** Home asks the Audiobooks page to open a title or a whole shelf. */
object ListenNav {
    var item: Item? = null
    var seeAll: ShelfDef? = null
}

/**
 * The Discover feed on Home (aniyomi-style rows): carry on, what next, and the shelves for tonight, new
 * releases and trending. [onBrowse] opens a title or a whole shelf on the Audiobooks page.
 */
@Composable
fun DiscoverFeed(onBrowse: (Item?, ShelfDef?) -> Unit, onPlay: (Story) -> Unit) {
    val going = Listen.recent.filter { App.progressFraction(it.id) > 0f && it.id !in App.completed }.take(3)
    Column {
        if (going.isNotEmpty()) {
            Heading("Continue listening", Modifier.padding(horizontal = 20.dp))
            going.forEach { RecentRow(it) { onPlay(it) } }
        }
        WhatNext(onOpenItem = { onBrowse(it, null) }, onPlay = onPlay)
        discoverShelves().forEach { def -> ShelfRow(def, onAll = { onBrowse(null, def) }) { onBrowse(it, null) } }
        Spacer(Modifier.height(12.dp))
    }
}

/** The Discover feed: what is new and trending, and picks from the hour and from what you have been listening to. */
fun discoverShelves(): List<ShelfDef> {
    val hour = java.time.LocalTime.now().hour
    val tag = app.murmur.data.Activity.byTag(365).firstOrNull()?.first
    val out = mutableListOf<ShelfDef>()
    if (hour >= 21 || hour < 5) {
        out += archiveShelf("For tonight", "$L AND (subject:fables OR subject:\"fairy tales\" OR subject:poetry OR subject:\"short stories\")", "week desc", true)
    }
    if (tag != null) out += archiveShelf("Because you like $tag", "$L AND subject:\"${tag.lowercase()}\"", "downloads desc", true)
    out += archiveShelf("New releases", L, "publicdate desc")
    out += archiveShelf("Trending this week", L, "week desc")
    return out
}

/** A short "what next" card: carry on, or a pick from your taste you have not heard. */
@Composable
private fun WhatNext(onOpenItem: (Item) -> Unit, onPlay: (Story) -> Unit) {
    val tag = remember { app.murmur.data.Activity.byTag(365).firstOrNull()?.first }
    val pick by produceState<Item?>(null, tag) {
        if (tag != null) value = Listen.archive("$L AND subject:\"${tag.lowercase()}\"").firstOrNull { !History.seen(it.key) }
    }
    if (pick == null) return
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 6.dp)) {
        Heading("What next?")
        pick?.let { p ->
            Quiet("Because you like $tag", Modifier.padding(top = 4.dp))
            ItemRow(p) { onOpenItem(p) }
        }
    }
}

/** "More like this" under a book: the same author or subjects. */
@Composable
fun SimilarRow(item: Item, tags: List<String>, onOpen: (Item) -> Unit) {
    val list by produceState<List<Item>?>(null, item.key) { value = Listen.similar(item, tags) }
    val l = list?.filter { it.key != item.key }
    if (l.isNullOrEmpty()) return
    Heading("More like this", Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(l, key = { "sim_" + it.key }) { ItemTile(it, Modifier.width(110.dp)) { onOpen(it) } }
    }
    Spacer(Modifier.height(16.dp))
}

/**
 * Free recorded audio, streamed or saved for offline: audiobooks (LibriVox, old-time radio) and
 * podcasts. Browse by shelf (tap a shelf's name for everything in it), search, or open a title.
 */
@Composable
fun ListenScreen(kind: ListenKind, onOpen: (Story) -> Unit) {
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Item?>(null) }
    var seeAll by remember { mutableStateOf<ShelfDef?>(null) }
    val myState = remember { MyListState() }
    var dlByAuthor by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ListenNav.item?.let { detail = it; ListenNav.item = null }
        ListenNav.seeAll?.let { seeAll = it; ListenNav.seeAll = null }
    }
    BackHandler(enabled = detail != null || seeAll != null) { if (detail != null) detail = null else seeAll = null }

    val d = detail
    if (d != null) {
        Detail(
            d, onOpen, onBack = { detail = null },
            onAuthor = { a -> detail = null; seeAll = archiveShelf(a, "$L AND creator:\"${a.replace("\"", "")}\"") },
            onOpenItem = { detail = it },
        )
        return
    }
    val all = seeAll
    if (all != null) {
        SeeAll(all, onOpen = { detail = it }, onBack = { seeAll = null })
        return
    }

    val q = query.trim()
    val results by produceState<app.murmur.data.Merged?>(null, q) {
        value = null
        if (q.length >= 2) {
            delay(450) // wait for the typing to settle
            val m = Listen.searchAll(q)
            value = m
            if (!m.isEmpty) app.murmur.data.Searches.addRecent(q)
            app.murmur.data.Searches.opened(q, m.books)
        }
    }

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                val title = when (kind) {
                    ListenKind.BOOKS -> "Audiobooks"
                    ListenKind.PODCASTS -> "Podcasts"
                    ListenKind.UPDATES -> "Updates"
                    ListenKind.MYLIST -> "My list"
                    ListenKind.DOWNLOADS -> "Downloads"
                }
                Text(title, style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
                when (kind) {
                    ListenKind.BOOKS -> Quiet("Free audiobooks from LibriVox, plus old-time radio.", Modifier.padding(top = 4.dp))
                    ListenKind.PODCASTS -> Quiet("Free podcasts. Browse, search and follow.", Modifier.padding(top = 4.dp))
                    else -> {}
                }
                if (App.incognito) Text("Incognito: nothing you listen to is recorded.", style = MaterialTheme.typography.labelMedium, color = Ink.ember, modifier = Modifier.padding(top = 4.dp))
                if (kind == ListenKind.BOOKS || kind == ListenKind.PODCASTS) OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search books, shows and your list") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink.dusk) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
        if (kind == ListenKind.UPDATES) {
            updatesContent(onOpenItem = { detail = it }, onPlay = onOpen)
        } else if (kind == ListenKind.MYLIST) {
            myListContent(myState, onOpenItem = { detail = it }, onPlay = onOpen)
        } else if (kind == ListenKind.DOWNLOADS) {
            downloadsContent(dlByAuthor, { dlByAuthor = it }, onOpenItem = { detail = it }, onPlay = onOpen)
        } else if (q.length >= 2) {
            val r = results
            item {
                val saved = app.murmur.data.Searches.isSaved(q)
                val tips = app.murmur.data.Searches.suggest(q)
                FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(horizontal = 22.dp)) {
                    Pill(if (saved) "Search saved" else "Save this search", saved) { app.murmur.data.Searches.toggleSaved(q) }
                    tips.forEach { t -> Pill(t, false) { query = t } }
                }
            }
            when {
                r == null -> item { Quiet("Looking...", Modifier.padding(22.dp)) }
                r.isEmpty -> item { Quiet("Nothing found for \"$q\". Check your connection or try another word.", Modifier.padding(22.dp)) }
                else -> {
                    if (r.mine.isNotEmpty()) { item { Heading("In your list", Modifier.padding(horizontal = 20.dp)) }; items(r.mine, key = { "m_" + it.key }) { ItemRow(it) { detail = it } } }
                    if (r.books.isNotEmpty()) { item { Heading("Audiobooks", Modifier.padding(horizontal = 20.dp)) }; items(r.books, key = { "b_" + it.key }) { ItemRow(it) { detail = it } } }
                    if (r.shows.isNotEmpty()) { item { Heading("Shows", Modifier.padding(horizontal = 20.dp)) }; items(r.shows, key = { "s_" + it.key }) { ItemRow(it) { detail = it } } }
                }
            }
        } else {
            val mine = Listen.recent.filter { (kind == ListenKind.PODCASTS) == (it.category == Categories.PODCASTS) }.take(5)
            if (mine.isNotEmpty()) {
                item { Heading("Continue listening", Modifier.padding(horizontal = 20.dp)) }
                items(mine, key = { "r_" + it.id }) { s -> RecentRow(s) { onOpen(s) } }
            }
            val savedQ = app.murmur.data.Searches.saved.toList()
            val recentQ = app.murmur.data.Searches.recent.toList().filter { it !in savedQ }.take(6)
            if (savedQ.isNotEmpty() || recentQ.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        if (savedQ.isNotEmpty()) {
                            Quiet("Saved searches")
                            FlowRow(horizontalArrangement = spacedRow) {
                                savedQ.forEach { sq ->
                                    val n = app.murmur.data.Searches.fresh[sq] ?: 0
                                    Pill(if (n > 0) "$sq (+$n)" else sq, n > 0) { query = sq }
                                }
                            }
                        }
                        if (recentQ.isNotEmpty()) {
                            Quiet("Recent")
                            FlowRow(horizontalArrangement = spacedRow) {
                                recentQ.forEach { rq -> Pill(rq, false) { query = rq } }
                                Pill("Clear", false) { app.murmur.data.Searches.clearRecent() }
                            }
                        }
                    }
                }
            }
            if (kind == ListenKind.BOOKS) item { WhatNext(onOpenItem = { detail = it }, onPlay = onOpen) }
            val shelves = if (kind == ListenKind.BOOKS) discoverShelves() + bookShelves else showShelves
            items(shelves, key = { kind.name + it.title }) { def -> ShelfRow(def, onAll = { seeAll = def }) { detail = it } }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

// ---- shelves and "see all" ---------------------------------------------------------------------------

@Composable
fun ShelfRow(def: ShelfDef, onAll: () -> Unit, onOpen: (Item) -> Unit) {
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
fun ItemTile(item: Item, modifier: Modifier, onClick: () -> Unit) {
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
