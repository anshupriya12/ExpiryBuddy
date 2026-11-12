package com.expirybuddy.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expirybuddy.app.domain.model.ItemCategory
import com.expirybuddy.app.ui.theme.expiryColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Small in-memory cache of decoded photos. Photos are decoded at (about) the size they are shown, never
 * at full resolution, and off the main thread. Stored photo URIs are unique per file, so an entry can
 * never go stale.
 */
private object PhotoCache {
    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt(),
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }
}

private fun decodeSampled(photoUri: String, targetPx: Int): ImageBitmap? {
    val path = Uri.parse(photoUri).path ?: return null
    if (!File(path).exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
}

@Composable
private fun rememberPhoto(photoUri: String?, targetPx: Int): State<ImageBitmap?> {
    val key = photoUri?.let { "$it@$targetPx" }
    // Starts from the cache so a list scrolled back into view shows its photos without a flash.
    val photo = remember(key) { mutableStateOf(key?.let(PhotoCache::get)) }
    LaunchedEffect(key) {
        photo.value = if (photoUri == null || key == null) {
            null
        } else {
            PhotoCache.get(key) ?: withContext(Dispatchers.IO) {
                decodeSampled(photoUri, targetPx)?.also { PhotoCache.put(key, it) }
            }
        }
    }
    return photo
}

/**
 * Square-ish thumbnail for lists: the item's photo, or its category icon on a peach tile when it has
 * none (or the file is missing). Decorative: the surrounding card carries the spoken description.
 */
@Composable
fun ItemThumbnail(
    photoUri: String?,
    category: ItemCategory,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val targetPx = with(LocalDensity.current) { (size * 2).roundToPx() }
    val photo by rememberPhoto(photoUri, targetPx)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.expiryColors.peach),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = photo
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/** Large photo for the details screen and the form preview. Renders nothing until decoded. */
@Composable
fun ItemPhoto(
    photoUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetPx: Int = 1080,
) {
    val photo by rememberPhoto(photoUri, targetPx)
    val bitmap = photo
    Box(modifier = modifier.background(MaterialTheme.expiryColors.peach), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
