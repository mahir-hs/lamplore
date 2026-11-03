@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.data.Categories
import app.murmur.data.Story

@Composable
fun LibraryScreen(onOpen: (Story) -> Unit) {
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var favOnly by remember { mutableStateOf(false) }

    val list = App.visibleStories
        .filter { cat == null || it.category == cat }
        .filter { !favOnly || it.id in App.favorites }
        .filter {
            query.isBlank() || it.title.contains(query, true) || it.author.contains(query, true) ||
                it.summary.contains(query, true)
        }

    LazyColumn(Modifier.padding(horizontal = 20.dp)) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search stories") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                FilterChip(selected = cat == null && !favOnly, onClick = { cat = null; favOnly = false }, label = { Text("All") })
                Categories.all.forEach { c ->
                    FilterChip(selected = cat == c, onClick = { cat = if (cat == c) null else c }, label = { Text(c) })
                }
                FilterChip(selected = favOnly, onClick = { favOnly = !favOnly }, label = { Text("♥ Saved") })
            }
        }
        if (list.isEmpty()) {
            item {
                Text(
                    "Nothing here yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        items(list, key = { it.id }) { s -> StoryRow(s) { onOpen(s) } }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp)) }
    }
}
