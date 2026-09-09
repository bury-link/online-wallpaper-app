package link.bury.onlinewallpaper.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.roundToInt
import link.bury.onlinewallpaper.R
import link.bury.onlinewallpaper.data.CuratedWallpaperSource
import link.bury.onlinewallpaper.data.RefreshInterval
import link.bury.onlinewallpaper.data.SavedSource
import link.bury.onlinewallpaper.wallpaper.BackgroundMode
import link.bury.onlinewallpaper.wallpaper.Framing
import link.bury.onlinewallpaper.wallpaper.HsvColor
import link.bury.onlinewallpaper.wallpaper.WallpaperBackground
import link.bury.onlinewallpaper.wallpaper.calculateFrame
import link.bury.onlinewallpaper.wallpaper.calculatePreviewFrame

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUrlChanged: (String) -> Unit,
    onSaveCurrentSource: () -> Unit,
    onSelectSavedSource: (SavedSource) -> Unit,
    onRemoveSavedSource: (SavedSource) -> Unit,
    onIntervalSelected: (RefreshInterval) -> Unit,
    onFrameFitChanged: (Float) -> Unit,
    onHorizontalPositionChanged: (Float) -> Unit,
    onVerticalPositionChanged: (Float) -> Unit,
    onBackgroundModeChanged: (BackgroundMode) -> Unit,
    onBackgroundColorChanged: (String) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onRefreshNow: () -> Unit,
    onScreenResumed: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        onScreenResumed()
        onPauseOrDispose { }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader()

            UrlField(state = state, onUrlChanged = onUrlChanged)

            SavedSourcesSection(
                sources = state.savedSources,
                canSaveCurrent = state.urlIsValid,
                onSaveCurrent = onSaveCurrentSource,
                onSelect = onSelectSavedSource,
                onRemove = onRemoveSavedSource,
            )

            CuratedSourcePicker(currentUrl = state.urlInput, onUrlChanged = onUrlChanged)

            IntervalPicker(selected = state.interval, onIntervalSelected = onIntervalSelected)

            FrameFitSlider(value = state.frameFit, onValueChange = onFrameFitChanged)

            BackgroundPicker(
                background = state.background,
                colorInput = state.backgroundColorInput ?: state.background.colorHex,
                onModeChanged = onBackgroundModeChanged,
                onColorChanged = onBackgroundColorChanged,
            )

            HorizontalPositionSlider(
                value = state.horizontalPosition,
                onValueChange = onHorizontalPositionChanged,
            )

            WallpaperPreview(
                thumbnail = state.thumbnail,
                frameFit = state.frameFit,
                horizontalPosition = state.horizontalPosition,
                verticalPosition = state.verticalPosition,
                background = state.background,
                onVerticalPositionChanged = onVerticalPositionChanged,
            )

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

            PrivacyPolicyCard()

            Text(
                text = stringResource(R.string.lock_screen_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BrandFooter()
        }
    }
}

@Composable
private fun AppHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BrandFooter() {
    val context = LocalContext.current
    TextButton(
        onClick = { context.openBrandWebsite() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bury") }
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Light,
                    ),
                ) { append("/") }
                withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("link") }
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
        )
    }
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
            Text(
                if (state.urlError != null) stringResource(R.string.image_url_error)
                else stringResource(R.string.image_url_hint),
            )
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

@Composable
private fun SavedSourcesSection(
    sources: List<SavedSource>,
    canSaveCurrent: Boolean,
    onSaveCurrent: () -> Unit,
    onSelect: (SavedSource) -> Unit,
    onRemove: (SavedSource) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.saved_sources), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onSaveCurrent, enabled = canSaveCurrent) { Text(stringResource(R.string.save_current_source)) }
        }
        if (sources.isEmpty()) {
            Text(stringResource(R.string.saved_sources_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sources.forEach { source ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(source.name, style = MaterialTheme.typography.titleSmall)
                            Text(source.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onSelect(source) }) { Text(stringResource(R.string.use_saved_source)) }
                        TextButton(onClick = { onRemove(source) }) { Text(stringResource(R.string.remove_saved_source)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuratedSourcePicker(currentUrl: String, onUrlChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = CuratedWallpaperSource.findByUrl(currentUrl)

    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selected?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.curated_sources_custom_url),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.curated_sources)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CuratedWallpaperSource.entries.forEach { source ->
                    DropdownMenuItem(
                        text = { Text(stringResource(source.labelRes)) },
                        onClick = {
                            expanded = false
                            onUrlChanged(source.url)
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.curated_sources_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
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
            value = stringResource(selected.labelRes),
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
                    text = { Text(stringResource(interval.labelRes)) },
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
private fun FrameFitSlider(value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.frame_fit_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = Framing.FIT..Framing.FILL,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.frame_fit_fit_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.frame_fit_fill_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackgroundPicker(
    background: WallpaperBackground,
    colorInput: String,
    onModeChanged: (BackgroundMode) -> Unit,
    onColorChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.background_label), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.background_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onModeChanged(BackgroundMode.BLURRED_IMAGE) }) {
                Text(if (background.mode == BackgroundMode.BLURRED_IMAGE) {
                    "✓ ${stringResource(R.string.background_blurred_image)}"
                } else stringResource(R.string.background_blurred_image))
            }
            TextButton(onClick = { onModeChanged(BackgroundMode.COLOR) }) {
                Text(if (background.mode == BackgroundMode.COLOR) {
                    "✓ ${stringResource(R.string.background_color)}"
                } else stringResource(R.string.background_color))
            }
        }
        if (background.mode == BackgroundMode.COLOR) {
            VisualColorPicker(colorHex = colorInput, onColorChanged = onColorChanged)
        }
    }
}

@Composable
private fun VisualColorPicker(colorHex: String, onColorChanged: (String) -> Unit) {
    var selected by remember(colorHex) { mutableStateOf(HsvColor.fromHex(colorHex)) }
    var paletteSize by remember { mutableStateOf(IntSize.Zero) }
    var hueSize by remember { mutableStateOf(IntSize.Zero) }

    fun update(color: HsvColor) {
        selected = color.normalized()
        onColorChanged(selected.toHex())
    }

    fun updatePalette(position: Offset) {
        if (paletteSize.width == 0 || paletteSize.height == 0) return
        update(
            selected.copy(
                saturation = position.x / paletteSize.width,
                value = 1f - position.y / paletteSize.height,
            ),
        )
    }

    fun updateHue(position: Offset) {
        if (hueSize.width == 0) return
        update(selected.copy(hue = position.x / hueSize.width * 360f))
    }

    val selectedColor = Color.hsv(selected.hue, selected.saturation, selected.value)
    val hueColor = Color.hsv(selected.hue, 1f, 1f)
    val pickerShape = RoundedCornerShape(16.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.color_picker_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(selectedColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
            )
        }

        Text(
            text = stringResource(R.string.color_picker_saturation_value),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .clip(pickerShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, pickerShape)
                .onSizeChanged { paletteSize = it }
                .pointerInput(paletteSize, selected.hue) {
                    detectDragGestures(
                        onDragStart = ::updatePalette,
                        onDrag = { change, _ -> updatePalette(change.position) },
                    )
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val marker = Offset(
                x = selected.saturation * size.width,
                y = (1f - selected.value) * size.height,
            )
            drawCircle(Color.White, radius = 14f, center = marker)
            drawCircle(Color.Black, radius = 12f, center = marker, style = Stroke(3f))
        }

        Text(
            text = stringResource(R.string.color_picker_hue),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .onSizeChanged { hueSize = it }
                .pointerInput(hueSize) {
                    detectDragGestures(
                        onDragStart = ::updateHue,
                        onDrag = { change, _ -> updateHue(change.position) },
                    )
                },
        ) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                ),
            )
            val markerX = selected.hue / 360f * size.width
            drawCircle(Color.White, radius = 12f, center = Offset(markerX, size.height / 2f))
            drawCircle(Color.Black, radius = 10f, center = Offset(markerX, size.height / 2f), style = Stroke(3f))
        }

        Text(
            text = stringResource(R.string.color_picker_presets),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorPickerPresets.forEach { preset ->
                val presetColor = Color(
                    red = preset.substring(1, 3).toInt(16),
                    green = preset.substring(3, 5).toInt(16),
                    blue = preset.substring(5, 7).toInt(16),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(presetColor)
                        .border(
                            width = if (selected.toHex() == preset) 3.dp else 1.dp,
                            color = if (selected.toHex() == preset) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { update(HsvColor.fromHex(preset)) },
                )
            }
        }
    }
}

private val ColorPickerPresets = listOf(
    "#16161A",
    "#FF6C00",
    "#E8495F",
    "#5B8CFF",
    "#38B48B",
    "#8B5CF6",
    "#F4C542",
)

@Composable
private fun HorizontalPositionSlider(value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.image_position_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.image_position_horizontal),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = Framing.POSITION_START..Framing.POSITION_END,
        )
    }
}

@Composable
private fun WallpaperPreview(
    thumbnail: android.graphics.Bitmap?,
    frameFit: Float,
    horizontalPosition: Float,
    verticalPosition: Float,
    background: WallpaperBackground,
    onVerticalPositionChanged: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.wallpaper_preview),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.size(4.dp))
        if (thumbnail == null) {
            Text(
                text = stringResource(R.string.wallpaper_preview_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val configuration = LocalConfiguration.current
        val screenRatio = configuration.screenWidthDp.toFloat() /
            configuration.screenHeightDp.coerceAtLeast(1).toFloat()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(400.dp),
                contentAlignment = Alignment.Center,
            ) {
                Slider(
                    value = verticalPosition,
                    onValueChange = onVerticalPositionChanged,
                    valueRange = Framing.POSITION_START..Framing.POSITION_END,
                    // requiredWidth avoids the narrow side container constraining the rotated
                    // control to a tiny line. Its 200dp visible length is about half the preview.
                    modifier = Modifier
                        .requiredWidth(200.dp)
                        .graphicsLayer { rotationZ = 90f },
                )
            }
            Canvas(
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(screenRatio)
                    .clip(RoundedCornerShape(0.dp))
                    .background(Color.Black)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp)),
            ) {
                val preview = calculatePreviewFrame(
                    screenWidth = configuration.screenWidthDp,
                    screenHeight = configuration.screenHeightDp,
                    availableWidth = size.width.roundToInt(),
                )
                if (preview.width == 0 || preview.height == 0) return@Canvas
                when (background.mode) {
                    BackgroundMode.COLOR -> drawRect(Color(android.graphics.Color.parseColor(background.colorHex)))
                    BackgroundMode.BLURRED_IMAGE -> {
                        val fill = calculateFrame(
                            thumbnail.width, thumbnail.height, preview.width, preview.height,
                            Framing.FILL, horizontalPosition, verticalPosition,
                        )
                        drawImage(
                            image = thumbnail.asImageBitmap(),
                            srcOffset = IntOffset(fill.crop.left, fill.crop.top),
                            srcSize = IntSize(fill.crop.width, fill.crop.height),
                            dstSize = IntSize(preview.width, preview.height),
                        )
                        drawRect(Color.Black.copy(alpha = 0.36f))
                    }
                }
                val frame = calculateFrame(
                    sourceWidth = thumbnail.width,
                    sourceHeight = thumbnail.height,
                    targetWidth = preview.width,
                    targetHeight = preview.height,
                    fit = frameFit,
                    horizontalPosition = horizontalPosition,
                    verticalPosition = verticalPosition,
                )
                drawImage(
                    image = thumbnail.asImageBitmap(),
                    srcOffset = IntOffset(frame.crop.left, frame.crop.top),
                    srcSize = IntSize(frame.crop.width, frame.crop.height),
                    dstOffset = IntOffset(frame.drawLeft, frame.drawTop),
                    dstSize = IntSize(frame.drawWidth, frame.drawHeight),
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
                    stringResource(state.interval.labelRes),
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
    val context = LocalContext.current
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

            if (state.lastCheckedAt > 0) {
                StatusLine(
                    label = stringResource(R.string.last_source_check),
                    value = "${stringResource(if (state.lastImageUnchanged) R.string.source_unchanged else R.string.source_changed)}\n${formatTimestamp(state.lastCheckedAt)}",
                )
            }

            StatusLine(
                label = stringResource(R.string.next_run),
                value = when {
                    !state.enabled -> stringResource(R.string.refresh_disabled)
                    state.nextRunAt != null -> formatTimestamp(state.nextRunAt)
                    else -> stringResource(R.string.waiting_to_be_scheduled)
                },
            )

            state.lastError?.let { error ->
                val localizedError = context.localizeWallpaperError(error)
                HorizontalDivider()
                StatusLine(
                    label = stringResource(R.string.last_error),
                    value = if (state.lastErrorAt > 0) {
                        "$localizedError\n${formatTimestamp(state.lastErrorAt)}"
                    } else {
                        localizedError
                    },
                    error = true,
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

private fun Context.localizeWallpaperError(error: String): String = when {
    error == "Set a valid http(s) image URL first" -> getString(R.string.error_invalid_url)
    error == "Not a valid image URL" -> getString(R.string.error_invalid_image_url)
    error == "Download timed out" -> getString(R.string.error_download_timeout)
    error.startsWith("Network error: ") -> getString(R.string.error_network, error.removePrefix("Network error: "))
    error.startsWith("Server returned HTTP ") -> error.removePrefix("Server returned HTTP ")
        .toIntOrNull()?.let { getString(R.string.error_server_http, it) } ?: error
    error == "Empty response body" -> getString(R.string.error_empty_response)
    error.startsWith("Download failed: ") -> getString(R.string.error_download_failed, error.removePrefix("Download failed: "))
    error == "Downloaded file was empty" -> getString(R.string.error_download_empty)
    error == "Downloaded file is not a decodable image" -> getString(R.string.error_not_image)
    error == "Image could not be decoded" -> getString(R.string.error_decode)
    error == "Not enough memory to decode the image" -> getString(R.string.error_memory)
    error == "This device does not allow setting wallpapers" -> getString(R.string.error_wallpaper_unsupported)
    error == "Changing the wallpaper is blocked by a device policy" -> getString(R.string.error_wallpaper_blocked)
    error.startsWith("Could not apply the wallpaper: ") -> getString(
        R.string.error_apply_wallpaper,
        error.removePrefix("Could not apply the wallpaper: "),
    )
    error == "Temporary failure" -> getString(R.string.error_temporary)
    else -> error
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

@Composable
private fun PrivacyPolicyCard() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.privacy_policy_summary),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { context.openPrivacyPolicy() }) {
                Text(stringResource(R.string.open_privacy_policy))
            }
        }
    }
}

private fun Context.openPrivacyPolicy() {
    openExternalUrl(PRIVACY_POLICY_URL, R.string.privacy_policy_unavailable)
}

private fun Context.openBrandWebsite() {
    openExternalUrl(BRAND_WEBSITE_URL, R.string.brand_website_unavailable)
}

private fun Context.openExternalUrl(url: String, unavailableMessage: Int) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(this, unavailableMessage, android.widget.Toast.LENGTH_SHORT).show()
    }
}

private const val PRIVACY_POLICY_URL = "https://bury.link/privacy/online-wallpaper"
private const val BRAND_WEBSITE_URL = "https://bury.link"

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
