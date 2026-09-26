package dev.redfox.alarmzy.domain.model

/**
 * Stopwatch snapshot. Times are wall-clock (`System.currentTimeMillis()`) instants so a running
 * stopwatch survives process death and reboots.
 *
 * @param startedAt wall-clock time when the current running segment began (only meaningful while [running]).
 * @param accumulatedMs time banked from previous running segments (before the last pause).
 * @param laps overall elapsed time at each lap press, oldest first.
 */
data class StopwatchState(
    val running: Boolean = false,
    val startedAt: Long = 0L,
    val accumulatedMs: Long = 0L,
    val laps: List<Long> = emptyList(),
) {
    val isIdle: Boolean get() = !running && accumulatedMs == 0L && laps.isEmpty()

    fun elapsedAt(now: Long): Long = accumulatedMs + if (running) (now - startedAt).coerceAtLeast(0) else 0

    /** Split durations for each recorded lap, oldest first. */
    val lapSplits: List<Long>
        get() = laps.mapIndexed { i, total -> total - (laps.getOrNull(i - 1) ?: 0L) }
}
