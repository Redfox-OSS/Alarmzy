package dev.redfox.alarmzy.presentation.groupedit

import dev.redfox.alarmzy.domain.model.Alarm

data class GroupEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val isEnabled: Boolean = true,
    val allAlarms: List<Alarm> = emptyList(),
    val selectedAlarmIds: Set<Long> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
)
