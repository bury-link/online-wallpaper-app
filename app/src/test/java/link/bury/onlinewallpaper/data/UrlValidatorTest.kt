package link.bury.onlinewallpaper.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun `accepts absolute http and https urls`() {
        assertTrue(UrlValidator.isValid("https://example.com/wallpaper.jpg"))
        assertTrue(UrlValidator.isValid("http://example.com/a.png?size=large"))
        assertTrue(UrlValidator.isValid("  https://example.com/a.webp  "))
    }

    @Test
    fun `rejects anything the downloader cannot fetch`() {
        assertFalse(UrlValidator.isValid(""))
        assertFalse(UrlValidator.isValid("   "))
        assertFalse(UrlValidator.isValid("example.com/a.jpg"))
        assertFalse(UrlValidator.isValid("ftp://example.com/a.jpg"))
        assertFalse(UrlValidator.isValid("file:///sdcard/a.jpg"))
        assertFalse(UrlValidator.isValid("https://"))
        assertFalse(UrlValidator.isValid("http://exa mple.com/a.jpg"))
    }
}
