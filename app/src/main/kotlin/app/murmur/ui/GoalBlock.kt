package app.murmur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.murmur.App
import java.time.LocalDate

/** The one-thing-this-week tracker. Lives on the You screen; the caller sets the side padding. */
@Composable
fun GoalBlock() {
    var draft by remember { mutableStateOf("") }
    Column {
        Heading("One thing this week")
        if (App.goalText.isBlank()) {
            Quiet("Pick the one thing that matters, then check it off each day you do it.")
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
            val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                    val epoch = monday + i
                    val done = epoch in App.goalDays
                    Box(
                        Modifier
                            .size(44.dp)
                            .semantics { contentDescription = names[i] + if (done) ", done" else if (epoch == today) ", tap to mark done" else "" }
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
            TextButton(onClick = { App.clearGoal() }, modifier = Modifier.padding(top = 4.dp)) {
                Text("Change goal", color = Ink.dusk)
            }
        }
    }
}
