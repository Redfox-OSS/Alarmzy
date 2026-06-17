package dev.redfox.alarmzy.presentation.alarmedit

import dev.redfox.alarmzy.domain.model.RepeatMode

sealed interface AlarmEditIntent {
    data class SetTime(val hour: Int, val minute: Int) : AlarmEditIntent
    data class SetLabel(val label: String) : AlarmEditIntent
    data class SetRepeatMode(val mode: RepeatMode) : AlarmEditIntent
    data class SetRingtone(val uri: String?, val name: String) : AlarmEditIntent
    data class SetVibration(val enabled: Boolean) : AlarmEditIntent
    data class SetSnoozeDuration(val minutes: Int) : AlarmEditIntent
    data class SetGradualVolume(val enabled: Boolean) : AlarmEditIntent
    data class SetGroup(val groupId: Long?) : AlarmEditIntent
    data class ToggleSeriesMode(val enabled: Boolean) : AlarmEditIntent
    data class SetSeriesEnd(val hour: Int, val minute: Int) : AlarmEditIntent
    data class SetSeriesInterval(val minutes: Int) : AlarmEditIntent
    data object Save : AlarmEditIntent
    data object ConfirmSave : AlarmEditIntent
    data object DismissDuplicateDialog : AlarmEditIntent
    data object Delete : AlarmEditIntent
}

sealed interface AlarmEditSideEffect {
    data object Saved : AlarmEditSideEffect
    data object Deleted : AlarmEditSideEffect
}
