package dev.redfox.alarmzy.presentation.groups

import dev.redfox.alarmzy.domain.model.AlarmGroup

data class GroupsUiState(
    val groups: List<AlarmGroup> = emptyList(),
    val expandedGroupIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)
