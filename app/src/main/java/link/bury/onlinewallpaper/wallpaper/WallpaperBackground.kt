package link.bury.onlinewallpaper.wallpaper

import java.util.Locale

/** The layer visible behind a FIT-framed image where the target has unused space. */
enum class BackgroundMode(val storageValue: String) {
    COLOR("color"),
    BLURRED_IMAGE("blurred_image");

    companion object {
        fun fromStorage(value: String?): BackgroundMode =
            entries.firstOrNull { it.storageValue == value } ?: BLURRED_IMAGE
    }
}

/**
 * A validated background configuration. The default is a softened, screen-filling copy of the
 * wallpaper; the alternative is a user-selected opaque hex color.
 */
data class WallpaperBackground(
    val mode: BackgroundMode = BackgroundMode.BLURRED_IMAGE,
    val colorHex: String = DEFAULT_COLOR_HEX,
) {
    companion object {
        /** Quiet charcoal chosen to complement the bury/link palette when a solid color is used. */
        const val DEFAULT_COLOR_HEX = "#16161A"
        private val OPAQUE_RGB = Regex("^#[0-9A-Fa-f]{6}$")

        fun isValidColor(value: String): Boolean = OPAQUE_RGB.matches(value.trim())

        fun fromStorage(modeValue: String?, colorValue: String?): WallpaperBackground =
            WallpaperBackground(
                mode = BackgroundMode.fromStorage(modeValue),
                colorHex = colorValue
                    ?.trim()
                    ?.takeIf { OPAQUE_RGB.matches(it) }
                    ?.uppercase(Locale.ROOT)
                    ?: DEFAULT_COLOR_HEX,
            )
    }
}
