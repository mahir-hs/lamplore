package app.murmur.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
    val all = App.visibleStories
    val results = if (query.isBlank()) emptyList() else all.filter {
        it.title.contains(query, true) || it.author.contains(query, true) || it.summary.contains(query, true)
    }

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Library", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by title or author") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink.dusk) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink.ember,
                        unfocusedBorderColor = Ink.line,
                        cursorColor = Ink.ember,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
        }
        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                item { Quiet("No story matches \"$query\". Try a shorter word, or add your own under Add.", Modifier.padding(22.dp)) }
            }
            items(results, key = { it.id }) { s -> StoryRow(s) { onOpen(s) } }
        } else {
            item { Shelf("Saved", all.filter { it.id in App.favorites }, onOpen) }
            item { Shelf("Continue", all.filter { App.hasProgress(it.id) && it.id !in App.completed }, onOpen) }
            Categories.all.forEach { c ->
                item { Shelf(c, all.filter { it.category == c }, onOpen) }
            }
            item { Shelf("Finished", all.filter { it.id in App.completed }, onOpen) }
            if (all.none { it.isUser }) {
                item {
                    Quiet(
                        "Your own stories will appear on a shelf called Yours. Paste one under Add and Murmur reads it aloud.",
                        Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}
