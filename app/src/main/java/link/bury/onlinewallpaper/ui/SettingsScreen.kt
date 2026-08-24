package link.bury.onlinewallpaper.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import link.bury.onlinewallpaper.R
import link.bury.onlinewallpaper.data.RefreshInterval

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUrlChanged: (String) -> Unit,
    onIntervalSelected: (RefreshInterval) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onRefreshNow: () -> Unit,
    onScreenResumed: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        onScreenResumed()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { AppBar() },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UrlField(state = state, onUrlChanged = onUrlChanged)

            IntervalPicker(selected = state.interval, onIntervalSelected = onIntervalSelected)

            EnableRow(state = state, onEnabledChanged = onEnabledChanged)

            Button(
                onClick = onRefreshNow,
                enabled = state.canRefreshNow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.setting_now))
                } else {
                    Text(stringResource(R.string.set_now))
                }
            }

            StatusCard(state = state)

            if (state.showBatteryHint) {
                BatteryHintCard(stalled = state.looksStalled)
            }

            Text(
                text = stringResource(R.string.lock_screen_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar() {
    TopAppBar(title = { Text(stringResource(R.string.app_name)) })
}

@Composable
private fun UrlField(state: SettingsUiState, onUrlChanged: (String) -> Unit) {
    OutlinedTextField(
        value = state.urlInput,
        onValueChange = onUrlChanged,
        label = { Text(stringResource(R.string.image_url)) },
        placeholder = { Text("https://example.com/wallpaper.jpg") },
        singleLine = true,
        isError = state.urlError != null,
        supportingText = {
            Text(state.urlError ?: stringResource(R.string.image_url_hint))
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalPicker(
    selected: RefreshInterval,
    onIntervalSelected: (RefreshInterval) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.refresh_interval)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RefreshInterval.entries.forEach { interval ->
                DropdownMenuItem(
                    text = { Text(interval.label) },
                    onClick = {
                        expanded = false
                        onIntervalSelected(interval)
                    },
                )
            }
        }
    }
}

@Composable
private fun EnableRow(state: SettingsUiState, onEnabledChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.automatic_refresh),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    if (state.enabled) R.string.refresh_on else R.string.refresh_off,
                    state.interval.label,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.enabled,
            onCheckedChange = onEnabledChanged,
            enabled = state.enabled || state.urlIsValid,
        )
    }
}

@Composable
private fun StatusCard(state: SettingsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium)

            StatusLine(
                label = stringResource(R.string.last_success),
                value = state.lastSuccessAt
                    .takeIf { it > 0 }
                    ?.let(::formatTimestamp)
                    ?: stringResource(R.string.never),
            )

            StatusLine(
                label = stringResource(R.string.next_run),
                value = when {
                    !state.enabled -> stringResource(R.string.refresh_disabled)
                    state.nextRunAt != null -> formatTimestamp(state.nextRunAt)
                    else -> stringResource(R.string.waiting_to_be_scheduled)
                },
            )

            state.lastError?.let { error ->
                HorizontalDivider()
                StatusLine(
                    label = stringResource(R.string.last_error),
                    value = if (state.lastErrorAt > 0) {
                        "$error\n${formatTimestamp(state.lastErrorAt)}"
                    } else {
                        error
                    },
                    error = true,
                )
            }

            state.thumbnail?.let { bitmap ->
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.current_wallpaper),
                    style = MaterialTheme.typography.labelLarge,
                )
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.current_wallpaper),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, error: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4,
        )
    }
}

@Composable
private fun BatteryHintCard(stalled: Boolean) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.battery_hint_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (stalled) R.string.battery_hint_stalled else R.string.battery_hint_body
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { context.openBatteryOptimizationSettings() }) {
                Text(stringResource(R.string.battery_hint_action))
            }
        }
    }
}

/**
 * Asks the system to exempt this app from battery optimization. Falls back to the general list
 * when the direct dialog is unavailable, and does nothing at all if neither screen exists — the
 * hint is advisory, never a requirement.
 */
@SuppressLint("BatteryLife")
private fun Context.openBatteryOptimizationSettings() {
    val request = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.fromParts("package", packageName, null),
    )
    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    for (intent in listOf(request, fallback)) {
        try {
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        } catch (_: SecurityException) {
            // Some OEM builds refuse the direct request; the settings list still works.
        }
    }
}
