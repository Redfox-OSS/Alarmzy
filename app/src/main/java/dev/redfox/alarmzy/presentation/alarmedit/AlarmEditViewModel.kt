package dev.redfox.alarmzy.presentation.alarmedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.model.Alarm
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
class AlarmEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val alarmId: Long? = savedStateHandle.get<Long>("alarmId")

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<AlarmEditSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val groups = groupRepository.getAllGroups().first()
            val state = if (alarmId != null) {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    AlarmEditUiState(
                        isNew = false,
                        hour = alarm.hour,
                        minute = alarm.minute,
                        label = alarm.label,
                        repeatMode = alarm.repeatMode,
                        ringtoneUri = alarm.ringtoneUri,
                        ringtoneName = alarm.ringtoneName ?: "Default",
                        vibrationEnabled = alarm.vibrationEnabled,
                        snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                        gradualVolumeIncrease = alarm.gradualVolumeIncrease,
                        selectedGroupId = alarm.groupId,
                        availableGroups = groups
                    )
                } else {
                    AlarmEditUiState(availableGroups = groups)
                }
            } else {
                AlarmEditUiState(availableGroups = groups)
            }
            _uiState.value = state
        }
    }

    fun onIntent(intent: AlarmEditIntent) {
        when (intent) {
            is AlarmEditIntent.SetTime -> _uiState.value = _uiState.value.copy(
                hour = intent.hour, minute = intent.minute
            )
            is AlarmEditIntent.SetLabel -> _uiState.value = _uiState.value.copy(
                label = intent.label
            )
            is AlarmEditIntent.SetRepeatMode -> _uiState.value = _uiState.value.copy(
                repeatMode = intent.mode
            )
            is AlarmEditIntent.SetRingtone -> _uiState.value = _uiState.value.copy(
                ringtoneUri = intent.uri, ringtoneName = intent.name
            )
            is AlarmEditIntent.SetVibration -> _uiState.value = _uiState.value.copy(
                vibrationEnabled = intent.enabled
            )
            is AlarmEditIntent.SetSnoozeDuration -> _uiState.value = _uiState.value.copy(
                snoozeDurationMinutes = intent.minutes
            )
            is AlarmEditIntent.SetGradualVolume -> _uiState.value = _uiState.value.copy(
                gradualVolumeIncrease = intent.enabled
            )
            is AlarmEditIntent.SetGroup -> _uiState.value = _uiState.value.copy(
                selectedGroupId = intent.groupId
            )
            is AlarmEditIntent.Save -> save()
            is AlarmEditIntent.Delete -> delete()
        }
    }

    private fun save() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val alarm = Alarm(
                id = if (state.isNew) 0 else alarmId ?: 0,
                hour = state.hour,
                minute = state.minute,
                label = state.label,
                isEnabled = true,
                repeatMode = state.repeatMode,
                ringtoneUri = state.ringtoneUri,
                ringtoneName = state.ringtoneName,
                vibrationEnabled = state.vibrationEnabled,
                snoozeDurationMinutes = state.snoozeDurationMinutes,
                gradualVolumeIncrease = state.gradualVolumeIncrease,
                groupId = state.selectedGroupId
            )
            alarmRepository.saveAlarm(alarm)
            _sideEffect.send(AlarmEditSideEffect.Saved)
        }
    }

    private fun delete() {
        if (alarmId == null) return
        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
            alarmRepository.deleteAlarm(alarm)
            _sideEffect.send(AlarmEditSideEffect.Deleted)
        }
    }
}
