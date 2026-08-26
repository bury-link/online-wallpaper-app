package link.bury.onlinewallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Frames [source] into exactly [targetWidth] x [targetHeight] according to [fit], a point on the
 * FIT-FILL slider (see [Framing]). At [Framing.FIT] the whole image is visible, letterboxed with a
 * solid background where its aspect ratio does not match the target; at [Framing.FILL] it is
 * center-cropped to cover the target completely, as the app has always done. See [calculateFrame]
 * for the pure layout calculation.
 *
 * Returns [source] itself when it is already framed correctly; otherwise a new bitmap that the
 * caller owns. Intermediate bitmaps are recycled here.
 */
fun frameBitmap(
    source: Bitmap,
    targetWidth: Int,
    targetHeight: Int,
    fit: Float,
    horizontalPosition: Float = Framing.POSITION_CENTER,
    verticalPosition: Float = Framing.POSITION_CENTER,
    background: WallpaperBackground = WallpaperBackground(),
): Bitmap {
    if (targetWidth <= 0 || targetHeight <= 0) return source

    val frame = calculateFrame(
        sourceWidth = source.width,
        sourceHeight = source.height,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
        fit = fit,
        horizontalPosition = horizontalPosition,
        verticalPosition = verticalPosition,
    )

    try {
        val cropped = if (frame.crop.left == 0 && frame.crop.top == 0 &&
            frame.crop.width == source.width && frame.crop.height == source.height
        ) {
            source
        } else {
            Bitmap.createBitmap(source, frame.crop.left, frame.crop.top, frame.crop.width, frame.crop.height)
        }

        val scaled = if (cropped.width == frame.drawWidth && cropped.height == frame.drawHeight) {
            cropped
        } else {
            Bitmap.createScaledBitmap(cropped, frame.drawWidth, frame.drawHeight, true)
        }
        if (cropped !== source && cropped !== scaled) cropped.recycle()

        // FILL always produces a scaled bitmap that already is the exact target size.
        if (scaled.width == targetWidth && scaled.height == targetHeight) return scaled

        // FIT (or a point between FIT and FILL) leaves unused space on one axis. It can be a
        // designed solid color or a softened, cover-scaled copy of the current image.
        val framed = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(framed).apply {
            when (background.mode) {
                BackgroundMode.COLOR -> drawColor(Color.parseColor(background.colorHex))
                BackgroundMode.BLURRED_IMAGE -> {
                    val blurred = blurredFillBitmap(
                        source, targetWidth, targetHeight, horizontalPosition, verticalPosition,
                    )
                    drawBitmap(blurred, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                    // Reduce visual noise so the uncropped foreground image remains legible.
                    drawColor(Color.argb(92, 0, 0, 0))
                    blurred.recycle()
                }
            }
            drawBitmap(scaled, frame.drawLeft.toFloat(), frame.drawTop.toFloat(), null)
        }
        if (scaled !== source) scaled.recycle()
        return framed
    } catch (_: OutOfMemoryError) {
        // Falling back to the unframed bitmap still produces a usable wallpaper.
        return source
    }
}

/**
 * Creates a low-resolution, cover-filled version of [source] and enlarges it with filtering.
 * This is a fast software blur that works from API 24 onward, including scheduled workers.
 */
private fun blurredFillBitmap(
    source: Bitmap,
    targetWidth: Int,
    targetHeight: Int,
    horizontalPosition: Float,
    verticalPosition: Float,
): Bitmap {
    val fill = calculateFrame(
        source.width, source.height, targetWidth, targetHeight, Framing.FILL,
        horizontalPosition, verticalPosition,
    )
    val cropped = if (fill.crop.left == 0 && fill.crop.top == 0 &&
        fill.crop.width == source.width && fill.crop.height == source.height
    ) source else Bitmap.createBitmap(source, fill.crop.left, fill.crop.top, fill.crop.width, fill.crop.height)
    val smallWidth = minOf(targetWidth, 56).coerceAtLeast(1)
    val smallHeight = (targetHeight.toLong() * smallWidth / targetWidth).toInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(cropped, smallWidth, smallHeight, true)
    if (cropped !== source) cropped.recycle()
    val result = Bitmap.createScaledBitmap(small, targetWidth, targetHeight, true)
    small.recycle()
    return result
}
