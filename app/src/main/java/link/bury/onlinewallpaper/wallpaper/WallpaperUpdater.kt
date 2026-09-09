package link.bury.onlinewallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.bury.onlinewallpaper.data.UrlValidator
import java.io.File
import java.io.IOException

sealed interface WallpaperUpdateResult {
    data class Applied(val imageHash: String, val lastModified: String?) : WallpaperUpdateResult
    data class Unchanged(val lastModified: String?) : WallpaperUpdateResult
}

/**
 * Downloads the configured image and applies it to the **lock screen only**
 * ([WallpaperManager.FLAG_LOCK]). The home screen wallpaper is never touched.
 */
class WallpaperUpdater(
    context: Context,
    private val downloader: ImageDownloader = ImageDownloader(),
) {

    private val appContext = context.applicationContext
    private val thumbnails = ThumbnailStore(appContext)

    /**
     * @param fit where to sit on the FIT-FILL framing slider; see [Framing].
     * @throws PermanentWallpaperException when retrying cannot help.
     * @throws TransientWallpaperException when the caller should retry later.
     */
    suspend fun applyFrom(
        url: String,
        fit: Float,
        horizontalPosition: Float,
        verticalPosition: Float,
        background: WallpaperBackground,
        previousImageHash: String?,
    ): WallpaperUpdateResult = withContext(Dispatchers.IO) {
        if (!UrlValidator.isValid(url)) {
            throw PermanentWallpaperException("Set a valid http(s) image URL first")
        }

        val wallpaperManager = WallpaperManager.getInstance(appContext)
        if (!wallpaperManager.isWallpaperSupported) {
            throw PermanentWallpaperException("This device does not allow setting wallpapers")
        }
        if (!wallpaperManager.isSetWallpaperAllowed) {
            throw PermanentWallpaperException("Changing the wallpaper is blocked by a device policy")
        }

        val temporaryFile = File.createTempFile("wallpaper", ".img", appContext.cacheDir)
        var bitmap: Bitmap? = null
        try {
            val downloaded = downloader.download(url, temporaryFile)
            if (ImageFingerprint.isUnchanged(previousImageHash, downloaded.sha256)) {
                return@withContext WallpaperUpdateResult.Unchanged(downloaded.lastModified)
            }

            val (targetWidth, targetHeight) = targetSize(wallpaperManager)
            val decoded = decodeDownsampled(temporaryFile, targetWidth, targetHeight)
            bitmap = decoded

            // Store the unframed source while it is still owned by this scope. frameBitmap may
            // create a separate framed bitmap, after which the source is deliberately recycled.
            // Saving it later would pass a recycled Bitmap to Bitmap.createScaledBitmap.
            thumbnails.save(decoded)

            // Frame to the screen's aspect ratio per the user's FIT-FILL preference and scale to
            // the exact target size, without distorting the image.
            val framed = frameBitmap(
                source = decoded,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                fit = fit,
                horizontalPosition = horizontalPosition,
                verticalPosition = verticalPosition,
                background = background,
            )
            if (framed !== decoded) {
                decoded.recycle()
                bitmap = framed
            }

            try {
                wallpaperManager.setBitmap(framed, null, true, WallpaperManager.FLAG_LOCK)
            } catch (e: IOException) {
                throw TransientWallpaperException(
                    "Could not apply the wallpaper: ${e.message ?: "system error"}", e
                )
            }
            WallpaperUpdateResult.Applied(downloaded.sha256, downloaded.lastModified)
        } finally {
            // Recycled only after setBitmap and the thumbnail write are done with it.
            bitmap?.recycle()
            temporaryFile.delete()
        }
    }

    /**
     * The size the system wants for a wallpaper. Falls back to the display size when the platform
     * reports no preference (some devices return 0).
     */
    private fun targetSize(wallpaperManager: WallpaperManager): Pair<Int, Int> {
        val metrics = appContext.resources.displayMetrics
        val width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: metrics.widthPixels
        val height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: metrics.heightPixels
        return width.coerceAtLeast(1) to height.coerceAtLeast(1)
    }
}
