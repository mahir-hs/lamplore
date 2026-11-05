package app.lamplore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.lamplore.audio.PlayerHub
import app.lamplore.data.Story
import app.lamplore.ui.AboutScreen
import app.lamplore.ui.FloatingTabBar
import app.lamplore.ui.HistoryScreen
import app.lamplore.ui.ListenKind
import app.lamplore.ui.edgeSwipeToChangeTab
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import app.lamplore.ui.AddStoryScreen
import app.lamplore.ui.WelcomeScreen
import app.lamplore.ui.Cover
import app.lamplore.ui.Glyph
import app.lamplore.ui.GlyphIcon
import app.lamplore.ui.bounceClick
import app.lamplore.ui.DoneScreen
import app.lamplore.ui.HomeScreen
import app.lamplore.ui.Ink
import app.lamplore.ui.LibraryScreen
import app.lamplore.ui.ListenScreen
import app.lamplore.ui.LamploreTheme
import app.lamplore.ui.PauseScreen
import app.lamplore.ui.RunClock
import app.lamplore.ui.motionReduced
import app.lamplore.ui.PlayerScreen
import app.lamplore.ui.SettingsScreen

enum class Screen { HOME, AUDIOBOOKS, PODCASTS, UPDATES, DOWNLOADS, MYLIST, LIBRARY, HISTORY, SETTINGS, ADD, ABOUT, PLAYER, PAUSE, DONE }

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            LamploreTheme {
                Box(Modifier.fillMaxSize().background(Ink.sky)) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent, contentColor = Ink.moth) {
                        Root()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.lamplore.data.Lock.onReturn()
    }

    override fun onStop() {
        app.lamplore.data.Lock.onLeave()
        PlayerHub.saveProgress()
        super.onStop()
    }
}

@Composable
private fun Root() {
    if (app.lamplore.data.Lock.locked) {
        app.lamplore.ui.LockScreen()
        return
    }
    // first run: offer a better voice before anything else
    if (!App.onboarded) {
        WelcomeScreen(onDone = { App.finishOnboarding() })
        return
    }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val ctx = LocalContext.current

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Back from the player goes to wherever it was opened from (Tonight, Library, Pause...), not always Library.
    var origin by rememberSaveable { mutableStateOf(Screen.HOME) }
    fun showPlayer() {
        if (screen != Screen.PLAYER) origin = screen
        screen = Screen.PLAYER
    }
    fun open(s: Story) {
        PlayerHub.play(s)
        showPlayer()
    }

    val tabList = listOf(Screen.HOME, Screen.AUDIOBOOKS, Screen.PODCASTS, Screen.UPDATES, Screen.DOWNLOADS, Screen.MYLIST, Screen.LIBRARY, Screen.HISTORY, Screen.SETTINGS)
    val tabs = tabList.toSet()
    BackHandler(enabled = screen != Screen.HOME) {
        screen = when (screen) {
            Screen.PLAYER -> origin
            Screen.ADD -> Screen.LIBRARY
            Screen.ABOUT -> Screen.SETTINGS
            else -> Screen.HOME
        }
    }
    val reduceMotion = remember { motionReduced(ctx) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Ink.moth,
        bottomBar = {
            Column {
                if (PlayerHub.current != null && screen != Screen.PLAYER) MiniPlayer { showPlayer() }
                if (screen in tabs) {
                    FloatingTabBar(
                        tabs = tabList, selected = screen, label = { it.label() }, onClick = { screen = it },
                        badge = { if (it == Screen.UPDATES) app.lamplore.data.Subs.unread() else 0 },
                        icon = { t, tint -> TabIcon(t, tint) },
                    )
                }
            }
        },
    ) { pad ->
        Box(
            Modifier.padding(pad).fillMaxSize().edgeSwipeToChangeTab { d ->
                val i = tabList.indexOf(screen)
                if (i >= 0) screen = tabList[(i + d).coerceIn(0, tabList.size - 1)]
            },
        ) {
            RunClock(active = screen == Screen.PLAYER)
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (reduceMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val up = targetState == Screen.PLAYER
                        (fadeIn(tween(280)) + slideInVertically(tween(320)) { if (up) it / 12 else it / 40 }) togetherWith
                            fadeOut(tween(160))
                    }
                },
                label = "screen",
            ) { target ->
                when (target) {
                    Screen.HOME -> HomeScreen(::open, { screen = Screen.PAUSE }, { screen = Screen.DONE }, onBrowse = { screen = Screen.AUDIOBOOKS })
                    Screen.LIBRARY -> LibraryScreen(::open) { screen = Screen.ADD }
                    Screen.AUDIOBOOKS -> ListenScreen(ListenKind.BOOKS, ::open)
                    Screen.PODCASTS -> ListenScreen(ListenKind.PODCASTS, ::open)
                    Screen.UPDATES -> ListenScreen(ListenKind.UPDATES, ::open)
                    Screen.DOWNLOADS -> ListenScreen(ListenKind.DOWNLOADS, ::open)
                    Screen.MYLIST -> ListenScreen(ListenKind.MYLIST, ::open)
                    Screen.HISTORY -> HistoryScreen(::open)
                    Screen.ADD -> AddStoryScreen(onSaved = ::open, onBack = { screen = Screen.LIBRARY })
                    Screen.SETTINGS -> SettingsScreen(onAbout = { screen = Screen.ABOUT }, onPlay = ::open, onShowPlayer = { showPlayer() })
                    Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.SETTINGS })
                    Screen.PLAYER -> PlayerScreen({ screen = origin }, { screen = Screen.DONE })
                    Screen.PAUSE -> PauseScreen({ screen = Screen.HOME }, ::open)
                    Screen.DONE -> DoneScreen { screen = Screen.HOME }
                }
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

private fun Screen.label() = when (this) {
    Screen.HOME -> "Home"
    Screen.AUDIOBOOKS -> "Audiobooks"
    Screen.PODCASTS -> "Podcasts"
    Screen.UPDATES -> "Updates"
    Screen.DOWNLOADS -> "Downloads"
    Screen.MYLIST -> "My list"
    Screen.LIBRARY -> "Stories"
    Screen.HISTORY -> "History"
    Screen.SETTINGS -> "You"
    else -> name
}

@Composable
private fun TabIcon(t: Screen, tint: Color) {
    val d = t.label()
    when (t) {
        Screen.HOME -> Icon(Icons.Outlined.Home, d, tint = tint)
        Screen.AUDIOBOOKS -> GlyphIcon(Glyph.BOOK, tint, 24.dp, d)
        Screen.PODCASTS -> GlyphIcon(Glyph.PODCAST, tint, 24.dp, d)
        Screen.UPDATES -> Icon(Icons.Outlined.Notifications, d, tint = tint)
        Screen.DOWNLOADS -> GlyphIcon(Glyph.DOWNLOAD, tint, 24.dp, d)
        Screen.MYLIST -> Icon(Icons.Outlined.Star, d, tint = tint)
        Screen.LIBRARY -> Icon(Icons.Outlined.List, d, tint = tint)
        Screen.HISTORY -> Icon(Icons.Outlined.DateRange, d, tint = tint)
        else -> Icon(Icons.Outlined.Person, d, tint = tint)
    }
}

@Composable
private fun MiniPlayer(onClick: () -> Unit) {
    val s = PlayerHub.current ?: return
    Surface(color = Ink.lampHigh, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            LinearProgressIndicator(progress = { PlayerHub.fraction }, color = Ink.ember, trackColor = Ink.lamp, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(s, Modifier.width(38.dp).aspectRatio(0.8f), corner = 6.dp, mat = 2.dp)
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
                    Modifier.size(48.dp).background(Ink.aurora, CircleShape).bounceClick(if (PlayerHub.playing) "Pause" else "Play") { PlayerHub.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(if (PlayerHub.playing) Glyph.PAUSE else Glyph.PLAY, Ink.onEmber, 24.dp, if (PlayerHub.playing) "Pause" else "Play")
                }
                // dismiss: stops playback and hides the bar (progress is saved first)
                Box(
                    Modifier.size(48.dp).bounceClick("Close player") { PlayerHub.stopAll() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close player", tint = Ink.dusk)
                }
            }
        }
    }
}
