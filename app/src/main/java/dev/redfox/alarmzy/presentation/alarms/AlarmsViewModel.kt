package dev.redfox.alarmzy.presentation.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dev.redfox.alarmzy.clock.ClockLiveAlerts
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val dataStore: DataStore<Preferences>,
    private val clockLiveAlerts: ClockLiveAlerts,
) : ViewModel() {

    private data class Selection(
        val active: Boolean = false,
        val ids: Set<Long> = emptySet()
    )

    private val _selection = MutableStateFlow(Selection())

    private val _uiState = MutableStateFlow(AlarmsUiState())
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                alarmRepository.getAllAlarms(),
                alarmRepository.getNextEnabledAlarm(),
                _selection,
                dataStore.data.map { it[KEY_LIVE_ALERTS_PROMPT_ASKED] ?: false }
            ) { alarms, nextAlarm, selection, promptAsked ->
                // Drop any selected ids that no longer exist (e.g. after deletion).
                val validIds = alarms.map { it.id }.toSet()
                val prunedIds = selection.ids intersect validIds
                AlarmsUiState(
                    alarms = alarms,
                    nextAlarm = nextAlarm,
                    isLoading = false,
                    selectionMode = selection.active,
                    selectedIds = prunedIds,
                    liveAlertsPromptAsked = promptAsked
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
            is AlarmsIntent.EnterSelection -> _selection.update {
                Selection(active = true, ids = setOf(intent.alarmId))
            }
            is AlarmsIntent.StartSelection -> _selection.value = Selection(active = true)
            is AlarmsIntent.ToggleSelection -> _selection.update { current ->
                val newIds = if (intent.alarmId in current.ids) {
                    current.ids - intent.alarmId
                } else {
                    current.ids + intent.alarmId
                }
                if (newIds.isEmpty()) Selection() else current.copy(ids = newIds)
            }
            is AlarmsIntent.SelectAll -> _selection.update { current ->
                current.copy(active = true, ids = _uiState.value.alarms.map { it.id }.toSet())
            }
            is AlarmsIntent.ClearSelection -> _selection.value = Selection()
            is AlarmsIntent.MarkLiveAlertsPromptAsked -> viewModelScope.launch {
                dataStore.edit { it[KEY_LIVE_ALERTS_PROMPT_ASKED] = true }
            }
            is AlarmsIntent.RestoreLiveAlerts -> clockLiveAlerts.restoreAll()
            is AlarmsIntent.DeleteSelected -> {
                val toDelete = _uiState.value.alarms.filter { it.id in _uiState.value.selectedIds }
                _selection.value = Selection()
                viewModelScope.launch {
                    toDelete.forEach { alarmRepository.deleteAlarm(it) }
                }
            }
        }
    }

    companion object {
        val KEY_LIVE_ALERTS_PROMPT_ASKED = booleanPreferencesKey("live_alerts_prompt_asked")
    }
}
