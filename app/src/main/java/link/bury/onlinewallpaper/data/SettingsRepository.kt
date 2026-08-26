package link.bury.onlinewallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import link.bury.onlinewallpaper.wallpaper.Framing
import link.bury.onlinewallpaper.wallpaper.BackgroundMode
import link.bury.onlinewallpaper.wallpaper.WallpaperBackground

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Single source of truth for the persisted [WallpaperSettings]. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val settings: Flow<WallpaperSettings> = dataStore.data.map { prefs ->
        WallpaperSettings(
            imageUrl = prefs[Keys.IMAGE_URL].orEmpty(),
            interval = RefreshInterval.fromMinutes(
                prefs[Keys.INTERVAL_MINUTES] ?: RefreshInterval.DEFAULT.minutes
            ),
            enabled = prefs[Keys.ENABLED] ?: false,
            frameFit = prefs[Keys.FRAME_FIT] ?: Framing.DEFAULT,
            horizontalPosition = prefs[Keys.HORIZONTAL_POSITION] ?: Framing.POSITION_CENTER,
            verticalPosition = prefs[Keys.VERTICAL_POSITION] ?: Framing.POSITION_CENTER,
            background = WallpaperBackground.fromStorage(
                modeValue = prefs[Keys.BACKGROUND_MODE],
                colorValue = prefs[Keys.BACKGROUND_COLOR],
            ),
            lastSuccessAt = prefs[Keys.LAST_SUCCESS_AT] ?: 0L,
            lastError = prefs[Keys.LAST_ERROR],
            lastErrorAt = prefs[Keys.LAST_ERROR_AT] ?: 0L,
        )
    }

    suspend fun current(): WallpaperSettings = settings.first()

    suspend fun setImageUrl(url: String) = edit { it[Keys.IMAGE_URL] = url.trim() }

    suspend fun setInterval(interval: RefreshInterval) =
        edit { it[Keys.INTERVAL_MINUTES] = interval.minutes }

    suspend fun setEnabled(enabled: Boolean) = edit { it[Keys.ENABLED] = enabled }

    suspend fun setFrameFit(frameFit: Float) =
        edit { it[Keys.FRAME_FIT] = frameFit.coerceIn(Framing.FIT, Framing.FILL) }

    suspend fun setHorizontalPosition(position: Float) = edit {
        it[Keys.HORIZONTAL_POSITION] = position.coerceIn(Framing.POSITION_START, Framing.POSITION_END)
    }

    suspend fun setVerticalPosition(position: Float) = edit {
        it[Keys.VERTICAL_POSITION] = position.coerceIn(Framing.POSITION_START, Framing.POSITION_END)
    }

    suspend fun setBackgroundMode(mode: BackgroundMode) = edit {
        it[Keys.BACKGROUND_MODE] = mode.storageValue
    }

    suspend fun setBackgroundColor(colorHex: String) = edit {
        it[Keys.BACKGROUND_COLOR] = WallpaperBackground.fromStorage(null, colorHex).colorHex
    }

    /** Records a successful run and clears any previous error. */
    suspend fun recordSuccess(timestamp: Long) = edit { prefs ->
        prefs[Keys.LAST_SUCCESS_AT] = timestamp
        prefs.remove(Keys.LAST_ERROR)
        prefs.remove(Keys.LAST_ERROR_AT)
    }

    suspend fun recordError(message: String, timestamp: Long) = edit { prefs ->
        prefs[Keys.LAST_ERROR] = message
        prefs[Keys.LAST_ERROR_AT] = timestamp
    }

    suspend fun clearError() = edit { prefs ->
        prefs.remove(Keys.LAST_ERROR)
        prefs.remove(Keys.LAST_ERROR_AT)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val IMAGE_URL = stringPreferencesKey("image_url")
        val INTERVAL_MINUTES = longPreferencesKey("interval_minutes")
        val ENABLED = booleanPreferencesKey("enabled")
        val FRAME_FIT = floatPreferencesKey("frame_fit")
        val HORIZONTAL_POSITION = floatPreferencesKey("horizontal_position")
        val VERTICAL_POSITION = floatPreferencesKey("vertical_position")
        val BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_ERROR = stringPreferencesKey("last_error")
        val LAST_ERROR_AT = longPreferencesKey("last_error_at")
    }
}
