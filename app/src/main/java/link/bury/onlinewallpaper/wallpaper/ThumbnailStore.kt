package link.bury.onlinewallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.IOException
import kotlin.math.max

/**
 * Keeps a small JPEG of the last applied wallpaper in the internal cache directory purely so the
 * settings screen can show a preview. Nothing is written to external storage, so no storage
 * permission is needed.
 */
class ThumbnailStore(context: Context) {

    private val file = File(context.applicationContext.cacheDir, FILE_NAME)

    fun save(source: Bitmap) {
        val scale = MAX_EDGE.toFloat() / max(source.width, source.height)
        val thumbnail = if (scale >= 1f) {
            source
        } else {
            Bitmap.createScaledBitmap(
                source,
                max(1, (source.width * scale).toInt()),
                max(1, (source.height * scale).toInt()),
                true,
            )
        }
        try {
            file.outputStream().use { thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        } catch (_: IOException) {
            // A missing preview is cosmetic; never fail a wallpaper update because of it.
            file.delete()
        } finally {
            if (thumbnail !== source) thumbnail.recycle()
        }
    }

    fun load(): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

    private companion object {
        const val FILE_NAME = "last_wallpaper_thumb.jpg"
        const val MAX_EDGE = 512
    }
}
