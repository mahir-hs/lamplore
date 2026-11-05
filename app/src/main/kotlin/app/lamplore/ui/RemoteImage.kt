package app.lamplore.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val memory = object : LruCache<String, ImageBitmap>(120) {}
private val gate = Semaphore(4) // a long shelf must not open dozens of connections at once

/**
 * A picture from the internet (book or podcast cover): downloaded once into the app cache, decoded
 * small, kept in memory. Shows [fallback] while loading and when there is no picture or no network.
 */
@Composable
fun RemoteImage(url: String, modifier: Modifier = Modifier, fallback: @Composable () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val bmp by produceState<ImageBitmap?>(memory[url], url) {
        if (value == null && url.isNotBlank()) {
            value = gate.withPermit { withContext(Dispatchers.IO) { load(ctx.cacheDir, url) } }?.also { memory.put(url, it) }
        }
    }
    val b = bmp
    if (b != null) Image(b, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) else fallback()
}

private fun load(cacheDir: File, url: String): ImageBitmap? = runCatching {
    val dir = File(cacheDir, "img").apply { mkdirs() }
    val f = File(dir, url.hashCode().toUInt().toString())
    if (!f.exists() || f.length() < 200) {
        var u = url
        for (hop in 0 until 4) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "Lamplore/0.4")
            val code = c.responseCode
            if (code in 300..399) {
                u = URL(URL(u), c.getHeaderField("Location") ?: return null).toString()
                c.disconnect()
            } else {
                if (code != 200) return null
                c.inputStream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                break
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 400) sample *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()
