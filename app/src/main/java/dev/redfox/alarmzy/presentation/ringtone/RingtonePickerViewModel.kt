package dev.redfox.alarmzy.presentation.ringtone

import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class RingtonePickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RingtonePickerUiState())
    val uiState: StateFlow<RingtonePickerUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<RingtonePickerSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var currentRingtone: android.media.Ringtone? = null

    init {
        loadRingtones()
    }

    private fun loadRingtones() {
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
        val cursor = ringtoneManager.cursor

        val ringtones = mutableListOf<RingtoneItem>()
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
            ringtones.add(RingtoneItem(uri = uri, name = title))
        }

        _uiState.value = _uiState.value.copy(ringtones = ringtones)
    }

    fun onIntent(intent: RingtonePickerIntent) {
        when (intent) {
            is RingtonePickerIntent.SelectRingtone -> {
                _uiState.value = _uiState.value.copy(
                    selectedUri = intent.uri
                )
            }
            is RingtonePickerIntent.PreviewRingtone -> {
                stopCurrentPreview()
                val uri = android.net.Uri.parse(intent.uri)
                currentRingtone = RingtoneManager.getRingtone(context, uri)?.also {
                    it.play()
                }
                _uiState.value = _uiState.value.copy(
                    isPlaying = true,
                    playingUri = intent.uri
                )
            }
            is RingtonePickerIntent.StopPreview -> {
                stopCurrentPreview()
                _uiState.value = _uiState.value.copy(
                    isPlaying = false,
                    playingUri = null
                )
            }
            is RingtonePickerIntent.Confirm -> {
                stopCurrentPreview()
                val state = _uiState.value
                val selectedItem = state.ringtones.find { it.uri == state.selectedUri }
                val channel = _sideEffect
                channel.trySend(
                    RingtonePickerSideEffect.Confirmed(
                        uri = state.selectedUri,
                        name = selectedItem?.name ?: "Default"
                    )
                )
            }
        }
    }

    private fun stopCurrentPreview() {
        currentRingtone?.stop()
        currentRingtone = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentPreview()
    }
}
