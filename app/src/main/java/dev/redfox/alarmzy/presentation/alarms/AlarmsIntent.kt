package dev.redfox.alarmzy.presentation.alarms

sealed interface AlarmsIntent {
    data class ToggleAlarm(val alarmId: Long, val enabled: Boolean) : AlarmsIntent
    data class EnterSelection(val alarmId: Long) : AlarmsIntent
    /** Enter selection mode with nothing selected yet (from the overflow menu). */
    data object StartSelection : AlarmsIntent
    data class ToggleSelection(val alarmId: Long) : AlarmsIntent
    data object SelectAll : AlarmsIntent
    data object ClearSelection : AlarmsIntent
    data object DeleteSelected : AlarmsIntent
    data object MarkLiveAlertsPromptAsked : AlarmsIntent
    /** Permissions changed: re-post running timer/stopwatch Live Updates. */
    data object RestoreLiveAlerts : AlarmsIntent
}

sealed interface AlarmsSideEffect {
    data class NavigateToEdit(val alarmId: Long?) : AlarmsSideEffect
}
