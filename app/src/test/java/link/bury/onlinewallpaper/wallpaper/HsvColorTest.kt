package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HsvColorTest {

    @Test
    fun `fromHex converts the brand orange to full saturation and brightness`() {
        val color = HsvColor.fromHex("#FF6C00")

        assertEquals(25.4f, color.hue, 0.2f)
        assertEquals(1f, color.saturation, 0.001f)
        assertEquals(1f, color.value, 0.001f)
    }

    @Test
    fun `toHex returns a normalized opaque hex color`() {
        val color = HsvColor(hue = 220f, saturation = 0.5f, value = 0.4f)

        assertEquals("#334466", color.toHex())
    }

    @Test
    fun `fromHex falls back safely for malformed values`() {
        val color = HsvColor.fromHex("not-a-color")

        assertEquals(HsvColor.fromHex(WallpaperBackground.DEFAULT_COLOR_HEX), color)
    }

    @Test
    fun `channels are clamped to usable picker ranges`() {
        val color = HsvColor(hue = 720f, saturation = -0.5f, value = 2f)

        assertTrue(color.normalized().hue in 0f..360f)
        assertEquals(0f, color.normalized().saturation, 0.001f)
        assertEquals(1f, color.normalized().value, 0.001f)
    }
}
