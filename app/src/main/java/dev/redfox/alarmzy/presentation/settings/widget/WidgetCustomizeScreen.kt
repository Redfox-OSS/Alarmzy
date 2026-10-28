package dev.redfox.alarmzy.presentation.settings.widget

import android.content.ComponentName
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.redfox.alarmzy.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.redfox.alarmzy.domain.model.WeatherInterval
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.widget.WeatherError
import dev.redfox.obsidian.components.ObTextField
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WidgetHorizontalAlign
import dev.redfox.alarmzy.domain.model.WidgetLayout
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.domain.model.WidgetVerticalAlign
import dev.redfox.alarmzy.domain.model.WidgetWeather
import dev.redfox.alarmzy.widget.ClockWidgetRenderer
import dev.redfox.alarmzy.widget.LaunchableApp
import dev.redfox.obsidian.components.ChoicePill
import dev.redfox.obsidian.components.DetailHeader
import dev.redfox.obsidian.components.DetailHeaderHeight
import dev.redfox.obsidian.components.ObCard
import dev.redfox.obsidian.components.ObDivider
import dev.redfox.obsidian.components.ObListRow
import dev.redfox.obsidian.components.ObSearchField
import dev.redfox.obsidian.components.ObSheet
import dev.redfox.obsidian.components.ObSlider
import dev.redfox.obsidian.components.ObSwitch
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.components.SectionLabel
import dev.redfox.obsidian.components.SwitchSize
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.motion.RevealColumn
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.ScreenGutter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun WidgetCustomizeScreen(
    uiState: WidgetCustomizeUiState,
    onIntent: (WidgetCustomizeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val config = uiState.config
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailHeaderHeight

    // Widgets may have been placed/removed (or apps installed) while we were away.
    LifecycleResumeEffect(Unit) {
        onIntent(WidgetCustomizeIntent.Refresh)
        onPauseOrDispose { }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(
                    top = topInset + 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Preview")
                WidgetPreview(
                    config = config,
                    weather = uiState.weather,
                    onPartClick = { onIntent(WidgetCustomizeIntent.OpenPicker(it)) }
                )
                Text(
                    if (config.showWeather) "Tap the date, time or weather to choose the app it opens."
                    else "Tap the date or the time to choose the app it opens.",
                    style = type.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("On your home screen")
                ObCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    ObListRow(
                        title = when (uiState.placedCount) {
                            0 -> "Not added yet"
                            1 -> "1 widget placed"
                            else -> "${uiState.placedCount} widgets placed"
                        },
                        subtitle = if (uiState.canPinWidget) "Edits apply instantly"
                        else "Long-press your home screen → Widgets → Alarmzy",
                        icon = R.drawable.ic_widgets,
                        trailing = if (uiState.canPinWidget) {
                            {
                                PillButton(
                                    text = "Add",
                                    containerColor = colors.accentContainerStrong,
                                    contentColor = colors.accentLight,
                                    onClick = { onIntent(WidgetCustomizeIntent.AddToHomeScreen) }
                                )
                            }
                        } else null
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Tap actions")
                ObCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    val parts = WidgetPart.entries.filter { it != WidgetPart.WEATHER || config.showWeather }
                    parts.forEachIndexed { index, part ->
                        if (index > 0) ObDivider(Modifier.padding(vertical = 12.dp))
                        PartAppRow(
                            part = part,
                            app = uiState.partApps[part],
                            isDefault = part in uiState.defaultParts,
                            onClick = { onIntent(WidgetCustomizeIntent.OpenPicker(part)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Size") {
                    Text(
                        "${(config.scale * 100).roundToInt()}%",
                        style = type.labelStrong,
                        color = colors.textSecondary,
                        modifier = Modifier.pressable { onIntent(WidgetCustomizeIntent.ResetScale) }
                    )
                }
                ObSlider(
                    value = config.scale,
                    onValueChange = { onIntent(WidgetCustomizeIntent.DragScale((it * 20).roundToInt() / 20f)) },
                    onValueChangeFinished = { onIntent(WidgetCustomizeIntent.CommitScale) },
                    valueRange = ClockWidgetConfig.MIN_SCALE..ClockWidgetConfig.MAX_SCALE,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Layout")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetLayout.entries.forEach { layout ->
                        ChoicePill(
                            text = layout.displayName,
                            selected = layout == config.layout,
                            onClick = { onIntent(WidgetCustomizeIntent.SetLayout(layout)) }
                        )
                    }
                }
                if (config.layout == WidgetLayout.VERTICAL) {
                    Text(
                        "Date, time and weather on separate lines. Resize the widget taller on your home screen to fit.",
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Horizontal position")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetHorizontalAlign.entries.forEach { align ->
                        ChoicePill(
                            text = align.displayName,
                            selected = align == config.horizontalAlign,
                            onClick = { onIntent(WidgetCustomizeIntent.SetHorizontalAlign(align)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Vertical position")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetVerticalAlign.entries.forEach { align ->
                        ChoicePill(
                            text = align.displayName,
                            selected = align == config.verticalAlign,
                            onClick = { onIntent(WidgetCustomizeIntent.SetVerticalAlign(align)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Style")
                ObCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    ObListRow(
                        title = "24-hour clock",
                        subtitle = if (config.use24Hour) "00:02" else "12:02",
                        icon = R.drawable.ic_alarm,
                        onClick = { onIntent(WidgetCustomizeIntent.SetUse24Hour(!config.use24Hour)) },
                        trailing = {
                            ObSwitch(
                                checked = config.use24Hour,
                                onCheckedChange = { onIntent(WidgetCustomizeIntent.SetUse24Hour(it)) },
                                size = SwitchSize.Medium
                            )
                        }
                    )
                    ObDivider(Modifier.padding(vertical = 12.dp))
                    ObListRow(
                        title = "Bold",
                        subtitle = "Heavier text, dividers and weather icon",
                        icon = R.drawable.ic_format_paint,
                        onClick = { onIntent(WidgetCustomizeIntent.SetBold(!config.bold)) },
                        trailing = {
                            ObSwitch(
                                checked = config.bold,
                                onCheckedChange = { onIntent(WidgetCustomizeIntent.SetBold(it)) },
                                size = SwitchSize.Medium
                            )
                        }
                    )
                }
            }

            WeatherSection(uiState = uiState, onIntent = onIntent)
        }

        DetailHeader(title = "Home-screen widget", onBack = onNavigateBack, modifier = Modifier.align(Alignment.TopCenter))
    }

    if (uiState.citySearch.open) {
        CitySearchSheet(search = uiState.citySearch, provider = uiState.config.weatherProvider, onIntent = onIntent)
    }

    if (uiState.apiKeySheet.open) {
        ApiKeySheet(sheet = uiState.apiKeySheet, hasSavedKey = uiState.apiKey != null, onIntent = onIntent)
    }

    uiState.pickerPart?.let { part ->
        AppPickerSheet(
            part = part,
            uiState = uiState,
            onIntent = onIntent
        )
    }
}

/**
 * The real widget [android.widget.RemoteViews] inflated in-app over a wallpaper-ish backdrop,
 * so the preview is pixel-identical to the home screen. Its parts open the app picker.
 */
@Composable
private fun WidgetPreview(config: ClockWidgetConfig, weather: WidgetWeather?, onPartClick: (WidgetPart) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val latestClick by rememberUpdatedState(onPartClick)
    // The widget is drawn as bitmaps, so tick the preview at each minute boundary like the real one.
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            val millis = System.currentTimeMillis()
            delay(60_000L - millis % 60_000L)
            value = ZonedDateTime.now()
        }
    }
    Box(
        Modifier
            .glow(Color.Black.copy(alpha = 0.5f), shape, radius = 24.dp, offsetY = 12.dp)
            .fillMaxWidth()
            // A 4x1 cell for the line; roughly 4x2 for the stack.
            .height(if (config.layout == WidgetLayout.VERTICAL) 200.dp else 132.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF2B3A55), Color(0xFF1B1F2B), Color(0xFF3A2B45)))
            )
            .border(1.dp, ObsidianTheme.colors.borderStrong, shape)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        // The widget's cell bounds, so alignment changes are visible.
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
        )
        AndroidView(
            factory = { ctx -> FrameLayout(ctx) },
            update = { frame ->
                val ctx = frame.context
                val views = ClockWidgetRenderer.build(ctx, config, weather, now, withClicks = false)
                val layout = ClockWidgetRenderer.layoutFor(config)
                val child = frame.getChildAt(0)
                // reapply() needs the same layout; a layout switch inflates afresh.
                if (child == null || frame.tag != layout) {
                    frame.removeAllViews()
                    frame.addView(views.apply(ctx, frame))
                    frame.tag = layout
                } else {
                    views.reapply(ctx, child)
                }
                WidgetPart.entries.forEach { part ->
                    ClockWidgetRenderer.partViews(part).forEach { id ->
                        frame.findViewById<View>(id)?.setOnClickListener { latestClick(part) }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WeatherSection(uiState: WidgetCustomizeUiState, onIntent: (WidgetCustomizeIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val config = uiState.config
    val provider = config.weatherProvider
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Weather")
        ObCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            ObListRow(
                title = "Show weather",
                subtitle = weatherStatusText(uiState),
                icon = R.drawable.ic_weather_cloud,
                onClick = {
                    when {
                        !config.showWeather -> onIntent(WidgetCustomizeIntent.SetShowWeather(true))
                        uiState.weatherError == WeatherError.NO_KEY -> onIntent(WidgetCustomizeIntent.OpenApiKeySheet)
                        uiState.weather == null || uiState.weatherError != null -> onIntent(WidgetCustomizeIntent.RefreshWeather)
                        else -> onIntent(WidgetCustomizeIntent.SetShowWeather(false))
                    }
                },
                trailing = {
                    ObSwitch(
                        checked = config.showWeather,
                        onCheckedChange = { onIntent(WidgetCustomizeIntent.SetShowWeather(it)) },
                        size = SwitchSize.Medium
                    )
                }
            )
            ObDivider(Modifier.padding(vertical = 12.dp))
            ObListRow(
                title = "City",
                subtitle = config.weatherCity?.displayName ?: "Choose a city",
                icon = R.drawable.ic_label,
                onClick = { onIntent(WidgetCustomizeIntent.OpenCitySearch) },
                trailing = {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.textSecondary)
                }
            )
            ObDivider(Modifier.padding(vertical = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Provider", style = type.labelStrong, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeatherProvider.entries.forEach { option ->
                        ChoicePill(
                            text = option.displayName,
                            selected = option == provider,
                            onClick = { onIntent(WidgetCustomizeIntent.SetProvider(option)) }
                        )
                    }
                }
            }
            if (provider.needsApiKey) {
                ObDivider(Modifier.padding(vertical = 12.dp))
                ObListRow(
                    title = "API key",
                    subtitle = uiState.apiKey?.let { "••••${it.takeLast(4)}" + if (uiState.weatherError == WeatherError.AUTH) " · rejected" else "" }
                        ?: "Not set · tap to add",
                    icon = R.drawable.ic_tune,
                    onClick = { onIntent(WidgetCustomizeIntent.OpenApiKeySheet) },
                    trailing = {
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.textSecondary)
                    }
                )
            }
            if (config.showWeather) {
                ObDivider(Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Update every", style = type.labelStrong, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        PillButton(
                            text = if (uiState.weatherRefreshing) "Updating…" else "Update now",
                            containerColor = colors.accentContainerStrong,
                            contentColor = colors.accentLight,
                            onClick = { onIntent(WidgetCustomizeIntent.RefreshWeather) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherInterval.entries.forEach { interval ->
                            ChoicePill(
                                text = interval.displayName,
                                selected = interval == config.weatherInterval,
                                onClick = { onIntent(WidgetCustomizeIntent.SetWeatherInterval(interval)) }
                            )
                        }
                    }
                }
                ObDivider(Modifier.padding(vertical = 12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Units", style = type.labelStrong, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoicePill(text = "°C", selected = !config.useFahrenheit, onClick = { onIntent(WidgetCustomizeIntent.SetFahrenheit(false)) })
                        ChoicePill(text = "°F", selected = config.useFahrenheit, onClick = { onIntent(WidgetCustomizeIntent.SetFahrenheit(true)) })
                    }
                }
            }
        }
        Text(
            when (provider) {
                WeatherProvider.OPEN_METEO ->
                    "Weather comes from Open-Meteo (open-meteo.com), free and without an account. "
                WeatherProvider.OPENWEATHER ->
                    "Weather comes from OpenWeather (openweathermap.org) with your own API key, which " +
                        "stays on this phone and is never backed up. "
            } + "Only the city you search for and its coordinates leave your phone — Alarmzy never " +
                "reads your location. Weather updates on your chosen interval while the screen is on " +
                "(never waking the phone); if an update fails, the last reading stays.",
            style = type.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

private fun weatherStatusText(uiState: WidgetCustomizeUiState): String {
    val config = uiState.config
    val weather = uiState.weather
    val provider = config.weatherProvider.displayName
    val problem = when (uiState.weatherError) {
        WeatherError.NO_KEY -> "add your API key"
        WeatherError.AUTH -> "$provider rejected the API key"
        WeatherError.NETWORK -> "couldn't reach $provider"
        null -> null
    }
    return when {
        !config.showWeather -> "Temperature and an icon after the time"
        uiState.weatherRefreshing && weather == null -> "Updating…"
        weather == null && problem != null -> problem.replaceFirstChar { it.uppercase() } + " · tap to retry"
        weather == null -> "Waiting for the first update · tap to retry"
        else -> {
            val minutes = ((System.currentTimeMillis() - weather.fetchedAtMillis) / 60_000L).coerceAtLeast(0)
            val age = when {
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                minutes < 24 * 60 -> "${minutes / 60} h ago"
                else -> "${minutes / (24 * 60)} d ago"
            }
            val reading = "${weather.temperature(config.useFahrenheit)}° · ${weather.condition.displayName} · updated $age"
            if (problem != null) "$reading · $problem" else reading
        }
    }
}

@Composable
private fun CitySearchSheet(search: CitySearchState, provider: WeatherProvider, onIntent: (WidgetCustomizeIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ObSheet(onDismiss = { onIntent(WidgetCustomizeIntent.CloseCitySearch) }, title = "Weather city") {
        ObSearchField(
            query = search.query,
            onQueryChange = { onIntent(WidgetCustomizeIntent.SetCityQuery(it)) },
            placeholder = "Search for a city",
            focusRequester = focusRequester,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 8.dp)
        )
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 420.dp),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp)
        ) {
            val message = when {
                search.searching -> "Searching…"
                search.error == WeatherError.NO_KEY -> "Add your ${provider.displayName} API key first."
                search.error == WeatherError.AUTH -> "${provider.displayName} rejected the API key."
                search.error == WeatherError.NETWORK -> "Couldn't reach ${provider.displayName}. Check your connection."
                search.query.trim().length < 2 -> "Type at least two letters"
                search.results.isEmpty() -> "No places match “${search.query.trim()}”"
                else -> null
            }
            if (message != null) {
                item(key = "message") {
                    Text(message, style = type.body, color = colors.textSecondary, modifier = Modifier.padding(16.dp))
                }
            }
            items(search.results, key = { "${it.latitude},${it.longitude}" }) { city ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(ObShapes.Tile)
                        .pressable(pressedScale = 0.985f) { onIntent(WidgetCustomizeIntent.PickCity(city)) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(city.name, style = type.labelStrong, color = colors.textPrimary)
                    if (city.detail.isNotBlank()) Text(city.detail, style = type.body, color = colors.textSecondary)
                }
            }
        }
    }
}

/** Paste, verify and save the OpenWeather API key. */
@Composable
private fun ApiKeySheet(sheet: ApiKeySheetState, hasSavedKey: Boolean, onIntent: (WidgetCustomizeIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val uriHandler = LocalUriHandler.current
    ObSheet(onDismiss = { onIntent(WidgetCustomizeIntent.CloseApiKeySheet) }, title = "OpenWeather API key") {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Use your own free key from openweathermap.org (My account → API keys). It's stored " +
                    "only on this phone and never included in backups.",
                style = type.body,
                color = colors.textSecondary
            )
            ObTextField(
                value = sheet.draft,
                onValueChange = { onIntent(WidgetCustomizeIntent.SetApiKeyDraft(it)) },
                placeholder = "Paste your API key",
                isError = sheet.check == WeatherError.AUTH,
                supportingText = when (sheet.check) {
                    WeatherError.AUTH -> "OpenWeather rejected this key. New keys can take up to 2 hours to activate — you can save it anyway."
                    WeatherError.NETWORK -> "Couldn't reach OpenWeather to check the key. You can save it anyway."
                    else -> null
                },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onIntent(WidgetCustomizeIntent.SaveApiKey()) }),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = when {
                    sheet.checking -> "Checking…"
                    sheet.check != null -> "Save anyway"
                    else -> "Check & save"
                },
                enabled = sheet.draft.isNotBlank() && !sheet.checking,
                onClick = { onIntent(WidgetCustomizeIntent.SaveApiKey(skipCheck = sheet.check != null)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "Get a free key",
                    onClick = { uriHandler.openUri("https://home.openweathermap.org/api_keys") }
                )
                if (hasSavedKey) {
                    PillButton(text = "Remove key", onClick = { onIntent(WidgetCustomizeIntent.RemoveApiKey) })
                }
            }
        }
    }
}

@Composable
private fun PartAppRow(part: WidgetPart, app: LaunchableApp?, isDefault: Boolean, onClick: () -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.985f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconTile(app?.component, 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(part.displayName, style = type.labelStrong, color = colors.textPrimary)
            Text(
                when {
                    app == null -> "No app found · tap to choose"
                    isDefault -> "Opens ${app.label} (default)"
                    else -> "Opens ${app.label}"
                },
                style = type.body,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.textSecondary)
    }
}

@Composable
private fun AppPickerSheet(
    part: WidgetPart,
    uiState: WidgetCustomizeUiState,
    onIntent: (WidgetCustomizeIntent) -> Unit
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val current = uiState.partApps[part]?.key.takeIf { part !in uiState.defaultParts }
    ObSheet(onDismiss = { onIntent(WidgetCustomizeIntent.ClosePicker) }, title = "${part.displayName} opens…") {
        ObSearchField(
            query = uiState.query,
            onQueryChange = { onIntent(WidgetCustomizeIntent.SetQuery(it)) },
            placeholder = "Search apps",
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 8.dp)
        )
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 520.dp),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp)
        ) {
            if (uiState.query.isBlank()) {
                item(key = "default") {
                    PickerRow(
                        title = "Default",
                        subtitle = when (part) {
                            WidgetPart.DATE -> "Your calendar app"
                            WidgetPart.CLOCK -> "Alarmzy"
                            WidgetPart.WEATHER -> "A weather app, if one is installed"
                        },
                        component = null,
                        selected = current == null,
                        onClick = { onIntent(WidgetCustomizeIntent.AssignApp(null)) }
                    )
                }
            }
            if (uiState.appsLoading) {
                item(key = "loading") {
                    Text("Loading apps…", style = type.body, color = colors.textSecondary, modifier = Modifier.padding(16.dp))
                }
            } else if (uiState.filteredApps.isEmpty()) {
                item(key = "empty") {
                    Text("No apps match “${uiState.query.trim()}”", style = type.body, color = colors.textSecondary, modifier = Modifier.padding(16.dp))
                }
            }
            items(uiState.filteredApps, key = { it.key }) { app ->
                PickerRow(
                    title = app.label,
                    subtitle = null,
                    component = app.component,
                    selected = app.key == current,
                    onClick = { onIntent(WidgetCustomizeIntent.AssignApp(app.key)) }
                )
            }
        }
    }
}

@Composable
private fun PickerRow(title: String, subtitle: String?, component: ComponentName?, selected: Boolean, onClick: () -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ObShapes.Tile)
            .background(if (selected) colors.accentContainerStrong else Color.Transparent)
            .pressable(pressedScale = 0.985f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconTile(component, 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = type.labelStrong, color = if (selected) colors.accentLight else colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = type.body, color = colors.textSecondary)
        }
        if (selected) {
            Icon(painterResource(R.drawable.ic_check_bold), contentDescription = "Selected", tint = colors.accentLight)
        }
    }
}

/** An app's launcher icon, loaded off the main thread; the widgets glyph when [component] is null. */
@Composable
private fun AppIconTile(component: ComponentName?, size: Dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, component, px) {
        value = component?.let {
            withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getActivityIcon(it).toBitmap(px, px).asImageBitmap() }.getOrNull()
            }
        }
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().clip(ObShapes.Tile).background(ObsidianTheme.colors.tile),
                contentAlignment = Alignment.Center
            ) {
                if (component == null) {
                    Icon(painterResource(R.drawable.ic_widgets), contentDescription = null, tint = ObsidianTheme.colors.textSecondary)
                }
            }
        }
    }
}
