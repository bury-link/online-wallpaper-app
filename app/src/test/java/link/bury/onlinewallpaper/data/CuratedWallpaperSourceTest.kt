package link.bury.onlinewallpaper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CuratedWallpaperSourceTest {

    @Test
    fun `findByUrl returns the matching curated source`() {
        val source = CuratedWallpaperSource.findByUrl(
            "https://services.swpc.noaa.gov/images/animations/ovation/north/latest.jpg"
        )

        assertEquals(CuratedWallpaperSource.NOAA_AURORA_NORTH, source)
    }

    @Test
    fun `findByUrl ignores surrounding whitespace`() {
        val source = CuratedWallpaperSource.findByUrl(
            "  https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0304.jpg  "
        )

        assertEquals(CuratedWallpaperSource.NASA_SDO_304, source)
    }

    @Test
    fun `findByUrl returns null for a custom webcam URL`() {
        assertNull(
            CuratedWallpaperSource.findByUrl(
                "https://www.golf-oberealp.de/fileadmin/webcam/golfclub_oberealp.jpg"
            )
        )
    }
}
