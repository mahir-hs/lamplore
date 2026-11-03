@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.audio.PlayerHub
import app.murmur.data.Story
import kotlinx.coroutines.delay

private val urges = listOf(
    "Bored" to "inspire",
    "Anxious" to "calm",
    "Lonely" to "inspire",
    "Angry" to "patience",
    "Tired" to "sleep",
)

@Composable
fun PauseScreen(onBack: () -> Unit, onOpen: (Story) -> Unit) {
    val t = rememberInfiniteTransition(label = "breath")
    val s by t.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    var inhale by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            inhale = !inhale
        }
    }
    var picked by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("← Back") }
        Text("Pause", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (App.faithTouches) "“In the remembrance of Allah do hearts find rest.” (13:28)"
            else "This urge will pass. You don't have to follow it.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(Modifier.padding(vertical = 36.dp).size(240.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(220.dp).scale(s)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            )
            Text(if (inhale) "Breathe in" else "Breathe out", style = MaterialTheme.typography.titleLarge)
        }
        Text("What were you about to scroll for?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = spacedRow, modifier = Modifier.padding(top = 8.dp)) {
            urges.forEach { (label, _) ->
                FilterChip(selected = picked == label, onClick = { picked = label }, label = { Text(label) })
            }
        }
        val mood = urges.firstOrNull { it.first == picked }?.second
        if (mood != null) {
            val pick = App.visibleStories.filter { mood in it.moods }.randomOrNull()
            if (pick != null) {
                Text(
                    "Try this instead:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                StoryRow(pick) { onOpen(pick) }
            }
        }
        Box(Modifier.padding(bottom = 32.dp))
    }
}

@Composable
fun DoneScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val streak = App.streak()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("← Back") }
        Text("Done for today", style = MaterialTheme.typography.headlineMedium)
        Text(
            "You listened for ${App.todaySeconds / 60} min today. Streak: $streak day${if (streak == 1) "" else "s"}.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (!saved) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("One line: what stayed with you?") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (text.isNotBlank()) App.addReflection(text, PlayerHub.current?.title)
                    App.markDone()
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text(if (text.isBlank()) "Finish without a note" else "Save and finish") }
        } else {
            SoftCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Rest well.", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "The stories will be here tomorrow. Put the phone down.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Back home") }
        }

        if (App.reflections.isNotEmpty()) {
            SectionTitle("Your reflections")
            App.reflections.take(10).forEach { r ->
                SoftCard(Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(r.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            r.date + (r.storyTitle?.let { "  ·  $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        Box(Modifier.padding(bottom = 32.dp))
    }
}
