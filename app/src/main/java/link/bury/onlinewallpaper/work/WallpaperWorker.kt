package link.bury.onlinewallpaper.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import link.bury.onlinewallpaper.data.SettingsRepository
import link.bury.onlinewallpaper.wallpaper.PermanentWallpaperException
import link.bury.onlinewallpaper.wallpaper.TransientWallpaperException
import link.bury.onlinewallpaper.wallpaper.WallpaperUpdater

/**
 * Fetches the configured image and applies it to the lock screen. Used both for the periodic
 * refresh and for the "Set now" button, which passes [KEY_MANUAL] so it runs even while the
 * schedule is disabled.
 */
class WallpaperWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = SettingsRepository(applicationContext)
        val settings = repository.current()
        val manual = inputData.getBoolean(KEY_MANUAL, false)

        // A scheduled run that arrives after the user switched the schedule off is a no-op rather
        // than an error: the work is cancelled separately, this just closes the race.
        if (!manual && !settings.enabled) return Result.success()

        return try {
            WallpaperUpdater(applicationContext).applyFrom(
                url = settings.imageUrl,
                fit = settings.frameFit,
                horizontalPosition = settings.horizontalPosition,
                verticalPosition = settings.verticalPosition,
            )
            repository.recordSuccess(System.currentTimeMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: PermanentWallpaperException) {
            fail(repository, e)
        } catch (e: TransientWallpaperException) {
            Log.w(TAG, "Transient failure on attempt ${runAttemptCount + 1}", e)
            repository.recordError(e.message ?: "Temporary failure", System.currentTimeMillis())
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        } catch (e: Exception) {
            fail(repository, e)
        }
    }

    private suspend fun fail(repository: SettingsRepository, e: Exception): Result {
        Log.w(TAG, "Giving up on this run", e)
        repository.recordError(e.message ?: e.javaClass.simpleName, System.currentTimeMillis())
        return Result.failure()
    }

    companion object {
        const val KEY_MANUAL = "manual"

        /** Attempts per run, including the first, before waiting for the next scheduled run. */
        private const val MAX_ATTEMPTS = 3
        private const val TAG = "WallpaperWorker"
    }
}
