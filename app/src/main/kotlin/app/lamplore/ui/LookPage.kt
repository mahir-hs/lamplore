@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.lamplore.data.Lock

/** Background, accent colour and the optional app lock. */
@Composable
fun ColumnScope.LookPage() {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }

    Heading("Background")
    FlowRow(horizontalArrangement = spacedRow) {
        Pill("Midnight", Looks.background == 0) { Looks.chooseBackground(0) }
        Pill("Black (AMOLED)", Looks.background == 1) { Looks.chooseBackground(1) }
    }
    Quiet("Black saves battery on OLED screens.", Modifier.padding(top = 2.dp))

    Heading("Accent")
    FlowRow(horizontalArrangement = spacedRow) {
        Pill("Aurora", Looks.accent == 0) { Looks.chooseAccent(0) }
        Pill("Sunrise", Looks.accent == 1) { Looks.chooseAccent(1) }
        Pill("Violet", Looks.accent == 2) { Looks.chooseAccent(2) }
        Pill("Rose", Looks.accent == 3) { Looks.chooseAccent(3) }
        if (Looks.hasDynamic) Pill("Match my phone", Looks.accent == 4) { Looks.chooseAccent(4) }
    }
    Quiet("A light theme is not offered: the story scenes and the player are painted for the dark.", Modifier.padding(top = 2.dp))

    Heading("App lock")
    val available = Lock.available(ctx)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Lock Lamplore", style = MaterialTheme.typography.titleMedium)
            Quiet("Ask for your fingerprint, face or phone PIN when Lamplore opens. Playback keeps going while locked.")
        }
        Switch(
            checked = Lock.enabled,
            onCheckedChange = { want ->
                if (!want) Lock.enable(false)
                else if (!available) msg = "Set a screen lock (PIN, pattern or fingerprint) in Android settings first."
                else (ctx as? FragmentActivity)?.let { a -> Lock.prompt(a) { Lock.enable(true); msg = "Lock is on." } }
            },
        )
    }
    if (Lock.enabled) {
        Quiet("Ask again after being away for", Modifier.padding(top = 10.dp))
        FlowRow(horizontalArrangement = spacedRow) {
            listOf(0 to "Always", 60 to "1 minute", 300 to "5 minutes").forEach { (s, l) -> Pill(l, Lock.graceSeconds == s) { Lock.changeGrace(s) } }
        }
    }
    if (msg.isNotBlank()) Quiet(msg, Modifier.padding(top = 8.dp))
}

/** Shown instead of the app while it is locked. */
@Composable
fun LockScreen() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { (ctx as? FragmentActivity)?.let { Lock.prompt(it) {} } }
    Box(Modifier.fillMaxSize().background(Ink.night), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Lamplore", style = MaterialTheme.typography.displayMedium)
            Quiet("Locked", Modifier.padding(top = 4.dp))
            PrimaryButton("Unlock", { (ctx as? FragmentActivity)?.let { Lock.prompt(it) {} } }, Modifier.padding(top = 24.dp))
        }
    }
}
