package dev.redfox.alarmzy.presentation.alarms

sealed interface AlarmsIntent {
    data class ToggleAlarm(val alarmId: Long, val enabled: Boolean) : AlarmsIntent
    data class EnterSelection(val alarmId: Long) : AlarmsIntent
    data class ToggleSelection(val alarmId: Long) : AlarmsIntent
    data object SelectAll : AlarmsIntent
    data object ClearSelection : AlarmsIntent
    data object DeleteSelected : AlarmsIntent
}

sealed interface AlarmsSideEffect {
    data class NavigateToEdit(val alarmId: Long?) : AlarmsSideEffect
}
