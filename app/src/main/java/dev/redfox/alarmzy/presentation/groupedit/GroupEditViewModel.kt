package dev.redfox.alarmzy.presentation.groupedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.FreemiumLimits
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.BillingRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val alarmRepository: AlarmRepository,
    private val billingRepository: BillingRepository,
    private val dataStore: DataStore<Preferences>
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
            val prefs = dataStore.data.first()
            val settingsRingtoneUri = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_URI]
            val settingsRingtoneName = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_NAME] ?: "Default"

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
                    color = group?.color,
                    icon = group?.icon,
                    ringtoneUri = group?.ringtoneUri,
                    ringtoneName = group?.ringtoneName,
                    settingsRingtoneName = settingsRingtoneName,
                    settingsRingtoneUri = settingsRingtoneUri,
                    allAlarms = allAlarms,
                    selectedAlarmIds = memberAlarmIds,
                    groupId = groupId
                )
            } else {
                // New groups start on the Settings sound; the user can override it.
                _uiState.value = GroupEditUiState(
                    allAlarms = allAlarms,
                    settingsRingtoneName = settingsRingtoneName,
                    settingsRingtoneUri = settingsRingtoneUri
                )
            }
        }
    }

    fun onIntent(intent: GroupEditIntent) {
        when (intent) {
            is GroupEditIntent.SetName -> _uiState.value = _uiState.value.copy(name = intent.name)
            is GroupEditIntent.SetColor -> _uiState.value = _uiState.value.copy(color = intent.color)
            is GroupEditIntent.SetIcon -> _uiState.value = _uiState.value.copy(icon = intent.icon)
            is GroupEditIntent.SetRingtone -> _uiState.value = _uiState.value.copy(
                ringtoneUri = intent.uri, ringtoneName = intent.name
            )
            is GroupEditIntent.UseSettingsRingtone -> _uiState.value = _uiState.value.copy(
                ringtoneUri = null, ringtoneName = null
            )
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

        viewModelScope.launch {
            val isPro = billingRepository.isProUnlocked().first()

            if (state.isNew && !isPro) {
                val existingGroupCount = groupRepository.getAllGroups().first().size
                if (existingGroupCount >= FreemiumLimits.FREE_GROUP_LIMIT) {
                    _sideEffect.send(GroupEditSideEffect.ShowPaywall)
                    return@launch
                }
            }
            if (!isPro && state.selectedAlarmIds.size > FreemiumLimits.FREE_ALARMS_PER_GROUP_LIMIT) {
                _sideEffect.send(GroupEditSideEffect.ShowPaywall)
                return@launch
            }

            _uiState.value = state.copy(isSaving = true)
            val group = AlarmGroup(
                id = if (state.isNew) 0 else groupId ?: 0,
                name = state.name,
                isEnabled = state.isEnabled,
                color = state.color,
                icon = state.icon,
                ringtoneUri = state.ringtoneUri,
                ringtoneName = state.ringtoneName
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
