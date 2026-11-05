package app.lamplore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lamplore.ai.AiModel
import app.lamplore.ai.AiModels
import app.lamplore.ai.DeviceCheck
import app.lamplore.ai.PhoneTests
import app.lamplore.ai.Verdict

/**
 * "Your phone": what this phone is, whether it can run each feature, and buttons that measure it for real.
 * The verdicts are estimates from memory, chip and storage; the tests below them are measurements.
 */
@Composable
fun PhoneCheckSection() {
    val ctx = LocalContext.current
    val report = remember { DeviceCheck.run(ctx) }

    Heading("This phone")
    Quiet(report.summary)
    Column(Modifier.padding(top = 8.dp)) {
        report.capabilities.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp, end = 12.dp).size(10.dp).background(verdictColor(c.verdict), CircleShape))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(verdictLabel(c.verdict), style = MaterialTheme.typography.labelMedium, color = verdictColor(c.verdict))
                    }
                    Quiet(c.detail)
                }
            }
        }
    }

    Text("Test it on this phone", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
    Quiet("These measure the real speed of this phone. Each one takes under a minute.", Modifier.padding(top = 2.dp))

    // voice speed
    GhostButton(
        if (PhoneTests.busy && PhoneTests.step.startsWith("Reading")) "Testing..." else "Test voice speed",
        { PhoneTests.testVoice(ctx) },
        Modifier.fillMaxWidth().padding(top = 12.dp),
    )
    if (PhoneTests.voiceResult.isNotBlank()) Quiet(PhoneTests.voiceResult, Modifier.padding(top = 6.dp))

    // the AI Director's models
    AiModels.all.forEach { model ->
        val cap = report.capabilities.firstOrNull { it.title.endsWith(if (model == AiModels.SMALL) "small model" else "large model") }
        if (cap != null && cap.verdict != Verdict.NO) ModelBlock(model)
    }
    if (PhoneTests.busy && PhoneTests.step.isNotBlank()) Quiet(PhoneTests.step + "...", Modifier.padding(top = 8.dp))
}

@Composable
private fun ModelBlock(model: AiModel) {
    val ctx = LocalContext.current
    val installed = AiModels.isInstalled(model)
    val prog = AiModels.progress[model.id]
    val err = AiModels.errors[model.id]
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(model.title, style = MaterialTheme.typography.titleMedium)
        when {
            prog != null -> {
                LinearProgressIndicator(progress = { prog }, color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Quiet("Downloading, ${(prog * 100).toInt()}%", Modifier.weight(1f))
                    TextButton(onClick = { AiModels.cancel(model) }) { Text("Pause", color = Ink.dusk) }
                }
            }
            installed -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    PrimaryButton(
                        if (PhoneTests.busy) "Testing..." else "Test the AI",
                        { PhoneTests.testAi(ctx, model) },
                        Modifier.weight(1f),
                        enabled = !PhoneTests.busy,
                    )
                    GhostButton("Remove", { AiModels.delete(model) }, Modifier.weight(0.6f))
                }
            }
            else -> GhostButton(
                "Download, ${model.sizeMb} MB",
                { AiModels.download(model) },
                Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        if (err != null) Text(err, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        PhoneTests.aiResults[model.id]?.forEach { r ->
            Quiet(r.line(), Modifier.padding(top = 6.dp))
        }
    }
}

private fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.GOOD -> Ink.moss
    Verdict.SLOW -> Ink.ember
    Verdict.NO -> Ink.error
}

private fun verdictLabel(v: Verdict) = when (v) {
    Verdict.GOOD -> "Works well"
    Verdict.SLOW -> "Works, slower"
    Verdict.NO -> "Not for this phone"
}
