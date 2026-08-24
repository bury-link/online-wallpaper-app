package link.bury.onlinewallpaper.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.bury.onlinewallpaper.data.RefreshInterval
import link.bury.onlinewallpaper.data.SettingsRepository
import link.bury.onlinewallpaper.data.UrlValidator
import link.bury.onlinewallpaper.data.WallpaperSettings
import link.bury.onlinewallpaper.wallpaper.ThumbnailStore
import link.bury.onlinewallpaper.work.WallpaperScheduler

data class SettingsUiState(
    val urlInput: String = "",
    val interval: RefreshInterval = RefreshInterval.DEFAULT,
    val enabled: Boolean = false,
    val lastSuccessAt: Long = 0L,
    val lastError: String? = null,
    val lastErrorAt: Long = 0L,
    /** Epoch millis of the next periodic run, or null when nothing is scheduled. */
    val nextRunAt: Long? = null,
    val isRefreshing: Boolean = false,
    val thumbnail: Bitmap? = null,
    /** True when the app is subject to battery optimization, which can delay the worker. */
    val batteryOptimized: Boolean = false,
    /** True when the schedule is on but the last success is far older than the interval. */
    val looksStalled: Boolean = false,
) {
    val urlIsValid: Boolean get() = UrlValidator.isValid(urlInput)
    val urlError: String?
        get() = if (urlInput.isBlank() || urlIsValid) null else "Enter a full http(s) image URL"
    val canRefreshNow: Boolean get() = urlIsValid && !isRefreshing
    val showBatteryHint: Boolean get() = enabled && (batteryOptimized || looksStalled)
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = SettingsRepository(context)
    private val thumbnails = ThumbnailStore(context)

    /** Local echo of the text field so typing is not fighting the persisted value. */
    private val urlInput = MutableStateFlow<String?>(null)
    private val thumbnail = MutableStateFlow<Bitmap?>(null)
    private val batteryOptimized = MutableStateFlow(false)
    private var persistUrlJob: Job? = null
    private var lastThumbnailStamp = -1L

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settings,
        WallpaperScheduler.periodicWorkInfo(context),
        WallpaperScheduler.manualWorkInfo(context),
        urlInput,
        combine(thumbnail, batteryOptimized) { thumb, optimized -> thumb to optimized },
    ) { settings, periodicInfo, manualInfo, typedUrl, (thumb, optimized) ->
        loadThumbnailIfChanged(settings.lastSuccessAt)
        SettingsUiState(
            urlInput = typedUrl ?: settings.imageUrl,
            interval = settings.interval,
            enabled = settings.enabled,
            lastSuccessAt = settings.lastSuccessAt,
            lastError = settings.lastError,
            lastErrorAt = settings.lastErrorAt,
            nextRunAt = periodicInfo?.nextRunAt(),
            isRefreshing = manualInfo?.state == WorkInfo.State.RUNNING ||
                manualInfo?.state == WorkInfo.State.ENQUEUED,
            thumbnail = thumb,
            batteryOptimized = optimized,
            looksStalled = settings.looksStalled(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshBatteryOptimizationState()
        restoreScheduleIfMissing()
    }

    fun onUrlChanged(url: String) {
        urlInput.value = url
        persistUrlJob?.cancel()
        persistUrlJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MILLIS)
            repository.setImageUrl(url)
        }
    }

    fun onIntervalSelected(interval: RefreshInterval) {
        viewModelScope.launch {
            repository.setInterval(interval)
            if (repository.current().enabled) {
                WallpaperScheduler.schedule(context, interval)
            }
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            flushPendingUrl()
            val settings = repository.current()
            if (enabled && !settings.hasValidUrl) return@launch

            repository.setEnabled(enabled)
            if (enabled) {
                repository.clearError()
                WallpaperScheduler.schedule(context, settings.interval)
            } else {
                WallpaperScheduler.cancel(context)
            }
        }
    }

    fun onRefreshNow() {
        viewModelScope.launch {
            flushPendingUrl()
            if (!repository.current().hasValidUrl) return@launch
            WallpaperScheduler.refreshNow(context)
        }
    }

    /** Re-read the exemption state; it can change while the user is in system settings. */
    fun refreshBatteryOptimizationState() {
        val power = context.getSystemService(PowerManager::class.java)
        val exempt = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        batteryOptimized.value = !exempt
    }

    private suspend fun flushPendingUrl() {
        persistUrlJob?.cancel()
        urlInput.value?.let { repository.setImageUrl(it) }
    }

    private fun restoreScheduleIfMissing() {
        viewModelScope.launch {
            val settings = repository.current()
            val scheduled = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(WallpaperScheduler.PERIODIC_WORK_NAME)
                .first()
                .any { !it.state.isFinished }
            if (settings.enabled && !scheduled) {
                WallpaperScheduler.schedule(context, settings.interval)
            }
        }
    }

    private fun loadThumbnailIfChanged(lastSuccessAt: Long) {
        if (lastSuccessAt == lastThumbnailStamp) return
        lastThumbnailStamp = lastSuccessAt
        viewModelScope.launch {
            thumbnail.value = withContext(Dispatchers.IO) { thumbnails.load() }
        }
    }

    private fun WorkInfo.nextRunAt(): Long? = nextScheduleTimeMillis
        .takeIf { it != Long.MAX_VALUE && state != WorkInfo.State.CANCELLED }

    private fun WallpaperSettings.looksStalled(): Boolean {
        if (!enabled || lastSuccessAt == 0L) return false
        val overdueBy = System.currentTimeMillis() - lastSuccessAt
        return overdueBy > 2 * interval.millis + STALL_GRACE_MILLIS
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MILLIS = 400L
        const val STALL_GRACE_MILLIS = 15 * 60 * 1000L
    }
}
