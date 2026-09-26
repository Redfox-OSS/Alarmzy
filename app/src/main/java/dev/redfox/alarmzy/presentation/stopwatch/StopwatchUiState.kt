package dev.redfox.alarmzy.presentation.stopwatch

import dev.redfox.alarmzy.domain.model.StopwatchState

data class StopwatchUiState(
    val stopwatch: StopwatchState = StopwatchState()
)
