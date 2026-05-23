package dev.redfox.alarmzy.presentation.groupedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val groupId: Long? = savedStateHandle.get<Long>("groupId")

    private val _uiState = MutableStateFlow(GroupEditUiState())
    val uiState: StateFlow<GroupEditUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<GroupEditSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val allAlarms = alarmRepository.getAllAlarms().first()

            if (groupId != null) {
                val group = groupRepository.getGroupById(groupId)
                val memberAlarmIds = allAlarms
                    .filter { it.groupId == groupId }
                    .map { it.id }
                    .toSet()

                _uiState.value = GroupEditUiState(
                    isNew = false,
                    name = group?.name ?: "",
                    isEnabled = group?.isEnabled ?: true,
                    allAlarms = allAlarms,
                    selectedAlarmIds = memberAlarmIds
                )
            } else {
                _uiState.value = GroupEditUiState(
                    allAlarms = allAlarms
                )
            }
        }
    }

    fun onIntent(intent: GroupEditIntent) {
        when (intent) {
            is GroupEditIntent.SetName -> _uiState.value = _uiState.value.copy(name = intent.name)
            is GroupEditIntent.ToggleAlarmSelection -> {
                val current = _uiState.value.selectedAlarmIds
                _uiState.value = _uiState.value.copy(
                    selectedAlarmIds = if (intent.alarmId in current)
                        current - intent.alarmId
                    else
                        current + intent.alarmId
                )
            }
            is GroupEditIntent.Save -> save()
            is GroupEditIntent.Delete -> delete()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val group = AlarmGroup(
                id = if (state.isNew) 0 else groupId ?: 0,
                name = state.name,
                isEnabled = state.isEnabled
            )
            val savedGroupId = groupRepository.saveGroup(group)

            val allAlarms = alarmRepository.getAllAlarms().first()
            for (alarm in allAlarms) {
                when {
                    alarm.id in state.selectedAlarmIds && alarm.groupId != savedGroupId -> {
                        alarmRepository.setAlarmGroup(alarm.id, savedGroupId)
                    }
                    alarm.id !in state.selectedAlarmIds && alarm.groupId == savedGroupId -> {
                        alarmRepository.setAlarmGroup(alarm.id, null)
                    }
                }
            }

            _sideEffect.send(GroupEditSideEffect.Saved)
        }
    }

    private fun delete() {
        if (groupId == null) return
        viewModelScope.launch {
            val group = groupRepository.getGroupById(groupId) ?: return@launch
            groupRepository.deleteGroup(group)
            _sideEffect.send(GroupEditSideEffect.Deleted)
        }
    }
}
