package link.bury.onlinewallpaper.wallpaper

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class DownloadedImage(val sha256: String, val lastModified: String?)

/** Downloads the remote image to a file so it can be decoded twice without buffering it in memory. */
class ImageDownloader(private val client: OkHttpClient = defaultClient) {

    /** Streams [url] into [destination] and returns its content fingerprint and source metadata. */
    fun download(url: String, destination: File): DownloadedImage {
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

        val lastModified = response.header("Last-Modified")
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
                val digest = MessageDigest.getInstance("SHA-256")
                body.byteStream().use { input ->
                    destination.outputStream().use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                            out.write(buffer, 0, count)
                        }
                    }
                }
                if (destination.length() == 0L) throw TransientWallpaperException("Downloaded file was empty")
                return DownloadedImage(
                    sha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) },
                    lastModified = lastModified,
                )
            } catch (e: IOException) {
                throw TransientWallpaperException("Download failed: ${e.message ?: "read error"}", e)
            }
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
