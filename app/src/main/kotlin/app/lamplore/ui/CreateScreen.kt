@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.ai.AiModels
import app.lamplore.ai.StoryWriter
import app.lamplore.data.Categories
import app.lamplore.data.Rights
import app.lamplore.data.Story
import app.lamplore.data.Templates
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Create: stories made for one listener. Offline templates with a name in them, or a story written by an AI model. */
@Composable
fun CreateScreen(onOpen: (Story) -> Unit) {
    val scope = rememberCoroutineScope()
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember)

    var name by remember { mutableStateOf("") }
    var pronoun by remember { mutableStateOf(Templates.Pronoun.THEY) }
    var animal by remember { mutableStateOf(Templates.animals.first()) }

    var idea by remember { mutableStateOf("") }
    var children by remember { mutableStateOf(true) }
    var minutes by remember { mutableStateOf(4) }
    var writing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(StoryWriter.model) }
    var source by remember { mutableStateOf(StoryWriter.source) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Create", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
        Quiet("A story made for one listener, read aloud in your chosen voice with sounds underneath.", Modifier.padding(top = 6.dp))

        // ---- personalised, offline ------------------------------------------------------
        Heading("A story with their name in it")
        Quiet("Works offline. Four gentle tales, with the name, a pronoun and an animal friend filled in.")
        OutlinedTextField(
            name, { name = it }, placeholder = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Quiet("Call them", Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Templates.Pronoun.entries.forEach { p -> Pill(p.label, pronoun == p) { pronoun = p } }
        }
        Quiet("Animal friend", Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Templates.animals.forEach { a -> Pill(a.replaceFirstChar { it.uppercase() }, animal == a) { animal = a } }
        }
        Templates.tales.forEach { t ->
            val shown = t.title.replace("{name}", name.ifBlank { "..." }).replace("{Animal}", animal.replaceFirstChar { it.uppercase() })
            Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Text(shown, style = MaterialTheme.typography.titleMedium, color = Ink.moth)
                Quiet(t.blurb)
                PrimaryButton(
                    "Make and listen",
                    { onOpen(Templates.fill(t, name, animal, pronoun)) },
                    Modifier.padding(top = 8.dp),
                    enabled = name.isNotBlank(),
                )
            }
        }

        // ---- written by AI ------------------------------------------------------------
        Heading("A new story from an idea")
        Quiet("Describe what you would like, and an AI model writes it. Nothing leaves the phone unless you pick \"My API key\".")
        OutlinedTextField(
            idea, { idea = it }, placeholder = { Text("A sleepy fox who finds a lantern in the snow") }, shape = RoundedCornerShape(14.dp), colors = colors,
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(top = 12.dp),
        )
        Quiet("For", Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Pill("A child", children) { children = true }
            Pill("An adult", !children) { children = false }
        }
        Quiet("Length", Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            listOf(2, 4, 8).forEach { m -> Pill("$m min", minutes == m) { minutes = m } }
        }
        Quiet("Written by", Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Pill("My API key", source == StoryWriter.Source.CLOUD) { source = StoryWriter.Source.CLOUD; StoryWriter.source = source }
            Pill("This phone", source == StoryWriter.Source.LOCAL) { source = StoryWriter.Source.LOCAL; StoryWriter.source = source }
        }

        if (source == StoryWriter.Source.CLOUD) {
            Quiet(
                "Your own Anthropic API key. It is stored encrypted on this phone and only sent to api.anthropic.com. Each story costs a few cents.",
                Modifier.padding(top = 10.dp),
            )
            OutlinedTextField(
                key, { key = it },
                placeholder = { Text(if (StoryWriter.hasKey) "Key saved. Paste a new one to replace it." else "sk-ant-...") },
                singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                model, { model = it }, label = { Text("Model") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Save key and model", {
                    if (key.isNotBlank()) { StoryWriter.apiKey = key; key = "" }
                    StoryWriter.model = model
                })
                if (StoryWriter.hasKey) GhostButton("Forget key", { StoryWriter.apiKey = ""; key = "" })
            }
        } else {
            Quiet(
                "Runs a small model on this phone, offline. Honest warning: these models are far weaker than the cloud ones, so stories are plainer and sometimes odd. Expect a minute or two.",
                Modifier.padding(top = 10.dp),
            )
            AiModels.all.forEach { m ->
                val pr = AiModels.progress[m.id]
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title, color = Ink.moth)
                        Quiet(
                            when {
                                AiModels.isInstalled(m) -> "Installed, ${m.sizeMb} MB"
                                pr != null -> "Downloading ${(pr * 100).toInt()}%"
                                else -> "${m.sizeMb} MB download"
                            },
                        )
                    }
                    if (!AiModels.isInstalled(m) && pr == null) GhostButton("Download", { AiModels.download(m) })
                    else if (pr != null) GhostButton("Cancel", { AiModels.cancel(m) })
                }
                AiModels.errors[m.id]?.let { Quiet(it) }
            }
        }

        PrimaryButton(
            if (writing) "Writing..." else "Write the story",
            {
                error = null
                writing = true
                title = ""; body = ""
                job = scope.launch {
                    StoryWriter.write(StoryWriter.Request(idea, "", children, minutes))
                        .onSuccess { title = it.title; body = it.body }
                        .onFailure { error = it.message ?: "Something went wrong." }
                    writing = false
                }
            },
            Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = idea.isNotBlank() && !writing,
        )
        if (writing) GhostButton("Cancel", { job?.cancel(); writing = false }, Modifier.padding(top = 6.dp))
        error?.let { Quiet(it, Modifier.padding(top = 8.dp)) }

        if (body.isNotBlank()) {
            Heading("Your story")
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 240.dp))
            Quiet("You can edit it before saving. Stories written by an AI model are marked as such.", Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 10.dp, bottom = 40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Save and listen", {
                    val s = App.addUserStory(title.ifBlank { "A bedtime story" }, "an AI model", Categories.YOURS, body, Rights.GENERATED)
                    body = ""; title = ""
                    onOpen(s)
                })
                GhostButton("Discard", { body = ""; title = "" })
            }
        } else {
            Quiet("", Modifier.padding(bottom = 40.dp))
        }
    }
}
