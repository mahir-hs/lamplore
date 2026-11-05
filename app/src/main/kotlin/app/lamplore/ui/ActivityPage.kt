@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.data.Activity
import app.lamplore.data.History
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKS = 17

/** One colour per topic, fixed so a topic never changes colour when the list changes. */
private fun topicColor(topic: String): Color = when (topic) {
    "Fables" -> Color(0xFF6FC3B2)
    "Classic tales" -> Color(0xFF8E9BF5)
    "Wisdom tales" -> Color(0xFFE0B36A)
    "Sleep & calm" -> Color(0xFF9A7BD6)
    "Yours" -> Color(0xFF7FB77E)
    "Audiobooks" -> Color(0xFFE58F7A)
    "Podcasts" -> Color(0xFF5FA8E8)
    "Old-time radio" -> Color(0xFFD98BB5)
    else -> Color(0xFF9AA5B8)
}

/** Heatmap steps: one hue, dark to light. Index 0 is "no listening". */
private val heat = listOf(Color(0xFF1B2547), Color(0xFF1F5A66), Color(0xFF2B8A8C), Color(0xFF46BDAE), Color(0xFF8DEBD0))

private fun level(seconds: Int): Int = when {
    seconds <= 0 -> 0
    seconds < 10 * 60 -> 1
    seconds < 30 * 60 -> 2
    seconds < 60 * 60 -> 3
    else -> 4
}

fun fmtTime(seconds: Int): String {
    val m = seconds / 60
    return when {
        seconds <= 0 -> "0 min"
        m < 1 -> "under a minute"
        m < 60 -> "$m min"
        else -> "${m / 60} h ${m % 60} min"
    }
}

/** Your listening: overview numbers, a heatmap of the last months, time per topic and a history. */
@Composable
fun ColumnScope.ActivityPage() {
    val today = LocalDate.now()
    // re-read whenever listening time changes
    val tick = App.todaySeconds
    val week = (0..6).map { Activity.seconds(today.minusDays(it.toLong())) }
    val month = (0..29).map { Activity.seconds(today.minusDays(it.toLong())) }

    Quiet("Kept on this phone only.", Modifier.padding(top = 2.dp))

    // ---- overview
    Heading("Overview")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        Stat("Today", fmtTime(Activity.seconds(today)))
        Stat("Last 7 days", fmtTime(week.sum()))
        Stat("Last 30 days", fmtTime(month.sum()))
        Stat("All time", fmtTime(Activity.totalSeconds()))
        Stat("Daily average", fmtTime(month.sum() / 30), "last 30 days")
        Stat("Streak", "${App.streak()} night${if (App.streak() == 1) "" else "s"}", "best ${Activity.bestStreak()}")
        Stat("Finished", "${App.completed.size}", "stories and books")
    }

    // ---- this week
    Heading("This week")
    WeekBars(week.reversed(), today)

    // ---- heatmap
    Heading("Last $WEEKS weeks")
    Heatmap(today)

    // ---- topics
    Heading("Time by topic")
    var period by remember { mutableIntStateOf(30) }
    FlowRow(horizontalArrangement = spacedRow) {
        listOf(7 to "7 days", 30 to "30 days", 0 to "All time").forEach { (d, label) -> Pill(label, period == d) { period = d } }
    }
    val topics = Activity.byTopic(period)
    if (topics.isEmpty()) {
        Quiet("Nothing yet. Time per topic is counted from when this update was installed.", Modifier.padding(top = 8.dp))
    } else {
        val max = topics.first().second.coerceAtLeast(1)
        val total = topics.sumOf { it.second }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            topics.forEach { (t, s) -> TopicBar(t, s, s.toFloat() / max, s * 100 / total) }
        }
    }

    // ---- weekday and hour
    Heading("By weekday")
    Quiet("Average minutes on each day, over the last 12 weeks.")
    val wd = Activity.weekdayMinutes(12)
    MiniBars(
        wd, listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), "Average minutes per weekday: " + wd.joinToString(),
        Modifier.padding(top = 8.dp),
    )
    Heading("By time of day")
    val hs = Activity.hourSeconds().map { it / 60 }
    MiniBars(
        hs, (0 until 24).map { if (it % 6 == 0) "$it" else "" }, "Minutes listened in each hour of the day",
        Modifier.padding(top = 8.dp),
    )
    Quiet("Hour of the day, 0 to 23. Counted from when this update was installed.", Modifier.padding(top = 2.dp))

    // ---- erase
    Heading("Erase")
    var sure by remember { mutableStateOf(false) }
    Quiet("History only hides entries. This is the one button that really deletes your history, activity and \"heard\" marks.")
    Text(
        if (sure) "Tap again to delete everything" else "Delete all activity data",
        style = MaterialTheme.typography.labelLarge, color = if (sure) Ink.ember else Ink.dusk,
        modifier = Modifier.bounceClick("Delete all activity data") { if (sure) { History.deleteEverything(); sure = false } else sure = true }.padding(vertical = 14.dp),
    )
    if (tick < 0) Spacer(Modifier.height(0.dp)) // keeps `tick` read so the page refreshes while listening
}

private fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
}

@Composable
private fun Stat(label: String, value: String, note: String = "") {
    Column(
        Modifier.width(158.dp).clip(RoundedCornerShape(14.dp)).background(Ink.lamp).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.dusk)
        Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
        if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
    }
}

@Composable
private fun WeekBars(seconds: List<Int>, today: LocalDate) {
    val max = (seconds.maxOrNull() ?: 0).coerceAtLeast(60 * 10)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    Column(Modifier.padding(top = 8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(110.dp)
                .semantics { contentDescription = "Minutes listened each of the last 7 days: " + days.indices.joinToString { "${days[it].dayOfWeek.name.lowercase()} ${seconds[it] / 60}" } },
        ) {
            val slot = size.width / 7
            val bar = slot * 0.46f
            seconds.forEachIndexed { i, s ->
                val h = if (s <= 0) 0f else (s.toFloat() / max * (size.height - 4.dp.toPx())).coerceAtLeast(4.dp.toPx())
                // empty days keep a faint baseline tick so the week still reads as seven days
                val x = slot * i + (slot - bar) / 2
                if (h == 0f) drawRoundRect(heat[0], Offset(x, size.height - 3.dp.toPx()), Size(bar, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                else drawRoundRect(heat[3], Offset(x, size.height - h), Size(bar, h), CornerRadius(4.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            days.forEachIndexed { i, d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = if (d == today) Ink.moth else Ink.dusk)
                    Text("${seconds[i] / 60}", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
                }
            }
        }
        Quiet("Minutes per day", Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Heatmap(today: LocalDate) {
    // columns are weeks (Monday first); the last column is this week
    val thisMonday = today.with(DayOfWeek.MONDAY)
    val first = thisMonday.minusWeeks(WEEKS - 1L)
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    val data = remember(today, App.todaySeconds) {
        (0 until WEEKS * 7).associate { i -> first.plusDays(i.toLong()).let { d -> d to Activity.seconds(d) } }
    }
    val gap = 3f
    Column(Modifier.padding(top = 8.dp)) {
        val p = picked
        Text(
            if (p == null) "Tap a day" else "${p.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))}: ${fmtTime(data[p] ?: 0)}",
            style = MaterialTheme.typography.labelMedium, color = if (p == null) Ink.dusk else Ink.moth,
        )
        Canvas(
            Modifier.fillMaxWidth().padding(top = 8.dp).height(((7 * 18) + 6 * 3).dp)
                .semantics { contentDescription = "Heatmap of listening, last $WEEKS weeks. Tap a day for its time." }
                .pointerInput(first) {
                    detectTapGestures { o ->
                        val w = (o.x / (size.width.toFloat() / WEEKS)).toInt().coerceIn(0, WEEKS - 1)
                        val row = (o.y / (size.height.toFloat() / 7)).toInt().coerceIn(0, 6)
                        val date = first.plusDays((w * 7 + row).toLong())
                        picked = if (date.isAfter(today)) null else date
                    }
                },
        ) {
            val cw = size.width / WEEKS
            val ch = size.height / 7
            for (w in 0 until WEEKS) for (r in 0 until 7) {
                val date = first.plusDays((w * 7 + r).toLong())
                if (date.isAfter(today)) continue
                val c = heat[level(data[date] ?: 0)]
                drawRoundRect(
                    c, Offset(w * cw + gap / 2, r * ch + gap / 2), Size(cw - gap, ch - gap), CornerRadius(4.dp.toPx()),
                )
                if (date == picked) drawRoundRect(
                    Ink.moth, Offset(w * cw + gap / 2, r * ch + gap / 2), Size(cw - gap, ch - gap), CornerRadius(4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
            heat.forEach { Box(Modifier.padding(start = 4.dp).size(14.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
            Text("  More", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
            Spacer(Modifier.weight(1f))
            Text("Rows are Mon to Sun", style = MaterialTheme.typography.labelSmall, color = Ink.dusk)
        }
    }
}

@Composable
fun TopicBar(topic: String, seconds: Int, fraction: Float, percent: Int, color: Color = topicColor(topic)) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(topic, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("${fmtTime(seconds)}  ·  $percent%", style = MaterialTheme.typography.labelMedium, color = Ink.dusk)
        }
        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Ink.lamp)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}
