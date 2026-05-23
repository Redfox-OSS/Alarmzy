package dev.redfox.alarmzy.presentation.alarms

import dev.redfox.alarmzy.domain.model.Alarm

sealed interface AlarmsIntent {
    data class ToggleAlarm(val alarmId: Long, val enabled: Boolean) : AlarmsIntent
    data class DeleteAlarm(val alarm: Alarm) : AlarmsIntent
}

sealed interface AlarmsSideEffect {
    data class NavigateToEdit(val alarmId: Long?) : AlarmsSideEffect
}
