@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun AddStoryScreen(onSaved: (Story) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Categories.YOURS) }
    var rights by remember { mutableStateOf<String?>(null) }
    val ok = title.isNotBlank() && body.trim().length >= 100 && rights != null
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Ink.ember,
        unfocusedBorderColor = Ink.line,
        cursorColor = Ink.ember,
    )

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 6.dp)) { Text("Back", color = Ink.dusk) }
        Text("Add a story", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 10.dp))
        Quiet("Paste or write a story. Murmur reads it aloud with the voices you've chosen, and keeps it on this phone.", Modifier.padding(top = 6.dp))

        OutlinedTextField(title, { title = it }, placeholder = { Text("Title") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
        OutlinedTextField(author, { author = it }, placeholder = { Text("Author, if not you") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        OutlinedTextField(
            body, { body = it },
            placeholder = { Text("Once upon a time...") },
            shape = RoundedCornerShape(14.dp),
            colors = colors,
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(top = 10.dp),
            supportingText = { Text("${body.trim().length} characters, at least 100", color = Ink.dusk) },
        )

        Heading("Shelf")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Categories.forUpload.forEach { c -> Pill(c, cat == c) { cat = c } }
        }

        Heading("Who owns it?")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Rights.userChoices.forEach { r -> Pill(r, rights == r) { rights = r } }
        }
        Quiet("Only add stories you have the right to share.", Modifier.padding(top = 8.dp))

        PrimaryButton(
            "Save and listen",
            {
                val s = App.addUserStory(title, author, cat, body, rights!!)
                title = ""; author = ""; body = ""; rights = null
                onSaved(s)
            },
            Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 36.dp),
            enabled = ok,
        )
    }
}
