@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.AmbienceKind
import app.murmur.audio.MusicMood
import app.murmur.audio.PackManager
import app.murmur.audio.PlayerHub
import app.murmur.audio.VoiceInfo
import app.murmur.audio.VoicePack
import app.murmur.audio.VoicePacks

@Composable
fun SettingsScreen(onAbout: () -> Unit) {
    var voices by remember { mutableStateOf<List<VoiceInfo>>(emptyList()) }
    var resolved by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var bytes by remember { mutableLongStateOf(App.audioBytes()) }
    val sliderColors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.lampHigh)

    LaunchedEffect(PackManager.installed.size, App.voiceName) {
        voices = PlayerHub.engine.voices()
        resolved = PlayerHub.engine.resolveVoice(App.voiceName)
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("You", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
        val nights = App.streak()
        Quiet(
            "${App.todaySeconds / 60} minutes today, $nights night${if (nights == 1) "" else "s"} in a row.",
            Modifier.padding(top = 6.dp),
        )

        Heading("Narrator")
        val current = voices.firstOrNull { it.name == (App.voiceName.ifBlank { resolved }) } ?: voices.firstOrNull { it.name == resolved }
        Box {
            GhostButton(current?.label ?: if (voices.isEmpty()) "Loading voices" else "Default voice", { menu = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                voices.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v.label + if (v.offline) "" else " (needs internet)") },
                        onClick = { App.voiceName = v.name; App.saveSettings(); menu = false },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { PlayerHub.preview() }) { Text("Hear this voice", color = Ink.ember) }
            if (PlayerHub.previewStatus.isNotBlank()) Quiet(PlayerHub.previewStatus)
        }
        Quiet("Pitch")
        Slider(value = App.pitch, onValueChange = { App.pitch = it }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.8f..1.2f, colors = sliderColors)

        Heading("Get more voices")
        Quiet("Natural voices that run on this phone, offline after a one-time download. Kokoro sounds best; Piper voices are small and quick. With a few installed, the Director gives dialogue its own voices.")
        Spacer(Modifier.height(8.dp))
        VoicePacks.all.forEach { pack -> PackRow(pack) }

        Heading("Story Director")
        Quiet("Murmur reads each story first and decides who speaks, how fast, and which background sound fits the scene. Turn any of these off to keep one fixed choice.")
        DirectorSwitch("Character voices", "Dialogue gets a different voice than the narrator, matched by gender.", App.autoVoice) { App.autoVoice = it; App.saveSettings() }
        DirectorSwitch("Pace", "Slower for calm and closing lines, quicker for action.", App.autoSpeed) { App.autoSpeed = it; App.saveSettings() }
        DirectorSwitch("Background sound", "Real recordings of rain, forest, night, fire and more, chosen from the scene and crossfaded.", App.autoSound) { App.autoSound = it; App.saveSettings() }
        DirectorSwitch("Music", "A quiet instrumental bed matched to the story's mood; tense scenes get the tension bed.", App.autoMusic) { App.autoMusic = it; App.saveSettings() }

        if (!App.autoSpeed) {
            Heading("Fixed pace")
            Quiet("${"%.2f".format(App.speed)}×")
            Slider(value = App.speed, onValueChange = { App.speed = it }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.7f..1.6f, colors = sliderColors)
        }
        if (!App.autoSound) {
            Heading("Fixed background sound")
            FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
                AmbienceKind.entries.forEach { k -> Pill(k.label, App.ambience == k) { App.ambience = k; App.saveSettings() } }
            }
        }
        if (!App.autoMusic) {
            Heading("Fixed music")
            FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
                MusicMood.entries.forEach { m -> Pill(m.label, App.music == m) { App.music = m; App.saveSettings() } }
            }
        }
        Heading("Background volume")
        Slider(value = App.ambienceVolume, onValueChange = { App.ambienceVolume = it; PlayerHub.updateAmbience() }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.05f..1f, colors = sliderColors)
        Heading("Music volume")
        Slider(value = App.musicVolume, onValueChange = { App.musicVolume = it; PlayerHub.updateAmbience() }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0f..0.8f, colors = sliderColors)

        GoalBlock()
        if (App.reflections.isNotEmpty()) {
            Heading("Your reflections")
            App.reflections.take(5).forEach { r ->
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(r.text, style = MaterialTheme.typography.bodyLarge)
                    Quiet(r.date + (r.storyTitle?.let { ", after $it" } ?: ""))
                }
            }
        }

        Heading("Online library")
        var picBytes by remember { mutableLongStateOf(app.murmur.data.RemoteContent.cacheBytes()) }
        Quiet(app.murmur.data.RemoteContent.status.ifBlank { "New books and illustrations are fetched from the internet, never bundled." })
        Quiet("Pictures saved on this phone: ${"%.1f".format(picBytes / 1_048_576.0)} MB. Each is downloaded once, when you first open its story.", Modifier.padding(top = 4.dp))
        Row {
            TextButton(onClick = { app.murmur.data.RemoteContent.refresh() }) { Text("Check for new books", color = Ink.ember) }
            TextButton(onClick = { app.murmur.data.RemoteContent.clearCache(); picBytes = 0L }) { Text("Clear pictures", color = Ink.ember) }
        }

        Heading("Storage")
        Quiet("Story audio on this phone: ${"%.1f".format(bytes / 1_048_576.0)} MB. Downloaded voices: ${"%.0f".format(PackManager.diskBytes() / 1_048_576.0)} MB.")
        Quiet("Story audio is rebuilt from the text whenever you need it, so it's safe to clear.")
        TextButton(onClick = { App.clearAudio(); bytes = App.audioBytes() }) { Text("Clear story audio", color = Ink.ember) }

        Heading("About")
        GhostButton("About and credits", onAbout, Modifier.fillMaxWidth().padding(top = 4.dp))
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun DirectorSwitch(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Quiet(desc)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PackRow(pack: VoicePack) {
    val installed = pack.id in PackManager.installed
    val prog = PackManager.progress[pack.id]
    val err = PackManager.errors[pack.id]
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pack.title, style = MaterialTheme.typography.titleMedium)
                Quiet("${pack.desc} About ${pack.sizeMb} MB.")
            }
            when {
                prog != null && prog < 1.5f -> TextButton(onClick = { PackManager.cancel(pack) }) { Text("Cancel", color = Ink.dusk) }
                prog != null -> Quiet("Unpacking")
                installed -> TextButton(onClick = { PackManager.delete(pack) }) { Text("Remove", color = Ink.dusk) }
                else -> TextButton(onClick = { PackManager.download(pack) }) { Text("Download", color = Ink.ember) }
            }
        }
        if (prog != null && prog < 1.5f) {
            LinearProgressIndicator(progress = { prog }, color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        } else if (prog != null) {
            LinearProgressIndicator(color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        if (installed && prog == null) Text("Installed", style = MaterialTheme.typography.labelSmall, color = Ink.moss)
        if (err != null) Text(err, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
}
