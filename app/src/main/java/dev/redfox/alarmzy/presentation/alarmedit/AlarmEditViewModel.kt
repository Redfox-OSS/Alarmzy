package dev.redfox.alarmzy.presentation.alarmedit

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import dev.redfox.alarmzy.domain.repository.GroupRepository
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
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
    private val groupRepository: GroupRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val alarmId: Long? = savedStateHandle.get<Long>("alarmId")

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<AlarmEditSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var pendingAlarms: List<Alarm> = emptyList()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val groups = groupRepository.getAllGroups().first()
            val prefs = dataStore.data.first()
            val defaultSnooze = prefs[SettingsViewModel.KEY_DEFAULT_SNOOZE] ?: 5
            val defaultVibration = prefs[SettingsViewModel.KEY_DEFAULT_VIBRATION] ?: true
            val defaultGradualVolume = prefs[SettingsViewModel.KEY_DEFAULT_GRADUAL_VOLUME] ?: false
            val defaultRingtoneUri = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_URI]
            val defaultRingtoneName = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_NAME] ?: "Default"

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
                    AlarmEditUiState(
                        availableGroups = groups,
                        vibrationEnabled = defaultVibration,
                        snoozeDurationMinutes = defaultSnooze,
                        gradualVolumeIncrease = defaultGradualVolume,
                        ringtoneUri = defaultRingtoneUri,
                        ringtoneName = defaultRingtoneName
                    )
                }
            } else {
                AlarmEditUiState(
                    availableGroups = groups,
                    vibrationEnabled = defaultVibration,
                    snoozeDurationMinutes = defaultSnooze,
                    gradualVolumeIncrease = defaultGradualVolume
                )
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
            is AlarmEditIntent.ToggleSeriesMode -> {
                if (intent.enabled) {
                    val s = _uiState.value
                    val endTotal = (s.hour * 60 + s.minute + 60).coerceAtMost(23 * 60 + 59)
                    _uiState.value = s.copy(
                        isSeries = true,
                        seriesEndHour = endTotal / 60,
                        seriesEndMinute = endTotal % 60
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSeries = false)
                }
            }
            is AlarmEditIntent.SetSeriesEnd -> _uiState.value = _uiState.value.copy(
                seriesEndHour = intent.hour, seriesEndMinute = intent.minute
            )
            is AlarmEditIntent.SetSeriesInterval -> _uiState.value = _uiState.value.copy(
                seriesIntervalMinutes = intent.minutes
            )
            is AlarmEditIntent.Save -> save()
            is AlarmEditIntent.ConfirmSave -> commit(pendingAlarms)
            is AlarmEditIntent.DismissDuplicateDialog -> {
                pendingAlarms = emptyList()
                _uiState.value = _uiState.value.copy(
                    showDuplicateDialog = false,
                    duplicateTimes = emptyList()
                )
            }
            is AlarmEditIntent.Delete -> delete()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        if (state.isSeries) {
            viewModelScope.launch {
                val alarms = state.seriesTimes.map { minutesOfDay ->
                    buildAlarm(state, minutesOfDay / 60, minutesOfDay % 60)
                }
                val existingTimes = alarmRepository.getAllAlarms().first()
                    .map { it.hour * 60 + it.minute }
                    .toSet()
                val duplicates = state.seriesTimes.filter { it in existingTimes }
                if (duplicates.isNotEmpty()) {
                    pendingAlarms = alarms
                    _uiState.value = state.copy(
                        showDuplicateDialog = true,
                        duplicateTimes = duplicates.map(::format12h)
                    )
                } else {
                    commit(alarms)
                }
            }
        } else {
            commit(listOf(buildAlarm(state, state.hour, state.minute)))
        }
    }

    private fun commit(alarms: List<Alarm>) {
        if (alarms.isEmpty()) return
        _uiState.value = _uiState.value.copy(isSaving = true, showDuplicateDialog = false)
        viewModelScope.launch {
            alarmRepository.saveAlarms(alarms)
            pendingAlarms = emptyList()
            _sideEffect.send(AlarmEditSideEffect.Saved)
        }
    }

    private fun buildAlarm(state: AlarmEditUiState, hour: Int, minute: Int): Alarm = Alarm(
        id = if (state.isNew || state.isSeries) 0 else alarmId ?: 0,
        hour = hour,
        minute = minute,
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

    private fun format12h(minutesOfDay: Int): String {
        val h = minutesOfDay / 60
        val m = minutesOfDay % 60
        val period = if (h < 12) "AM" else "PM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%d:%02d %s".format(h12, m, period)
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
