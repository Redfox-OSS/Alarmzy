package dev.redfox.alarmzy.presentation.groups

sealed interface GroupsIntent {
    data class ToggleGroup(val groupId: Long, val enabled: Boolean) : GroupsIntent
    data class ToggleExpand(val groupId: Long) : GroupsIntent
    data class ToggleAlarmInGroup(val alarmId: Long, val enabled: Boolean) : GroupsIntent
    data class EnterSelection(val groupId: Long) : GroupsIntent
    /** Enter selection mode with nothing selected yet (from the overflow menu). */
    data object StartSelection : GroupsIntent
    data class ToggleSelection(val groupId: Long) : GroupsIntent
    data object SelectAll : GroupsIntent
    data object ClearSelection : GroupsIntent
    data object DeleteSelected : GroupsIntent
}
