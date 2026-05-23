package dev.redfox.alarmzy.presentation.alarmedit

import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode

data class AlarmEditUiState(
    val isNew: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0,
    val label: String = "",
    val repeatMode: RepeatMode = RepeatMode.OneTime,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "Default",
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val gradualVolumeIncrease: Boolean = false,
    val selectedGroupId: Long? = null,
    val availableGroups: List<AlarmGroup> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
)
