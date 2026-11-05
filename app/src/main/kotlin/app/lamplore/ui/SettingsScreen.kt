@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.lamplore.ui

import androidx.compose.foundation.layout.Arrangement
import app.lamplore.ai.AiModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lamplore.App
import app.lamplore.audio.AmbienceKind
import app.lamplore.audio.MusicMood
import app.lamplore.audio.PackManager
import app.lamplore.audio.PlayerHub
import app.lamplore.audio.VoiceInfo
import app.lamplore.audio.VoicePack
import app.lamplore.audio.VoicePacks

private enum class Page(val title: String) {
    ACTIVITY("Your activity"),
    HISTORY("History"),
    TASTE("Your taste"),
    BACKUP("Backup and restore"),
    DOWNLOADS("Downloads and updates"),
    BOOKMARKS("Bookmarks"),
    LOOK("Look and lock"),
    STORAGE_MGR("Storage manager"),
    DIAGNOSTICS("Diagnostics"),
    VOICES("Voices"),
    READING("How stories are read"),
    PHONE("Your phone and AI"),
    STORAGE("Storage and books"),
    NOTES("Your notes"),
}

/**
 * You. A short hub: your week, then one row per area. Each area is its own page, so nothing here is a long scroll.
 */
@Composable
fun SettingsScreen(onAbout: () -> Unit, onPlay: (app.lamplore.data.Story) -> Unit = {}, onShowPlayer: () -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }
    val current = page?.let { name -> Page.entries.firstOrNull { it.name == name } }
    when (current) {
        null -> Hub(onOpen = { page = it.name }, onAbout = onAbout)
        Page.ACTIVITY -> PageFrame(current.title, { page = null }) { ActivityPage() }
        Page.HISTORY -> PageFrame(current.title, { page = null }) { HistoryPage(onPlay) }
        Page.TASTE -> PageFrame(current.title, { page = null }) { TastePage() }
        Page.BACKUP -> PageFrame(current.title, { page = null }) { BackupPage() }
        Page.DOWNLOADS -> PageFrame(current.title, { page = null }) { DownloadSettingsPage() }
        Page.BOOKMARKS -> PageFrame(current.title, { page = null }) { BookmarksPage(onShowPlayer) }
        Page.LOOK -> PageFrame(current.title, { page = null }) { LookPage() }
        Page.STORAGE_MGR -> PageFrame(current.title, { page = null }) { StorageManagerPage() }
        Page.DIAGNOSTICS -> PageFrame(current.title, { page = null }) { DiagnosticsPage() }
        Page.VOICES -> PageFrame(current.title, { page = null }) { VoicesPage() }
        Page.READING -> PageFrame(current.title, { page = null }) { ReadingPage() }
        Page.PHONE -> PageFrame(current.title, { page = null }) { PhoneCheckSection() }
        Page.STORAGE -> PageFrame(current.title, { page = null }) { StoragePage() }
        Page.NOTES -> PageFrame(current.title, { page = null }) { NotesPage() }
    }
}

@Composable
private fun PageFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 6.dp)) { Text("Back", color = Ink.dusk) }
        Text(title, style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 6.dp))
        content()
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun Hub(onOpen: (Page) -> Unit, onAbout: () -> Unit) {
    val nights = App.streak()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("You", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 28.dp))
        Quiet(
            "${App.todaySeconds / 60} minutes today, $nights night${if (nights == 1) "" else "s"} in a row.",
            Modifier.padding(top = 6.dp),
        )
        GoalBlock()

        Heading("Activity")
        HubRow("Your activity", "${app.lamplore.data.Activity.seconds(java.time.LocalDate.now()) / 60} min today, heatmap, charts") { onOpen(Page.ACTIVITY) }
        HubRow("History", "${app.lamplore.data.History.visible().size} titles, with how far you got") { onOpen(Page.HISTORY) }
        HubRow("Bookmarks", "${app.lamplore.data.Bookmarks.items.size} saved moments") { onOpen(Page.BOOKMARKS) }
        HubRow("Your taste", "Top genres, authors and what you come back to") { onOpen(Page.TASTE) }
        DirectorSwitch("Incognito", "Nothing you listen to is added to history, activity or \"heard\" marks.", App.incognito) { App.incognito = it; App.saveSettings() }

        Heading("Settings")
        HubRow("Voices", narratorSummary()) { onOpen(Page.VOICES) }
        HubRow("How stories are read", readingSummary()) { onOpen(Page.READING) }
        HubRow("Your phone and AI", aiSummary()) { onOpen(Page.PHONE) }
        HubRow("Storage and books", storageSummary()) { onOpen(Page.STORAGE) }
        if (App.reflections.isNotEmpty()) HubRow("Your notes", "${App.reflections.size} saved") { onOpen(Page.NOTES) }
        HubRow("Downloads and updates", "${app.lamplore.data.Downloads.saved.size} saved, ${app.lamplore.data.Subs.shows.size} followed" + if (app.lamplore.data.Downloads.wifiOnly) ", Wi-Fi only" else "") { onOpen(Page.DOWNLOADS) }
        HubRow("Backup and restore", if (app.lamplore.data.Backup.intervalDays > 0) "Automatic backup on" else "Save everything to a file") { onOpen(Page.BACKUP) }
        HubRow("Look and lock", if (app.lamplore.data.Lock.enabled) "Black or midnight, accent, lock is on" else "Background, accent colour, app lock") { onOpen(Page.LOOK) }
        HubRow("Storage manager", "Per-title sizes and bulk delete") { onOpen(Page.STORAGE_MGR) }
        HubRow("Diagnostics", if (app.lamplore.data.Crash.last(androidx.compose.ui.platform.LocalContext.current) != null) "A crash was recorded" else "Device info and the last crash") { onOpen(Page.DIAGNOSTICS) }
        HubRow("About and credits", "Version 0.4", onAbout)
        Spacer(Modifier.height(36.dp))
    }
}

/** One line in the hub: what it is, what it is set to now, and a chevron. */
@Composable
private fun HubRow(title: String, summary: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).bounceClick(title, onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Quiet(summary, Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Ink.dusk)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
    }
}

private fun narratorSummary(): String {
    val v = App.voiceName
    val name = if (v.startsWith("sherpa:")) {
        val parts = v.split(":")
        VoicePacks.byId(parts.getOrNull(1) ?: "")?.speakers?.getOrNull(parts.getOrNull(2)?.toIntOrNull() ?: 0) ?: "Downloaded voice"
    } else "Phone voice"
    return "$name, ${PackManager.installed.size} downloaded"
}

private fun readingSummary(): String {
    val fixed = listOf(App.autoVoice, App.autoSpeed, App.autoSound, App.autoMusic).count { !it }
    return if (fixed == 0) "Voices, pace, sound and music: all automatic" else "$fixed of 4 set by hand"
}

private fun aiSummary(): String {
    val models = AiModels.installed.size
    return if (models == 0) "What this phone can run. AI Director not set up" else "AI Director: $models model${if (models == 1) "" else "s"} installed"
}

private fun storageSummary(): String {
    val mb = (App.audioBytes() + PackManager.diskBytes() + AiModels.diskBytes() + app.lamplore.data.RemoteContent.cacheBytes()) / 1_048_576
    return "$mb MB used on this phone"
}

@Composable
private fun VoicesPage() {
    var voices by remember { mutableStateOf<List<VoiceInfo>>(emptyList()) }
    var resolved by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val sliderColors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.lampHigh)
    LaunchedEffect(PackManager.installed.size, App.voiceName) {
        voices = PlayerHub.engine.voices()
        resolved = PlayerHub.engine.resolveVoice(App.voiceName)
    }

    Heading("Narrator")
    val current = voices.firstOrNull { it.name == (App.voiceName.ifBlank { resolved }) } ?: voices.firstOrNull { it.name == resolved }
    Box {
        GhostButton(current?.label ?: if (voices.isEmpty()) "Loading voices" else "Default voice", { menu = true }, Modifier.fillMaxWidth())
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            voices.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.label + if (v.offline) "" else " (needs internet)") },
                    onClick = { App.voiceName = v.name; App.saveSettings(); menu = false },
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        TextButton(onClick = { PlayerHub.preview() }) { Text("Hear this voice", color = Ink.ember) }
        if (PlayerHub.previewStatus.isNotBlank()) Quiet(PlayerHub.previewStatus)
    }
    Quiet("Pitch")
    Slider(value = App.pitch, onValueChange = { App.pitch = it }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.8f..1.2f, colors = sliderColors)

    Heading("Get more voices")
    Quiet("Natural voices that run on this phone, offline after a one-time download. Kokoro sounds best; Piper voices are small and quick. With a few installed, the Director gives dialogue its own voices.")
    Spacer(Modifier.height(8.dp))
    VoicePacks.all.forEach { pack -> PackRow(pack) }
}

@Composable
private fun ReadingPage() {
    val sliderColors = SliderDefaults.colors(thumbColor = Ink.ember, activeTrackColor = Ink.ember, inactiveTrackColor = Ink.lampHigh)
    Quiet("Lamplore reads each story first and decides who speaks, how fast, and which background sound fits the scene. Turn any of these off to keep one fixed choice.", Modifier.padding(top = 8.dp))
    DirectorSwitch("Character voices", "Dialogue gets a different voice than the narrator, matched by gender.", App.autoVoice) { App.autoVoice = it; App.saveSettings() }
    DirectorSwitch("Pace", "Slower for calm and closing lines, quicker for action.", App.autoSpeed) { App.autoSpeed = it; App.saveSettings() }
    DirectorSwitch("Background sound", "Real recordings of rain, forest, night, fire and more, chosen from the scene and crossfaded.", App.autoSound) { App.autoSound = it; App.saveSettings() }
    DirectorSwitch("Music", "A quiet instrumental bed matched to the story's mood; tense scenes get the tension bed.", App.autoMusic) { App.autoMusic = it; App.saveSettings() }

    if (!App.autoSpeed) {
        Heading("Fixed pace")
        Quiet("${"%.2f".format(App.speed)}×")
        Slider(value = App.speed, onValueChange = { App.speed = it }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.7f..1.6f, colors = sliderColors)
    }
    if (!App.autoSound) {
        Heading("Fixed background sound")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AmbienceKind.entries.forEach { k -> Pill(k.label, App.ambience == k) { App.ambience = k; App.saveSettings() } }
        }
    }
    if (!App.autoMusic) {
        Heading("Fixed music")
        FlowRow(horizontalArrangement = spacedRow, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            MusicMood.entries.forEach { m -> Pill(m.label, App.music == m) { App.music = m; App.saveSettings() } }
        }
    }
    Heading("Background volume")
    Slider(value = App.ambienceVolume, onValueChange = { App.ambienceVolume = it; PlayerHub.updateAmbience() }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0.05f..1f, colors = sliderColors)
    Heading("Music volume")
    Slider(value = App.musicVolume, onValueChange = { App.musicVolume = it; PlayerHub.updateAmbience() }, onValueChangeFinished = { App.saveSettings() }, valueRange = 0f..0.8f, colors = sliderColors)
}

@Composable
private fun StoragePage() {
    var bytes by remember { mutableLongStateOf(App.audioBytes()) }
    var picBytes by remember { mutableLongStateOf(app.lamplore.data.RemoteContent.cacheBytes()) }
    Heading("Online books")
    Quiet(app.lamplore.data.RemoteContent.status.ifBlank { "New books and illustrations are fetched from the internet, never bundled." })
    Quiet("Pictures saved on this phone: ${"%.1f".format(picBytes / 1_048_576.0)} MB. Each is downloaded once, when you first open its story.", Modifier.padding(top = 4.dp))
    Row {
        TextButton(onClick = { app.lamplore.data.RemoteContent.refresh() }) { Text("Check for new books", color = Ink.ember) }
        TextButton(onClick = { app.lamplore.data.RemoteContent.clearCache(); picBytes = 0L }) { Text("Clear pictures", color = Ink.ember) }
    }

    Heading("On this phone")
    Quiet("Story audio: ${"%.1f".format(bytes / 1_048_576.0)} MB. Downloaded voices: ${"%.0f".format(PackManager.diskBytes() / 1_048_576.0)} MB. AI models: ${"%.0f".format(AiModels.diskBytes() / 1_048_576.0)} MB.")
    Quiet("Story audio is rebuilt from the text whenever you need it, so it's safe to clear.", Modifier.padding(top = 4.dp))
    TextButton(onClick = { App.clearAudio(); bytes = App.audioBytes() }) { Text("Clear story audio", color = Ink.ember) }

    Heading("Downloaded audiobooks and podcasts")
    var dl by remember { mutableLongStateOf(app.lamplore.data.Downloads.bytes()) }
    Quiet("${app.lamplore.data.Downloads.saved.size} saved for offline, ${"%.0f".format(dl / 1_048_576.0)} MB. Remove single items from the Listen tab.")
    TextButton(onClick = { app.lamplore.data.Downloads.removeAll(); dl = 0L }) { Text("Remove all downloads", color = Ink.ember) }
}

@Composable
private fun DownloadSettingsPage() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Heading("Downloads")
    DirectorSwitch("Wi-Fi only", "Downloads wait until the phone is on Wi-Fi (or any unmetered network).", app.lamplore.data.Downloads.wifiOnly) { app.lamplore.data.Downloads.changeWifiOnly(it) }
    DirectorSwitch("Delete after finishing", "A downloaded title is removed from the phone when you listen to the end.", app.lamplore.data.Downloads.autoDelete) { app.lamplore.data.Downloads.changeAutoDelete(it) }
    Quiet("Downloads at the same time", Modifier.padding(top = 14.dp))
    FlowRow {
        listOf(1, 2, 3).forEach { n -> Pill("$n", app.lamplore.data.Downloads.maxParallel == n) { app.lamplore.data.Downloads.changeMaxParallel(n) } }
    }
    Quiet("Two is a good balance. Three is faster on a good connection and uses more battery.", Modifier.padding(top = 2.dp))

    Heading("New episodes")
    Quiet("Lamplore checks the shows you follow in the background and tells you when there is something new. Turn on Auto-download on a show to fetch its new episodes for you.")
    FlowRow {
        listOf(0 to "Off", 6 to "Every 6 hours", 12 to "Every 12 hours", 24 to "Daily").forEach { (h, l) ->
            Pill(l, app.lamplore.data.Subs.intervalHours == h) { app.lamplore.data.Subs.setInterval(ctx, h) }
        }
    }
    Quiet("Android decides the exact time to save battery, so a check can be a little late.", Modifier.padding(top = 2.dp))
}

@Composable
private fun NotesPage() {
    Quiet("One line after a story, kept on this phone.", Modifier.padding(top = 8.dp))
    App.reflections.take(30).forEach { r ->
        Column(Modifier.padding(top = 16.dp)) {
            Text(r.text, style = MaterialTheme.typography.bodyLarge)
            Quiet(r.date + (r.storyTitle?.let { ", after $it" } ?: ""))
        }
    }
}

@Composable
private fun DirectorSwitch(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Quiet(desc)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PackRow(pack: VoicePack) {
    val installed = pack.id in PackManager.installed
    val prog = PackManager.progress[pack.id]
    val err = PackManager.errors[pack.id]
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pack.title, style = MaterialTheme.typography.titleMedium)
                Quiet("${pack.desc} About ${pack.sizeMb} MB.")
            }
            when {
                prog != null && prog < 1.5f -> TextButton(onClick = { PackManager.cancel(pack) }) { Text("Cancel", color = Ink.dusk) }
                prog != null -> Quiet("Unpacking")
                installed -> TextButton(onClick = { PackManager.delete(pack) }) { Text("Remove", color = Ink.dusk) }
                else -> TextButton(onClick = { PackManager.download(pack) }) { Text("Download", color = Ink.ember) }
            }
        }
        if (prog != null && prog < 1.5f) {
            LinearProgressIndicator(progress = { prog }, color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        } else if (prog != null) {
            LinearProgressIndicator(color = Ink.ember, trackColor = Ink.lampHigh, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        if (installed && prog == null) Text("Installed", style = MaterialTheme.typography.labelSmall, color = Ink.moss)
        if (err != null) Text(err, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
}
