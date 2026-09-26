package dev.redfox.alarmzy.presentation.alarms

import dev.redfox.alarmzy.domain.model.Alarm

data class AlarmsUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarm: Alarm? = null,
    val isLoading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    /** Whether the one-time Live Alerts prompt was already shown (defaults true until loaded). */
    val liveAlertsPromptAsked: Boolean = true
) {
    val selectedCount: Int get() = selectedIds.size
}
