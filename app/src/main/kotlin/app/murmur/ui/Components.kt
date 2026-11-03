package app.murmur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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

/** A small text pill used for moods and choices. Selected = amber text on lamp, not a bordered chip. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Ink.ember else Ink.lamp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink.onEmber else Ink.moth,
        )
    }
}

/** Cover with title beneath, for shelves. */
@Composable
fun CoverTile(story: Story, width: Dp, onClick: () -> Unit) {
    val done = story.id in App.completed
    Column(Modifier.width(width).clickable { onClick() }) {
        Box {
            Cover(story, Modifier.width(width).aspectRatio(0.78f))
            if (done) Dot(Modifier.align(Alignment.TopEnd).padding(10.dp), Ink.moss)
            if (story.id in App.favorites) Dot(Modifier.align(Alignment.TopStart).padding(10.dp), Ink.ember)
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
    Heading(title, Modifier.padding(horizontal = 22.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(stories, key = { it.id }) { s -> CoverTile(s, 128.dp) { onOpen(s) } }
    }
}

/** List row with a small cover: used in search results and library lists. */
@Composable
fun StoryRow(story: Story, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(story, Modifier.width(56.dp).aspectRatio(0.78f), corner = 10.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Quiet(story.meta())
        }
        if (story.id in App.completed) Dot(Modifier.padding(start = 8.dp), Ink.moss)
    }
}

fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

val spacedRow = Arrangement.spacedBy(8.dp)
