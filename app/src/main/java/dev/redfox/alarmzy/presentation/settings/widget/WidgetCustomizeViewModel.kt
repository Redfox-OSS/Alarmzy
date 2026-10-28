package dev.redfox.alarmzy.presentation.settings.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.widget.ClockWidgetProvider
import dev.redfox.alarmzy.widget.ClockWidgetRenderer
import dev.redfox.alarmzy.widget.ClockWidgetStore
import dev.redfox.alarmzy.widget.LaunchableApp
import dev.redfox.alarmzy.widget.OpenWeatherClient
import dev.redfox.alarmzy.widget.WeatherException
import dev.redfox.alarmzy.widget.WeatherKeyStore
import dev.redfox.alarmzy.widget.WeatherSources
import dev.redfox.alarmzy.widget.WidgetApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class WidgetCustomizeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ClockWidgetStore,
    private val sources: WeatherSources,
    private val openWeather: OpenWeatherClient,
    private val keyStore: WeatherKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetCustomizeUiState())
    val uiState: StateFlow<WidgetCustomizeUiState> = _uiState.asStateFlow()

    /** Slider position while dragging, so the preview follows without a DataStore write per frame. */
    private val scaleDraft = MutableStateFlow<Float?>(null)
    private val picker = MutableStateFlow(PickerState())
    private val apps = MutableStateFlow<List<LaunchableApp>?>(null)
    /** Bumped on resume so app resolution re-runs after installs/uninstalls. */
    private val refreshTick = MutableStateFlow(0)

    private data class PickerState(val part: WidgetPart? = null, val query: String = "")
    private val citySearch = MutableStateFlow(CitySearchState())
    private val weatherRefreshing = MutableStateFlow(false)
    private val apiKeySheet = MutableStateFlow(ApiKeySheetState())

    init {
        // App resolution queries PackageManager, so it only re-runs on config changes and resumes,
        // not on every keystroke in the picker search.
        val resolvedApps = combine(store.config, refreshTick) { config, _ -> config }
            .map { config ->
                WidgetPart.entries.mapNotNull { part ->
                    WidgetApps.resolve(context, part, config.appFor(part))?.let { part to it }
                }.toMap() to ClockWidgetRenderer.widgetIds(context).size
            }
            .flowOn(Dispatchers.Default)

        val base = combine(store.config, resolvedApps, scaleDraft) { config, (resolved, placed), draft ->
            Triple(config, resolved to placed, draft)
        }
        val weather = combine(store.weatherStatus, citySearch, weatherRefreshing) { status, search, refreshing ->
            Triple(status, search, refreshing)
        }

        val apiKey = combine(keyStore.key, apiKeySheet) { key, sheet -> key to sheet }

        viewModelScope.launch {
            combine(base, picker, apps, weather, apiKey) { (config, appInfo, draft), pickerState, appList, (status, search, refreshing), (key, sheet) ->
                val (resolved, placed) = appInfo
                WidgetCustomizeUiState(
                    config = if (draft != null) config.copy(scale = draft) else config,
                    partApps = resolved,
                    defaultParts = WidgetPart.entries.filter { config.appFor(it) == null }.toSet(),
                    pickerPart = pickerState.part,
                    apps = appList.orEmpty(),
                    appsLoading = appList == null,
                    query = pickerState.query,
                    placedCount = placed,
                    canPinWidget = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported,
                    weather = status.weather?.takeIf { w -> config.weatherCity?.let(w::isFor) == true },
                    weatherError = status.lastError,
                    weatherRefreshing = refreshing,
                    citySearch = search,
                    apiKey = key,
                    apiKeySheet = sheet
                )
            }.collect { _uiState.value = it }
        }

        // Geocode while typing, debounced; a newer query cancels the one in flight.
        viewModelScope.launch {
            citySearch.map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(400)
                .collectLatest { query ->
                    if (query.length < 2) {
                        citySearch.update { it.copy(results = emptyList(), searching = false, error = null) }
                        return@collectLatest
                    }
                    citySearch.update { it.copy(searching = true, error = null) }
                    // Searched with the selected provider, so OpenWeather users never hit Open-Meteo.
                    val source = sources.forProvider(store.current().weatherProvider)
                    val (results, error) = try {
                        source.searchCities(query) to null
                    } catch (e: WeatherException) {
                        emptyList<WeatherCity>() to e.error
                    }
                    citySearch.update { it.copy(results = results, searching = false, error = error) }
                }
        }
        loadApps()
        viewModelScope.launch { keyStore.load() }
    }

    fun onIntent(intent: WidgetCustomizeIntent) {
        when (intent) {
            is WidgetCustomizeIntent.DragScale -> scaleDraft.value = intent.scale
            WidgetCustomizeIntent.CommitScale -> {
                val scale = scaleDraft.value ?: return
                viewModelScope.launch {
                    store.update { it.copy(scale = scale) }
                    scaleDraft.value = null
                }
            }
            WidgetCustomizeIntent.ResetScale -> save { it.copy(scale = ClockWidgetConfig.DEFAULT_SCALE) }
            is WidgetCustomizeIntent.SetLayout -> save { it.copy(layout = intent.layout) }
            is WidgetCustomizeIntent.SetHorizontalAlign -> save { it.copy(horizontalAlign = intent.align) }
            is WidgetCustomizeIntent.SetVerticalAlign -> save { it.copy(verticalAlign = intent.align) }
            is WidgetCustomizeIntent.SetUse24Hour -> save { it.copy(use24Hour = intent.use24Hour) }
            is WidgetCustomizeIntent.SetBold -> save { it.copy(bold = intent.bold) }
            is WidgetCustomizeIntent.OpenPicker -> picker.value = PickerState(part = intent.part)
            WidgetCustomizeIntent.ClosePicker -> picker.value = PickerState()
            is WidgetCustomizeIntent.SetQuery -> picker.update { it.copy(query = intent.query) }
            is WidgetCustomizeIntent.AssignApp -> {
                val part = picker.value.part ?: return
                picker.value = PickerState()
                save { it.withApp(part, intent.component) }
            }
            WidgetCustomizeIntent.AddToHomeScreen -> {
                AppWidgetManager.getInstance(context)
                    .requestPinAppWidget(ComponentName(context, ClockWidgetProvider::class.java), null, null)
            }
            WidgetCustomizeIntent.Refresh -> {
                refreshTick.update { it + 1 }
                loadApps()
                fetchWeather(force = false)
            }
            is WidgetCustomizeIntent.SetShowWeather -> {
                if (intent.show && _uiState.value.config.weatherCity == null) {
                    // Nothing to show without a place; turned on once one is picked.
                    citySearch.value = CitySearchState(open = true)
                } else {
                    viewModelScope.launch {
                        store.update { it.copy(showWeather = intent.show) }
                        if (intent.show) fetchWeather(force = false)
                    }
                }
            }
            is WidgetCustomizeIntent.SetFahrenheit -> save { it.copy(useFahrenheit = intent.fahrenheit) }
            WidgetCustomizeIntent.OpenCitySearch -> citySearch.value = CitySearchState(open = true)
            WidgetCustomizeIntent.CloseCitySearch -> citySearch.value = CitySearchState()
            is WidgetCustomizeIntent.SetCityQuery -> citySearch.update { it.copy(query = intent.query) }
            is WidgetCustomizeIntent.PickCity -> {
                citySearch.value = CitySearchState()
                viewModelScope.launch {
                    store.update { it.copy(weatherCity = intent.city, showWeather = true) }
                    fetchWeather(force = true)
                }
            }
            WidgetCustomizeIntent.RefreshWeather -> fetchWeather(force = true)
            // A shorter interval may already make the reading stale: check right away.
            is WidgetCustomizeIntent.SetWeatherInterval -> viewModelScope.launch {
                store.update { it.copy(weatherInterval = intent.interval) }
                fetchWeather(force = false)
            }
            is WidgetCustomizeIntent.SetProvider -> viewModelScope.launch {
                store.update { it.copy(weatherProvider = intent.provider) }
                if (intent.provider.needsApiKey && keyStore.current() == null) {
                    apiKeySheet.value = ApiKeySheetState(open = true)
                } else {
                    fetchWeather(force = true)
                }
            }
            WidgetCustomizeIntent.OpenApiKeySheet ->
                apiKeySheet.value = ApiKeySheetState(open = true, draft = keyStore.key.value.orEmpty())
            WidgetCustomizeIntent.CloseApiKeySheet -> apiKeySheet.value = ApiKeySheetState()
            is WidgetCustomizeIntent.SetApiKeyDraft ->
                apiKeySheet.update { it.copy(draft = intent.key.trim(), check = null) }
            is WidgetCustomizeIntent.SaveApiKey -> saveApiKey(intent.skipCheck)
            WidgetCustomizeIntent.RemoveApiKey -> viewModelScope.launch {
                keyStore.save(null)
                apiKeySheet.value = ApiKeySheetState()
            }
        }
    }

    /**
     * Checks the key with OpenWeather before saving. A rejected or unchecked key can still be
     * saved on a second tap ([skipCheck]): brand-new keys take up to a couple of hours to activate.
     */
    private fun saveApiKey(skipCheck: Boolean) {
        val draft = apiKeySheet.value.draft.trim()
        if (draft.isEmpty() || apiKeySheet.value.checking) return
        viewModelScope.launch {
            if (!skipCheck) {
                apiKeySheet.update { it.copy(checking = true, check = null) }
                val error = try {
                    openWeather.verify(draft)
                    null
                } catch (e: WeatherException) {
                    e.error
                }
                if (error != null) {
                    apiKeySheet.update { it.copy(checking = false, check = error) }
                    return@launch
                }
            }
            keyStore.save(draft)
            apiKeySheet.value = ApiKeySheetState()
            fetchWeather(force = true)
        }
    }

    private fun fetchWeather(force: Boolean) {
        if (weatherRefreshing.value) return
        viewModelScope.launch {
            weatherRefreshing.value = true
            try {
                store.refreshWeather(force)
            } finally {
                weatherRefreshing.value = false
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            apps.value = withContext(Dispatchers.Default) { WidgetApps.launcherApps(context) }
        }
    }

    private fun save(transform: (ClockWidgetConfig) -> ClockWidgetConfig) {
        viewModelScope.launch { store.update(transform) }
    }
}
