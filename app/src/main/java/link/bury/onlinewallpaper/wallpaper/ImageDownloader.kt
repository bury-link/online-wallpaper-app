package link.bury.onlinewallpaper.wallpaper

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

/** Downloads the remote image to a file so it can be decoded twice without buffering it in memory. */
class ImageDownloader(private val client: OkHttpClient = defaultClient) {

    /** Streams [url] into [destination]. Throws [TransientWallpaperException] on retryable errors. */
    fun download(url: String, destination: File) {
        val request = try {
            Request.Builder().url(url).header("Accept", "image/*").build()
        } catch (e: IllegalArgumentException) {
            throw PermanentWallpaperException("Not a valid image URL", e)
        }

        val response = try {
            client.newCall(request).execute()
        } catch (e: InterruptedIOException) {
            throw TransientWallpaperException("Download timed out", e)
        } catch (e: IOException) {
            throw TransientWallpaperException("Network error: ${e.message ?: "no connection"}", e)
        }

        response.use {
            if (!it.isSuccessful) {
                val message = "Server returned HTTP ${it.code}"
                throw if (it.code.isRetryable()) {
                    TransientWallpaperException(message)
                } else {
                    PermanentWallpaperException(message)
                }
            }
            val body = it.body ?: throw TransientWallpaperException("Empty response body")
            try {
                destination.outputStream().use { out -> body.byteStream().copyTo(out) }
            } catch (e: IOException) {
                throw TransientWallpaperException("Download failed: ${e.message ?: "read error"}", e)
            }
        }

        if (destination.length() == 0L) {
            throw TransientWallpaperException("Downloaded file was empty")
        }
    }

    private fun Int.isRetryable(): Boolean = this == 408 || this == 429 || this >= 500

    companion object {
        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(3, TimeUnit.MINUTES)
                .build()
        }
    }
}
