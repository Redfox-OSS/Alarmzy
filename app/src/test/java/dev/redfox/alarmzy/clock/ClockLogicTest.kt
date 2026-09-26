package dev.redfox.alarmzy.clock

import dev.redfox.alarmzy.domain.model.StopwatchState
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus
import dev.redfox.alarmzy.presentation.common.countdownReadout
import dev.redfox.alarmzy.presentation.common.durationPill
import dev.redfox.alarmzy.presentation.common.hmsToMillis
import dev.redfox.alarmzy.presentation.common.lapTime
import dev.redfox.alarmzy.presentation.common.millisToHms
import dev.redfox.alarmzy.presentation.common.stopwatchReadout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockLogicTest {

    @Test
    fun stopwatch_elapsed_includes_running_segment() {
        val s = StopwatchState(running = true, startedAt = 1_000, accumulatedMs = 5_000)
        assertEquals(7_500, s.elapsedAt(3_500))
        assertEquals(5_000, s.copy(running = false).elapsedAt(3_500))
    }

    @Test
    fun stopwatch_lap_splits_are_differences_of_totals() {
        val s = StopwatchState(laps = listOf(63_320, 91_440, 133_240))
        assertEquals(listOf(63_320L, 28_120L, 41_800L), s.lapSplits)
    }

    @Test
    fun stopwatch_idle_only_when_nothing_recorded() {
        assertTrue(StopwatchState().isIdle)
        assertEquals(false, StopwatchState(accumulatedMs = 1).isIdle)
    }

    @Test
    fun readouts_format_as_designed() {
        assertEquals(listOf("04", "42"), stopwatchReadout(282_150).main)
        assertEquals("1", stopwatchReadout(282_150).tenths)
        assertEquals(listOf("1", "02", "03"), stopwatchReadout(3_723_000).main)
        assertEquals("00:32.14", lapTime(32_140))
        assertEquals("1:02:03.40", lapTime(3_723_400))
    }

    @Test
    fun countdown_rounds_up_to_the_second() {
        assertEquals("14:48", countdownReadout(887_001))
        assertEquals("00:01", countdownReadout(1))
        assertEquals("00:00", countdownReadout(0))
        assertEquals("1:05:00", countdownReadout(3_900_000))
        assertEquals("15M 00S", durationPill(900_000))
    }

    @Test
    fun hms_round_trips() {
        assertEquals(Triple(1, 2, 3), millisToHms(hmsToMillis(1, 2, 3)))
    }

    @Test
    fun timer_remaining_by_status() {
        val t = TimerState(status = TimerStatus.RUNNING, durationMs = 60_000, endAtMillis = 100_000, remainingMs = 7_000)
        assertEquals(40_000, t.remainingAt(60_000))
        assertEquals(0, t.remainingAt(200_000))
        assertEquals(7_000, t.copy(status = TimerStatus.PAUSED).remainingAt(60_000))
        assertEquals(0, t.copy(status = TimerStatus.FINISHED).remainingAt(60_000))
    }
}
