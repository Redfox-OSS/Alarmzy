package dev.redfox.alarmzy.presentation.settings.widget

import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.domain.model.WidgetWeather
import dev.redfox.alarmzy.widget.LaunchableApp
import dev.redfox.alarmzy.widget.WeatherError

data class WidgetCustomizeUiState(
    val config: ClockWidgetConfig = ClockWidgetConfig(),
    /** The app each part opens right now (user pick or default); missing = nothing found. */
    val partApps: Map<WidgetPart, LaunchableApp> = emptyMap(),
    /** Which parts still use their default app. */
    val defaultParts: Set<WidgetPart> = WidgetPart.entries.toSet(),
    /** Part whose app picker sheet is open. */
    val pickerPart: WidgetPart? = null,
    val apps: List<LaunchableApp> = emptyList(),
    val appsLoading: Boolean = true,
    val query: String = "",
    /** Widgets currently on the home screen. */
    val placedCount: Int = 0,
    val canPinWidget: Boolean = false,
    /** Latest reading for the configured city, or null until one arrives. */
    val weather: WidgetWeather? = null,
    /** Why the latest weather update failed; null when it worked. */
    val weatherError: WeatherError? = null,
    val weatherRefreshing: Boolean = false,
    val citySearch: CitySearchState = CitySearchState(),
    /** Saved OpenWeather key (shown masked). */
    val apiKey: String? = null,
    val apiKeySheet: ApiKeySheetState = ApiKeySheetState()
) {
    val filteredApps: List<LaunchableApp>
        get() = if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
}

/** The weather city search sheet (the selected provider's geocoding). */
data class CitySearchState(
    val open: Boolean = false,
    val query: String = "",
    val results: List<WeatherCity> = emptyList(),
    val searching: Boolean = false,
    val error: WeatherError? = null
)

/** The OpenWeather API key sheet. [check] is why the last verification failed. */
data class ApiKeySheetState(
    val open: Boolean = false,
    val draft: String = "",
    val checking: Boolean = false,
    val check: WeatherError? = null
)
