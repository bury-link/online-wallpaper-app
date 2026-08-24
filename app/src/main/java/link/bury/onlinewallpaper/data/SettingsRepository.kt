package link.bury.onlinewallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_ERROR = stringPreferencesKey("last_error")
        val LAST_ERROR_AT = longPreferencesKey("last_error_at")
    }
}
