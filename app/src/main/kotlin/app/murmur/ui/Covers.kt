package app.murmur.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.murmur.data.Plate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.murmur.data.Story

/**
 * Cover art that says what the story is about: the scene the story lives in (rain, forest, sea...)
 * with the story's own subject drawn in front (a tortoise, a lighthouse, a lion...). Shelf covers
 * are still pictures; the hero and player covers (`animated`) move.
 */
@Composable
fun Cover(story: Story, modifier: Modifier = Modifier, corner: Dp = 10.dp, animated: Boolean = false, mat: Dp = 5.dp) {
    val scene = story.artScene
    val motif = story.artMotif
    val seed = story.id.hashCode() and 0xFFFF
    val plate = story.plates.firstOrNull()
    val img = plate?.let { rememberPlate(it.asset) }
    // every cover, real plate or drawn, sits on the same paper mat so shelves read as one set
    Box(modifier.clip(RoundedCornerShape(corner)).background(Ink.paper).padding(mat)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape((corner - mat).coerceAtLeast(2.dp)))) {
            if (animated) {
                Canvas(Modifier.fillMaxSize()) {
                    val t = Clock.t
                    if (plate != null && img != null) {
                        drawPlateCover(img, plate, 1.04f + 0.03f * kotlin.math.sin(t * 0.25f))
                    } else {
                        drawScene(scene, t, seed)
                        drawMotif(motif, t)
                    }
                }
            } else {
                StaticCover(story, img)
            }
        }
    }
}

/**
 * Shelf covers never move, so each one is painted once into a small bitmap (off the main thread)
 * and reused. Drawing the full scene and motif for every tile while scrolling was what made the
 * Library laggy.
 */
@Composable
private fun StaticCover(story: Story, img: ImageBitmap?) {
    val plate = story.plates.firstOrNull()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val w = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val h = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        // bucket sizes so tiny layout differences share one bitmap
        val key = "${story.id}|${plate?.asset}|${img != null}|${w / 8}x${h / 8}"
        val bmp by androidx.compose.runtime.produceState<ImageBitmap?>(coverCache[key], key) {
            if (value == null) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val out = ImageBitmap(w, h)
                    val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
                    scope.draw(density, androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(out), androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())) {
                        if (plate != null && img != null) {
                            drawPlateCover(img, plate, 1f)
                        } else {
                            drawScene(story.artScene, 0f, story.id.hashCode() and 0xFFFF)
                            drawMotif(story.artMotif, 0f)
                        }
                    }
                    out
                }.also { coverCache.put(key, it) }
            }
        }
        val b = bmp
        if (b != null) {
            Canvas(Modifier.fillMaxSize()) { drawImage(b, dstSize = IntSize(size.width.toInt(), size.height.toInt())) }
        } else {
            Box(Modifier.fillMaxSize().background(coverTint(story)))
        }
    }
}

private val coverCache = object : android.util.LruCache<String, ImageBitmap>(60) {}

/** Tint used for glows behind a cover. */
fun coverTint(story: Story): Color = scenePalette(story.artScene).bottom

/** Fills the canvas with the plate's crop region, centered, scaled to cover (with an optional slow zoom). */
private fun DrawScope.drawPlateCover(img: ImageBitmap, plate: Plate, zoom: Float) {
    val sx = plate.l * img.width
    val sy = plate.t * img.height
    val sw = (plate.r - plate.l) * img.width
    val sh = (plate.b - plate.t) * img.height
    val k = maxOf(size.width / sw, size.height / sh) * zoom
    // visible part of the crop, in source pixels
    val vw = size.width / k
    val vh = size.height / k
    val srcX = sx + (sw - vw) / 2f
    val srcY = sy + (sh - vh) / 2f
    drawImage(
        img,
        srcOffset = IntOffset(srcX.toInt().coerceAtLeast(0), srcY.toInt().coerceAtLeast(0)),
        srcSize = IntSize(vw.toInt().coerceAtMost(img.width), vh.toInt().coerceAtMost(img.height)),
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
}
