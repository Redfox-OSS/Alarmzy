package dev.redfox.alarmzy.presentation.ringtone

sealed interface RingtonePickerIntent {
    data class SelectRingtone(val uri: String, val name: String) : RingtonePickerIntent
    data class PreviewRingtone(val uri: String) : RingtonePickerIntent
    data object StopPreview : RingtonePickerIntent
    data object Confirm : RingtonePickerIntent
}

sealed interface RingtonePickerSideEffect {
    data class Confirmed(val uri: String?, val name: String) : RingtonePickerSideEffect
}
