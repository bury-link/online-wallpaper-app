package link.bury.onlinewallpaper.wallpaper

import android.graphics.Bitmap

/**
 * Center-crops [source] to the aspect ratio of [targetWidth] x [targetHeight] and then scales it to
 * exactly that size, so the wallpaper fills the screen without being stretched.
 *
 * Returns [source] itself when it already has the requested size; otherwise a new bitmap that the
 * caller owns. Intermediate bitmaps are recycled here.
 */
fun cropAndScale(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    if (targetWidth <= 0 || targetHeight <= 0) return source
    if (source.width == targetWidth && source.height == targetHeight) return source

    val crop = calculateCenterCrop(source.width, source.height, targetWidth, targetHeight)

    try {
        val cropped = if (crop.left == 0 && crop.top == 0 &&
            crop.width == source.width && crop.height == source.height
        ) {
            source
        } else {
            Bitmap.createBitmap(source, crop.left, crop.top, crop.width, crop.height)
        }

        if (cropped.width == targetWidth && cropped.height == targetHeight) return cropped

        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        if (cropped !== source && cropped !== scaled) cropped.recycle()
        return scaled
    } catch (_: OutOfMemoryError) {
        // Falling back to the uncropped bitmap still produces a usable wallpaper.
        return source
    }
}
