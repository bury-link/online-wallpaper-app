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
import link.bury.onlinewallpaper.wallpaper.BackgroundMode
import link.bury.onlinewallpaper.wallpaper.Framing
import link.bury.onlinewallpaper.wallpaper.ThumbnailStore
import link.bury.onlinewallpaper.wallpaper.WallpaperBackground
import link.bury.onlinewallpaper.work.WallpaperScheduler

data class SettingsUiState(
    val urlInput: String = "",
    val interval: RefreshInterval = RefreshInterval.DEFAULT,
    val enabled: Boolean = false,
    /** Where the FIT-FILL framing slider sits; see [Framing]. */
    val frameFit: Float = Framing.DEFAULT,
    val horizontalPosition: Float = Framing.POSITION_CENTER,
    val verticalPosition: Float = Framing.POSITION_CENTER,
    val background: WallpaperBackground = WallpaperBackground(),
    /** Raw text remains available while the user is composing a custom hex color. */
    val backgroundColorInput: String? = null,
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

private data class EditedValues(
    val url: String?,
    val frameFit: Float?,
    val horizontalPosition: Float?,
    val verticalPosition: Float?,
    val backgroundColor: String?,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = SettingsRepository(context)
    private val thumbnails = ThumbnailStore(context)

    /** Local echo of inputs so editing is not fighting the persisted value. */
    private val urlInput = MutableStateFlow<String?>(null)
    private val frameFitInput = MutableStateFlow<Float?>(null)
    private val horizontalPositionInput = MutableStateFlow<Float?>(null)
    private val verticalPositionInput = MutableStateFlow<Float?>(null)
    private val backgroundColorInput = MutableStateFlow<String?>(null)
    private val thumbnail = MutableStateFlow<Bitmap?>(null)
    private val batteryOptimized = MutableStateFlow(false)
    private var persistUrlJob: Job? = null
    private var persistFrameFitJob: Job? = null
    private var persistPositionJob: Job? = null
    private var persistBackgroundColorJob: Job? = null
    private var lastThumbnailStamp = -1L

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settings,
        WallpaperScheduler.periodicWorkInfo(context),
        WallpaperScheduler.manualWorkInfo(context),
        combine(
            urlInput,
            frameFitInput,
            horizontalPositionInput,
            verticalPositionInput,
            backgroundColorInput,
        ) { typedUrl, typedFrameFit, typedHorizontal, typedVertical, typedBackgroundColor ->
            EditedValues(typedUrl, typedFrameFit, typedHorizontal, typedVertical, typedBackgroundColor)
        },
        combine(thumbnail, batteryOptimized) { thumb, optimized -> thumb to optimized },
    ) { settings, periodicInfo, manualInfo, edited, (thumb, optimized) ->
        loadThumbnailIfChanged(settings.lastSuccessAt)
        SettingsUiState(
            urlInput = edited.url ?: settings.imageUrl,
            interval = settings.interval,
            enabled = settings.enabled,
            frameFit = edited.frameFit ?: settings.frameFit,
            horizontalPosition = edited.horizontalPosition ?: settings.horizontalPosition,
            verticalPosition = edited.verticalPosition ?: settings.verticalPosition,
            background = WallpaperBackground.fromStorage(
                settings.background.mode.storageValue,
                edited.backgroundColor ?: settings.background.colorHex,
            ),
            backgroundColorInput = edited.backgroundColor,
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

    fun onFrameFitChanged(fit: Float) {
        frameFitInput.value = fit
        persistFrameFitJob?.cancel()
        persistFrameFitJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MILLIS)
            repository.setFrameFit(fit)
        }
    }

    fun onHorizontalPositionChanged(position: Float) {
        horizontalPositionInput.value = position
        persistPosition()
    }

    fun onVerticalPositionChanged(position: Float) {
        verticalPositionInput.value = position
        persistPosition()
    }

    fun onBackgroundModeChanged(mode: BackgroundMode) {
        viewModelScope.launch { repository.setBackgroundMode(mode) }
    }

    fun onBackgroundColorChanged(colorHex: String) {
        backgroundColorInput.value = colorHex
        persistBackgroundColorJob?.cancel()
        persistBackgroundColorJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MILLIS)
            if (WallpaperBackground.isValidColor(colorHex)) repository.setBackgroundColor(colorHex)
        }
    }

    private fun persistPosition() {
        persistPositionJob?.cancel()
        persistPositionJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MILLIS)
            horizontalPositionInput.value?.let { repository.setHorizontalPosition(it) }
            verticalPositionInput.value?.let { repository.setVerticalPosition(it) }
        }
    }

    fun onIntervalSelected(interval: RefreshInterval) {
        viewModelScope.launch {
            repository.setInterval(interval)
            if (repository.current().enabled) WallpaperScheduler.schedule(context, interval)
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            flushPendingInput()
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
            flushPendingInput()
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

    private suspend fun flushPendingInput() {
        persistUrlJob?.cancel()
        persistFrameFitJob?.cancel()
        persistPositionJob?.cancel()
        persistBackgroundColorJob?.cancel()
        urlInput.value?.let { repository.setImageUrl(it) }
        frameFitInput.value?.let { repository.setFrameFit(it) }
        horizontalPositionInput.value?.let { repository.setHorizontalPosition(it) }
        verticalPositionInput.value?.let { repository.setVerticalPosition(it) }
        backgroundColorInput.value
            ?.takeIf(WallpaperBackground::isValidColor)
            ?.let { repository.setBackgroundColor(it) }
    }

    private fun restoreScheduleIfMissing() {
        viewModelScope.launch {
            val settings = repository.current()
            val scheduled = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(WallpaperScheduler.PERIODIC_WORK_NAME)
                .first()
                .any { !it.state.isFinished }
            if (settings.enabled && !scheduled) WallpaperScheduler.schedule(context, settings.interval)
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
        return System.currentTimeMillis() - lastSuccessAt > 2 * interval.millis + STALL_GRACE_MILLIS
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MILLIS = 400L
        const val STALL_GRACE_MILLIS = 15 * 60 * 1000L
    }
}
