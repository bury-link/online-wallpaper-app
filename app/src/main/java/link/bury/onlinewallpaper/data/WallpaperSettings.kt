package link.bury.onlinewallpaper.data

/** Everything the app persists, as a single immutable snapshot. */
data class WallpaperSettings(
    val imageUrl: String = "",
    val interval: RefreshInterval = RefreshInterval.DEFAULT,
    val enabled: Boolean = false,
    /** Epoch millis of the last wallpaper that was applied successfully, or 0 if never. */
    val lastSuccessAt: Long = 0L,
    /** Message of the most recent failure, or null once a later run succeeds. */
    val lastError: String? = null,
    /** Epoch millis of [lastError], or 0 if there is none. */
    val lastErrorAt: Long = 0L,
) {
    val hasValidUrl: Boolean get() = UrlValidator.isValid(imageUrl)
}
