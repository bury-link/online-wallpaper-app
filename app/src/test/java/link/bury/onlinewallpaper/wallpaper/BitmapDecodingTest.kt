package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapDecodingTest {

    @Test
    fun `does not downsample images at or below the target size`() {
        assertEquals(1, calculateInSampleSize(1080, 1920, 1080, 1920))
        assertEquals(1, calculateInSampleSize(800, 600, 1080, 1920))
    }

    @Test
    fun `halves until a further halving would drop below the target`() {
        assertEquals(4, calculateInSampleSize(4320, 7680, 1080, 1920))
        assertEquals(2, calculateInSampleSize(2160, 3840, 1080, 1920))
        // 3000x4000 halved is 1500x2000, still above target; halved again would be below.
        assertEquals(2, calculateInSampleSize(3000, 4000, 1080, 1920))
    }

    @Test
    fun `falls back to no downsampling when the target is unknown`() {
        assertEquals(1, calculateInSampleSize(4000, 3000, 0, 0))
    }
}
