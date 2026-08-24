package link.bury.onlinewallpaper.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import link.bury.onlinewallpaper.data.RefreshInterval
import java.util.concurrent.TimeUnit

/** Owns the two unique work chains: the periodic refresh and the manual "Set now" run. */
object WallpaperScheduler {

    const val PERIODIC_WORK_NAME = "wallpaper-refresh"
    const val MANUAL_WORK_NAME = "wallpaper-refresh-now"

    private val networkRequired = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** (Re)starts the periodic refresh. Safe to call repeatedly; the period restarts each time. */
    fun schedule(context: Context, interval: RefreshInterval) {
        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(
            interval.minutes, TimeUnit.MINUTES,
        )
            .setConstraints(networkRequired)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * Runs the refresh once, right now, regardless of the schedule. No network constraint, so a
     * device that is offline reports the error straight away instead of queueing silently.
     */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setInputData(Data.Builder().putBoolean(WallpaperWorker.KEY_MANUAL, true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun periodicWorkInfo(context: Context): Flow<WorkInfo?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(PERIODIC_WORK_NAME)
            .map { infos -> infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull() }

    fun manualWorkInfo(context: Context): Flow<WorkInfo?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(MANUAL_WORK_NAME)
            .map { infos -> infos.lastOrNull() }
}
