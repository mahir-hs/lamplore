@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
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

private fun fmtSpeed(f: Float) = "%.2f".format(f).trimEnd('0').trimEnd('.') + "×"

fun sceneColor(k: AmbienceKind): Color = when (k) {
    AmbienceKind.RAIN -> Color(0xFF5B7C99)
    AmbienceKind.STORM -> Color(0xFF40506B)
    AmbienceKind.WIND -> Color(0xFF8FA3A8)
    AmbienceKind.OCEAN -> Color(0xFF3E6E8E)
    AmbienceKind.STREAM -> Color(0xFF5F9EA0)
    AmbienceKind.FOREST -> Color(0xFF4F7F5A)
    AmbienceKind.NIGHT -> Color(0xFF3A3570)
    AmbienceKind.FIRE -> Color(0xFFC8743A)
    AmbienceKind.DESERT -> Color(0xFFC9A16B)
    AmbienceKind.CAVE -> Color(0xFF5A4F55)
    AmbienceKind.HEARTBEAT -> Color(0xFFA0404A)
    AmbienceKind.CALM -> Color(0xFF6F7F8F)
    AmbienceKind.NONE -> Ink.line
}

@Composable
fun PlayerScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val story = PlayerHub.current
    val ctx = LocalContext.current
    var tune by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 10.dp, top = 6.dp)) {
            Text("Back", color = Ink.dusk)
        }
        if (story == null) {
            Quiet("Pick a story from the library.", Modifier.padding(22.dp))
            return@Column
        }

        // Cover with a soft tinted glow behind it
        Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.1f).background(
                    Brush.radialGradient(
                        0f to coverTint(story).copy(alpha = 0.75f),
                        0.55f to coverTint(story).copy(alpha = 0.2f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            Cover(story, Modifier.width(230.dp).aspectRatio(0.78f), corner = 22.dp)
        }

        Column(Modifier.padding(horizontal = 26.dp)) {
            Text(
                story.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            )
            Quiet(story.meta(), Modifier.fillMaxWidth().padding(top = 4.dp).also { }, )

            // Scrubber over the whole story
            Scrubber(PlayerHub.fraction, Modifier.fillMaxWidth().padding(top = 18.dp)) { PlayerHub.seekToFraction(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs((PlayerHub.positionSeconds * 1000).toLong()), style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
                Text(formatMs((PlayerHub.totalSeconds * 1000).toLong()), style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
            }
            SceneStrip()
            Text(
                PlayerHub.status.ifBlank { nowLine() },
                style = MaterialTheme.typography.labelMedium,
                color = if (PlayerHub.status.isBlank()) Ink.dusk else Ink.ember,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (PlayerHub.status.startsWith("Preparing for smooth")) {
                Quiet(
                    "This voice is slower than real time on this phone, so Murmur builds a head start first.",
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                TextButton(onClick = { PlayerHub.startNow() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Start now anyway", color = Ink.ember)
                }
            }

            // Transport
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { PlayerHub.previous() }) { Text("Prev", color = Ink.dusk) }
                TextButton(onClick = { PlayerHub.seekBy(-15_000) }) { Text("15s", color = Ink.moth) }
                Box(
                    Modifier.size(76.dp).background(Ink.ember, CircleShape).clickable { PlayerHub.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (PlayerHub.playing) PauseGlyph() else Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Ink.onEmber, modifier = Modifier.size(40.dp))
                }
                TextButton(onClick = { PlayerHub.seekBy(15_000) }) { Text("15s", color = Ink.moth) }
                TextButton(onClick = { PlayerHub.next() }) { Text("Next", color = Ink.dusk) }
            }

            if (PlayerHub.finished) {
                Column(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("The end.", style = MaterialTheme.typography.headlineSmall)
                    Quiet("A good moment to put the phone down.", Modifier.padding(top = 4.dp))
                    PrimaryButton("Finish tonight", onDone, Modifier.fillMaxWidth().padding(top = 14.dp))
                }
            }

            // Quiet actions
            FlowRow(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 36.dp), horizontalArrangement = Arrangement.Center) {
                Pill("Tune", false) { tune = true }
                Spacer(Modifier.width(8.dp))
                Pill(if (story.id in App.favorites) "Saved" else "Save", story.id in App.favorites) { App.toggleFavorite(story.id) }
                Spacer(Modifier.width(8.dp))
                Pill("Restart", false) { PlayerHub.restart() }
                Spacer(Modifier.width(8.dp))
                Pill("Report", false) {
                    App.report(story.id)
                    PlayerHub.stopAll()
                    Toast.makeText(ctx, "Reported and hidden.", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                if (story.isUser) {
                    Spacer(Modifier.width(8.dp))
                    Pill("Delete", false) {
                        PlayerHub.stopAll()
                        App.deleteUserStory(story.id)
                        onBack()
                    }
                }
            }
        }
    }

    if (tune) {
        ModalBottomSheet(onDismissRequest = { tune = false }, containerColor = Ink.lamp) { TuneSheet() }
    }
}

private fun nowLine(): String {
    val amb = PlayerHub.nowAmbience.let { if (it == AmbienceKind.NONE) "no background" else it.label.lowercase() }
    return "${PlayerHub.nowRole.label()}, $amb, ${fmtSpeed(PlayerHub.nowSpeed)}"
}

/** Thin scrubber: tap or drag anywhere on the track to seek within the whole story. */
@Composable
private fun Scrubber(fraction: Float, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    var drag by remember { mutableFloatStateOf(-1f) }
    val f = if (drag >= 0f) drag else fraction
    Canvas(
        modifier
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { onSeek(drag); drag = -1f },
                    onDragCancel = { drag = -1f },
                ) { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        val y = size.height / 2
        val h = 4.dp.toPx()
        drawRoundRect(Ink.lampHigh, Offset(0f, y - h / 2), Size(size.width, h), CornerRadius(h / 2))
        drawRoundRect(Ink.ember, Offset(0f, y - h / 2), Size(size.width * f, h), CornerRadius(h / 2))
        drawCircle(Ink.ember, 7.dp.toPx(), Offset(size.width * f, y))
    }
}

@Composable
private fun PauseGlyph() {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(8.dp).height(28.dp).background(Ink.onEmber, RoundedCornerShape(3.dp)))
        Box(Modifier.width(8.dp).height(28.dp).background(Ink.onEmber, RoundedCornerShape(3.dp)))
    }
}

/** The story's weather: one band per segment, colored by the background sound the Director chose. */
@Composable
private fun SceneStrip() {
    val plan = PlayerHub.plan ?: return
    val segs = plan.segments
    val weights = segs.map { it.text.length.toFloat().coerceAtLeast(8f) }
    val total = weights.sum()
    val f = PlayerHub.fraction
    Canvas(Modifier.fillMaxWidth().height(10.dp).padding(top = 2.dp)) {
        var x = 0f
        segs.forEachIndexed { i, s ->
            val w = size.width * weights[i] / total
            drawRoundRect(sceneColor(s.ambience), topLeft = androidx.compose.ui.geometry.Offset(x, 0f), size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
            x += w
        }
        val mx = size.width * f
        drawRect(Ink.moth, topLeft = androidx.compose.ui.geometry.Offset(mx - 1.5f, -2f), size = androidx.compose.ui.geometry.Size(3f, size.height + 4f))
    }
}

@Composable
private fun TuneSheet() {
    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
        Text("Tune this story", style = MaterialTheme.typography.headlineSmall)
        PlayerHub.plan?.let { Quiet("Director: ${it.summary()}", Modifier.padding(top = 4.dp)) }
        val rv = PlayerHub.roleVoices
        if (rv.isNotEmpty()) Quiet(rv.entries.joinToString(". ") { (r, v) -> "${r.label()}: ${v.label}" } + ".")

        Heading("Pace")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Pill("Auto", App.autoSpeed) { App.autoSpeed = true; App.saveSettings() }
            listOf(0.8f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f).forEach { s ->
                Pill("${s}×", !App.autoSpeed && App.speed == s) { App.autoSpeed = false; App.speed = s; App.saveSettings() }
            }
        }

        Heading("Sleep timer")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            listOf(0, 10, 20, 30, 60).forEach { m ->
                Pill(if (m == 0) "Off" else "$m min", m == 0 && PlayerHub.sleepLeftMs == 0L) { PlayerHub.setSleep(m) }
            }
        }
        if (PlayerHub.sleepLeftMs > 0) Quiet("Stops in ${formatMs(PlayerHub.sleepLeftMs)}", Modifier.padding(top = 8.dp))

        Heading("Background sound")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            Pill("Auto, by scene", App.autoSound) { App.autoSound = true; App.saveSettings() }
            AmbienceKind.entries.forEach { k ->
                Pill(k.label, !App.autoSound && App.ambience == k) { App.autoSound = false; App.ambience = k; App.saveSettings() }
            }
        }
        Slider(
            value = App.ambienceVolume,
            onValueChange = { App.ambienceVolume = it; PlayerHub.updateAmbience() },
            onValueChangeFinished = { App.saveSettings() },
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.lampHigh),
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Character voices", style = MaterialTheme.typography.titleMedium)
                Quiet("Dialogue gets its own voice. Applies when you restart the story.")
            }
            Switch(checked = App.autoVoice, onCheckedChange = { App.autoVoice = it; App.saveSettings() })
        }
    }
}
