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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.murmur.audio.PlayerHub
import app.murmur.data.Story
import app.murmur.ui.AddStoryScreen
import app.murmur.ui.Cover
import app.murmur.ui.DoneScreen
import app.murmur.ui.HomeScreen
import app.murmur.ui.Ink
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
                    NavigationBar(containerColor = Ink.lamp, tonalElevation = 0.dp) {
                        Tab(Screen.HOME, "Home", Icons.Outlined.Home, screen) { screen = it }
                        Tab(Screen.LIBRARY, "Library", Icons.Outlined.List, screen) { screen = it }
                        Tab(Screen.ADD, "Add", Icons.Outlined.Add, screen) { screen = it }
                        Tab(Screen.SETTINGS, "Settings", Icons.Outlined.Settings, screen) { screen = it }
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
private fun androidx.compose.foundation.layout.RowScope.Tab(
    target: Screen,
    label: String,
    icon: ImageVector,
    current: Screen,
    onSelect: (Screen) -> Unit,
) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onSelect(target) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Ink.lampHigh,
            selectedIconColor = Ink.ember,
            selectedTextColor = Ink.ember,
            unselectedIconColor = Ink.dusk,
            unselectedTextColor = Ink.dusk,
        ),
    )
}

@Composable
private fun MiniPlayer(onClick: () -> Unit) {
    val s = PlayerHub.current ?: return
    Surface(color = Ink.lampHigh, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            LinearProgressIndicator(progress = { PlayerHub.fraction }, color = Ink.ember, trackColor = Ink.lamp, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(s, Modifier.width(38.dp).aspectRatio(0.78f), corner = 8.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(
                        PlayerHub.status.ifBlank { if (PlayerHub.playing) "Playing" else "Paused" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.dusk,
                    )
                }
                Box(
                    Modifier.size(40.dp).background(Ink.ember, CircleShape).clickable { PlayerHub.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (PlayerHub.playing) {
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.width(4.dp).height(14.dp).background(Ink.onEmber, RoundedCornerShape(2.dp)))
                            Box(Modifier.width(4.dp).height(14.dp).background(Ink.onEmber, RoundedCornerShape(2.dp)))
                        }
                    } else Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Ink.onEmber)
                }
            }
        }
    }
}
