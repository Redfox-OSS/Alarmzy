package dev.redfox.alarmzy.presentation.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.clock.StopwatchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopwatchViewModel @Inject constructor(
    private val engine: StopwatchEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            engine.state.collect { _uiState.value = StopwatchUiState(stopwatch = it) }
        }
    }

    fun onIntent(intent: StopwatchIntent) {
        when (intent) {
            StopwatchIntent.Start -> engine.start()
            StopwatchIntent.Pause -> engine.pause()
            StopwatchIntent.Lap -> engine.lap()
            StopwatchIntent.Reset -> engine.reset()
        }
    }
}
