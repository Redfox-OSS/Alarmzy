package dev.redfox.alarmzy.presentation.ringtone

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
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
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The ringtone currently configured by the caller, so it can be pre-selected and
    // (if it's a custom file not in the system list) shown again on re-entry.
    private val initialUri: String? =
        savedStateHandle.get<String>("uri")?.takeIf { it.isNotBlank() }
    private val initialName: String? =
        savedStateHandle.get<String>("name")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(RingtonePickerUiState())
    val uiState: StateFlow<RingtonePickerUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<RingtonePickerSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var previewPlayer: MediaPlayer? = null

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

        // Re-add the currently selected custom sound if it isn't a system alarm tone,
        // so the user sees and can keep their previous choice.
        val list = if (initialUri != null && ringtones.none { it.uri == initialUri }) {
            val name = initialName ?: queryDisplayName(initialUri)
            listOf(RingtoneItem(uri = initialUri, name = name, isCustom = true)) + ringtones
        } else {
            ringtones
        }

        _uiState.value = _uiState.value.copy(
            ringtones = list,
            selectedUri = initialUri
        )
    }

    fun onIntent(intent: RingtonePickerIntent) {
        when (intent) {
            is RingtonePickerIntent.SelectRingtone -> {
                _uiState.value = _uiState.value.copy(selectedUri = intent.uri)
            }
            is RingtonePickerIntent.AddCustomRingtone -> {
                val name = queryDisplayName(intent.uri)
                val item = RingtoneItem(uri = intent.uri, name = name, isCustom = true)
                val existing = _uiState.value.ringtones
                val newList = if (existing.any { it.uri == intent.uri }) existing
                else listOf(item) + existing
                _uiState.value = _uiState.value.copy(
                    ringtones = newList,
                    selectedUri = intent.uri
                )
            }
            is RingtonePickerIntent.PreviewRingtone -> {
                startPreview(intent.uri)
            }
            is RingtonePickerIntent.StopPreview -> {
                stopPreview()
                _uiState.value = _uiState.value.copy(isPlaying = false, playingUri = null)
            }
            is RingtonePickerIntent.Confirm -> {
                stopPreview()
                val state = _uiState.value
                val selectedItem = state.ringtones.find { it.uri == state.selectedUri }
                _sideEffect.trySend(
                    RingtonePickerSideEffect.Confirmed(
                        uri = state.selectedUri,
                        name = selectedItem?.name ?: "Default"
                    )
                )
            }
        }
    }

    private fun startPreview(uriString: String) {
        stopPreview()
        val started = runCatching {
            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, Uri.parse(uriString))
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }.isSuccess
        _uiState.value = _uiState.value.copy(
            isPlaying = started,
            playingUri = if (started) uriString else null
        )
    }

    private fun stopPreview() {
        previewPlayer?.apply {
            runCatching { if (isPlaying) stop() }
            release()
        }
        previewPlayer = null
    }

    /** Resolves a human-readable name for a picked audio file. */
    private fun queryDisplayName(uriString: String): String {
        val uri = Uri.parse(uriString)
        val name = runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Custom sound"
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
