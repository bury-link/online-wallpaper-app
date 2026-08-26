package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperBackgroundTest {

    @Test
    fun colorMode_normalizesOpaqueSixDigitHexColors() {
        val background = WallpaperBackground.fromStorage(
            modeValue = "color",
            colorValue = "#ff6c00",
        )

        assertEquals(BackgroundMode.COLOR, background.mode)
        assertEquals("#FF6C00", background.colorHex)
    }

    @Test
    fun invalidStoredValues_fallBackToTheDesignedBlurredImageBackground() {
        val background = WallpaperBackground.fromStorage(
            modeValue = "unexpected",
            colorValue = "not-a-color",
        )

        assertEquals(BackgroundMode.BLURRED_IMAGE, background.mode)
        assertEquals(WallpaperBackground.DEFAULT_COLOR_HEX, background.colorHex)
    }
}
