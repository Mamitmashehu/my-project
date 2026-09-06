package com.scarguard.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object BitmapUtils {
    /** Decodes a JPEG from disk downsampled to roughly [reqSize]px, to keep thumbnails cheap. */
    fun decodeSampled(path: String, reqSize: Int): Bitmap? {
        if (!File(path).exists()) return null
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)
        var sampleSize = 1
        var halfWidth = boundsOptions.outWidth / 2
        var halfHeight = boundsOptions.outHeight / 2
        while (halfWidth / sampleSize >= reqSize && halfHeight / sampleSize >= reqSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(path, options)
    }

    /** Decodes full-resolution -- used right before running analysis, not for display. */
    fun decodeFull(path: String): Bitmap? = BitmapFactory.decodeFile(path)
}
