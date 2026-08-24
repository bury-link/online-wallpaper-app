package link.bury.onlinewallpaper.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import link.bury.onlinewallpaper.ui.theme.OnlineWallpaperTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OnlineWallpaperTheme {
                val viewModel: SettingsViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onUrlChanged = viewModel::onUrlChanged,
                    onIntervalSelected = viewModel::onIntervalSelected,
                    onEnabledChanged = viewModel::onEnabledChanged,
                    onRefreshNow = viewModel::onRefreshNow,
                    onScreenResumed = viewModel::refreshBatteryOptimizationState,
                )
            }
        }
    }
}
