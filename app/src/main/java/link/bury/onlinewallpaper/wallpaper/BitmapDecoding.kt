package link.bury.onlinewallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Decodes [file] downsampled to roughly [targetWidth] x [targetHeight] so a 40-megapixel photo does
 * not blow up the heap. If the decode still runs out of memory we halve the resolution and retry a
 * couple of times before giving up.
 */
fun decodeDownsampled(file: File, targetWidth: Int, targetHeight: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw PermanentWallpaperException("Downloaded file is not a decodable image")
    }

    var sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
    repeat(MAX_DECODE_ATTEMPTS) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        try {
            BitmapFactory.decodeFile(file.absolutePath, options)?.let { return it }
            throw PermanentWallpaperException("Image could not be decoded")
        } catch (_: OutOfMemoryError) {
            sampleSize *= 2
        }
    }
    throw TransientWallpaperException("Not enough memory to decode the image")
}

/** Largest power of two that keeps both dimensions at or above the requested size. */
internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): Int {
    if (targetWidth <= 0 || targetHeight <= 0) return 1
    var sampleSize = 1
    while (width / (sampleSize * 2) >= targetWidth && height / (sampleSize * 2) >= targetHeight) {
        sampleSize *= 2
    }
    return sampleSize
}

private const val MAX_DECODE_ATTEMPTS = 3
