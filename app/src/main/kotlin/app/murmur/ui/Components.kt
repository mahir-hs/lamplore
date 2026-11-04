package app.murmur.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.data.Story

/** Section heading: serif, no eyebrow, generous top space. */
@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier.padding(top = 28.dp, bottom = 12.dp),
    )
}

/** One sentence of secondary information. Replaces meta strings joined with dots. */
@Composable
fun Quiet(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink.dusk, modifier = modifier)
}

fun Story.meta(): String {
    val src = author.replace(", retold", "")
    val retold = author.endsWith("retold")
    return buildString {
        append("$minutes min")
        append(if (retold) ", retold from $src" else ", by $src")
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink.ember, contentColor = Ink.onEmber),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink.lampHigh, contentColor = Ink.moth),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** A text pill for moods and choices: a 48 dp touch target around a smaller visible pill. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) Ink.ember else Ink.lamp)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Ink.onEmber else Ink.moth,
            )
        }
    }
}

/** Cover with title beneath, for shelves. */
@Composable
fun CoverTile(story: Story, width: Dp, onClick: () -> Unit) {
    val openLabel = "Open ${story.title}"
    val done = story.id in App.completed
    Column(Modifier.width(width).bounceClick(openLabel, onClick)) {
        Box {
            Cover(story, Modifier.width(width).aspectRatio(0.8f))
            if (done) Dot(Modifier.align(Alignment.TopEnd).padding(12.dp).semantics { contentDescription = "Finished" }, Ink.moss)
            if (story.id in App.favorites) Dot(Modifier.align(Alignment.TopStart).padding(12.dp).semantics { contentDescription = "Saved" }, Ink.ember)
        }
        Text(
            story.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text("${story.minutes} min", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
    }
}

@Composable
private fun Dot(modifier: Modifier, color: Color) {
    Box(modifier.size(10.dp).background(color, CircleShape))
}

@Composable
fun Shelf(title: String, stories: List<Story>, onOpen: (Story) -> Unit) {
    if (stories.isEmpty()) return
    Heading(title, Modifier.padding(horizontal = 20.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(stories, key = { it.id }) { s -> CoverTile(s, 128.dp) { onOpen(s) } }
    }
}

/** List row with a small cover: used in search results and library lists. */
@Composable
fun StoryRow(story: Story, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick("Open ${story.title}", onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(story, Modifier.width(56.dp).aspectRatio(0.8f), corner = 8.dp, mat = 3.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Quiet(story.meta())
        }
        if (story.id in App.completed) Dot(Modifier.padding(start = 8.dp).semantics { contentDescription = "Finished" }, Ink.moss)
    }
}

fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

val spacedRow = Arrangement.spacedBy(8.dp)

/** Spring-scale feedback on press, so every tap feels acknowledged. */
fun Modifier.bounceClick(label: String? = null, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")
    this.scale(scale).clickable(
        interactionSource = src, indication = null, onClickLabel = label,
        role = androidx.compose.ui.semantics.Role.Button, onClick = onClick,
    )
}

enum class Glyph { PLAY, PAUSE, PREV, NEXT, BACK15, FWD15, MOON }

/** Transport icons drawn by hand (the icon pack in use has no pause or skip). */
@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, size: Dp = 28.dp, description: String? = null) {
    Box(
        Modifier.size(size).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            fun tri(x0: Float, x1: Float): Path = Path().apply {
                moveTo(x0, h * 0.18f); lineTo(x1, h * 0.5f); lineTo(x0, h * 0.82f); close()
            }
            when (glyph) {
                Glyph.PLAY -> drawPath(tri(w * 0.26f, w * 0.84f), tint)
                Glyph.PAUSE -> {
                    drawRoundRect(tint, Offset(w * 0.2f, h * 0.15f), Size(w * 0.2f, h * 0.7f), CornerRadius(w * 0.06f))
                    drawRoundRect(tint, Offset(w * 0.6f, h * 0.15f), Size(w * 0.2f, h * 0.7f), CornerRadius(w * 0.06f))
                }
                Glyph.NEXT -> {
                    drawPath(tri(w * 0.14f, w * 0.7f), tint)
                    drawRoundRect(tint, Offset(w * 0.74f, h * 0.18f), Size(w * 0.12f, h * 0.64f), CornerRadius(w * 0.04f))
                }
                Glyph.PREV -> {
                    val p = Path().apply { moveTo(w * 0.86f, h * 0.18f); lineTo(w * 0.3f, h * 0.5f); lineTo(w * 0.86f, h * 0.82f); close() }
                    drawPath(p, tint)
                    drawRoundRect(tint, Offset(w * 0.14f, h * 0.18f), Size(w * 0.12f, h * 0.64f), CornerRadius(w * 0.04f))
                }
                Glyph.MOON -> {
                    val outer = Path().apply { addOval(Rect(Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f))) }
                    val bite = Path().apply { addOval(Rect(Offset(w * 0.34f, h * 0.02f), Size(w * 0.7f, h * 0.7f))) }
                    drawPath(Path.combine(PathOperation.Difference, outer, bite), tint)
                }
                Glyph.BACK15, Glyph.FWD15 -> {
                    val back = glyph == Glyph.BACK15
                    val sw = w * 0.08f
                    val inset = w * 0.12f
                    drawArc(
                        tint, if (back) -60f else 240f, if (back) -280f else 280f, false,
                        Offset(inset, inset), Size(w - inset * 2, h - inset * 2), style = Stroke(sw, cap = StrokeCap.Round),
                    )
                    val ax = if (back) w * 0.24f else w * 0.76f
                    val dir = if (back) 1f else -1f
                    val head = Path().apply {
                        moveTo(ax - dir * w * 0.14f, h * 0.06f); lineTo(ax + dir * w * 0.06f, h * 0.2f); lineTo(ax - dir * w * 0.14f, h * 0.32f); close()
                    }
                    drawPath(head, tint)
                }
            }
        }
        if (glyph == Glyph.BACK15 || glyph == Glyph.FWD15) {
            Text("15", style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
