package dev.redfox.alarmzy.domain.model

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Countdown timer snapshot. [endAtMillis] is wall-clock (`System.currentTimeMillis()`) so it can
 * be scheduled with AlarmManager RTC and re-armed after a reboot.
 *
 * @param durationMs total length of the current run (what the ring's progress is measured against).
 * @param endAtMillis when the timer hits zero; only meaningful while [TimerStatus.RUNNING].
 * @param remainingMs time left when paused; only meaningful while [TimerStatus.PAUSED].
 * @param pickerMs last duration chosen on the picker, restored when the timer is idle again.
 */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val durationMs: Long = 0L,
    val endAtMillis: Long = 0L,
    val remainingMs: Long = 0L,
    val pickerMs: Long = DEFAULT_PICKER_MS,
) {
    fun remainingAt(nowMillis: Long): Long = when (status) {
        TimerStatus.RUNNING -> (endAtMillis - nowMillis).coerceAtLeast(0L)
        TimerStatus.PAUSED -> remainingMs
        TimerStatus.FINISHED -> 0L
        TimerStatus.IDLE -> pickerMs
    }

    companion object {
        const val DEFAULT_PICKER_MS = 15 * 60_000L
    }
}
