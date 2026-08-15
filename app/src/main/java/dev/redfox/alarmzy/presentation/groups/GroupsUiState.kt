package dev.redfox.alarmzy.presentation.groups

import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.FreemiumLimits

data class GroupsUiState(
    val groups: List<AlarmGroup> = emptyList(),
    val expandedGroupIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isPro: Boolean = false
) {
    val selectedCount: Int get() = selectedIds.size
    val canCreateGroup: Boolean get() = isPro || groups.size < FreemiumLimits.FREE_GROUP_LIMIT
}
