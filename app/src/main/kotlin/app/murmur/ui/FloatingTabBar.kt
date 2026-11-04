package app.murmur.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * A rounded pill that floats above the content and holds any number of tabs (the idea and layout of
 * aniyomi's FloatingTabBar). It scrolls sideways when the tabs do not fit; the selected tab is centred and
 * shows its name, the others show only an icon. A number in [badge] is drawn as a small dot with a count.
 */
@Composable
fun <T> FloatingTabBar(
    tabs: List<T>,
    selected: T?,
    label: (T) -> String,
    onClick: (T) -> Unit,
    badge: (T) -> Int,
    icon: @Composable (T, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    // left edge and width of every tab inside the scrolled row
    val bounds = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val selectedIndex = tabs.indexOf(selected)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        // the name grows while it animates in, so the target moves with it and the scroll follows
        snapshotFlow { bounds[selectedIndex]?.let { (left, width) -> left + width / 2 - viewport / 2 } }
            .collectLatest { target -> if (target != null) scroll.animateScrollTo(target) }
    }
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = Ink.lamp.copy(alpha = 0.96f),
            modifier = Modifier.shadow(10.dp, CircleShape),
        ) {
            Row(
                Modifier.onSizeChanged { viewport = it.width }.horizontalScroll(scroll).padding(6.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { i, tab ->
                    val on = i == selectedIndex
                    val tint = if (on) Ink.ember else Ink.dusk
                    Row(
                        Modifier
                            .onPlaced { bounds[i] = it.positionInParent().x.toInt() to it.size.width }
                            .clip(CircleShape)
                            .background(if (on) Ink.lampHigh else Color.Transparent)
                            .clickable(role = Role.Tab, onClickLabel = label(tab)) { onClick(tab) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            icon(tab, tint)
                            val n = badge(tab)
                            if (n > 0) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(start = 14.dp).clip(CircleShape).background(Ink.ember).padding(horizontal = 4.dp),
                                ) { Text(if (n > 9) "9+" else "$n", style = MaterialTheme.typography.labelSmall, color = Ink.onEmber) }
                            }
                        }
                        AnimatedVisibility(visible = on, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                            Text(label(tab), style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calls [onSwipe] with 1 (next tab) or -1 (previous tab) when a finger that started within [edge] of the left
 * or right side of the screen is dragged clearly sideways. Only the screen edge is watched, so lists and rows
 * of covers keep their own scrolling.
 */
fun Modifier.edgeSwipeToChangeTab(edge: Dp = 24.dp, onSwipe: (Int) -> Unit): Modifier = composed {
    val current by rememberUpdatedState(onSwipe)
    pointerInput(edge) {
        val edgePx = edge.toPx()
        val trigger = 72.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x > edgePx && down.position.x < size.width - edgePx) return@awaitEachGesture
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                if (!ch.pressed) break
                val dx = ch.position.x - down.position.x
                val dy = ch.position.y - down.position.y
                if (kotlin.math.abs(dx) > trigger && kotlin.math.abs(dx) > 2 * kotlin.math.abs(dy)) {
                    current(if (dx < 0) 1 else -1)
                    ch.consume()
                    break
                }
            }
        }
    }
}
