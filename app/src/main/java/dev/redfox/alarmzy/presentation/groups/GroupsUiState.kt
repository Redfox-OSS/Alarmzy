package dev.redfox.alarmzy.presentation.groups

import dev.redfox.alarmzy.domain.model.AlarmGroup

data class GroupsUiState(
    val groups: List<AlarmGroup> = emptyList(),
    val expandedGroupIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int get() = selectedIds.size
}
