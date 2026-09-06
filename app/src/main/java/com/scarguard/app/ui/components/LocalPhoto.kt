package com.scarguard.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scarguard.app.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a JPEG from app storage off the main thread and renders it, with a neutral placeholder
 * (just the surface color) while it loads or if the path is missing.
 */
@Composable
fun LocalPhotoThumbnail(path: String?, modifier: Modifier = Modifier) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, path) {
        value = if (path != null) {
            withContext(Dispatchers.IO) { BitmapUtils.decodeSampled(path, 400) }
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        bitmapState.value?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
