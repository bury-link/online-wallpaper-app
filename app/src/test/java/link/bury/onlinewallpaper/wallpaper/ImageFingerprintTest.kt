package link.bury.onlinewallpaper.wallpaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFingerprintTest {

    @Test
    fun `same nonblank fingerprint skips a redundant wallpaper apply`() {
        assertTrue(ImageFingerprint.isUnchanged("abc123", "abc123"))
    }

    @Test
    fun `missing or different fingerprints require an apply`() {
        assertFalse(ImageFingerprint.isUnchanged(null, "abc123"))
        assertFalse(ImageFingerprint.isUnchanged("abc123", "def456"))
        assertFalse(ImageFingerprint.isUnchanged("", "abc123"))
    }
}
