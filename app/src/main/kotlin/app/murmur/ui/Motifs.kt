package app.murmur.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.murmur.data.Motif
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Paper = Color(0xFFF2E8DC)
private val Shade = Color(0xFFCDBFAE)
private val Dark = Color(0xFF15110F)
private val Amber = Color(0xFFE0A458)
private val Gold = Color(0xFFFFD27A)

/** Tiny drawing kit: coordinates are in units of `s`, origin at (cx, cy), y pointing down. */
private class Pen(val d: DrawScope, val cx: Float, val cy: Float, val s: Float) {
    fun p(x: Float, y: Float) = Offset(cx + x * s, cy + y * s)
    fun oval(x: Float, y: Float, rx: Float, ry: Float, c: Color) =
        d.drawOval(c, Offset(cx + (x - rx) * s, cy + (y - ry) * s), Size(rx * 2 * s, ry * 2 * s))
    fun circle(x: Float, y: Float, r: Float, c: Color) = d.drawCircle(c, r * s, p(x, y))
    fun poly(c: Color, vararg xy: Float) {
        val path = Path().apply {
            moveTo(cx + xy[0] * s, cy + xy[1] * s)
            var i = 2
            while (i < xy.size) { lineTo(cx + xy[i] * s, cy + xy[i + 1] * s); i += 2 }
            close()
        }
        d.drawPath(path, c)
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, c: Color) =
        d.drawLine(c, p(x1, y1), p(x2, y2), strokeWidth = w * s, cap = StrokeCap.Round)
    fun rect(x: Float, y: Float, w: Float, h: Float, c: Color) =
        d.drawRect(c, p(x, y), Size(w * s, h * s))
    fun glow(x: Float, y: Float, r: Float, c: Color, a: Float) =
        d.drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), p(x, y), r * s), r * s, p(x, y))
    fun curve(w: Float, c: Color, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val path = Path().apply { moveTo(cx + x0 * s, cy + y0 * s); cubicTo(cx + x1 * s, cy + y1 * s, cx + x2 * s, cy + y2 * s, cx + x3 * s, cy + y3 * s) }
        d.drawPath(path, c, style = Stroke(w * s, cap = StrokeCap.Round))
    }
}

fun DrawScope.drawMotif(m: Motif, t: Float) {
    val s = min(size.width, size.height * 0.85f)
    val pen = Pen(this, size.width / 2f, size.height * 0.64f, s)
    // soft ground shadow so the subject sits on the scene
    pen.oval(0f, 0.34f, 0.42f, 0.035f, Color.Black.copy(alpha = 0.28f))
    when (m) {
        Motif.TORTOISE_HARE -> pen.tortoiseHare()
        Motif.LION -> pen.lion()
        Motif.CROW_PITCHER -> pen.crowPitcher()
        Motif.SUN_WIND -> pen.sunWind(t)
        Motif.MOUNTAIN -> pen.mountain()
        Motif.HORSE -> pen.horse(t)
        Motif.POT -> pen.pot(t)
        Motif.LANTERN -> pen.lantern(t)
        Motif.FOX_GRAPES -> pen.foxGrapes()
        Motif.WOLF -> pen.wolf()
        Motif.ANT -> pen.ant()
        Motif.GOOSE -> pen.goose(t)
        Motif.SWAN -> pen.swan(t)
        Motif.ELEPHANT -> pen.elephant()
        Motif.LIGHTHOUSE -> pen.lighthouse(t)
        Motif.TRAIN -> pen.train(t)
        Motif.CABIN -> pen.cabin(t)
        Motif.BOAT -> pen.boat(t)
        Motif.MOON -> pen.moonHouses()
        Motif.BOOK -> pen.book(t)
    }
}

private fun Pen.tortoiseHare() {
    // sleeping hare on the left
    oval(-0.28f, 0.24f, 0.17f, 0.1f, Paper)
    circle(-0.5f, 0.21f, 0.07f, Paper)
    oval(-0.46f, 0.06f, 0.022f, 0.09f, Paper)
    oval(-0.53f, 0.07f, 0.02f, 0.085f, Shade)
    line(-0.545f, 0.21f, -0.5f, 0.215f, 0.012f, Dark)
    circle(-0.11f, 0.22f, 0.03f, Paper)
    // tortoise on the right
    oval(0.06f, 0.3f, 0.05f, 0.035f, Shade)
    oval(0.28f, 0.3f, 0.05f, 0.035f, Shade)
    oval(0.17f, 0.2f, 0.25f, 0.16f, Color(0xFF9DB58A))
    circle(0.17f, 0.15f, 0.07f, Color(0xFF7E9A6C))
    circle(0.05f, 0.21f, 0.05f, Color(0xFF7E9A6C))
    circle(0.3f, 0.21f, 0.05f, Color(0xFF7E9A6C))
    circle(0.43f, 0.22f, 0.07f, Paper)
    circle(0.45f, 0.205f, 0.012f, Dark)
}

private fun Pen.lion() {
    val mane = Amber
    circle(0f, 0.1f, 0.33f, mane.copy(alpha = 0.9f))
    for (i in 0 until 14) {
        val a = i * 2f * PI.toFloat() / 14f
        circle(cos(a) * 0.3f, 0.1f + sin(a) * 0.3f, 0.07f, Color(0xFFC98638))
    }
    circle(-0.17f, -0.1f, 0.06f, Color(0xFFD99A4A))
    circle(0.17f, -0.1f, 0.06f, Color(0xFFD99A4A))
    circle(0f, 0.12f, 0.21f, Color(0xFFF2D49B))
    circle(-0.07f, 0.07f, 0.018f, Dark)
    circle(0.07f, 0.07f, 0.018f, Dark)
    poly(Color(0xFF5B3A22), -0.04f, 0.12f, 0.04f, 0.12f, 0f, 0.17f)
    line(0f, 0.17f, 0f, 0.21f, 0.01f, Color(0xFF5B3A22))
    line(0f, 0.21f, -0.05f, 0.24f, 0.01f, Color(0xFF5B3A22))
    line(0f, 0.21f, 0.05f, 0.24f, 0.01f, Color(0xFF5B3A22))
    // the little mouse at the lion's feet
    oval(0.34f, 0.31f, 0.06f, 0.035f, Shade)
    circle(0.4f, 0.295f, 0.025f, Shade)
}

private fun Pen.crowPitcher() {
    // pitcher
    oval(0.2f, 0.13f, 0.17f, 0.2f, Color(0xFFD9A06A))
    oval(0.2f, -0.1f, 0.075f, 0.1f, Color(0xFFD9A06A))
    oval(0.2f, -0.19f, 0.1f, 0.025f, Color(0xFFB9803A))
    oval(0.2f, 0.28f, 0.14f, 0.05f, Color(0xFF8E5E32).copy(alpha = 0.5f))
    // pebbles rising in the jar
    circle(0.13f, 0.27f, 0.03f, Shade); circle(0.2f, 0.28f, 0.03f, Shade); circle(0.27f, 0.27f, 0.03f, Shade)
    circle(0.17f, 0.22f, 0.028f, Paper); circle(0.24f, 0.23f, 0.028f, Paper)
    // crow
    val crow = Color(0xFF8D93A8)
    poly(crow, -0.45f, 0.1f, -0.3f, 0.1f, -0.26f, 0.2f, -0.45f, 0.24f)
    oval(-0.22f, 0.15f, 0.14f, 0.09f, crow)
    circle(-0.08f, 0.04f, 0.065f, crow)
    poly(Gold, -0.03f, 0.025f, 0.07f, 0.045f, -0.03f, 0.065f)
    circle(-0.095f, 0.03f, 0.011f, Dark)
    line(-0.22f, 0.22f, -0.22f, 0.3f, 0.015f, Gold)
    line(-0.15f, 0.22f, -0.15f, 0.3f, 0.015f, Gold)
    // a pebble in the beak
    circle(0.1f, 0.05f, 0.02f, Paper)
}

private fun Pen.sunWind(t: Float) {
    val c = p(0.18f, -0.02f)
    glow(0.18f, -0.02f, 0.5f, Gold, 0.45f)
    for (i in 0 until 12) {
        val a = i * 2f * PI.toFloat() / 12f + t * 0.15f
        d.drawLine(Gold, Offset(c.x + cos(a) * 0.24f * s, c.y + sin(a) * 0.24f * s), Offset(c.x + cos(a) * 0.33f * s, c.y + sin(a) * 0.33f * s), strokeWidth = 0.025f * s, cap = StrokeCap.Round)
    }
    circle(0.18f, -0.02f, 0.19f, Color(0xFFFFE2A0))
    circle(0.14f, -0.04f, 0.012f, Dark); circle(0.22f, -0.04f, 0.012f, Dark)
    curve(0.012f, Dark, 0.14f, 0.02f, 0.17f, 0.05f, 0.2f, 0.05f, 0.23f, 0.02f)
    // wind lines
    val sway = sin(t * 1.4f) * 0.02f
    curve(0.022f, Paper.copy(alpha = 0.85f), -0.52f, 0.0f + sway, -0.4f, -0.08f, -0.3f, 0.08f, -0.18f, 0.0f)
    curve(0.022f, Paper.copy(alpha = 0.7f), -0.5f, 0.1f - sway, -0.38f, 0.03f, -0.28f, 0.17f, -0.14f, 0.1f)
    curve(0.022f, Paper.copy(alpha = 0.55f), -0.46f, -0.1f + sway, -0.36f, -0.17f, -0.28f, -0.03f, -0.18f, -0.1f)
    // traveler in a cloak
    poly(Color(0xFF6B4A8E), 0.05f, 0.14f, 0.2f, 0.14f, 0.28f, 0.34f, -0.03f, 0.34f)
    circle(0.125f, 0.1f, 0.04f, Paper)
}

private fun Pen.mountain() {
    poly(Shade, 0.1f, 0.34f, 0.32f, -0.1f, 0.55f, 0.34f)
    poly(Paper, -0.5f, 0.34f, -0.02f, -0.32f, 0.46f, 0.34f)
    poly(Color.White, -0.12f, -0.17f, -0.02f, -0.32f, 0.08f, -0.18f, 0.03f, -0.13f, -0.03f, -0.17f, -0.07f, -0.12f)
    poly(Color(0xFFBCAF9F), -0.02f, -0.32f, 0.46f, 0.34f, 0.12f, 0.34f)
    // the stonecutter at the foot with a hammer
    circle(-0.36f, 0.26f, 0.025f, Dark)
    line(-0.36f, 0.285f, -0.36f, 0.34f, 0.025f, Dark)
    line(-0.36f, 0.3f, -0.31f, 0.26f, 0.012f, Dark)
    line(-0.33f, 0.24f, -0.29f, 0.27f, 0.022f, Dark)
}

private fun Pen.horse(t: Float) {
    val body = Color(0xFFB77B4B)
    val mane = Color(0xFF4A2E1C)
    val tail = sin(t * 2f) * 0.03f
    curve(0.04f, mane, -0.2f, 0.06f, -0.34f + tail, 0.1f, -0.36f, 0.22f + tail, -0.3f, 0.32f)
    for (x in listOf(-0.16f, -0.08f, 0.1f, 0.18f)) line(x, 0.14f, x, 0.32f, 0.04f, Color(0xFF8E5A33))
    oval(0f, 0.08f, 0.24f, 0.12f, body)
    poly(body, 0.1f, 0.06f, 0.2f, -0.22f, 0.3f, -0.18f, 0.26f, 0.1f)
    poly(body, 0.2f, -0.26f, 0.4f, -0.16f, 0.37f, -0.08f, 0.2f, -0.13f)
    poly(body, 0.22f, -0.26f, 0.24f, -0.34f, 0.27f, -0.25f)
    curve(0.035f, mane, 0.19f, -0.24f, 0.15f, -0.12f, 0.12f, -0.02f, 0.1f, 0.06f)
    circle(0.28f, -0.19f, 0.012f, Dark)
}

private fun Pen.pot(t: Float) {
    // fire
    for (i in 0 until 3) {
        val f = 1f + 0.18f * sin(t * (7f + i * 2f) + i)
        poly(listOf(Color(0xFFE2582B), Color(0xFFFF9A3C), Color(0xFFFFD27A))[i],
            -0.1f + i * 0.1f, 0.34f, -0.04f + i * 0.1f - 0.02f, 0.34f - 0.16f * f, 0.0f + i * 0.1f, 0.34f)
    }
    line(-0.18f, 0.35f, 0.18f, 0.32f, 0.04f, Color(0xFF3A2216))
    // pot
    d.drawArc(Color(0xFF3F3A38), 0f, 180f, true, p(-0.26f, -0.02f), Size(0.52f * s, 0.42f * s))
    oval(0f, 0.0f, 0.28f, 0.04f, Paper)
    oval(0f, 0.0f, 0.24f, 0.025f, Color(0xFFE5C87A))
    circle(0f, -0.04f, 0.055f, Shade)  // the stone
    line(-0.28f, 0.0f, -0.34f, 0.05f, 0.025f, Paper)
    line(0.28f, 0.0f, 0.34f, 0.05f, 0.025f, Paper)
    // steam
    for (i in 0 until 3) {
        val life = (t * 0.35f + i / 3f) % 1f
        val x = -0.1f + i * 0.1f + sin(t * 2f + i) * 0.03f
        circle(x, -0.06f - life * 0.32f, 0.03f + life * 0.03f, Paper.copy(alpha = 0.55f * (1f - life)))
    }
}

private fun Pen.lantern(t: Float) {
    oval(0f, 0.46f, 0.6f, 0.24f, Color(0xFF1B3A2E))
    oval(0.05f, 0.44f, 0.5f, 0.2f, Color(0xFF244C3B))
    curve(0.03f, Shade.copy(alpha = 0.6f), -0.3f, 0.34f, -0.1f, 0.3f, 0.12f, 0.26f, 0.02f, 0.2f)
    val flick = 0.85f + 0.15f * sin(t * 6f) * sin(t * 2.3f)
    glow(0f, -0.1f, 0.55f, Amber, 0.5f * flick)
    line(0f, 0.22f, 0f, -0.05f, 0.025f, Color(0xFF5B4632))
    line(0f, -0.05f, 0.1f, -0.05f, 0.018f, Color(0xFF5B4632))
    // lantern hanging from the pole arm
    poly(Dark, 0.04f, -0.16f, 0.16f, -0.16f, 0.1f, -0.22f)
    rect(0.05f, -0.16f, 0.1f, 0.14f, Color(0xFFFFD98A))
    rect(0.05f, -0.16f, 0.1f, 0.012f, Dark); rect(0.05f, -0.03f, 0.1f, 0.012f, Dark)
    line(0.1f, -0.16f, 0.1f, -0.02f, 0.01f, Dark.copy(alpha = 0.5f))
}

private fun Pen.foxGrapes() {
    // vine and grapes
    line(-0.1f, -0.36f, 0.5f, -0.3f, 0.025f, Color(0xFF5B4632))
    poly(Color(0xFF5E9A4B), 0.0f, -0.35f, 0.14f, -0.28f, 0.04f, -0.22f)
    line(0.28f, -0.3f, 0.28f, -0.24f, 0.015f, Color(0xFF5B4632))
    val g = Color(0xFF8B5CB8)
    for ((x, y) in listOf(0.2f to -0.2f, 0.28f to -0.2f, 0.36f to -0.2f, 0.24f to -0.13f, 0.32f to -0.13f, 0.28f to -0.07f)) {
        circle(x, y, 0.04f, g); circle(x - 0.01f, y - 0.012f, 0.012f, Color(0xFFC9A6E8))
    }
    // fox standing, reaching up
    val fur = Color(0xFFE07B39)
    curve(0.09f, fur, -0.3f, 0.3f, -0.46f, 0.24f, -0.48f, 0.1f, -0.38f, 0.04f)
    oval(-0.18f, 0.12f, 0.1f, 0.2f, fur)
    oval(-0.17f, 0.14f, 0.05f, 0.14f, Paper)
    circle(-0.1f, -0.14f, 0.08f, fur)
    poly(fur, -0.15f, -0.2f, -0.12f, -0.3f, -0.08f, -0.2f)
    poly(fur, -0.08f, -0.2f, -0.03f, -0.28f, -0.03f, -0.17f)
    poly(Paper, -0.06f, -0.12f, 0.06f, -0.1f, -0.06f, -0.06f)
    circle(0.05f, -0.1f, 0.012f, Dark)
    circle(-0.08f, -0.15f, 0.011f, Dark)
    line(-0.1f, -0.05f, 0.08f, -0.2f, 0.035f, fur)
    line(-0.22f, 0.3f, -0.22f, 0.34f, 0.04f, Dark); line(-0.12f, 0.3f, -0.12f, 0.34f, 0.04f, Dark)
}

private fun Pen.wolf() {
    glow(-0.2f, -0.12f, 0.7f, Paper, 0.35f)
    circle(-0.2f, -0.12f, 0.3f, Color(0xFFF8EFD8))
    circle(-0.1f, -0.2f, 0.04f, Color(0xFFE6DBC0)); circle(-0.28f, -0.02f, 0.05f, Color(0xFFE6DBC0))
    // a wolf sitting in profile, head thrown back to howl, snout to the upper left
    val w = Color(0xFF0E0C14)
    poly(w,
        -0.22f, 0.34f, -0.20f, 0.17f, -0.27f, 0.04f, -0.30f, -0.10f, -0.39f, -0.23f, -0.43f, -0.33f,
        -0.36f, -0.34f, -0.30f, -0.28f, -0.27f, -0.39f, -0.22f, -0.27f, -0.16f, -0.14f, -0.08f, 0.0f,
        0.02f, 0.07f, 0.10f, 0.18f, 0.20f, 0.30f, 0.12f, 0.34f,
    )
    // sheep on the far hill
    circle(0.3f, 0.3f, 0.04f, Paper); circle(0.4f, 0.31f, 0.035f, Paper)
}

private fun Pen.ant() {
    val body = Color(0xFF3A2A22)
    val rim = Color(0xFFB98C6B)
    for (x in listOf(-0.2f, -0.06f, 0.06f)) {
        line(x, 0.16f, x - 0.06f, 0.3f, 0.018f, rim); line(x, 0.16f, x + 0.05f, 0.3f, 0.018f, rim)
    }
    oval(-0.2f, 0.14f, 0.14f, 0.1f, rim)
    oval(-0.2f, 0.14f, 0.12f, 0.085f, body)
    circle(-0.02f, 0.15f, 0.06f, rim); circle(-0.02f, 0.15f, 0.05f, body)
    circle(0.1f, 0.12f, 0.07f, rim); circle(0.1f, 0.12f, 0.058f, body)
    line(0.14f, 0.07f, 0.2f, -0.03f, 0.012f, rim); line(0.12f, 0.06f, 0.15f, -0.05f, 0.012f, rim)
    circle(0.13f, 0.11f, 0.012f, Paper)
    // a grain of wheat carried overhead
    oval(0.02f, -0.06f, 0.15f, 0.06f, Gold)
    line(-0.1f, -0.06f, 0.14f, -0.06f, 0.008f, Color(0xFFC79A3E))
    line(0.0f, 0.09f, 0.02f, 0.0f, 0.016f, rim)
    // grass
    for (i in 0 until 8) { val x = -0.46f + i * 0.13f; poly(Color(0xFF4F7F5A), x, 0.34f, x + 0.02f, 0.2f + (i % 3) * 0.03f, x + 0.05f, 0.34f) }
}

private fun Pen.goose(t: Float) {
    val glow = 0.6f + 0.4f * sin(t * 2f)
    // nest
    oval(0f, 0.3f, 0.34f, 0.07f, Color(0xFF8A6A3E))
    oval(-0.06f, 0.18f, 0.27f, 0.15f, Paper)
    oval(-0.1f, 0.18f, 0.16f, 0.09f, Shade)
    curve(0.075f, Paper, 0.1f, 0.12f, 0.2f, 0.05f, 0.1f, -0.05f, 0.18f, -0.2f)
    circle(0.2f, -0.23f, 0.06f, Paper)
    poly(Color(0xFFF0A23A), 0.24f, -0.25f, 0.34f, -0.22f, 0.24f, -0.19f)
    circle(0.21f, -0.25f, 0.011f, Dark)
    // golden eggs
    glow(0.3f, 0.28f, 0.28f, Gold, 0.55f * glow)
    oval(0.3f, 0.27f, 0.06f, 0.08f, Gold)
    glow(0.46f, 0.31f, 0.2f, Gold, 0.4f * glow)
    oval(0.46f, 0.3f, 0.045f, 0.06f, Color(0xFFFFE08F))
}

private fun Pen.swan(t: Float) {
    val bob = sin(t * 1.1f) * 0.01f
    oval(0f, 0.3f, 0.5f, 0.07f, Color(0xFF5E92B6).copy(alpha = 0.55f))
    oval(0f, 0.33f, 0.2f, 0.02f, Paper.copy(alpha = 0.35f))
    oval(-0.04f, 0.2f + bob, 0.24f, 0.11f, Paper)
    poly(Paper, -0.28f, 0.16f + bob, -0.4f, 0.1f, -0.26f, 0.22f + bob)
    oval(-0.06f, 0.18f + bob, 0.15f, 0.07f, Shade)
    curve(0.07f, Paper, 0.14f, 0.14f + bob, 0.28f, 0.1f, 0.0f, -0.05f, 0.17f, -0.2f + bob)
    circle(0.18f, -0.22f + bob, 0.05f, Paper)
    poly(Amber, 0.21f, -0.235f + bob, 0.31f, -0.215f + bob, 0.21f, -0.2f + bob)
    circle(0.185f, -0.235f + bob, 0.01f, Dark)
}

private fun Pen.elephant() {
    val skin = Color(0xFFB9B3B8)
    val dark = Color(0xFF8C868E)
    for (x in listOf(-0.2f, -0.08f, 0.1f, 0.2f)) line(x, 0.14f, x, 0.32f, 0.07f, dark)
    oval(-0.02f, 0.06f, 0.3f, 0.2f, skin)
    curve(0.02f, dark, -0.32f, 0.0f, -0.4f, 0.1f, -0.36f, 0.22f, -0.34f, 0.28f)
    circle(0.3f, -0.02f, 0.15f, skin)
    oval(0.2f, -0.0f, 0.08f, 0.14f, dark)
    curve(0.075f, skin, 0.4f, 0.04f, 0.5f, 0.14f, 0.46f, 0.26f, 0.42f, 0.32f)
    poly(Color(0xFFF6F0E4), 0.37f, 0.1f, 0.47f, 0.2f, 0.4f, 0.14f)
    circle(0.35f, -0.04f, 0.013f, Dark)
}

private fun Pen.lighthouse(t: Float) {
    // beam swings back and forth
    val k = cos(t * 0.9f)
    val lx = 0f
    val ly = -0.26f
    val reach = 0.95f * k
    poly(Gold.copy(alpha = 0.28f), lx, ly, lx + reach, ly - 0.14f, lx + reach, ly + 0.1f)
    glow(0f, ly, 0.3f, Gold, 0.5f)
    // rock
    poly(Color(0xFF2B2A33), -0.5f, 0.34f, -0.3f, 0.24f, -0.12f, 0.28f, 0.14f, 0.24f, 0.34f, 0.27f, 0.52f, 0.34f)
    // tower with stripes
    poly(Paper, -0.11f, 0.26f, 0.11f, 0.26f, 0.07f, -0.16f, -0.07f, -0.16f)
    poly(Color(0xFFC95A4A), -0.1f, 0.12f, 0.1f, 0.12f, 0.093f, 0.0f, -0.093f, 0.0f)
    poly(Color(0xFFC95A4A), -0.082f, -0.1f, 0.082f, -0.1f, 0.075f, -0.16f, -0.075f, -0.16f)
    rect(-0.1f, -0.19f, 0.2f, 0.03f, Dark)
    rect(-0.065f, -0.31f, 0.13f, 0.12f, Gold)
    poly(Dark, -0.085f, -0.31f, 0f, -0.39f, 0.085f, -0.31f)
    rect(-0.025f, 0.14f, 0.05f, 0.12f, Dark)
}

private fun Pen.train(t: Float) {
    for (i in 0 until 4) {
        val life = (t * 0.3f + i / 4f) % 1f
        circle(-0.05f - life * 0.4f, -0.2f - life * 0.24f, 0.035f + life * 0.06f, Paper.copy(alpha = 0.5f * (1f - life)))
    }
    line(-0.5f, 0.325f, 0.5f, 0.325f, 0.012f, Shade)
    // engine
    rect(-0.1f, 0.0f, 0.5f, 0.24f, Color(0xFF3F3D4A))
    oval(0.4f, 0.12f, 0.03f, 0.12f, Color(0xFF3F3D4A))
    rect(-0.4f, -0.1f, 0.3f, 0.34f, Color(0xFF55536A))
    rect(-0.43f, -0.14f, 0.36f, 0.04f, Paper)
    rect(-0.34f, -0.04f, 0.14f, 0.12f, Gold)
    rect(0.2f, -0.12f, 0.07f, 0.13f, Color(0xFF3F3D4A))
    rect(0.18f, -0.14f, 0.11f, 0.03f, Paper)
    rect(-0.1f, 0.04f, 0.5f, 0.015f, Paper)
    for (x in listOf(-0.3f, -0.1f, 0.12f, 0.3f)) {
        circle(x, 0.27f, 0.06f, Paper); circle(x, 0.27f, 0.04f, Dark)
    }
    glow(0.42f, 0.05f, 0.18f, Gold, 0.6f)
    circle(0.42f, 0.05f, 0.03f, Gold)
}

private fun Pen.cabin(t: Float) {
    for (i in 0 until 3) {
        val life = (t * 0.25f + i / 3f) % 1f
        circle(0.14f + sin(t + i) * 0.02f * life, -0.24f - life * 0.3f, 0.025f + life * 0.04f, Paper.copy(alpha = 0.4f * (1f - life)))
    }
    pineTree(-0.4f, 0.34f, 0.5f); pineTree(0.42f, 0.34f, 0.44f)
    rect(0.1f, -0.3f, 0.07f, 0.16f, Color(0xFF6B5A52))
    rect(-0.24f, -0.02f, 0.48f, 0.32f, Color(0xFFD9C3A5))
    poly(Color(0xFF5E3B2E), -0.32f, 0.0f, 0f, -0.27f, 0.32f, 0.0f)
    rect(-0.18f, 0.08f, 0.09f, 0.22f, Color(0xFF4A3426))
    glow(0.1f, 0.1f, 0.3f, Gold, 0.55f)
    rect(0.04f, 0.04f, 0.14f, 0.12f, Gold)
    line(0.11f, 0.04f, 0.11f, 0.16f, 0.008f, Dark); line(0.04f, 0.1f, 0.18f, 0.1f, 0.008f, Dark)
}

private fun Pen.pineTree(x: Float, base: Float, h: Float) {
    val c = Color(0xFF1F4A33)
    poly(c, x, base - h, x + h * 0.3f, base - h * 0.4f, x - h * 0.3f, base - h * 0.4f)
    poly(c, x, base - h * 0.75f, x + h * 0.38f, base - h * 0.1f, x - h * 0.38f, base - h * 0.1f)
    rect(x - h * 0.03f, base - h * 0.1f, h * 0.06f, h * 0.1f, Color(0xFF4A3426))
}

private fun Pen.boat(t: Float) {
    val bob = sin(t * 1.3f) * 0.012f
    val tilt = sin(t * 1.3f + 1f) * 0.01f
    oval(0f, 0.3f, 0.52f, 0.075f, Color(0xFF4F8DB0).copy(alpha = 0.5f))
    oval(0f, 0.34f, 0.22f, 0.02f, Paper.copy(alpha = 0.3f))
    line(0f, 0.2f + bob, 0f, -0.3f + bob, 0.016f, Color(0xFF5B4632))
    poly(Paper, 0.02f, -0.28f + bob, 0.02f, 0.14f + bob, 0.3f, 0.14f + bob + tilt)
    poly(Shade, -0.02f, -0.18f + bob, -0.02f, 0.14f + bob, -0.2f, 0.14f + bob)
    poly(Color(0xFF7B5236), -0.3f, 0.16f + bob, 0.34f, 0.16f + bob + tilt, 0.24f, 0.28f + bob, -0.2f, 0.28f + bob)
    glow(-0.28f, 0.1f + bob, 0.18f, Gold, 0.6f)
    circle(-0.28f, 0.12f + bob, 0.025f, Gold)
}

private fun Pen.moonHouses() {
    val moon = Path().apply { addOval(androidx.compose.ui.geometry.Rect(p(-0.28f, -0.34f), Size(0.56f * s, 0.56f * s))) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(p(-0.12f, -0.4f), Size(0.5f * s, 0.5f * s))) }
    glow(-0.02f, -0.06f, 0.6f, Paper, 0.3f)
    d.drawPath(Path.combine(PathOperation.Difference, moon, bite), Color(0xFFF8EFD8))
    for ((x, y) in listOf(0.3f to -0.28f, 0.42f to -0.1f, -0.42f to -0.22f, 0.2f to -0.4f)) circle(x, y, 0.014f, Paper)
    val house = Color(0xFF0E0C14)
    rect(-0.4f, 0.18f, 0.2f, 0.16f, house); poly(house, -0.43f, 0.18f, -0.3f, 0.07f, -0.17f, 0.18f)
    rect(-0.12f, 0.12f, 0.24f, 0.22f, house); poly(house, -0.15f, 0.12f, 0f, -0.0f, 0.15f, 0.12f)
    rect(0.2f, 0.2f, 0.2f, 0.14f, house); poly(house, 0.17f, 0.2f, 0.3f, 0.09f, 0.43f, 0.2f)
    rect(-0.05f, 0.2f, 0.04f, 0.05f, Gold); rect(-0.33f, 0.24f, 0.04f, 0.05f, Gold); rect(0.3f, 0.25f, 0.04f, 0.04f, Gold)
}

private fun Pen.book(t: Float) {
    glow(0f, 0.0f, 0.55f, Amber, 0.35f + 0.1f * sin(t * 2f))
    poly(Paper, -0.4f, 0.2f, -0.02f, 0.12f, -0.02f, -0.22f, -0.4f, -0.14f)
    poly(Shade, 0.4f, 0.2f, 0.02f, 0.12f, 0.02f, -0.22f, 0.4f, -0.14f)
    for (i in 0 until 4) {
        line(-0.33f, -0.06f + i * 0.055f, -0.1f, -0.1f + i * 0.055f + 0.0f, 0.01f, Shade)
        line(0.1f, -0.1f + i * 0.055f, 0.33f, -0.06f + i * 0.055f, 0.01f, Paper.copy(alpha = 0.7f))
    }
    for (i in 0 until 5) {
        val life = (t * 0.3f + i * 0.2f) % 1f
        circle(-0.2f + i * 0.1f, -0.2f - life * 0.2f, 0.012f, Gold.copy(alpha = 1f - life))
    }
}
