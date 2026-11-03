@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.AmbienceKind
import app.murmur.audio.PlayerHub
import app.murmur.audio.Role

private fun Role.label() = when (this) {
    Role.NARRATOR -> "Narrator"
    Role.MALE -> "Male character"
    Role.FEMALE -> "Female character"
}

private fun fmtSpeed(f: Float) = "%.2f".format(f).trimEnd('0').trimEnd('.') + "x"

@Composable
fun PlayerScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val story = PlayerHub.current
    val ctx = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("← Back") }
        if (story == null) {
            Text("Pick a story from the library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Text(story.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${story.author}  ·  ${story.category}  ·  ${story.minutes} min",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            story.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        LinearProgressIndicator(
            progress = { PlayerHub.fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        )
        Text(
            if (PlayerHub.status.isNotBlank()) PlayerHub.status
            else "Part ${(PlayerHub.part + 1).coerceAtMost(PlayerHub.parts.coerceAtLeast(1))} of ${PlayerHub.parts}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Now: ${PlayerHub.nowRole.label()}  ·  ${PlayerHub.nowAmbience.label}  ·  ${fmtSpeed(PlayerHub.nowSpeed)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (PlayerHub.status.startsWith("Preparing for smooth")) {
            Text(
                "This voice generates slower than it plays on this phone, so Murmur prepares a head start " +
                    "to avoid pauses. Piper voices start almost instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = { PlayerHub.startNow() }) { Text("Start now anyway") }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { PlayerHub.previous() }) { Text("Prev") }
            TextButton(onClick = { PlayerHub.seekBy(-15_000) }) { Text("-15s") }
            Button(onClick = { PlayerHub.togglePlay() }) { Text(if (PlayerHub.playing) "Pause" else "Play") }
            TextButton(onClick = { PlayerHub.seekBy(15_000) }) { Text("+15s") }
            TextButton(onClick = { PlayerHub.next() }) { Text("Next") }
        }

        if (PlayerHub.finished) {
            SoftCard(Modifier.padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("The story is finished.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A good moment to stop scrolling for today.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) { Text("Finish for today") }
                }
            }
        }

        SectionTitle("Story Director")
        SoftCard {
            Column(Modifier.padding(14.dp)) {
                val pl = PlayerHub.plan
                Text(
                    pl?.summary() ?: "Reading the story...",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val rv = PlayerHub.roleVoices
                if (rv.isNotEmpty()) {
                    Text(
                        rv.entries.joinToString("  ·  ") { (r, v) -> "${r.label()}: ${v.label}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Character voices")
                        Text(
                            "Dialogue is read by a different voice than the narrator. Takes effect on restart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = App.autoVoice, onCheckedChange = { App.autoVoice = it; App.saveSettings() })
                }
            }
        }

        SectionTitle("Pace")
        FlowRow(horizontalArrangement = spacedRow) {
            FilterChip(
                selected = App.autoSpeed,
                onClick = { App.autoSpeed = true; App.saveSettings() },
                label = { Text("Auto") },
            )
            listOf(0.8f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f).forEach { s ->
                FilterChip(
                    selected = !App.autoSpeed && App.speed == s,
                    onClick = { App.autoSpeed = false; App.speed = s; App.saveSettings() },
                    label = { Text("${s}x") },
                )
            }
        }

        SectionTitle("Sleep timer")
        FlowRow(horizontalArrangement = spacedRow) {
            listOf(0, 10, 20, 30, 60).forEach { m ->
                FilterChip(
                    selected = (m == 0 && PlayerHub.sleepLeftMs == 0L),
                    onClick = { PlayerHub.setSleep(m) },
                    label = { Text(if (m == 0) "Off" else "$m min") },
                )
            }
        }
        if (PlayerHub.sleepLeftMs > 0) {
            Text(
                "Stops in ${formatMs(PlayerHub.sleepLeftMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        SectionTitle("Background sound")
        FlowRow(horizontalArrangement = spacedRow) {
            FilterChip(
                selected = App.autoSound,
                onClick = { App.autoSound = true; App.saveSettings() },
                label = { Text("Auto (by scene)") },
            )
            AmbienceKind.entries.forEach { k ->
                FilterChip(
                    selected = !App.autoSound && App.ambience == k,
                    onClick = { App.autoSound = false; App.ambience = k; App.saveSettings() },
                    label = { Text(k.label) },
                )
            }
        }
        Text("Volume", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Slider(
            value = App.ambienceVolume,
            onValueChange = { App.ambienceVolume = it; PlayerHub.updateAmbience() },
            onValueChangeFinished = { App.saveSettings() },
            valueRange = 0.05f..1f,
        )

        SectionTitle("This story")
        FlowRow(horizontalArrangement = spacedRow) {
            OutlinedButton(onClick = { App.toggleFavorite(story.id) }) {
                Text(if (story.id in App.favorites) "♥ Saved" else "♡ Save")
            }
            OutlinedButton(onClick = { PlayerHub.restart() }) { Text("Restart") }
            OutlinedButton(onClick = {
                App.report(story.id)
                PlayerHub.stopAll()
                Toast.makeText(ctx, "Reported and hidden. Thank you.", Toast.LENGTH_SHORT).show()
                onBack()
            }) { Text("Report") }
            if (story.isUser) {
                OutlinedButton(onClick = {
                    PlayerHub.stopAll()
                    App.deleteUserStory(story.id)
                    onBack()
                }) { Text("Delete") }
            }
        }
        Text(
            "Rights: ${story.rights}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
        )
    }
}
