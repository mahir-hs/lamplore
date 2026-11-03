@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.murmur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.murmur.App
import app.murmur.data.Moods
import app.murmur.data.Story
import java.time.LocalDate

@Composable
fun HomeScreen(onOpen: (Story) -> Unit, onPause: () -> Unit, onDone: () -> Unit) {
    var mood by remember { mutableStateOf<String?>(null) }
    val streak = App.streak().also { App.todaySeconds; App.doneToday }
    val visible = App.visibleStories
    val resume = visible.firstOrNull { App.hasProgress(it.id) && it.id !in App.completed }
    val picks = (if (mood == null) visible.filter { it.id !in App.completed } else visible.filter { mood in it.moods })
        .filter { it.id != resume?.id }
        .take(4)

    LazyColumn(Modifier.padding(horizontal = 20.dp)) {
        item {
            Text(
                if (App.faithTouches) "Assalamu alaikum" else "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "Put the scroll down. Listen to something that settles you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            SoftCard(Modifier.padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Today", "${App.todaySeconds / 60} min")
                    Stat("Streak", "$streak day${if (streak == 1) "" else "s"}")
                    Stat("Done today", if (App.doneToday) "✓" else "-")
                }
            }
        }

        if (resume != null) {
            item { SectionTitle("Continue listening") }
            item { StoryRow(resume) { onOpen(resume) } }
        }

        item { SectionTitle("What do you need right now?") }
        item {
            FlowRow(horizontalArrangement = spacedRow) {
                Moods.all.forEach { (key, label) ->
                    FilterChip(
                        selected = mood == key,
                        onClick = { mood = if (mood == key) null else key },
                        label = { Text(label) },
                    )
                }
            }
        }
        items(picks) { s -> StoryRow(s) { onOpen(s) } }

        item { SectionTitle("My one thing this week") }
        item { GoalCard() }

        item { SectionTitle("Take care") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedRow) {
                OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text("Pause for a minute") }
                Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text(if (App.doneToday) "Reflect again" else "Finish for today")
                }
            }
            Text(
                "Stories end. The feed doesn't. Listen, reflect, then put the phone down.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalCard() {
    var draft by remember { mutableStateOf("") }
    SoftCard {
        Column(Modifier.padding(16.dp)) {
            if (App.goalText.isBlank()) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("The one thing that matters this week") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { App.setGoal(draft); draft = "" },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Set it") }
            } else {
                Text(App.goalText, style = MaterialTheme.typography.titleMedium)
                val monday = App.mondayEpoch()
                val today = LocalDate.now().toEpochDay()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                        val epoch = monday + i
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d, style = MaterialTheme.typography.labelMedium)
                            Checkbox(
                                checked = epoch in App.goalDays,
                                onCheckedChange = { if (epoch == today) App.toggleGoalToday() },
                                enabled = epoch == today,
                            )
                        }
                    }
                }
                TextButton(onClick = { App.clearGoal() }) { Text("Change goal") }
            }
        }
    }
}
