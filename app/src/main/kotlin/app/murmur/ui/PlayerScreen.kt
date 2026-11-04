@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.AmbienceKind
import app.murmur.audio.MusicMood
import app.murmur.audio.PlayerHub
import app.murmur.audio.Role
import app.murmur.audio.Segment
import app.murmur.data.Plate

private fun Role.label() = when (this) {
    Role.NARRATOR -> "Narrator"
    Role.MALE -> "Male character"
    Role.FEMALE -> "Female character"
    Role.CHILD -> "Young voice"
    Role.DEEP -> "Deep voice"
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

private enum class Sheet { NONE, TUNE, DIRECTOR, PICTURES }

private val sleepSteps = listOf(0, 10, 20, 30, 60)

/**
 * The player. Everything you need while listening is on one screen without scrolling: the scene and cover,
 * the title, a scrubber that doubles as the story's weather, and the transport. The rest (how the story is
 * read, voices, pictures from the book, report) is one tap away in a sheet or the menu.
 */
@Composable
fun PlayerScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val story = PlayerHub.current
    val ctx = LocalContext.current
    if (story == null) {
        Column {
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 10.dp, top = 6.dp)) { Text("Back", color = Ink.dusk) }
            Quiet("Pick a story from the library.", Modifier.padding(22.dp))
        }
        return
    }

    var sheet by remember { mutableStateOf(Sheet.NONE) }
    var menu by remember { mutableStateOf(false) }
    var sleepIdx by remember { mutableIntStateOf(if (PlayerHub.sleepLeftMs > 0) 1 else 0) }

    val scene = PlayerHub.nowAmbience.let { if (it == AmbienceKind.NONE) AmbienceKind.CALM else it }
    val status = PlayerHub.status
    val waiting = status.isNotBlank() && status != "Finished" && !status.startsWith("Voice engine")
    val percent = Regex("(\\d+)%").find(status)?.groupValues?.get(1)?.toIntOrNull()
    // the one choreographed moment: when the story ends, the scene dims toward night
    val dim by animateFloatAsState(if (PlayerHub.finished) 0.6f else 0f, tween(1800), label = "dim")
    val saved = story.id in App.favorites

    Column(Modifier.fillMaxSize().padding(bottom = 14.dp)) {
        // ---- top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTap("Back", onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink.moth) }
            Spacer(Modifier.weight(1f))
            val left = PlayerHub.sleepLeftMs
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Sleep timer, " + if (left > 0) "${formatMs(left)} left" else "off" }
                    .bounceClick("Change sleep timer") {
                        sleepIdx = (sleepIdx + 1) % sleepSteps.size
                        PlayerHub.setSleep(sleepSteps[sleepIdx])
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.MOON, if (left > 0) Ink.ember else Ink.moth, 22.dp)
                if (left > 0) Text("  ${formatMs(left)}", style = MaterialTheme.typography.labelMedium, color = Ink.ember)
            }
            IconTap(if (saved) "Remove from saved" else "Save this story", { App.toggleFavorite(story.id) }) {
                Icon(
                    if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (saved) "Saved" else "Not saved",
                    tint = if (saved) Ink.ember else Ink.moth,
                )
            }
            Box {
                IconTap("More options", { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Ink.moth) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.lampHigh) {
                    MenuRow("Tune this story") { menu = false; sheet = Sheet.TUNE }
                    MenuRow("How it is being read") { menu = false; sheet = Sheet.DIRECTOR }
                    if (story.plates.isNotEmpty()) MenuRow("Pictures from the book") { menu = false; sheet = Sheet.PICTURES }
                    MenuRow("Restart story") { menu = false; PlayerHub.restart() }
                    MenuRow("Report this story") {
                        menu = false
                        App.report(story.id)
                        PlayerHub.stopAll()
                        Toast.makeText(ctx, "Reported and hidden.", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                    if (story.isUser) MenuRow("Delete this story") {
                        menu = false
                        PlayerHub.stopAll()
                        App.deleteUserStory(story.id)
                        onBack()
                    }
                }
            }
        }

        // ---- art: the living scene with the cover on its paper mat
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            SceneBackdrop(scene, Modifier.matchParentSize())
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Ink.night.copy(alpha = 0.35f), 0.25f to Color.Transparent, 0.7f to Color.Transparent, 1f to Ink.night),
                ),
            )
            Cover(
                story,
                Modifier
                    .fillMaxHeight(0.78f)
                    .aspectRatio(0.8f)
                    .then(
                        if (story.plates.isNotEmpty()) Modifier.bounceClick("Show pictures from the book") { sheet = Sheet.PICTURES }
                        else Modifier,
                    ),
                corner = 10.dp,
                mat = 7.dp,
                animated = PlayerHub.playing,
            )
            Box(Modifier.matchParentSize().background(Ink.night.copy(alpha = dim)))
        }

        // ---- title and what the Director is doing
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                story.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(
                story.meta(),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.dusk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .then(if (!waiting) Modifier.bounceClick("Show how this story is read") { sheet = Sheet.DIRECTOR } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when {
                        status.startsWith("Preparing for smooth") -> "Getting the voice ready" + (percent?.let { ", $it%" } ?: "")
                        status.isNotBlank() -> status
                        else -> nowLine()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status.isBlank()) Ink.dusk else Ink.ember,
                    textAlign = TextAlign.Center,
                )
            }
            if (status.startsWith("Preparing for smooth")) {
                TextButton(onClick = { PlayerHub.startNow() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Start now anyway", color = Ink.ember)
                }
            }
        }

        // ---- scrubber (the scene strip) and time
        SceneScrubber(
            PlayerHub.fraction,
            PlayerHub.plan?.segments,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) { PlayerHub.seekToFraction(it) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs((PlayerHub.positionSeconds * 1000).toLong()), style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
            Text(formatMs((PlayerHub.totalSeconds * 1000).toLong()), style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
        }

        // ---- transport, or the end of the story
        if (PlayerHub.finished) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("The end.", style = MaterialTheme.typography.headlineSmall)
                Quiet("A good moment to put the phone down.", Modifier.padding(top = 2.dp))
                PrimaryButton("Finish tonight", onDone, Modifier.fillMaxWidth().padding(top = 12.dp))
                TextButton(onClick = { PlayerHub.restart() }) { Text("Listen again", color = Ink.dusk) }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(Glyph.PREV, Ink.dusk, "Previous part") { PlayerHub.previous() }
                TransportButton(Glyph.BACK15, Ink.moth, "Back 15 seconds") { PlayerHub.seekBy(-15_000) }
                PlayButton(waiting, percent)
                TransportButton(Glyph.FWD15, Ink.moth, "Forward 15 seconds") { PlayerHub.seekBy(15_000) }
                TransportButton(Glyph.NEXT, Ink.dusk, "Next part") { PlayerHub.next() }
            }
        }
    }

    if (sheet != Sheet.NONE) {
        ModalBottomSheet(onDismissRequest = { sheet = Sheet.NONE }, containerColor = Ink.lamp) {
            when (sheet) {
                Sheet.TUNE -> TuneSheet()
                Sheet.DIRECTOR -> DirectorSheet()
                Sheet.PICTURES -> Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) { PlateGallery(story.plates) }
                Sheet.NONE -> {}
            }
        }
    }
}

private fun nowLine(): String {
    val amb = PlayerHub.nowAmbience.let { if (it.file == null) null else it.label.lowercase() }
    val mus = PlayerHub.nowMusic.let { if (it.file == null) null else "${it.label.lowercase()} music" }
    return listOfNotNull(PlayerHub.nowRole.label(), amb, mus, fmtSpeed(PlayerHub.nowSpeed)).joinToString(", ")
}

@Composable
private fun IconTap(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).bounceClick(label, onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, color = Ink.moth) }, onClick = onClick)
}

/**
 * One bar that is both the scrubber and the story's weather: each stretch is coloured by the background
 * sound the Director chose, bright where you have listened and dim ahead. Tap or drag to seek.
 */
@Composable
private fun SceneScrubber(fraction: Float, segments: List<Segment>?, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    var drag by remember { mutableFloatStateOf(-1f) }
    val f = if (drag >= 0f) drag else fraction
    Canvas(
        modifier
            .height(40.dp)
            .semantics { contentDescription = "Story position, ${(f * 100).toInt()} percent" }
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
        val h = 8.dp.toPx()
        val round = CornerRadius(h / 2)
        val track = Path().apply { addRoundRect(RoundRect(0f, y - h / 2, size.width, y + h / 2, round)) }
        clipPath(track) {
            if (segments.isNullOrEmpty()) {
                drawRect(Ink.lampHigh, Offset(0f, y - h / 2), Size(size.width, h))
                drawRect(Ink.ember, Offset(0f, y - h / 2), Size(size.width * f, h))
            } else {
                val weights = segments.map { it.text.length.toFloat().coerceAtLeast(8f) }
                val total = weights.sum()
                for (pass in 0..1) {
                    var x = 0f
                    val draw = {
                        segments.forEachIndexed { i, s ->
                            val w = size.width * weights[i] / total
                            val c = sceneColor(s.ambience)
                            drawRect(if (pass == 0) c.copy(alpha = 0.3f) else c, Offset(x, y - h / 2), Size(w + 0.5f, h))
                            x += w
                        }
                    }
                    if (pass == 0) draw() else clipRect(right = size.width * f) { draw() }
                }
            }
        }
        drawCircle(Ink.moth, 9.dp.toPx(), Offset(size.width * f, y))
        drawCircle(Ink.ember, 4.5.dp.toPx(), Offset(size.width * f, y))
    }
}

@Composable
private fun TransportButton(glyph: Glyph, tint: Color, description: String, onClick: () -> Unit) {
    Box(Modifier.size(56.dp).bounceClick(description, onClick), contentAlignment = Alignment.Center) {
        GlyphIcon(glyph, tint, 30.dp, description)
    }
}

/** Play or pause. While the voice is getting ready, a ring around it shows progress instead of a paragraph. */
@Composable
private fun PlayButton(waiting: Boolean, percent: Int?) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        if (waiting) WaitingRing(percent)
        Box(
            Modifier.size(72.dp).background(Ink.ember, CircleShape).bounceClick(if (PlayerHub.playing) "Pause" else "Play") { PlayerHub.togglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(if (PlayerHub.playing) Glyph.PAUSE else Glyph.PLAY, Ink.onEmber, 36.dp, if (PlayerHub.playing) "Pause" else "Play")
        }
    }
}

@Composable
private fun WaitingRing(percent: Int?) {
    val spin by rememberInfiniteTransition(label = "wait").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )
    Canvas(Modifier.size(88.dp)) {
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Ink.ember.copy(alpha = 0.2f), size.minDimension / 2 - 2.dp.toPx(), style = stroke)
        val sweep = if (percent != null) 360f * percent / 100f else 90f
        drawArc(Ink.ember, if (percent != null) -90f else spin, sweep, false, Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), style = stroke)
    }
}

/** How the story is being read right now, with each value cross-fading when the Director changes it. */
@Composable
private fun DirectorSheet() {
    val plan = PlayerHub.plan
    val amb = PlayerHub.nowAmbience.let { if (it.file == null && it != AmbienceKind.HEARTBEAT && it != AmbienceKind.CALM) "None" else it.label }
    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
        Text("How this story is read", style = MaterialTheme.typography.headlineSmall)
        Quiet("The Director reads the story and picks the voice, background sound, music and pace for each part.", Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DirectorCell("Voice", PlayerHub.nowRole.label().removeSuffix(" character"), Modifier.weight(1f))
            DirectorCell("Sound", amb, Modifier.weight(1f))
            DirectorCell("Music", PlayerHub.nowMusic.label, Modifier.weight(1f))
            DirectorCell("Pace", fmtSpeed(PlayerHub.nowSpeed), Modifier.weight(0.7f))
        }
        Quiet(plan?.summary() ?: "Reading the story to plan voices, sounds and music...", Modifier.padding(top = 16.dp))
        val rv = PlayerHub.roleVoices
        if (rv.isNotEmpty()) Quiet(rv.entries.joinToString(". ") { (r, v) -> "${r.label()}: ${v.label}" } + ".", Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DirectorCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
        Crossfade(value, animationSpec = tween(900), label = label) { v ->
            Text(v, style = MaterialTheme.typography.titleSmall, color = Ink.moth, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TuneSheet() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
        Text("Tune this story", style = MaterialTheme.typography.headlineSmall)
        PlayerHub.plan?.let { Quiet("Director: ${it.summary()}", Modifier.padding(top = 4.dp)) }
        val rv = PlayerHub.roleVoices
        if (rv.isNotEmpty()) Quiet(rv.entries.joinToString(". ") { (r, v) -> "${r.label()}: ${v.label}" } + ".")

        Heading("Pace")
        Quiet("Better voices bake the pace into the speech, so a change applies when you restart the story.")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Pill("Auto", App.autoSpeed) { App.autoSpeed = true; App.saveSettings() }
            listOf(0.8f, 0.9f, 1.0f, 1.15f, 1.3f, 1.5f).forEach { s ->
                Pill("${s}×", !App.autoSpeed && App.speed == s) { App.autoSpeed = false; App.speed = s; App.saveSettings() }
            }
        }

        Heading("Sleep timer")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            listOf(0, 10, 20, 30, 60).forEach { m ->
                Pill(if (m == 0) "Off" else "$m min", m == 0 && PlayerHub.sleepLeftMs == 0L) { PlayerHub.setSleep(m) }
            }
        }
        if (PlayerHub.sleepLeftMs > 0) Quiet("Stops in ${formatMs(PlayerHub.sleepLeftMs)}", Modifier.padding(top = 8.dp))

        Heading("Background sound")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
            modifier = Modifier.padding(top = 6.dp).semantics { contentDescription = "Background sound volume" },
        )

        Heading("Music")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Pill("Auto, by story", App.autoMusic) { App.autoMusic = true; App.saveSettings() }
            MusicMood.entries.forEach { m ->
                Pill(m.label, !App.autoMusic && App.music == m) { App.autoMusic = false; App.music = m; App.saveSettings() }
            }
        }
        Slider(
            value = App.musicVolume,
            onValueChange = { App.musicVolume = it; PlayerHub.updateAmbience() },
            onValueChangeFinished = { App.saveSettings() },
            valueRange = 0f..0.8f,
            colors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.lampHigh),
            modifier = Modifier.padding(top = 6.dp).semantics { contentDescription = "Music volume" },
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

/** The real illustrations from the public-domain book behind this story: one plate, or a swipeable row. */
@Composable
private fun PlateGallery(plates: List<Plate>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text("From the book", style = MaterialTheme.typography.headlineSmall)
        if (plates.size > 1) Quiet("${plates.size} pictures. Swipe to see them all.", Modifier.padding(top = 2.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(plates.size) { i -> PlateImage(plates[i], tall = plates.size > 1) }
        }
        Quiet(plates.first().credit, Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun PlateImage(plate: Plate, tall: Boolean) {
    val img = rememberPlate(plate.asset)
    val ratio = img?.let { it.width.toFloat() / it.height } ?: 1f
    val mod = if (tall) Modifier.height(300.dp).aspectRatio(ratio) else Modifier.fillMaxWidth().aspectRatio(ratio)
    Box(mod.clip(RoundedCornerShape(6.dp)).background(Ink.lampHigh)) {
        if (img != null) Canvas(Modifier.matchParentSize()) {
            drawImage(img, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        }
    }
}
