@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import app.murmur.audio.AmbienceKind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.data.Categories
import app.murmur.data.Moods
import app.murmur.data.Story
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(onOpen: (Story) -> Unit, onPause: () -> Unit, onDone: () -> Unit) {
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
            Box(Modifier.fillMaxWidth()) {
                SceneArt(skyFor(), Modifier.matchParentSize())
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Ink.night),
                    ),
                )
            Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 18.dp)) {
                DirectorBadge(Modifier.padding(top = 20.dp))
                Text(greeting(), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 52.dp))
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
        }

        if (hero != null) {
            item { Hero(hero, resume != null) { onOpen(hero) } }
        }

        item {
            Heading("What do you need right now?", Modifier.padding(horizontal = 22.dp))
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Moods.all, key = { it.first }) { (key, label) ->
                    MoodCard(label, moodScene(key), mood == key) { mood = if (mood == key) null else key }
                }
            }
        }

        item { Shelf(if (mood == null) "Tonight" else Moods.all.first { it.first == mood }.second, picks.take(10), onOpen) }

        item { GoalBlock() }

        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 28.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedRow) {
                    GhostButton("Pause for a minute", onPause, Modifier.weight(1f))
                    GhostButton(if (App.doneToday) "Reflect again" else "Finish tonight", onDone, Modifier.weight(1f))
                }
                Quiet("Stories end. The feed doesn't.", Modifier.padding(top = 14.dp))
            }
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
    val breath = rememberInfiniteTransition(label = "glow")
    val glow by breath.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    Box(Modifier.padding(top = 26.dp).fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .alpha(glow)
                .background(
                    Brush.radialGradient(
                        0f to coverTint(story).copy(alpha = 0.9f),
                        0.6f to coverTint(story).copy(alpha = 0.25f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Column(Modifier.padding(horizontal = 22.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.18f).clickable { onOpen() }) {
                Cover(story, Modifier.fillMaxSize(), corner = 24.dp, animated = true)
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                1f to Ink.night.copy(alpha = 0.85f),
                            ),
                            RoundedCornerShape(24.dp),
                        ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(story.title, style = MaterialTheme.typography.headlineMedium, color = Ink.moth)
                    Text(story.meta(), style = MaterialTheme.typography.bodyMedium, color = Ink.moth.copy(alpha = 0.8f))
                }
            }
            PrimaryButton(
                if (resuming) "Continue listening" else "Listen",
                onOpen,
                Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun GoalBlock() {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 22.dp)) {
        Heading("One thing this week")
        if (App.goalText.isBlank()) {
            Quiet("Pick the one thing that matters. Check it off each day you did it.")
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("For example: walk after dinner") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink.ember,
                    unfocusedBorderColor = Ink.line,
                    cursorColor = Ink.ember,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (draft.isNotBlank()) {
                PrimaryButton("Set it", { App.setGoal(draft); draft = "" }, Modifier.padding(top = 10.dp))
            }
        } else {
            Text(App.goalText, style = MaterialTheme.typography.titleLarge)
            val monday = App.mondayEpoch()
            val today = LocalDate.now().toEpochDay()
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                    val epoch = monday + i
                    val done = epoch in App.goalDays
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(if (done) Ink.ember else Ink.lamp, CircleShape)
                                .clickable(enabled = epoch == today) { App.toggleGoalToday() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                d,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (done) Ink.onEmber else if (epoch == today) Ink.moth else Ink.dusk,
                            )
                        }
                    }
                }
            }
            TextButton(onClick = { App.clearGoal() }, modifier = Modifier.padding(top = 4.dp)) {
                Text("Change goal", color = Ink.dusk)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun skyFor(): AmbienceKind {
    val h = LocalTime.now().hour
    return if (h in 6..17) AmbienceKind.CALM else AmbienceKind.NIGHT
}

private fun moodScene(key: String): AmbienceKind = when (key) {
    "calm" -> AmbienceKind.STREAM
    "sleep" -> AmbienceKind.NIGHT
    "learn" -> AmbienceKind.FOREST
    "inspire" -> AmbienceKind.DESERT
    "patience" -> AmbienceKind.RAIN
    else -> AmbienceKind.CALM
}

/** A mood as a small living picture: the scene the Director would pick, with the label over it. */
@Composable
private fun MoodCard(label: String, scene: AmbienceKind, selected: Boolean, onClick: () -> Unit) {
    val ring by androidx.compose.animation.animateColorAsState(if (selected) Ink.ember else Color.Transparent, label = "ring")
    Box(
        Modifier
            .size(width = 118.dp, height = 78.dp)
            .bounceClick(onClick)
            .border(2.dp, ring, RoundedCornerShape(18.dp)),
    ) {
        SceneArt(scene, Modifier.matchParentSize(), corner = 18.dp, seed = label.length)
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.3f to Color.Transparent, 1f to Ink.night.copy(alpha = 0.8f)),
                RoundedCornerShape(18.dp),
            ),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = Ink.moth, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
    }
}

/** Says out loud that a Director is choosing voices, sound and music: a pulsing dot and one line. */
@Composable
fun DirectorBadge(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "badge").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "dot",
    )
    Row(
        modifier
            .background(Ink.night.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).alpha(pulse).background(Ink.ember, CircleShape))
        Text("  AI Director", style = MaterialTheme.typography.labelMedium, color = Ink.moth)
        Text("  scores every story", style = MaterialTheme.typography.labelMedium, color = Ink.dusk)
    }
}
