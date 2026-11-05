package app.lamplore.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lamplore.audio.AmbienceKind
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * One shared animation clock, in seconds. Scenes read it only inside a draw block, so a frame
 * redraws the canvases without recomposing anything.
 */
object Clock {
    var t by mutableFloatStateOf(0f)
}

/** Drives [Clock] only while `active` (the player is open), so other screens cost no frames. */
@Composable
fun RunClock(active: Boolean) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(active) {
        if (!active || motionReduced(ctx)) return@LaunchedEffect // scenes stay still
        val start = withFrameNanos { it }
        while (true) withFrameNanos { Clock.t = (it - start) / 1_000_000_000f }
    }
}

/** True when the phone's "remove animations" (animator duration scale 0) is on. */
fun motionReduced(ctx: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

class ScenePalette(val top: Color, val bottom: Color, val accent: Color)

fun scenePalette(k: AmbienceKind): ScenePalette = when (k) {
    AmbienceKind.RAIN -> ScenePalette(Color(0xFF16222E), Color(0xFF34506A), Color(0xFFA8C4DC))
    AmbienceKind.STORM -> ScenePalette(Color(0xFF0C111C), Color(0xFF2B3550), Color(0xFFE8EEFF))
    AmbienceKind.WIND -> ScenePalette(Color(0xFF1F2E36), Color(0xFF557680), Color(0xFFDCEBEE))
    AmbienceKind.OCEAN -> ScenePalette(Color(0xFF0E2236), Color(0xFF2B5C7D), Color(0xFFF2E3B8))
    AmbienceKind.STREAM -> ScenePalette(Color(0xFF14292B), Color(0xFF3A6B6A), Color(0xFFBFE7DF))
    AmbienceKind.FOREST -> ScenePalette(Color(0xFF0F2118), Color(0xFF2F5B3E), Color(0xFFE6F2A2))
    AmbienceKind.NIGHT -> ScenePalette(Color(0xFF0D0F26), Color(0xFF2C2A5C), Color(0xFFF1E6C0))
    AmbienceKind.FIRE -> ScenePalette(Color(0xFF1A0E0A), Color(0xFF4A2414), Color(0xFFFFB45C))
    AmbienceKind.DESERT -> ScenePalette(Color(0xFF3A2438), Color(0xFFC77E4E), Color(0xFFFFE2A8))
    AmbienceKind.CAVE -> ScenePalette(Color(0xFF0B0A10), Color(0xFF2A2433), Color(0xFF7FE0D0))
    AmbienceKind.HEARTBEAT -> ScenePalette(Color(0xFF1A0A0E), Color(0xFF4A1620), Color(0xFFFF7A86))
    AmbienceKind.CALM, AmbienceKind.NONE -> ScenePalette(Color(0xFF17142E), Color(0xFF3B3466), Color(0xFFBFA8F2))
}

/** One animated, procedurally drawn scene. */
@Composable
fun SceneArt(kind: AmbienceKind, modifier: Modifier = Modifier, corner: Dp = 0.dp, seed: Int = 0) {
    Box(modifier.clip(RoundedCornerShape(corner))) {
        Canvas(Modifier.fillMaxSize()) { drawScene(kind, Clock.t, seed) }
    }
}

/** The same scene, cross-fading when the Director moves to a new one. */
@Composable
fun SceneBackdrop(kind: AmbienceKind, modifier: Modifier = Modifier, corner: Dp = 0.dp) {
    Crossfade(kind, modifier = modifier, animationSpec = tween(1600), label = "scene") { k ->
        SceneArt(k, Modifier.fillMaxSize(), corner)
    }
}

// ---------------------------------------------------------------- drawing

private fun rf(i: Int, salt: Int = 0): Float {
    val x = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return x - floor(x)
}

private fun DrawScope.sky(p: ScenePalette) {
    drawRect(Brush.verticalGradient(listOf(p.top, p.bottom)))
}

private fun DrawScope.stars(t: Float, n: Int, maxY: Float, color: Color, seed: Int) {
    for (i in 0 until n) {
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (0.8f + rf(i, 3) * 1.8f) + rf(i, 4) * 6.28f))
        val r = (0.5f + rf(i, 5) * 1.3f) * size.minDimension * 0.005f
        drawCircle(color.copy(alpha = 0.85f * tw), r, Offset(rf(i + seed, 1) * size.width, rf(i + seed, 2) * size.height * maxY))
    }
}

private fun DrawScope.moon(cx: Float, cy: Float, r: Float, color: Color, t: Float) {
    val glow = 1f + 0.06f * sin(t * 0.7f)
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.38f), Color.Transparent), Offset(cx, cy), r * 3.6f * glow),
        r * 3.6f * glow, Offset(cx, cy),
    )
    drawCircle(color, r, Offset(cx, cy))
}

/** A soft ridge: baseY is a fraction of height, `drift` slides the shape sideways over time. */
private fun DrawScope.ridge(baseY: Float, amp: Float, freq: Float, phase: Float, color: Color, drift: Float = 0f) {
    val path = Path()
    val steps = 28
    path.moveTo(0f, size.height)
    for (s in 0..steps) {
        val x = size.width * s / steps
        val u = s.toFloat() / steps
        val y = size.height * (baseY + amp * sin((u * freq + phase + drift) * 2f * PI.toFloat()) +
            amp * 0.4f * sin((u * freq * 2.3f + phase * 1.7f) * 2f * PI.toFloat()))
        path.lineTo(x, y)
    }
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.pine(x: Float, baseY: Float, h: Float, color: Color) {
    val w = h * 0.38f
    val path = Path().apply {
        moveTo(x, baseY - h)
        lineTo(x + w * 0.55f, baseY - h * 0.5f)
        lineTo(x + w * 0.25f, baseY - h * 0.5f)
        lineTo(x + w, baseY - h * 0.08f)
        lineTo(x - w, baseY - h * 0.08f)
        lineTo(x - w * 0.25f, baseY - h * 0.5f)
        lineTo(x - w * 0.55f, baseY - h * 0.5f)
        close()
    }
    drawPath(path, color)
    drawRect(color, Offset(x - w * 0.08f, baseY - h * 0.08f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.1f))
}

private fun DrawScope.rain(t: Float, count: Int, speed: Float, slant: Float, len: Float, color: Color, alpha: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until count) {
        val sp = speed * (0.7f + rf(i, 1) * 0.6f)
        val y = ((t * sp + rf(i, 2)) % 1f)
        val x = (rf(i, 3) + y * slant) % 1f
        val a = alpha * (0.4f + 0.6f * rf(i, 4))
        val px = x * w
        val py = y * (h + len * h) - len * h
        drawLine(color.copy(alpha = a), Offset(px, py), Offset(px + slant * len * h, py + len * h), strokeWidth = 1.2f + rf(i, 6) * 1.4f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.waves(t: Float, baseY: Float, layers: Int, color: Color, top: Color) {
    for (l in 0 until layers) {
        val path = Path()
        val by = size.height * (baseY + l * 0.08f)
        val amp = size.height * (0.02f + l * 0.008f)
        path.moveTo(0f, size.height)
        val steps = 32
        for (s in 0..steps) {
            val u = s.toFloat() / steps
            val y = by + amp * sin(u * 7f + t * (0.8f + l * 0.35f) * (if (l % 2 == 0) 1f else -1f) + l)
            path.lineTo(size.width * u, y)
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(path, lerp(top, color, (l + 1f) / layers))
    }
}

private fun DrawScope.fireflies(t: Float, n: Int, color: Color) {
    for (i in 0 until n) {
        val x = (rf(i, 1) + 0.04f * sin(t * (0.3f + rf(i, 2) * 0.4f) + i)) * size.width
        val y = (0.35f + rf(i, 3) * 0.55f + 0.03f * sin(t * (0.4f + rf(i, 4) * 0.5f) + i * 2)) * size.height
        val pulse = max(0f, sin(t * (0.9f + rf(i, 5)) + rf(i, 6) * 6.28f))
        val r = size.minDimension * 0.012f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.75f * pulse), Color.Transparent), Offset(x, y), r * 5f), r * 5f, Offset(x, y))
        drawCircle(color.copy(alpha = 0.4f + 0.6f * pulse), r * 0.6f, Offset(x, y))
    }
}

private fun DrawScope.flame(cx: Float, baseY: Float, h: Float, w: Float, color: Color, t: Float, ph: Float) {
    val sway = sin(t * 5f + ph) * w * 0.12f
    val hh = h * (0.92f + 0.12f * sin(t * 7f + ph * 2))
    val path = Path().apply {
        moveTo(cx - w / 2, baseY)
        cubicTo(cx - w * 0.6f, baseY - hh * 0.4f, cx - w * 0.1f + sway, baseY - hh * 0.6f, cx + sway * 1.6f, baseY - hh)
        cubicTo(cx + w * 0.15f + sway, baseY - hh * 0.6f, cx + w * 0.62f, baseY - hh * 0.38f, cx + w / 2, baseY)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.embers(t: Float, cx: Float, baseY: Float, n: Int, color: Color) {
    for (i in 0 until n) {
        val life = (t * (0.25f + rf(i, 1) * 0.25f) + rf(i, 2)) % 1f
        val x = cx + (rf(i, 3) - 0.5f) * size.width * 0.16f + sin(t + i) * size.width * 0.03f * life
        val y = baseY - life * size.height * 0.55f
        drawCircle(color.copy(alpha = (1f - life) * 0.9f), size.minDimension * 0.005f * (1f - life * 0.5f) + 1f, Offset(x, y))
    }
}

internal fun DrawScope.drawScene(kind: AmbienceKind, t: Float, seed: Int) {
    val p = scenePalette(kind)
    val w = size.width
    val h = size.height
    sky(p)
    when (kind) {
        AmbienceKind.NIGHT -> {
            stars(t, 46, 0.65f, p.accent, seed)
            moon(w * 0.7f, h * 0.24f, w * 0.07f, p.accent, t)
            // shooting star every ~9s
            val s = (t / 9f + rf(seed, 9)) % 1f
            if (s < 0.06f) {
                val u = s / 0.06f
                val a = 1f - u
                val x = w * (0.1f + 0.5f * u)
                val y = h * (0.05f + 0.22f * u)
                drawLine(Color.White.copy(alpha = a), Offset(x, y), Offset(x - w * 0.09f, y - h * 0.05f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            ridge(0.78f, 0.04f, 1.4f, 0.1f, lerp(p.bottom, Color.Black, 0.45f))
            ridge(0.88f, 0.03f, 1.1f, 0.5f, lerp(p.bottom, Color.Black, 0.75f))
            val grass = lerp(p.bottom, Color.Black, 0.8f)
            for (i in 0 until 7) pine(w * (0.08f + i * 0.15f + rf(i, 8) * 0.05f), h * 1.0f, h * (0.16f + rf(i, 9) * 0.1f), grass)
            fireflies(t, 8, Color(0xFFE6F2A2))
        }
        AmbienceKind.RAIN, AmbienceKind.STORM -> {
            val storm = kind == AmbienceKind.STORM
            // clouds
            for (i in 0 until 5) {
                val cx = ((rf(i, 1) + t * (0.004f + rf(i, 2) * 0.006f)) % 1.3f - 0.15f) * w
                val cy = h * (0.1f + rf(i, 3) * 0.2f)
                val r = w * (0.18f + rf(i, 4) * 0.12f)
                drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = if (storm) 0.55f else 0.35f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
            }
            if (storm) {
                val c = (t * 0.23f + rf(seed, 4)) % 1f
                if (c < 0.05f) {
                    val a = (1f - c / 0.05f)
                    drawRect(Color.White.copy(alpha = 0.35f * a))
                    val bolt = Path().apply {
                        val sx = w * (0.3f + rf(seed + (t * 0.23f).toInt(), 6) * 0.4f)
                        moveTo(sx, 0f); lineTo(sx - w * 0.04f, h * 0.18f); lineTo(sx + w * 0.02f, h * 0.2f)
                        lineTo(sx - w * 0.05f, h * 0.42f); lineTo(sx + w * 0.01f, h * 0.43f); lineTo(sx - w * 0.07f, h * 0.62f)
                    }
                    drawPath(bolt, Color.White.copy(alpha = a), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                }
            }
            ridge(0.82f, 0.035f, 1.2f, 0.3f, lerp(p.bottom, Color.Black, 0.55f))
            ridge(0.92f, 0.025f, 1.6f, 0.8f, lerp(p.bottom, Color.Black, 0.8f))
            rain(t, if (storm) 150 else 90, if (storm) 1.7f else 1.1f, if (storm) 0.18f else 0.06f, if (storm) 0.09f else 0.06f, p.accent, if (storm) 0.55f else 0.4f)
            // ripples
            for (i in 0 until 6) {
                val life = (t * 0.5f + rf(i, 1)) % 1f
                val x = rf(i, 2) * w
                val y = h * (0.9f + rf(i, 3) * 0.08f)
                drawOval(p.accent.copy(alpha = (1f - life) * 0.35f), Offset(x - life * w * 0.04f, y - life * h * 0.006f), androidx.compose.ui.geometry.Size(life * w * 0.08f, life * h * 0.012f), style = Stroke(1.5f))
            }
        }
        AmbienceKind.WIND -> {
            stars(t, 18, 0.4f, p.accent, seed)
            for (i in 0 until 9) {
                val y = h * (0.15f + rf(i, 1) * 0.6f)
                val prog = (t * (0.08f + rf(i, 2) * 0.1f) + rf(i, 3)) % 1.4f - 0.2f
                val x0 = w * prog
                val len = w * (0.18f + rf(i, 4) * 0.22f)
                val path = Path().apply {
                    moveTo(x0, y)
                    cubicTo(x0 + len * 0.3f, y - h * 0.03f, x0 + len * 0.6f, y + h * 0.03f, x0 + len, y - h * 0.005f * sin(t + i))
                }
                drawPath(path, p.accent.copy(alpha = 0.28f), style = Stroke(2.2f, cap = StrokeCap.Round))
            }
            ridge(0.8f, 0.05f, 1.0f, 0.2f, lerp(p.bottom, Color.Black, 0.5f), drift = t * 0.01f)
            // leaves
            for (i in 0 until 8) {
                val prog = (t * (0.1f + rf(i, 1) * 0.12f) + rf(i, 2)) % 1.2f - 0.1f
                val x = w * prog
                val y = h * (0.3f + rf(i, 3) * 0.5f) + sin(t * 2f + i) * h * 0.04f
                drawOval(Color(0xFFB9C98A).copy(alpha = 0.7f), Offset(x, y), androidx.compose.ui.geometry.Size(w * 0.02f, w * 0.01f))
            }
        }
        AmbienceKind.OCEAN -> {
            stars(t, 22, 0.35f, p.accent, seed)
            moon(w * 0.3f, h * 0.22f, w * 0.06f, p.accent, t)
            // moon path on water
            for (i in 0 until 8) {
                val y = h * (0.52f + i * 0.04f)
                val ww = w * (0.04f + i * 0.012f) * (1f + 0.3f * sin(t * 2 + i))
                drawLine(p.accent.copy(alpha = 0.3f - i * 0.03f), Offset(w * 0.3f - ww, y), Offset(w * 0.3f + ww, y), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            waves(t, 0.5f, 5, Color(0xFF081A2C), p.bottom)
        }
        AmbienceKind.STREAM -> {
            stars(t, 20, 0.35f, p.accent, seed)
            ridge(0.5f, 0.05f, 1.1f, 0.4f, lerp(p.bottom, Color.Black, 0.45f))
            ridge(0.62f, 0.04f, 1.3f, 0.9f, lerp(p.bottom, Color.Black, 0.6f))
            // flowing water ribbon
            val path = Path()
            val steps = 24
            for (s in 0..steps) {
                val u = s.toFloat() / steps
                val x = w * (0.55f + 0.18f * sin(u * 3.2f + 0.6f) * (0.4f + u))
                val y = h * (0.58f + 0.42f * u)
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, p.accent.copy(alpha = 0.35f), style = Stroke(w * 0.07f, cap = StrokeCap.Round))
            for (i in 0 until 20) {
                val u = (t * (0.06f + rf(i, 1) * 0.06f) + rf(i, 2)) % 1f
                val x = w * (0.55f + 0.18f * sin(u * 3.2f + 0.6f) * (0.4f + u)) + (rf(i, 3) - 0.5f) * w * 0.05f
                val y = h * (0.58f + 0.42f * u)
                drawCircle(Color.White.copy(alpha = 0.55f * sin(u * PI.toFloat())), w * 0.004f + 1f, Offset(x, y))
            }
            ridge(0.95f, 0.02f, 2f, 0.1f, lerp(p.bottom, Color.Black, 0.85f))
        }
        AmbienceKind.FOREST -> {
            // light shafts
            for (i in 0 until 3) {
                val x = w * (0.2f + i * 0.28f)
                val a = 0.07f + 0.04f * sin(t * 0.5f + i)
                val shaft = Path().apply { moveTo(x, 0f); lineTo(x + w * 0.1f, 0f); lineTo(x + w * 0.32f, h); lineTo(x + w * 0.1f, h); close() }
                drawPath(shaft, p.accent.copy(alpha = a))
            }
            ridge(0.62f, 0.04f, 1.2f, 0.3f, lerp(p.bottom, Color.Black, 0.4f))
            for (i in 0 until 9) pine(w * (rf(i, 1)), h * 0.78f, h * (0.28f + rf(i, 2) * 0.12f) * (1f + 0.01f * sin(t + i)), lerp(p.bottom, Color.Black, 0.55f))
            ridge(0.8f, 0.03f, 1.5f, 0.2f, lerp(p.bottom, Color.Black, 0.7f))
            for (i in 0 until 7) pine(w * (rf(i, 5)), h * 1.02f, h * (0.38f + rf(i, 6) * 0.16f), lerp(p.bottom, Color.Black, 0.85f))
            fireflies(t, 22, p.accent)
        }
        AmbienceKind.FIRE -> {
            stars(t, 28, 0.5f, Color(0xFFFFE3B0), seed)
            val glow = 0.85f + 0.15f * sin(t * 9f) * sin(t * 3.3f)
            val cx = w * 0.5f
            val by = h * 0.82f
            drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.5f * glow), Color.Transparent), Offset(cx, by), w * 0.6f), w * 0.6f, Offset(cx, by))
            ridge(0.86f, 0.03f, 1.2f, 0.5f, lerp(p.bottom, Color.Black, 0.7f))
            for (i in 0 until 7) pine(w * (0.04f + i * 0.16f), h * 0.9f, h * (0.2f + rf(i, 2) * 0.1f), Color(0xFF0A0605))
            // logs
            drawLine(Color(0xFF2B1810), Offset(cx - w * 0.14f, by + h * 0.02f), Offset(cx + w * 0.14f, by - h * 0.01f), strokeWidth = h * 0.035f, cap = StrokeCap.Round)
            drawLine(Color(0xFF2B1810), Offset(cx - w * 0.14f, by - h * 0.01f), Offset(cx + w * 0.14f, by + h * 0.02f), strokeWidth = h * 0.035f, cap = StrokeCap.Round)
            flame(cx, by, h * 0.28f, w * 0.2f, Color(0xFFE2582B), t, 0f)
            flame(cx - w * 0.04f, by, h * 0.2f, w * 0.13f, Color(0xFFFF9A3C), t, 1.7f)
            flame(cx + w * 0.05f, by, h * 0.22f, w * 0.12f, Color(0xFFFFB45C), t, 3.1f)
            flame(cx, by, h * 0.13f, w * 0.08f, Color(0xFFFFE9A8), t, 5.2f)
            embers(t, cx, by - h * 0.1f, 26, Color(0xFFFFC27A))
        }
        AmbienceKind.DESERT -> {
            val sunY = h * (0.38f + 0.02f * sin(t * 0.2f))
            drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.55f), Color.Transparent), Offset(w * 0.5f, sunY), w * 0.55f), w * 0.55f, Offset(w * 0.5f, sunY))
            drawCircle(p.accent, w * 0.09f, Offset(w * 0.5f, sunY))
            ridge(0.62f, 0.05f, 0.8f, 0.1f, lerp(p.bottom, Color(0xFF3A1F2B), 0.35f), drift = t * 0.004f)
            ridge(0.74f, 0.05f, 0.9f, 0.6f, lerp(p.bottom, Color(0xFF3A1F2B), 0.6f), drift = -t * 0.006f)
            ridge(0.88f, 0.04f, 1.1f, 0.3f, lerp(p.bottom, Color(0xFF1C0E18), 0.85f), drift = t * 0.01f)
            for (i in 0 until 40) {
                val prog = (t * (0.07f + rf(i, 1) * 0.1f) + rf(i, 2)) % 1.1f - 0.05f
                val y = h * (0.55f + rf(i, 3) * 0.4f) + sin(t * 3 + i) * h * 0.005f
                drawCircle(p.accent.copy(alpha = 0.4f), 1.4f, Offset(w * prog, y))
            }
        }
        AmbienceKind.CAVE -> {
            // stalactites
            val rock = Color(0xFF05040A)
            for (i in 0 until 9) {
                val x = w * (i / 8f) + (rf(i, 1) - 0.5f) * w * 0.05f
                val len = h * (0.12f + rf(i, 2) * 0.28f)
                val path = Path().apply { moveTo(x - w * 0.05f, 0f); lineTo(x, len); lineTo(x + w * 0.05f, 0f); close() }
                drawPath(path, rock)
            }
            ridge(0.9f, 0.04f, 1.6f, 0.4f, rock)
            // glowing crystals
            for (i in 0 until 5) {
                val x = w * (0.12f + i * 0.19f)
                val y = h * (0.82f + rf(i, 4) * 0.08f)
                val pulse = 0.5f + 0.5f * sin(t * 1.2f + i * 1.9f)
                drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.5f * pulse), Color.Transparent), Offset(x, y), w * 0.1f), w * 0.1f, Offset(x, y))
                val c = Path().apply { moveTo(x, y - h * 0.07f); lineTo(x + w * 0.02f, y); lineTo(x - w * 0.02f, y); close() }
                drawPath(c, p.accent.copy(alpha = 0.7f + 0.3f * pulse))
            }
            // drips
            for (i in 0 until 4) {
                val life = (t * 0.35f + rf(i, 7)) % 1f
                val x = w * (0.15f + rf(i, 8) * 0.7f)
                val y = h * (0.2f + life * life * 0.65f)
                drawCircle(p.accent.copy(alpha = 0.8f * (1f - life * 0.4f)), w * 0.006f + 1f, Offset(x, y))
            }
        }
        AmbienceKind.HEARTBEAT -> {
            val ph = t % 1.1f
            val beat = exp(-ph * 7f) + 0.6f * exp(-((ph - 0.28f) * 9f) * ((ph - 0.28f) * 9f))
            val c = Offset(w * 0.5f, h * 0.5f)
            for (i in 0 until 4) {
                val rr = w * (0.14f + i * 0.12f) * (1f + 0.12f * beat)
                drawCircle(p.accent.copy(alpha = (0.28f - i * 0.06f) * (0.5f + beat)), rr, c, style = Stroke(2.5f))
            }
            drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.55f), Color.Transparent), c, w * 0.4f * (1f + 0.15f * beat)), w * 0.4f * (1f + 0.15f * beat), c)
            ridge(0.92f, 0.03f, 1.4f, 0.2f, Color(0xFF0A0306))
        }
        AmbienceKind.CALM, AmbienceKind.NONE -> {
            // slow aurora
            for (i in 0 until 4) {
                val cx = w * (0.2f + i * 0.2f + 0.08f * sin(t * 0.18f + i * 1.7f))
                val cy = h * (0.35f + 0.1f * sin(t * 0.22f + i))
                val r = w * (0.34f + 0.05f * sin(t * 0.3f + i))
                val col = lerp(p.accent, Color(0xFF6FD6C2), i / 3f)
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.22f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
            }
            stars(t, 40, 0.6f, Color.White, seed)
            ridge(0.84f, 0.04f, 1.2f, 0.7f, lerp(p.bottom, Color.Black, 0.6f), drift = t * 0.003f)
            ridge(0.92f, 0.03f, 1.5f, 0.2f, lerp(p.bottom, Color.Black, 0.85f))
        }
    }
}
