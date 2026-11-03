@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import app.murmur.data.Rights
import app.murmur.data.Story

@Composable
fun AddStoryScreen(onSaved: (Story) -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Categories.YOURS) }
    var rights by remember { mutableStateOf<String?>(null) }
    val ok = title.isNotBlank() && body.trim().length >= 100 && rights != null

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Add a story", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
        Text(
            "Write or paste a story and Murmur will read it aloud in the voice you choose. It stays on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            author, { author = it }, label = { Text("Author (optional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        SectionTitle("Shelf")
        FlowRow(horizontalArrangement = spacedRow) {
            Categories.forUpload.forEach { c ->
                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
            }
        }
        OutlinedTextField(
            body, { body = it },
            label = { Text("Story text") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(top = 12.dp),
            supportingText = { Text("${body.trim().length} characters (at least 100)") },
        )
        SectionTitle("Rights")
        Text(
            "Only add stories you have the right to share.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
            Rights.userChoices.forEach { r ->
                FilterChip(selected = rights == r, onClick = { rights = r }, label = { Text(r) })
            }
        }
        Button(
            onClick = {
                val s = App.addUserStory(title, author, cat, body, rights!!)
                title = ""; author = ""; body = ""; rights = null
                onSaved(s)
            },
            enabled = ok,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
        ) { Text("Save and listen") }
    }
}
