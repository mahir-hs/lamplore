@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.data.Moods
import app.murmur.data.Story
import java.time.LocalTime

/**
 * Tonight. One idea per screen: a greeting, the story to listen to, what you need, and a shelf.
 * Nothing here moves; the living scenes belong to the player.
 */
@Composable
fun HomeScreen(onOpen: (Story) -> Unit, onPause: () -> Unit, onDone: () -> Unit, onBrowse: () -> Unit = {}) {
    var mood by remember { mutableStateOf<String?>(null) }
    val streak = App.streak()
    val visible = App.visibleStories
    val resume = visible.firstOrNull { App.hasProgress(it.id) && it.id !in App.completed }
    val unfinished = visible.filter { it.id !in App.completed }
    val hero = resume ?: unfinished.firstOrNull() ?: visible.firstOrNull()
    val picks = (if (mood == null) unfinished else visible.filter { mood in it.moods })
        .filter { it.id != hero?.id }

    LazyColumn {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(greeting(), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
                Quiet(
                    when {
                        App.todaySeconds < 60 && streak == 0 -> "Nothing yet tonight. One story is enough."
                        App.todaySeconds < 60 -> "Nothing yet today. $streak night${if (streak == 1) "" else "s"} in a row so far."
                        else -> "${App.todaySeconds / 60} minutes today, $streak night${if (streak == 1) "" else "s"} in a row."
                    },
                    Modifier.padding(top = 6.dp),
                )
            }
        }

        if (hero != null) {
            item { Hero(hero, resume != null) { onOpen(hero) } }
        }

        item {
            Heading("What do you need right now?", Modifier.padding(horizontal = 20.dp))
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = spacedRow,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Moods.all.forEach { (key, label) ->
                    Pill(label, mood == key) { mood = if (mood == key) null else key }
                }
            }
        }

        item { Shelf(if (mood == null) "Tonight" else Moods.all.first { it.first == mood }.second, picks.take(10), onOpen) }

        item {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
                TextButton(onClick = onPause) { Text("Pause for a minute", color = Ink.ember) }
                TextButton(onClick = onDone) { Text("Finish tonight", color = Ink.dusk) }
            }
        }

        item {
            Heading("Discover", Modifier.padding(horizontal = 20.dp))
            DiscoverFeed({ i, d -> ListenNav.item = i; ListenNav.seeAll = d; onBrowse() }, onOpen)
        }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "Still up?"
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        h < 21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun Hero(story: Story, resuming: Boolean, onOpen: () -> Unit) {
    Column(
        Modifier
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(coverTint(story).copy(alpha = 0.55f), Color.Transparent), Offset(size.width / 2, size.width * 0.5f), size.width * 0.85f),
                    size.width * 0.85f, Offset(size.width / 2, size.width * 0.5f),
                )
            }
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp),
    ) {
        Cover(
            story,
            Modifier.fillMaxWidth().aspectRatio(1.3f).bounceClick("Open ${story.title}", onOpen),
            corner = 10.dp,
            mat = 6.dp,
        )
        Text(story.title, style = StoryTitle, modifier = Modifier.padding(top = 14.dp))
        Quiet(story.meta(), Modifier.padding(top = 2.dp))
        PrimaryButton(
            if (resuming) "Continue listening" else "Listen",
            onOpen,
            Modifier.fillMaxWidth().padding(top = 14.dp),
        )
    }
}
