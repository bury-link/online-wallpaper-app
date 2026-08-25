package link.bury.onlinewallpaper.data

import link.bury.onlinewallpaper.wallpaper.Framing

/** Everything the app persists, as a single immutable snapshot. */
data class WallpaperSettings(
    val imageUrl: String = "",
    val interval: RefreshInterval = RefreshInterval.DEFAULT,
    val enabled: Boolean = false,
    /** Where this sits on the FIT-FILL framing slider; see [Framing]. */
    val frameFit: Float = Framing.DEFAULT,
    /** -1 = left, 0 = center, +1 = right. */
    val horizontalPosition: Float = Framing.POSITION_CENTER,
    /** -1 = top, 0 = center, +1 = bottom. */
    val verticalPosition: Float = Framing.POSITION_CENTER,
    /** Epoch millis of the last wallpaper that was applied successfully, or 0 if never. */
    val lastSuccessAt: Long = 0L,
    /** Message of the most recent failure, or null once a later run succeeds. */
    val lastError: String? = null,
    /** Epoch millis of [lastError], or 0 if there is none. */
    val lastErrorAt: Long = 0L,
) {
    val hasValidUrl: Boolean get() = UrlValidator.isValid(imageUrl)
}
