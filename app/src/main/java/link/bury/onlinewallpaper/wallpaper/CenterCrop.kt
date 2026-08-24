package link.bury.onlinewallpaper.wallpaper

/** A region of a source image, in source pixels. Right/bottom are exclusive. */
data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * The largest centered region of a [sourceWidth] x [sourceHeight] image that has the same aspect
 * ratio as [targetWidth] x [targetHeight]. Scaling that region to the target therefore fills it
 * completely without distorting the image.
 */
fun calculateCenterCrop(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): CropRect {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
        return CropRect(0, 0, maxOf(sourceWidth, 0), maxOf(sourceHeight, 0))
    }

    // Compare aspect ratios by cross-multiplying so no floating point rounding creeps in.
    val sourceIsWider = sourceWidth.toLong() * targetHeight > targetWidth.toLong() * sourceHeight

    return if (sourceIsWider) {
        val cropWidth = (sourceHeight.toLong() * targetWidth / targetHeight)
            .toInt()
            .coerceIn(1, sourceWidth)
        val left = (sourceWidth - cropWidth) / 2
        CropRect(left = left, top = 0, right = left + cropWidth, bottom = sourceHeight)
    } else {
        val cropHeight = (sourceWidth.toLong() * targetHeight / targetWidth)
            .toInt()
            .coerceIn(1, sourceHeight)
        val top = (sourceHeight - cropHeight) / 2
        CropRect(left = 0, top = top, right = sourceWidth, bottom = top + cropHeight)
    }
}
