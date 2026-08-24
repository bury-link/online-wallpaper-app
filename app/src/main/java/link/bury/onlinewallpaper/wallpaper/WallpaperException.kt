package link.bury.onlinewallpaper.wallpaper

/**
 * Something went wrong that is likely to fix itself: no connectivity, a timeout, a 5xx from the
 * server. The worker asks WorkManager to retry with backoff.
 */
class TransientWallpaperException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Something went wrong that retrying will not fix: the URL is malformed, the server said 404, the
 * response is not a decodable image. The worker gives up until the next scheduled run.
 */
class PermanentWallpaperException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
