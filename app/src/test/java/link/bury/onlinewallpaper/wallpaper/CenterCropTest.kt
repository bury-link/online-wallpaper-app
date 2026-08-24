package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class CenterCropTest {

    @Test
    fun wideSourceIntoTallTarget_cropsHorizontallyCentered() {
        val crop = calculateCenterCrop(
            sourceWidth = 3000,
            sourceHeight = 1000,
            targetWidth = 1000,
            targetHeight = 2000,
        )

        assertEquals(1250, crop.left)
        assertEquals(1750, crop.right)
        assertEquals(0, crop.top)
        assertEquals(1000, crop.bottom)
    }

    @Test
    fun tallSourceIntoWideTarget_cropsVerticallyCentered() {
        val crop = calculateCenterCrop(
            sourceWidth = 1000,
            sourceHeight = 3000,
            targetWidth = 2000,
            targetHeight = 1000,
        )

        assertEquals(1250, crop.top)
        assertEquals(1750, crop.bottom)
        assertEquals(0, crop.left)
        assertEquals(1000, crop.right)
    }

    @Test
    fun matchingAspectRatio_usesFullSource() {
        val crop = calculateCenterCrop(
            sourceWidth = 2160,
            sourceHeight = 3840,
            targetWidth = 1080,
            targetHeight = 1920,
        )

        assertEquals(CropRect(left = 0, top = 0, right = 2160, bottom = 3840), crop)
    }
}
