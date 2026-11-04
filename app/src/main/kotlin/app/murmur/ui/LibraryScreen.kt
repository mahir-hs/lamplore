@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import app.murmur.data.RemoteContent

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
fun LibraryScreen(onOpen: (Story) -> Unit, onAdd: () -> Unit) {
    var query by remember { mutableStateOf("") }
    // length filter: bedtime stories are chosen by how long you have
    var length by remember { mutableStateOf<Int?>(null) }
    val all = App.visibleStories.filter { s ->
        when (length) {
            0 -> s.minutes < 8
            1 -> s.minutes in 8..12
            2 -> s.minutes > 12
            else -> true
        }
    }
    val results = if (query.isBlank()) emptyList() else all.filter {
        it.title.contains(query, true) || it.author.contains(query, true) || it.summary.contains(query, true)
    }

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Stories", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
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
                FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf("Under 8 min", "8 to 12 min", "Over 12 min").forEachIndexed { i, label ->
                        Pill(label, length == i) { length = if (length == i) null else i }
                    }
                }
                val online = RemoteContent.status
                if (online.isNotBlank() && !online.startsWith("No online library")) Quiet(online, Modifier.padding(top = 2.dp))
            }
        }
        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                item { Quiet("No story matches \"$query\". Try a shorter word, or add your own story.", Modifier.padding(22.dp)) }
            }
            items(results, key = { it.id }) { s -> StoryRow(s) { onOpen(s) } }
        } else {
            if (length != null && all.isEmpty()) {
                item { Quiet("No stories this length yet. Try another length.", Modifier.padding(20.dp)) }
            }
            item { Shelf("Saved", all.filter { it.id in App.favorites }, onOpen) }
            item { Shelf("Continue", all.filter { App.hasProgress(it.id) && it.id !in App.completed }, onOpen) }
            Categories.all.forEach { c ->
                item { Shelf(c, all.filter { it.category == c }, onOpen) }
            }
            item { Shelf("Finished", all.filter { it.id in App.completed }, onOpen) }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    if (all.none { it.isUser }) {
                        Quiet("Your own stories appear on a shelf called Yours. Paste one in and Murmur reads it aloud.")
                    }
                    GhostButton("Add your own story", onAdd, Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}
