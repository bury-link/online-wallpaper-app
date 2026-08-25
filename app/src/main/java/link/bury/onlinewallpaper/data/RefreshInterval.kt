package link.bury.onlinewallpaper.data

import androidx.annotation.StringRes
import link.bury.onlinewallpaper.R
import java.util.concurrent.TimeUnit

/**
 * The refresh intervals the user can pick from.
 *
 * WorkManager clamps periodic work to a minimum period of 15 minutes, so that is the shortest
 * option we offer.
 */
enum class RefreshInterval(val minutes: Long, @StringRes val labelRes: Int) {
    MINUTES_15(15, R.string.interval_15_minutes),
    MINUTES_30(30, R.string.interval_30_minutes),
    HOURS_1(60, R.string.interval_1_hour),
    HOURS_6(6 * 60, R.string.interval_6_hours),
    HOURS_12(12 * 60, R.string.interval_12_hours),
    DAYS_1(24 * 60, R.string.interval_1_day);

    val millis: Long get() = TimeUnit.MINUTES.toMillis(minutes)

    companion object {
        val DEFAULT = HOURS_6

        fun fromMinutes(minutes: Long): RefreshInterval =
            entries.firstOrNull { it.minutes == minutes } ?: DEFAULT
    }
}
