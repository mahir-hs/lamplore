@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import app.murmur.audio.PackManager
import app.murmur.audio.PlayerHub
import app.murmur.audio.VoiceInfo
import app.murmur.audio.VoicePack
import app.murmur.audio.VoicePacks

@Composable
fun SettingsScreen() {
    var voices by remember { mutableStateOf<List<VoiceInfo>>(emptyList()) }
    var resolved by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var bytes by remember { mutableLongStateOf(App.audioBytes()) }

    LaunchedEffect(PackManager.installed.size, App.voiceName) {
        voices = PlayerHub.engine.voices()
        resolved = PlayerHub.engine.resolveVoice(App.voiceName)
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))

        SectionTitle("Voice")
        val current = voices.firstOrNull { it.name == (App.voiceName.ifBlank { resolved }) }
            ?: voices.firstOrNull { it.name == resolved }
        Box {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(current?.label ?: if (voices.isEmpty()) "Loading voices..." else "Default voice")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                voices.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v.label + if (v.offline) "" else "  (online)") },
                        onClick = {
                            App.voiceName = v.name
                            App.saveSettings()
                            menu = false
                        },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { PlayerHub.preview() }) { Text("Hear this voice") }
            if (PlayerHub.previewStatus.isNotBlank()) {
                Text(
                    "  " + PlayerHub.previewStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text("Pitch  ${"%.2f".format(App.pitch)}", modifier = Modifier.padding(top = 12.dp))
        Slider(
            value = App.pitch,
            onValueChange = { App.pitch = it },
            onValueChangeFinished = { App.saveSettings() },
            valueRange = 0.8f..1.2f,
        )
        Text("Speed  ${"%.2f".format(App.speed)}x" + if (App.autoSpeed) "  (Director sets pace; moving this takes over)" else "")
        Slider(
            value = App.speed,
            onValueChange = { App.speed = it },
            onValueChangeFinished = { App.autoSpeed = false; App.saveSettings() },
            valueRange = 0.7f..1.6f,
        )

        SectionTitle("Better voices (download once)")
        Text(
            "Natural-sounding voices that run entirely on your phone, with no internet needed after the download. " +
                "Kokoro sounds best; Piper voices are smaller and faster.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VoicePacks.all.forEach { pack -> PackCard(pack) }

        SectionTitle("Story Director")
        Text(
            "Murmur reads each story first and decides who speaks, how fast, and which background sound " +
                "fits each scene. Turn any of these off to use your own fixed choice instead.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DirectorSwitch("Character voices", "Dialogue gets a different voice than the narrator, matched by gender.", App.autoVoice) { App.autoVoice = it; App.saveSettings() }
        DirectorSwitch("Pace", "Slower for calm and closing lines, quicker for action.", App.autoSpeed) { App.autoSpeed = it; App.saveSettings() }
        DirectorSwitch("Background sound", "Rain, forest, night, fire and more, chosen from the scene and crossfaded.", App.autoSound) { App.autoSound = it; App.saveSettings() }

        SectionTitle("Background sound")
        Text(
            if (App.autoSound) "Director is choosing. Pick one below to override." else "Fixed sound for every story.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 6.dp)) {
            FilterChip(
                selected = App.autoSound,
                onClick = { App.autoSound = true; App.saveSettings() },
                label = { Text("Auto") },
            )
            AmbienceKind.entries.forEach { k ->
                FilterChip(
                    selected = !App.autoSound && App.ambience == k,
                    onClick = { App.autoSound = false; App.ambience = k; App.saveSettings() },
                    label = { Text(k.label) },
                )
            }
        }
        Text("Volume", modifier = Modifier.padding(top = 8.dp))
        Slider(
            value = App.ambienceVolume,
            onValueChange = { App.ambienceVolume = it; PlayerHub.updateAmbience() },
            onValueChangeFinished = { App.saveSettings() },
            valueRange = 0.05f..1f,
        )
        Text(
            "Nature-style sounds only. No music.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Reminders & tone")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Islamic touches (optional)")
                Text(
                    "Off by default. Adds an Islamic greeting and a remembrance line on the Home and Pause screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = App.faithTouches, onCheckedChange = { App.faithTouches = it; App.saveSettings() })
        }

        SectionTitle("Storage")
        Text("Generated story audio: ${"%.1f".format(bytes / 1_048_576.0)} MB")
        Text("Downloaded voices: ${"%.1f".format(PackManager.diskBytes() / 1_048_576.0)} MB")
        Text(
            "Story audio is created from the text on your phone and can be rebuilt any time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { App.clearAudio(); bytes = App.audioBytes() },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Delete generated story audio") }

        SectionTitle("About")
        Text(
            "Murmur 0.2.0. Public-domain tales and original stories. The Quranic narrative retellings are " +
                "simplified and should be reviewed by a qualified scholar before any public release. " +
                "Voices: Piper and Kokoro via sherpa-onnx.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp),
        )
    }
}

@Composable
private fun DirectorSwitch(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PackCard(pack: VoicePack) {
    val installed = pack.id in PackManager.installed
    val prog = PackManager.progress[pack.id]
    val err = PackManager.errors[pack.id]
    SoftCard(Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pack.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "~${pack.sizeMb} MB",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                pack.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            when {
                prog != null && prog < 1.5f -> {
                    LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Downloading ${(prog * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { PackManager.cancel(pack) }) { Text("Cancel") }
                    }
                }
                prog != null -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Text("Unpacking...", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
                installed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✓ Installed", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { PackManager.delete(pack) }) { Text("Remove") }
                    }
                }
                else -> {
                    Button(onClick = { PackManager.download(pack) }, modifier = Modifier.padding(top = 8.dp)) { Text("Download") }
                }
            }
            if (err != null) {
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
