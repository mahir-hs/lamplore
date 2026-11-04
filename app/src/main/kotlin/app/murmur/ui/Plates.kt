package app.murmur.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decoded plates, kept small: a handful of ~100 KB JPEGs from assets/plates. */
private val cache = object : LruCache<String, ImageBitmap>(12) {}

@Composable
fun rememberPlate(asset: String): ImageBitmap? {
    val ctx = LocalContext.current.applicationContext
    val state = produceState(cache[asset], asset) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    if ('.' in asset) {
                        // online plate: cached download
                        app.murmur.data.RemoteContent.plateFile(asset)?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
                    } else {
                        ctx.assets.open("plates/$asset.jpg").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                    }
                }.getOrNull()
            }?.also { cache.put(asset, it) }
        }
    }
    return state.value
}
