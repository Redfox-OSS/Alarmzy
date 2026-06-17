package dev.redfox.alarmzy.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                SettingsUiState(
                    themeMode = runCatching {
                        ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name)
                    }.getOrDefault(ThemeMode.SYSTEM),
                    accentColor = runCatching {
                        AccentColor.valueOf(prefs[KEY_ACCENT_COLOR] ?: AccentColor.DYNAMIC.name)
                    }.getOrDefault(AccentColor.DYNAMIC),
                    defaultRingtoneUri = prefs[KEY_DEFAULT_RINGTONE_URI],
                    defaultRingtoneName = prefs[KEY_DEFAULT_RINGTONE_NAME] ?: "Default",
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
            is SettingsIntent.SetAccentColor -> save { prefs ->
                prefs[KEY_ACCENT_COLOR] = intent.accent.name
            }
            is SettingsIntent.SetDefaultRingtone -> save { prefs ->
                if (intent.uri != null) {
                    prefs[KEY_DEFAULT_RINGTONE_URI] = intent.uri
                } else {
                    prefs.remove(KEY_DEFAULT_RINGTONE_URI)
                }
                prefs[KEY_DEFAULT_RINGTONE_NAME] = intent.name
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
            dataStore.edit { block(it) }
        }
    }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_DEFAULT_RINGTONE_URI = stringPreferencesKey("default_ringtone_uri")
        val KEY_DEFAULT_RINGTONE_NAME = stringPreferencesKey("default_ringtone_name")
        val KEY_DEFAULT_SNOOZE = intPreferencesKey("default_snooze")
        val KEY_DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
        val KEY_DEFAULT_GRADUAL_VOLUME = booleanPreferencesKey("default_gradual_volume")
    }
}
