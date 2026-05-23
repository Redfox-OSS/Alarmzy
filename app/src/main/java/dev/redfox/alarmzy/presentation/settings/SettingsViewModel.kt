package dev.redfox.alarmzy.presentation.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { prefs ->
                SettingsUiState(
                    themeMode = ThemeMode.valueOf(
                        prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
                    ),
                    defaultSnoozeDuration = prefs[KEY_DEFAULT_SNOOZE] ?: 5,
                    defaultVibration = prefs[KEY_DEFAULT_VIBRATION] ?: true,
                    defaultGradualVolume = prefs[KEY_DEFAULT_GRADUAL_VOLUME] ?: false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetThemeMode -> save { prefs ->
                prefs[KEY_THEME_MODE] = intent.mode.name
            }
            is SettingsIntent.SetDefaultSnoozeDuration -> save { prefs ->
                prefs[KEY_DEFAULT_SNOOZE] = intent.minutes
            }
            is SettingsIntent.SetDefaultVibration -> save { prefs ->
                prefs[KEY_DEFAULT_VIBRATION] = intent.enabled
            }
            is SettingsIntent.SetDefaultGradualVolume -> save { prefs ->
                prefs[KEY_DEFAULT_GRADUAL_VOLUME] = intent.enabled
            }
        }
    }

    private fun save(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch {
            context.dataStore.edit { block(it) }
        }
    }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DEFAULT_SNOOZE = intPreferencesKey("default_snooze")
        val KEY_DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
        val KEY_DEFAULT_GRADUAL_VOLUME = booleanPreferencesKey("default_gradual_volume")
    }
}
