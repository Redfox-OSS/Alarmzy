package dev.redfox.alarmzy.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private data class Selection(
        val active: Boolean = false,
        val ids: Set<Long> = emptySet()
    )

    private val _expandedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selection = MutableStateFlow(Selection())

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                groupRepository.getAllGroupsWithAlarms(),
                _expandedGroupIds,
                _selection
            ) { groups, expanded, selection ->
                val validIds = groups.map { it.id }.toSet()
                GroupsUiState(
                    groups = groups,
                    expandedGroupIds = expanded intersect validIds,
                    isLoading = false,
                    selectionMode = selection.active,
                    selectedIds = selection.ids intersect validIds
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: GroupsIntent) {
        when (intent) {
            is GroupsIntent.ToggleGroup -> viewModelScope.launch {
                groupRepository.toggleGroup(intent.groupId, intent.enabled)
            }
            is GroupsIntent.ToggleExpand -> _expandedGroupIds.update { current ->
                if (intent.groupId in current) current - intent.groupId
                else current + intent.groupId
            }
            is GroupsIntent.ToggleAlarmInGroup -> viewModelScope.launch {
                alarmRepository.toggleAlarm(intent.alarmId, intent.enabled)
            }
            is GroupsIntent.EnterSelection -> _selection.update {
                Selection(active = true, ids = setOf(intent.groupId))
            }
            is GroupsIntent.ToggleSelection -> _selection.update { current ->
                val newIds = if (intent.groupId in current.ids) {
                    current.ids - intent.groupId
                } else {
                    current.ids + intent.groupId
                }
                if (newIds.isEmpty()) Selection() else current.copy(ids = newIds)
            }
            is GroupsIntent.SelectAll -> _selection.update { current ->
                current.copy(active = true, ids = _uiState.value.groups.map { it.id }.toSet())
            }
            is GroupsIntent.ClearSelection -> _selection.value = Selection()
            is GroupsIntent.DeleteSelected -> {
                val toDelete = _uiState.value.groups.filter { it.id in _uiState.value.selectedIds }
                _selection.value = Selection()
                viewModelScope.launch {
                    toDelete.forEach { groupRepository.deleteGroup(it) }
                }
            }
        }
    }
}
