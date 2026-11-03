package app.murmur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.murmur.audio.PlayerHub
import app.murmur.data.Story
import app.murmur.ui.AddStoryScreen
import app.murmur.ui.DoneScreen
import app.murmur.ui.HomeScreen
import app.murmur.ui.LibraryScreen
import app.murmur.ui.MurmurTheme
import app.murmur.ui.PauseScreen
import app.murmur.ui.PlayerScreen
import app.murmur.ui.SettingsScreen

enum class Screen { HOME, LIBRARY, ADD, SETTINGS, PLAYER, PAUSE, DONE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MurmurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root()
                }
            }
        }
    }

    override fun onStop() {
        PlayerHub.saveProgress()
        super.onStop()
    }
}

@Composable
private fun Root() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val ctx = LocalContext.current

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun open(s: Story) {
        PlayerHub.play(s)
        screen = Screen.PLAYER
    }

    val tabs = setOf(Screen.HOME, Screen.LIBRARY, Screen.ADD, Screen.SETTINGS)
    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.PLAYER) Screen.LIBRARY else Screen.HOME
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                if (PlayerHub.current != null && screen != Screen.PLAYER) MiniPlayer { screen = Screen.PLAYER }
                if (screen in tabs) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            Triple(Screen.HOME, "🏠", "Home"),
                            Triple(Screen.LIBRARY, "📚", "Library"),
                            Triple(Screen.ADD, "➕", "Add"),
                            Triple(Screen.SETTINGS, "⚙️", "Settings"),
                        ).forEach { (s, icon, label) ->
                            NavigationBarItem(
                                selected = screen == s,
                                onClick = { screen = s },
                                icon = { Text(icon) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(::open, { screen = Screen.PAUSE }, { screen = Screen.DONE })
                Screen.LIBRARY -> LibraryScreen(::open)
                Screen.ADD -> AddStoryScreen(::open)
                Screen.SETTINGS -> SettingsScreen()
                Screen.PLAYER -> PlayerScreen({ screen = Screen.LIBRARY }, { screen = Screen.DONE })
                Screen.PAUSE -> PauseScreen({ screen = Screen.HOME }, ::open)
                Screen.DONE -> DoneScreen { screen = Screen.HOME }
            }
        }
    }
}

@Composable
private fun MiniPlayer(onClick: () -> Unit) {
    val s = PlayerHub.current ?: return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            LinearProgressIndicator(progress = { PlayerHub.fraction }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(
                        PlayerHub.status.ifBlank { "Part ${PlayerHub.part + 1} of ${PlayerHub.parts}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { PlayerHub.togglePlay() }) { Text(if (PlayerHub.playing) "Pause" else "Play") }
            }
        }
    }
}
