package link.bury.onlinewallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

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

        // FIT (or a point between FIT and FILL) leaves borders on one axis; paint them in and
        // place the image according to the requested horizontal/vertical position.
        val framed = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(framed).apply {
            drawColor(BORDER_COLOR)
            drawBitmap(scaled, frame.drawLeft.toFloat(), frame.drawTop.toFloat(), null)
        }
        if (scaled !== source) scaled.recycle()
        return framed
    } catch (_: OutOfMemoryError) {
        // Falling back to the unframed bitmap still produces a usable wallpaper.
        return source
    }
}

private const val BORDER_COLOR = Color.BLACK
