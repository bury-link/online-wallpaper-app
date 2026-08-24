package link.bury.onlinewallpaper.data

import java.net.URI

/** Accepts only absolute http(s) URLs with a host, which is all the downloader can handle. */
object UrlValidator {

    fun isValid(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
