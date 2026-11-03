package app.murmur.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.murmur.data.Categories
import app.murmur.data.Story
import kotlin.random.Random

/**
 * Generated cover art: a paper-cut night landscape (sky, moon, layered hills, a few stars),
 * seeded by the story id so every story has its own fixed cover, tinted by its shelf.
 */
private class CoverPalette(val skyTop: Color, val skyBottom: Color, val moon: Color, val hill: Color)

private fun paletteFor(story: Story): CoverPalette = when (story.category) {
    Categories.FABLES -> CoverPalette(Color(0xFF1E3A36), Color(0xFF3B5B4A), Color(0xFFF2D8A0), Color(0xFF14261F))
    Categories.FAITH -> CoverPalette(Color(0xFF1C2244), Color(0xFF3E3A6B), Color(0xFFF3E2B0), Color(0xFF141531))
    Categories.CALM -> CoverPalette(Color(0xFF231B3A), Color(0xFF4A3358), Color(0xFFEFE3D0), Color(0xFF17112A))
    else -> CoverPalette(Color(0xFF3A2A1C), Color(0xFF6B4A2E), Color(0xFFF5E0B8), Color(0xFF261B12))
}

@Composable
fun Cover(story: Story, modifier: Modifier = Modifier, corner: Dp = 18.dp) {
    val p = paletteFor(story)
    val rnd = Random(story.id.hashCode())
    val moonX = 0.25f + rnd.nextFloat() * 0.5f
    val moonY = 0.18f + rnd.nextFloat() * 0.22f
    val moonR = 0.09f + rnd.nextFloat() * 0.07f
    val stars = List(14) { Offset(rnd.nextFloat(), rnd.nextFloat() * 0.55f) to (0.6f + rnd.nextFloat()) }
    val hills = List(3) { i ->
        val base = 0.62f + i * 0.12f
        Triple(base + rnd.nextFloat() * 0.06f, 0.08f + rnd.nextFloat() * 0.12f, rnd.nextFloat())
    }
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(p.skyTop, p.skyBottom)))
            for ((pos, s) in stars) {
                drawCircle(p.moon.copy(alpha = 0.55f), radius = s * size.minDimension * 0.006f, center = Offset(pos.x * size.width, pos.y * size.height))
            }
            val mc = Offset(moonX * size.width, moonY * size.height)
            val mr = moonR * size.width
            drawCircle(Brush.radialGradient(listOf(p.moon.copy(alpha = 0.35f), Color.Transparent), center = mc, radius = mr * 3.2f), radius = mr * 3.2f, center = mc)
            drawCircle(p.moon, radius = mr, center = mc)
            hills.forEachIndexed { i, (baseY, amp, phase) ->
                val tint = lerp(p.skyBottom, p.hill, 0.45f + i * 0.27f)
                hill(baseY, amp, phase, tint)
            }
        }
    }
}

private fun DrawScope.hill(baseY: Float, amp: Float, phase: Float, color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * (baseY + amp * 0.5f))
        val c1 = Offset(w * (0.25f + phase * 0.2f), h * (baseY - amp))
        val c2 = Offset(w * (0.6f + phase * 0.2f), h * (baseY + amp * 0.6f))
        cubicTo(c1.x, c1.y, c2.x, c2.y, w, h * (baseY - amp * 0.2f))
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

/** Flat tint used behind the player when the cover is enlarged. */
fun coverTint(story: Story): Color = paletteFor(story).skyBottom

@Suppress("unused")
private val sizeUnused = Size.Zero
