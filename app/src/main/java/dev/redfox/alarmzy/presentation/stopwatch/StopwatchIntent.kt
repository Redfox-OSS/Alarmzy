package dev.redfox.alarmzy.presentation.stopwatch

sealed interface StopwatchIntent {
    data object Start : StopwatchIntent
    data object Pause : StopwatchIntent
    data object Lap : StopwatchIntent
    data object Reset : StopwatchIntent
}
