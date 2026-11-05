@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import app.lamplore.App
import app.lamplore.audio.PlayerHub
import app.lamplore.data.Story
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

    Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 10.dp, top = 6.dp).align(Alignment.Start)) { Text("Back", color = Ink.dusk) }
        Text("Pause", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 10.dp))
        Quiet(
            "This urge will pass. You don't have to follow it.",
            Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )
        Box(Modifier.padding(vertical = 34.dp).size(250.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(230.dp).scale(s).background(Ink.ember.copy(alpha = 0.18f), CircleShape))
            Box(Modifier.size(150.dp).scale(s).background(Ink.ember.copy(alpha = 0.22f), CircleShape))
            Text(if (inhale) "Breathe in" else "Breathe out", style = MaterialTheme.typography.headlineSmall)
        }
        Text("What were you about to scroll for?", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        FlowRow(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = spacedRow, verticalArrangement = spacedRow) {
            urges.forEach { (label, _) -> Pill(label, picked == label) { picked = label } }
        }
        val mood = urges.firstOrNull { it.first == picked }?.second
        if (mood != null) {
            val pick = remember(mood) { App.visibleStories.filter { mood in it.moods }.randomOrNull() }
            if (pick != null) {
                Quiet("Try this instead", Modifier.padding(top = 8.dp))
                StoryRow(pick) { onOpen(pick) }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
fun DoneScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val streak = App.streak()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 6.dp)) { Text("Back", color = Ink.dusk) }
        Text("Finish tonight", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 10.dp))
        Quiet(
            "${App.todaySeconds / 60} minutes today. $streak night${if (streak == 1) "" else "s"} in a row.",
            Modifier.padding(top = 6.dp, bottom = 18.dp),
        )
        if (!saved) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("One line: what stayed with you?") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.ember, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                if (text.isBlank()) "Finish without a note" else "Save and finish",
                {
                    if (text.isNotBlank()) App.addReflection(text, PlayerHub.current?.title)
                    App.markDone()
                    saved = true
                },
                Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        } else {
            Text("Rest well.", style = MaterialTheme.typography.headlineSmall)
            Quiet("The stories will be here tomorrow. Put the phone down.", Modifier.padding(top = 4.dp))
            GhostButton("Back home", onBack, Modifier.fillMaxWidth().padding(top = 16.dp))
        }

        if (App.reflections.isNotEmpty()) {
            Heading("Your reflections")
            App.reflections.take(10).forEach { r ->
                Column(Modifier.padding(bottom = 14.dp)) {
                    Text(r.text, style = MaterialTheme.typography.bodyLarge)
                    Quiet(r.date + (r.storyTitle?.let { ", after $it" } ?: ""))
                }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}
