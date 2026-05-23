package dev.redfox.alarmzy.presentation.groups

import dev.redfox.alarmzy.domain.model.AlarmGroup

sealed interface GroupsIntent {
    data class ToggleGroup(val groupId: Long, val enabled: Boolean) : GroupsIntent
    data class ToggleExpand(val groupId: Long) : GroupsIntent
    data class DeleteGroup(val group: AlarmGroup) : GroupsIntent
    data class ToggleAlarmInGroup(val alarmId: Long, val enabled: Boolean) : GroupsIntent
}
