@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lamplore.data.Activity
import app.lamplore.data.History

/**
 * What you like, worked out from what you actually listened to: top genres, authors and titles, what is
 * rising or fading, and what you come back to. Built from the permanent journal, so hiding history
 * does not change it.
 */
@Composable
fun ColumnScope.TastePage() {
    var period by remember { mutableIntStateOf(30) }
    Quiet("Worked out on this phone from your listening. Genres come from the recordings' own subjects.", Modifier.padding(top = 4.dp))
    FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
        listOf(30 to "30 days", 365 to "This year", 0 to "All time").forEach { (d, l) -> Pill(l, period == d) { period = d } }
    }

    val genres = Activity.byTag(period).take(8)
    Heading("Top genres")
    if (genres.isEmpty()) Quiet("Nothing yet. Genres appear after you have listened to audiobooks.")
    else {
        val max = genres.first().second.coerceAtLeast(1)
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            genres.forEach { (g, s) -> TopicBar(g, s, s.toFloat() / max, s * 100 / genres.sumOf { it.second }, entityColor(g)) }
        }
    }

    // rising and falling: this 30 days against the 30 days before
    val now = Activity.byTag(30).toMap()
    val before = Activity.byTag(30, previous = true).toMap()
    val change = (now.keys + before.keys).map { it to ((now[it] ?: 0) - (before[it] ?: 0)) / 60 }.filter { it.second != 0 }
    val rising = change.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
    val falling = change.filter { it.second < 0 }.sortedBy { it.second }.take(3)
    if (rising.isNotEmpty() || falling.isNotEmpty()) {
        Heading("Rising and fading")
        Quiet("Last 30 days compared with the 30 days before.")
        rising.forEach { (g, m) -> Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(g, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall); Text("+$m min", color = Ink.moss, style = MaterialTheme.typography.labelLarge) } }
        falling.forEach { (g, m) -> Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(g, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall); Text("$m min", color = Ink.ember, style = MaterialTheme.typography.labelLarge) } }
    }

    val authors = Activity.byAuthor(period).take(6)
    if (authors.isNotEmpty()) {
        Heading("Top authors and shows")
        val max = authors.first().second.coerceAtLeast(1)
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            authors.forEach { (a, s) -> TopicBar(a, s, s.toFloat() / max, s * 100 / authors.sumOf { it.second }, entityColor(a)) }
        }
    }

    val titles = Activity.topTitles(period).take(5)
    if (titles.isNotEmpty()) {
        Heading("Most listened")
        titles.forEach { (_, title, s) ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(fmtTime(s), color = Ink.dusk, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    val again = History.items.filter { it.days > 1 }.sortedByDescending { it.days }.take(5)
    if (again.isNotEmpty()) {
        Heading("Come back to")
        Quiet("Titles you listened to on more than one day.")
        again.forEach { h ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(h.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${h.days} days", color = Ink.dusk, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    val binges = Activity.biggestDay().take(3).filter { it.third >= 30 * 60 }
    if (binges.isNotEmpty()) {
        Heading("Biggest sittings")
        binges.forEach { (_, title, s) ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${fmtTime(s)} in one day", color = Ink.dusk, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
