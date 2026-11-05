@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lamplore.audio.Mixer

/** Sounds: a stand-alone mixer for the bundled field recordings and music beds, with a sleep timer. */
@Composable
fun MixerScreen() {
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val active = Mixer.levels.values.count { it > 0f }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Sounds", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
        Quiet("Rain, fire, ocean and quiet music, mixed to taste. Works offline and keeps playing with the screen off.", Modifier.padding(top = 6.dp))

        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                if (Mixer.playing) "Pause" else "Play",
                { Mixer.toggle() },
                Modifier.weight(1f),
                enabled = active > 0,
            )
            if (active > 0) GhostButton("Clear", { Mixer.clear() })
        }
        if (Mixer.timerLeft >= 0) {
            val m = Mixer.timerLeft / 60
            val s = Mixer.timerLeft % 60
            Quiet("Fades out in $m:${"%02d".format(s)}", Modifier.padding(top = 8.dp))
        } else if (active == 0) {
            Quiet("Move a slider to start.", Modifier.padding(top = 8.dp))
        }

        Heading("Sleep timer")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (m, l) ->
                Pill(l, Mixer.timerMinutes == m) { Mixer.setTimer(m) }
            }
        }

        if (Mixer.presets.isNotEmpty()) {
            Heading("Your mixes")
            FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
                Mixer.presets.forEach { p -> Pill(p.name, false) { Mixer.load(p) } }
            }
        }

        Heading("Nature")
        Mixer.channels.filter { !it.music }.forEach { ChannelRow(it) }
        Heading("Music")
        Mixer.channels.filter { it.music }.forEach { ChannelRow(it) }

        Heading("Save this mix")
        if (!naming) {
            GhostButton("Save as a mix", { naming = true }, Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(
                name, { name = it }, placeholder = { Text("Name, e.g. Rainy cabin") }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Save", { Mixer.savePreset(name); name = ""; naming = false }, enabled = name.isNotBlank() && active > 0)
                GhostButton("Cancel", { naming = false })
            }
        }
        Mixer.presets.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, Modifier.weight(1f), color = Ink.moth)
                TextButton(onClick = { Mixer.deletePreset(p) }) { Text("Delete", color = Ink.dusk) }
            }
        }
        Quiet("Sound credits are in About. Nothing here uses the internet.", Modifier.padding(top = 18.dp, bottom = 36.dp))
    }
}

@Composable
private fun ChannelRow(c: Mixer.Channel) {
    val v = Mixer.levels[c.file] ?: 0f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(c.label, Modifier.width(128.dp), color = if (v > 0f) Ink.moth else Ink.dusk, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = v, onValueChange = { Mixer.setLevel(c.file, it) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.line),
        )
    }
}
