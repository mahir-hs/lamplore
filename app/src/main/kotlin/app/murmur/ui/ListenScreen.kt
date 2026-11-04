@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.murmur.data.Listen
import app.murmur.data.Show
import app.murmur.data.Story
import kotlinx.coroutines.delay

private enum class Kind { BOOKS, PODCASTS }

/**
 * Free recorded audio: public-domain audiobooks (LibriVox) and podcasts, streamed on demand.
 * Both are looked up live, so nothing is bundled and nothing is re-hosted.
 */
@Composable
fun ListenScreen(onOpen: (Story) -> Unit) {
    var kind by remember { mutableStateOf(Kind.BOOKS) }
    var query by remember { mutableStateOf("") }
    var books by remember { mutableStateOf<List<Story>>(emptyList()) }
    var shows by remember { mutableStateOf<List<Show>>(emptyList()) }
    var openShow by remember { mutableStateOf<Show?>(null) }
    var episodes by remember { mutableStateOf<List<Story>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf("") }

    // typing waits a moment, then searches; a chip sets the query the same way
    LaunchedEffect(query, kind) {
        val q = query.trim()
        if (q.length < 2) { books = emptyList(); shows = emptyList(); searched = ""; loading = false; return@LaunchedEffect }
        delay(450)
        loading = true
        if (kind == Kind.BOOKS) books = Listen.audiobooks(q) else shows = Listen.shows(q)
        searched = q
        loading = false
    }
    LaunchedEffect(openShow) {
        val s = openShow ?: return@LaunchedEffect
        episodes = emptyList()
        loading = true
        episodes = Listen.episodes(s)
        loading = false
    }

    val show = openShow
    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Listen", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
                Quiet("Free audiobooks read by volunteers, and podcasts. Streamed, nothing to download.", Modifier.padding(top = 4.dp))
                if (show == null) {
                    FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 10.dp)) {
                        Pill("Audiobooks", kind == Kind.BOOKS) { kind = Kind.BOOKS }
                        Pill("Podcasts", kind == Kind.PODCASTS) { kind = Kind.PODCASTS }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(if (kind == Kind.BOOKS) "Title or author, e.g. Alice, Grimm" else "Search podcasts") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink.dusk) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                    val ideas = if (kind == Kind.BOOKS) listOf("Fairy", "Fables", "Tales", "Stories", "Alice", "Grimm", "Andersen", "Aesop", "Sherlock")
                    else listOf("Sleep stories", "Bedtime stories", "Meditation", "Calm", "Fairy tales", "Short stories", "History")
                    FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 2.dp)) {
                        ideas.forEach { Pill(it, query.equals(it, true)) { query = it } }
                    }
                } else {
                    GhostButton("Back to results", { openShow = null }, Modifier.padding(top = 14.dp))
                    Text(show.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp))
                    Quiet(show.author)
                }
                if (loading) Quiet("Looking...", Modifier.padding(top = 12.dp))
            }
        }

        if (show != null) {
            if (!loading && episodes.isEmpty()) item { Quiet("No episodes could be loaded from this show's feed.", Modifier.padding(22.dp)) }
            items(episodes, key = { it.id }) { s -> StreamRow(s) { onOpen(s) } }
        } else if (query.trim().length < 2) {
            val mine = Listen.recent
            if (mine.isNotEmpty()) {
                item { Heading("Recently played", Modifier.padding(horizontal = 20.dp)) }
                items(mine, key = { "r_" + it.id }) { s -> StreamRow(s) { onOpen(s) } }
            } else {
                item { Quiet("Pick an idea above or search. Whatever you play is kept here so you can carry on later.", Modifier.padding(22.dp)) }
            }
        } else if (kind == Kind.BOOKS) {
            if (!loading && searched == query.trim() && books.isEmpty()) {
                item { Quiet("No audiobook starts with \"${query.trim()}\". LibriVox searches from the start of a title or an author's last name.", Modifier.padding(22.dp)) }
            }
            items(books, key = { it.id }) { s -> StreamRow(s) { onOpen(s) } }
        } else {
            if (!loading && searched == query.trim() && shows.isEmpty()) {
                item { Quiet("No podcast found. Check your connection or try another word.", Modifier.padding(22.dp)) }
            }
            items(shows, key = { it.feedUrl }) { sh ->
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxWidth().bounceClick("Open ${sh.name}") { openShow = sh }.padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text(sh.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Quiet(sh.author)
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun StreamRow(s: Story, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().bounceClick("Play ${s.title}", onClick).padding(horizontal = 22.dp, vertical = 12.dp)) {
        Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        val len = if (s.minutes > 1 && s.tracks.sumOf { it.seconds } > 0) {
            val m = s.minutes
            (if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min") + (if (s.tracks.size > 1) ", ${s.tracks.size} parts" else "")
        } else if (s.tracks.size > 1) "${s.tracks.size} parts" else ""
        Quiet(listOf(s.author, len).filter { it.isNotBlank() }.joinToString("  ·  "))
        if (s.summary.isNotBlank()) Quiet(s.summary, Modifier.padding(top = 2.dp))
    }
}
