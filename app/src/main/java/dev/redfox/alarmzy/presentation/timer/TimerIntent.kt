package dev.redfox.alarmzy.presentation.timer

sealed interface TimerIntent {
    data class SetDuration(val durationMs: Long) : TimerIntent
    data object Start : TimerIntent
    data object Pause : TimerIntent
    data object Resume : TimerIntent
    /** Cancels a running/paused timer, or stops a ringing one. */
    data object Cancel : TimerIntent
}
