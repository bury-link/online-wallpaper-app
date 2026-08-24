package link.bury.onlinewallpaper.data

import java.util.concurrent.TimeUnit

/**
 * The refresh intervals the user can pick from.
 *
 * WorkManager clamps periodic work to a minimum period of 15 minutes, so that is the shortest
 * option we offer.
 */
enum class RefreshInterval(val minutes: Long, val label: String) {
    MINUTES_15(15, "15 minutes"),
    MINUTES_30(30, "30 minutes"),
    HOURS_1(60, "1 hour"),
    HOURS_6(6 * 60, "6 hours"),
    HOURS_12(12 * 60, "12 hours"),
    DAYS_1(24 * 60, "1 day");

    val millis: Long get() = TimeUnit.MINUTES.toMillis(minutes)

    companion object {
        val DEFAULT = HOURS_6

        fun fromMinutes(minutes: Long): RefreshInterval =
            entries.firstOrNull { it.minutes == minutes } ?: DEFAULT
    }
}
