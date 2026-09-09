package link.bury.onlinewallpaper.data

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** A named user bookmark for a personal or public wallpaper image URL. */
data class SavedSource(val name: String, val url: String)

/** Stable, dependency-free persistence format for [SavedSource] entries in Preferences DataStore. */
object SavedSources {
    fun encode(sources: List<SavedSource>): String = sources.joinToString("\n") { source ->
        "${escape(source.name)}\t${escape(source.url)}"
    }

    fun decode(value: String?): List<SavedSource> = value.orEmpty()
        .lineSequence()
        .mapNotNull { row ->
            val parts = row.split('\t', limit = 2)
            if (parts.size != 2) null else runCatching {
                SavedSource(unescape(parts[0]), unescape(parts[1]))
            }.getOrNull()
        }
        .filter { it.name.isNotBlank() && UrlValidator.isValid(it.url) }
        .toList()

    fun addOrReplace(existing: List<SavedSource>, name: String, url: String): List<SavedSource> {
        val normalizedUrl = url.trim()
        if (!UrlValidator.isValid(normalizedUrl)) return existing
        val normalizedName = name.trim().ifBlank { defaultName(normalizedUrl) }
        return existing.filterNot { it.url == normalizedUrl } + SavedSource(normalizedName, normalizedUrl)
    }

    fun remove(existing: List<SavedSource>, url: String): List<SavedSource> =
        existing.filterNot { it.url == url }

    fun defaultName(url: String): String = runCatching {
        URI(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: url

    private fun escape(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun unescape(value: String): String = URLDecoder.decode(value, "UTF-8")
}
