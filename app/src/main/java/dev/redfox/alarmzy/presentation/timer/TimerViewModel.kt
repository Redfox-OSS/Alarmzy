package dev.redfox.alarmzy.presentation.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.clock.TimerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val engine: TimerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            engine.state.collect { _uiState.value = TimerUiState(timer = it) }
        }
    }

    fun onIntent(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.SetDuration -> engine.setPicker(intent.durationMs)
            TimerIntent.Start -> engine.start(_uiState.value.timer.pickerMs)
            TimerIntent.Pause -> engine.pause()
            TimerIntent.Resume -> engine.resume()
            TimerIntent.Cancel -> engine.cancel()
        }
    }
}
