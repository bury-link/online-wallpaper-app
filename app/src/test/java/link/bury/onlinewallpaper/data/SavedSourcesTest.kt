package link.bury.onlinewallpaper.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedSourcesTest {

    @Test
    fun `codec round trips names and URLs containing spaces and punctuation`() {
        val sources = listOf(
            SavedSource("Golf: Morgen", "https://example.com/cam.jpg?view=morning"),
            SavedSource("DWD Radar", "https://example.com/radar.png"),
        )

        assertEquals(sources, SavedSources.decode(SavedSources.encode(sources)))
    }

    @Test
    fun `adding an existing URL replaces its name without duplication`() {
        val result = SavedSources.addOrReplace(
            existing = listOf(SavedSource("Old name", "https://example.com/image.jpg")),
            name = "New name",
            url = " https://example.com/image.jpg ",
        )

        assertEquals(listOf(SavedSource("New name", "https://example.com/image.jpg")), result)
    }
}
