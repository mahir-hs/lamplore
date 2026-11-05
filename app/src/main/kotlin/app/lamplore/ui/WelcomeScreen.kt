package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.audio.PackManager
import app.lamplore.audio.PlayerHub
import app.lamplore.audio.VoicePack
import app.lamplore.audio.VoicePacks

private val firstRunPacks = listOf(
    "vits-piper-en_US-lessac-medium-int8",
    "vits-piper-en_US-ryan-medium-int8",
    "vits-piper-en_US-amy-medium-int8",
)

/**
 * First run: the phone's built-in voice is the weakest, so the first thing Lamplore offers is a better one.
 * Each voice is about 20 MB, downloaded once; after that it works without internet.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val packs = firstRunPacks.mapNotNull { VoicePacks.byId(it) }
    val anyInstalled = packs.any { it.id in PackManager.installed }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Lamplore reads stories aloud.", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 56.dp))
        Quiet(
            "Pick a voice for the narrator. It downloads once, about 20 MB, then works without internet.",
            Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        packs.forEach { VoiceChoice(it, onDone) }
        if (anyInstalled) {
            PrimaryButton("Continue", onDone, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
        TextButton(onClick = onDone, modifier = Modifier.padding(top = 8.dp, bottom = 36.dp)) {
            Text("Use the phone's voice for now", color = Ink.dusk)
        }
    }
}

@Composable
private fun VoiceChoice(pack: VoicePack, onDone: () -> Unit) {
    val voice = "sherpa:${pack.id}:0"
    val installed = pack.id in PackManager.installed
    val prog = PackManager.progress[pack.id]
    val err = PackManager.errors[pack.id]
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(pack.title.removePrefix("Piper - "), style = MaterialTheme.typography.titleLarge)
        Quiet(pack.desc.removeSuffix(" Fast and light."), Modifier.padding(top = 2.dp))
        when {
            prog != null && prog < 1.5f -> {
                LinearProgressIndicator(progress = { prog }, color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Quiet("Downloading, ${(prog * 100).toInt()}%", Modifier.weight(1f))
                    TextButton(onClick = { PackManager.cancel(pack) }) { Text("Cancel", color = Ink.dusk) }
                }
            }
            prog != null -> {
                LinearProgressIndicator(color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Quiet("Getting it ready", Modifier.padding(top = 4.dp))
            }
            installed -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    GhostButton("Hear a sample", { PlayerHub.preview(voice) }, Modifier.weight(1f))
                    PrimaryButton("Use this voice", { App.voiceName = voice; App.saveSettings(); onDone() }, Modifier.weight(1f))
                }
                if (PlayerHub.previewStatus.isNotBlank()) Quiet(PlayerHub.previewStatus, Modifier.padding(top = 4.dp))
            }
            else -> GhostButton("Download, ${pack.sizeMb} MB", { PackManager.download(pack) }, Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        if (err != null) {
            Text("$err. Check your connection and try again.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
