package dev.redfox.alarmzy.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.getAllGroupsWithAlarms().collect { groups ->
                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false
                )
            }
        }
    }

    fun onIntent(intent: GroupsIntent) {
        when (intent) {
            is GroupsIntent.ToggleGroup -> viewModelScope.launch {
                groupRepository.toggleGroup(intent.groupId, intent.enabled)
            }
            is GroupsIntent.ToggleExpand -> {
                val current = _uiState.value.expandedGroupIds
                _uiState.value = _uiState.value.copy(
                    expandedGroupIds = if (intent.groupId in current)
                        current - intent.groupId
                    else
                        current + intent.groupId
                )
            }
            is GroupsIntent.DeleteGroup -> viewModelScope.launch {
                groupRepository.deleteGroup(intent.group)
            }
            is GroupsIntent.ToggleAlarmInGroup -> viewModelScope.launch {
                alarmRepository.toggleAlarm(intent.alarmId, intent.enabled)
            }
        }
    }
}
