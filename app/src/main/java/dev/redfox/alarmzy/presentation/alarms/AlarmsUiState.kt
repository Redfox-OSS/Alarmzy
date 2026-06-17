package dev.redfox.alarmzy.presentation.alarms

import dev.redfox.alarmzy.domain.model.Alarm

data class AlarmsUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarm: Alarm? = null,
    val isLoading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedIds.size
}
