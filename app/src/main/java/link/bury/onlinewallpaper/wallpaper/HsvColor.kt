package link.bury.onlinewallpaper.wallpaper

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Picker-friendly HSV representation with conversion to the app's persisted #RRGGBB format. */
data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
) {
    fun normalized(): HsvColor {
        val normalizedHue = ((hue % 360f) + 360f) % 360f
        return HsvColor(
            hue = normalizedHue,
            saturation = saturation.coerceIn(0f, 1f),
            value = value.coerceIn(0f, 1f),
        )
    }

    fun toHex(): String {
        val color = normalized()
        val chroma = color.value * color.saturation
        val secondary = chroma * (1f - abs((color.hue / 60f % 2f) - 1f))
        val (red, green, blue) = when {
            color.hue < 60f -> Triple(chroma, secondary, 0f)
            color.hue < 120f -> Triple(secondary, chroma, 0f)
            color.hue < 180f -> Triple(0f, chroma, secondary)
            color.hue < 240f -> Triple(0f, secondary, chroma)
            color.hue < 300f -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
        val match = color.value - chroma
        return "#%02X%02X%02X".format(
            ((red + match) * 255f).roundToInt(),
            ((green + match) * 255f).roundToInt(),
            ((blue + match) * 255f).roundToInt(),
        )
    }

    companion object {
        fun fromHex(hex: String): HsvColor {
            val normalized = WallpaperBackground.fromStorage(
                modeValue = BackgroundMode.COLOR.storageValue,
                colorValue = hex,
            ).colorHex
            val red = normalized.substring(1, 3).toInt(16) / 255f
            val green = normalized.substring(3, 5).toInt(16) / 255f
            val blue = normalized.substring(5, 7).toInt(16) / 255f
            val maximum = max(red, max(green, blue))
            val minimum = min(red, min(green, blue))
            val delta = maximum - minimum
            val hue = when {
                delta == 0f -> 0f
                maximum == red -> 60f * (((green - blue) / delta) % 6f)
                maximum == green -> 60f * (((blue - red) / delta) + 2f)
                else -> 60f * (((red - green) / delta) + 4f)
            }
            return HsvColor(
                hue = hue,
                saturation = if (maximum == 0f) 0f else delta / maximum,
                value = maximum,
            ).normalized()
        }
    }
}
