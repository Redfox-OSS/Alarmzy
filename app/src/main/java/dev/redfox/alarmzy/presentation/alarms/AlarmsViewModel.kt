package dev.redfox.alarmzy.presentation.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmsUiState())
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                alarmRepository.getAllAlarms(),
                alarmRepository.getNextEnabledAlarm()
            ) { alarms, nextAlarm ->
                AlarmsUiState(
                    alarms = alarms,
                    nextAlarm = nextAlarm,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: AlarmsIntent) {
        when (intent) {
            is AlarmsIntent.ToggleAlarm -> viewModelScope.launch {
                alarmRepository.toggleAlarm(intent.alarmId, intent.enabled)
            }
            is AlarmsIntent.DeleteAlarm -> viewModelScope.launch {
                alarmRepository.deleteAlarm(intent.alarm)
            }
        }
    }
}
