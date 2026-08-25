package link.bury.onlinewallpaper.wallpaper

import kotlin.math.roundToInt

/**
 * Endpoints of the FIT-FILL framing slider. Values in between interpolate smoothly: as [Framing]
 * moves from [FIT] towards [FILL], less of the source image is used and it is scaled up further,
 * so cropping increases continuously instead of jumping between two fixed layouts.
 */
object Framing {
    /** The entire image is visible; borders are added if its aspect ratio does not match the target. */
    const val FIT = 0f

    /** The target is filled completely via a center crop, as [calculateCenterCrop] would produce. */
    const val FILL = 1f

    /** Centered image placement, preserving the original framing behaviour. */
    const val POSITION_CENTER = 0f

    /** The first/upper/left crop edge. */
    const val POSITION_START = -1f

    /** The last/lower/right crop edge. */
    const val POSITION_END = 1f

    /** Matches the app's original, crop-to-fill-only behaviour. */
    val DEFAULT = FILL
}

/**
 * How to draw a [sourceWidth] x [sourceHeight] image into a [targetWidth] x [targetHeight] area.
 *
 * [crop] is the region of the source to use. [drawWidth] x [drawHeight] is the size that region
 * should be scaled to, preserving its own aspect ratio. When that is smaller than the target on an
 * axis, the caller should center the result on that axis rather than stretch it, to reproduce the
 * borders a FIT framing calls for.
 */
data class FrameResult(
    val crop: CropRect,
    val drawWidth: Int,
    val drawHeight: Int,
    /** Placement of the scaled image inside the target, e.g. for FIT borders. */
    val drawLeft: Int,
    val drawTop: Int,
)

/** The pixel size of the on-screen preview frame, retaining the device screen's aspect ratio. */
data class PreviewFrame(val width: Int, val height: Int)

/**
 * Fits a device-shaped preview into the available UI width without changing its aspect ratio.
 * The preview uses this size as the target passed to [calculateFrame], so it shows exactly the
 * same FIT-FILL geometry that is used for the real lock-screen wallpaper.
 */
fun calculatePreviewFrame(
    screenWidth: Int,
    screenHeight: Int,
    availableWidth: Int,
): PreviewFrame {
    if (screenWidth <= 0 || screenHeight <= 0 || availableWidth <= 0) {
        return PreviewFrame(0, 0)
    }
    return PreviewFrame(
        width = availableWidth,
        height = (availableWidth.toLong() * screenHeight / screenWidth).toInt().coerceAtLeast(1),
    )
}

/**
 * Calculates how to frame a [sourceWidth] x [sourceHeight] image into a [targetWidth] x
 * [targetHeight] area at a point on the FIT-FILL slider given by [fit] (clamped to
 * [Framing.FIT]..[Framing.FILL]).
 *
 * At [Framing.FIT] the whole source is used and scaled down (or up) to fit entirely inside the
 * target, leaving borders on one axis unless the aspect ratios already match. At [Framing.FILL]
 * the source is center-cropped to the target's aspect ratio, exactly like [calculateCenterCrop],
 * and scaled to cover the target with no borders. In between, the crop shrinks from the full
 * source towards that center crop, so the image progressively fills more of the target as [fit]
 * increases.
 */
fun calculateFrame(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
    fit: Float,
    horizontalPosition: Float = Framing.POSITION_CENTER,
    verticalPosition: Float = Framing.POSITION_CENTER,
): FrameResult {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
        val width = maxOf(sourceWidth, 0)
        val height = maxOf(sourceHeight, 0)
        return FrameResult(
            crop = CropRect(0, 0, width, height),
            drawWidth = width,
            drawHeight = height,
            drawLeft = 0,
            drawTop = 0,
        )
    }

    val t = fit.coerceIn(Framing.FIT, Framing.FILL)
    val fillCrop = calculateCenterCrop(sourceWidth, sourceHeight, targetWidth, targetHeight)

    val cropWidth = lerp(sourceWidth, fillCrop.width, t).coerceIn(1, sourceWidth)
    val cropHeight = lerp(sourceHeight, fillCrop.height, t).coerceIn(1, sourceHeight)
    // Moving the image right/down reveals source pixels further left/up, so crop alignment is the
    // inverse of the UI direction. This makes a right/down drag feel visually natural.
    val left = alignedOffset(
        available = sourceWidth - cropWidth,
        position = -horizontalPosition,
    )
    val top = alignedOffset(
        available = sourceHeight - cropHeight,
        position = -verticalPosition,
    )
    val crop = CropRect(left = left, top = top, right = left + cropWidth, bottom = top + cropHeight)

    // At FILL the crop's aspect ratio already matches the target by construction, but integer
    // rounding in calculateCenterCrop can leave it a hair off; snapping to the exact target size
    // here guarantees FILL always covers it completely with no stray border, as the endpoint
    // requires.
    val (drawWidth, drawHeight) = if (t >= Framing.FILL) {
        targetWidth to targetHeight
    } else {
        val scale = minOf(targetWidth.toDouble() / cropWidth, targetHeight.toDouble() / cropHeight)
        (cropWidth * scale).roundToInt().coerceIn(1, targetWidth) to
            (cropHeight * scale).roundToInt().coerceIn(1, targetHeight)
    }

    return FrameResult(
        crop = crop,
        drawWidth = drawWidth,
        drawHeight = drawHeight,
        drawLeft = alignedOffset(targetWidth - drawWidth, horizontalPosition),
        drawTop = alignedOffset(targetHeight - drawHeight, verticalPosition),
    )
}

private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()

/** Maps -1 (start) through 0 (center) to +1 (end) within the available cropped-out pixels. */
private fun alignedOffset(available: Int, position: Float): Int =
    (available * ((position.coerceIn(Framing.POSITION_START, Framing.POSITION_END) + 1f) / 2f))
        .roundToInt()
        .coerceIn(0, available)
