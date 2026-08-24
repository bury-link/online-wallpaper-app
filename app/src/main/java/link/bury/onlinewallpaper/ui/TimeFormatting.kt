package link.bury.onlinewallpaper.ui

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date

/** "24 Aug 2026, 11:45 (2 hours ago)" — absolute first, because "ago" alone gets vague fast. */
fun formatTimestamp(millis: Long): String {
    val absolute = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(millis))
    val relative = DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    return "$absolute ($relative)"
}
