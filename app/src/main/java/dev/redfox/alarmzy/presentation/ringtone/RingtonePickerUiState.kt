package dev.redfox.alarmzy.presentation.ringtone

data class RingtonePickerUiState(
    val ringtones: List<RingtoneItem> = emptyList(),
    val selectedUri: String? = null,
    val isPlaying: Boolean = false,
    val playingUri: String? = null
)

data class RingtoneItem(
    val uri: String,
    val name: String
)
