package link.bury.onlinewallpaper.wallpaper

/** Decides whether a downloaded image is identical to the last one successfully applied. */
object ImageFingerprint {
    fun isUnchanged(previous: String?, current: String): Boolean =
        previous?.isNotBlank() == true && current.isNotBlank() && previous == current
}
