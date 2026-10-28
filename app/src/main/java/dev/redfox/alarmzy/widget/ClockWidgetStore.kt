package dev.redfox.alarmzy.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WeatherInterval
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.domain.model.WidgetHorizontalAlign
import dev.redfox.alarmzy.domain.model.WidgetLayout
import dev.redfox.alarmzy.domain.model.WidgetVerticalAlign
import dev.redfox.alarmzy.domain.model.WidgetWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Where the widget's weather stands, for the customization page. */
data class WeatherStatus(
    val weather: WidgetWeather? = null,
    val lastAttemptMillis: Long = 0L,
    /** Why the latest attempt failed; null when it succeeded. */
    val lastError: WeatherError? = null
)

/**
 * Persists the clock widget's [ClockWidgetConfig] and cached weather in the settings DataStore.
 * Every [update] re-renders the placed widgets right away — don't push widget updates from
 * elsewhere.
 */
@Singleton
class ClockWidgetStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val sources: WeatherSources
) {
    val config: Flow<ClockWidgetConfig> = dataStore.data.map { it.toConfig() }.distinctUntilChanged()

    val weatherStatus: Flow<WeatherStatus> = dataStore.data.map { prefs ->
        WeatherStatus(
            weather = prefs.toWeather(),
            lastAttemptMillis = prefs[KEY_WEATHER_ATTEMPT_AT] ?: 0L,
            lastError = prefs[KEY_WEATHER_ERROR]?.let { name -> WeatherError.entries.firstOrNull { it.name == name } }
        )
    }.distinctUntilChanged()

    suspend fun current(): ClockWidgetConfig = config.first()

    suspend fun update(transform: (ClockWidgetConfig) -> ClockWidgetConfig) {
        refreshWidgets(dataStore.edit { it.write(transform(it.toConfig())) })
    }

    /** Re-renders the placed widgets from [prefs] (a fresh DataStore read when null). */
    suspend fun refreshWidgets(prefs: Preferences? = null) = withContext(Dispatchers.Default) {
        val snapshot = prefs ?: dataStore.data.first()
        val config = snapshot.toConfig()
        val weather = snapshot.toWeather()?.takeIf { w -> config.weatherCity?.let(w::isFor) == true }
        ClockWidgetRenderer.updateAll(context, config, weather)
    }

    /**
     * Fetches current weather when it's enabled and the cache is stale (or for another city), at
     * most once per [RETRY_AFTER_MILLIS] after a failure. [force] skips the staleness checks.
     * Returns false when a fetch was attempted and failed.
     */
    suspend fun refreshWeather(force: Boolean = false): Boolean {
        val prefs = dataStore.data.first()
        val config = prefs.toConfig()
        val city = config.weatherCity ?: return true
        if (!config.showWeather) return true
        val cached = prefs.toWeather()?.takeIf { it.isFor(city) }
        val now = System.currentTimeMillis()
        if (!force) {
            if (cached != null && now - cached.fetchedAtMillis < config.weatherInterval.millis) return true
            if (now - (prefs[KEY_WEATHER_ATTEMPT_AT] ?: 0L) < RETRY_AFTER_MILLIS) return true
        }
        dataStore.edit { it[KEY_WEATHER_ATTEMPT_AT] = now }
        val error = try {
            // Bounded: this runs inside the widget tick's broadcast window.
            val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
                sources.forProvider(config.weatherProvider).currentWeather(city)
            } ?: throw WeatherException(WeatherError.NETWORK)
            dataStore.edit { it.writeWeather(fetched) }
            null
        } catch (e: WeatherException) {
            e.error
        }
        // A failure only records why: the previous reading stays on screen, whatever its age.
        val saved = dataStore.edit { p ->
            if (error == null) p.remove(KEY_WEATHER_ERROR) else p[KEY_WEATHER_ERROR] = error.name
        }
        refreshWidgets(saved)
        return error == null
    }

    private fun Preferences.toConfig() = ClockWidgetConfig(
        scale = this[KEY_SCALE] ?: ClockWidgetConfig.DEFAULT_SCALE,
        layout = this[KEY_LAYOUT]
            ?.let { name -> WidgetLayout.entries.firstOrNull { it.name == name } }
            ?: WidgetLayout.HORIZONTAL,
        horizontalAlign = this[KEY_H_ALIGN]
            ?.let { name -> WidgetHorizontalAlign.entries.firstOrNull { it.name == name } }
            ?: WidgetHorizontalAlign.CENTER,
        verticalAlign = this[KEY_V_ALIGN]
            ?.let { name -> WidgetVerticalAlign.entries.firstOrNull { it.name == name } }
            ?: WidgetVerticalAlign.CENTER,
        use24Hour = this[KEY_24_HOUR] ?: true,
        bold = this[KEY_BOLD] ?: false,
        showWeather = this[KEY_SHOW_WEATHER] ?: false,
        weatherCity = toCity(),
        useFahrenheit = this[KEY_FAHRENHEIT] ?: false,
        weatherInterval = this[KEY_INTERVAL]
            ?.let { name -> WeatherInterval.entries.firstOrNull { it.name == name } }
            ?: WeatherInterval.MIN_30,
        weatherProvider = this[KEY_PROVIDER]
            ?.let { name -> WeatherProvider.entries.firstOrNull { it.name == name } }
            ?: WeatherProvider.OPEN_METEO,
        dateApp = this[KEY_APP_DATE],
        clockApp = this[KEY_APP_CLOCK],
        weatherApp = this[KEY_APP_WEATHER]
    )

    private fun Preferences.toCity(): WeatherCity? {
        val name = this[KEY_CITY_NAME] ?: return null
        return WeatherCity(
            name = name,
            detail = this[KEY_CITY_DETAIL].orEmpty(),
            latitude = this[KEY_CITY_LAT] ?: return null,
            longitude = this[KEY_CITY_LON] ?: return null
        )
    }

    private fun Preferences.toWeather(): WidgetWeather? {
        val temperature = this[KEY_WEATHER_TEMP_C] ?: return null
        return WidgetWeather(
            temperatureC = temperature,
            // Readings cached before providers were recorded all came from Open-Meteo / WMO.
            source = this[KEY_WEATHER_SOURCE]
                ?.let { name -> WeatherProvider.entries.firstOrNull { it.name == name } }
                ?: WeatherProvider.OPEN_METEO,
            code = this[KEY_WEATHER_CODE] ?: return null,
            windKmh = this[KEY_WEATHER_WIND_KMH] ?: 0.0,
            isDay = this[KEY_WEATHER_IS_DAY] ?: true,
            fetchedAtMillis = this[KEY_WEATHER_FETCHED_AT] ?: 0L,
            latitude = this[KEY_WEATHER_LAT] ?: return null,
            longitude = this[KEY_WEATHER_LON] ?: return null
        )
    }

    private fun MutablePreferences.write(config: ClockWidgetConfig) {
        this[KEY_SCALE] = config.scale
        this[KEY_LAYOUT] = config.layout.name
        this[KEY_H_ALIGN] = config.horizontalAlign.name
        this[KEY_V_ALIGN] = config.verticalAlign.name
        this[KEY_24_HOUR] = config.use24Hour
        this[KEY_BOLD] = config.bold
        this[KEY_SHOW_WEATHER] = config.showWeather
        this[KEY_FAHRENHEIT] = config.useFahrenheit
        this[KEY_PROVIDER] = config.weatherProvider.name
        this[KEY_INTERVAL] = config.weatherInterval.name
        val city = config.weatherCity
        if (city == null) {
            listOf(KEY_CITY_NAME, KEY_CITY_DETAIL).forEach { remove(it) }
            listOf(KEY_CITY_LAT, KEY_CITY_LON).forEach { remove(it) }
        } else {
            this[KEY_CITY_NAME] = city.name
            this[KEY_CITY_DETAIL] = city.detail
            this[KEY_CITY_LAT] = city.latitude
            this[KEY_CITY_LON] = city.longitude
        }
        setOrRemove(KEY_APP_DATE, config.dateApp)
        setOrRemove(KEY_APP_CLOCK, config.clockApp)
        setOrRemove(KEY_APP_WEATHER, config.weatherApp)
    }

    private fun MutablePreferences.writeWeather(weather: WidgetWeather) {
        this[KEY_WEATHER_TEMP_C] = weather.temperatureC
        this[KEY_WEATHER_SOURCE] = weather.source.name
        this[KEY_WEATHER_CODE] = weather.code
        this[KEY_WEATHER_WIND_KMH] = weather.windKmh
        this[KEY_WEATHER_IS_DAY] = weather.isDay
        this[KEY_WEATHER_FETCHED_AT] = weather.fetchedAtMillis
        this[KEY_WEATHER_LAT] = weather.latitude
        this[KEY_WEATHER_LON] = weather.longitude
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    companion object {
        const val RETRY_AFTER_MILLIS = 5 * 60_000L
        private const val FETCH_TIMEOUT_MILLIS = 8_000L

        private val KEY_SCALE = floatPreferencesKey("widget_scale")
        private val KEY_LAYOUT = stringPreferencesKey("widget_layout")
        private val KEY_H_ALIGN = stringPreferencesKey("widget_h_align")
        private val KEY_V_ALIGN = stringPreferencesKey("widget_v_align")
        private val KEY_24_HOUR = booleanPreferencesKey("widget_24_hour")
        private val KEY_BOLD = booleanPreferencesKey("widget_bold")
        private val KEY_SHOW_WEATHER = booleanPreferencesKey("widget_show_weather")
        private val KEY_FAHRENHEIT = booleanPreferencesKey("widget_fahrenheit")
        private val KEY_PROVIDER = stringPreferencesKey("widget_weather_provider")
        private val KEY_INTERVAL = stringPreferencesKey("widget_weather_interval")
        private val KEY_CITY_NAME = stringPreferencesKey("widget_city_name")
        private val KEY_CITY_DETAIL = stringPreferencesKey("widget_city_detail")
        private val KEY_CITY_LAT = doublePreferencesKey("widget_city_lat")
        private val KEY_CITY_LON = doublePreferencesKey("widget_city_lon")
        private val KEY_APP_DATE = stringPreferencesKey("widget_app_date")
        private val KEY_APP_CLOCK = stringPreferencesKey("widget_app_clock")
        private val KEY_APP_WEATHER = stringPreferencesKey("widget_app_weather")

        private val KEY_WEATHER_TEMP_C = doublePreferencesKey("widget_weather_temp_c")
        private val KEY_WEATHER_SOURCE = stringPreferencesKey("widget_weather_source")
        private val KEY_WEATHER_CODE = intPreferencesKey("widget_weather_code")
        private val KEY_WEATHER_WIND_KMH = doublePreferencesKey("widget_weather_wind_kmh")
        private val KEY_WEATHER_IS_DAY = booleanPreferencesKey("widget_weather_is_day")
        private val KEY_WEATHER_FETCHED_AT = longPreferencesKey("widget_weather_fetched_at")
        private val KEY_WEATHER_LAT = doublePreferencesKey("widget_weather_lat")
        private val KEY_WEATHER_LON = doublePreferencesKey("widget_weather_lon")
        private val KEY_WEATHER_ATTEMPT_AT = longPreferencesKey("widget_weather_attempt_at")
        private val KEY_WEATHER_ERROR = stringPreferencesKey("widget_weather_error")
    }
}
