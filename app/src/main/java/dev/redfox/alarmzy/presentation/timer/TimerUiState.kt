package dev.redfox.alarmzy.presentation.timer

import dev.redfox.alarmzy.domain.model.TimerState

data class TimerUiState(
    val timer: TimerState = TimerState()
) {
    companion object {
        /** Quick presets under the picker (10, 15, 30 min). */
        val Presets: List<Long> = listOf(10 * 60_000L, 15 * 60_000L, 30 * 60_000L)
    }
}
