package dev.redfox.alarmzy.presentation.settings.customize

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AlarmScreenCustomizeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmScreenCustomizeUiState())
    val uiState: StateFlow<AlarmScreenCustomizeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                AlarmScreenCustomizeUiState(
                    backgroundImagePath = prefs[KEY_BG_IMAGE_PATH]?.takeIf { File(it).exists() },
                    buttonLayout = runCatching {
                        AlarmButtonLayout.valueOf(prefs[KEY_BUTTON_LAYOUT] ?: AlarmButtonLayout.BOTH.name)
                    }.getOrDefault(AlarmButtonLayout.BOTH),
                    clockPosition = runCatching {
                        AlarmClockPosition.valueOf(prefs[KEY_CLOCK_POSITION] ?: AlarmClockPosition.MIDDLE.name)
                    }.getOrDefault(AlarmClockPosition.MIDDLE),
                    clockTextSizeSp = prefs[KEY_CLOCK_TEXT_SIZE] ?: 72
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: AlarmScreenCustomizeIntent) {
        when (intent) {
            is AlarmScreenCustomizeIntent.PickedBackgroundImage -> viewModelScope.launch {
                copyImageToInternalStorage(intent.uri)
            }
            is AlarmScreenCustomizeIntent.RemoveBackgroundImage -> viewModelScope.launch {
                _uiState.value.backgroundImagePath?.let { File(it).delete() }
                dataStore.edit { prefs -> prefs.remove(KEY_BG_IMAGE_PATH) }
            }
            is AlarmScreenCustomizeIntent.SetButtonLayout -> save { prefs ->
                prefs[KEY_BUTTON_LAYOUT] = intent.layout.name
            }
            is AlarmScreenCustomizeIntent.SetClockPosition -> save { prefs ->
                prefs[KEY_CLOCK_POSITION] = intent.position.name
            }
            is AlarmScreenCustomizeIntent.SetClockTextSize -> save { prefs ->
                prefs[KEY_CLOCK_TEXT_SIZE] = intent.sizeSp
            }
        }
    }

    /** Copies the picked photo into app-private storage — Photo Picker grants aren't persistable. */
    private suspend fun copyImageToInternalStorage(uri: Uri) {
        val previousPath = _uiState.value.backgroundImagePath
        val newFile = File(context.filesDir, "alarm_background_${System.currentTimeMillis()}.jpg")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                newFile.outputStream().use { output -> input.copyTo(output) }
            }
        }.getOrNull() != null

        if (copied) {
            previousPath?.let { File(it).delete() }
            dataStore.edit { prefs -> prefs[KEY_BG_IMAGE_PATH] = newFile.absolutePath }
        } else {
            newFile.delete()
        }
    }

    private fun save(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch {
            dataStore.edit { block(it) }
        }
    }

    companion object {
        val KEY_BG_IMAGE_PATH = stringPreferencesKey("alarm_bg_image_path")
        val KEY_BUTTON_LAYOUT = stringPreferencesKey("alarm_button_layout")
        val KEY_CLOCK_POSITION = stringPreferencesKey("alarm_clock_position")
        val KEY_CLOCK_TEXT_SIZE = intPreferencesKey("alarm_clock_text_size_sp")
    }
}
