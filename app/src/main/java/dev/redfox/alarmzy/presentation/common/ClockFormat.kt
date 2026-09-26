package dev.redfox.alarmzy.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis

/**
 * Current value of [clock], refreshed every frame while [active] (for live stopwatch/timer
 * readouts). Engines only store start/end timestamps; the UI derives the reading from this.
 */
@Composable
fun rememberTicker(active: Boolean, clock: () -> Long): Long {
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(active) {
        now = clock()
        while (active) {
            withFrameMillis { }
            now = clock()
        }
    }
    return now
}

/** Main readout split for styling: "04:42" (or "1:04:42") plus the tenths digit "1". */
data class StopwatchReadout(val main: List<String>, val tenths: String)

fun stopwatchReadout(ms: Long): StopwatchReadout {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val parts = if (h > 0) listOf("$h", "%02d".format(m), "%02d".format(s)) else listOf("%02d".format(m), "%02d".format(s))
    return StopwatchReadout(parts, "${(ms % 1000) / 100}")
}

/** Lap time "00:32.14", or "1:02:03.40" past an hour. */
fun lapTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val cs = (ms % 1000) / 10
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}

/** Countdown readout, rounded *up* to the second so it reads 00:01 until it truly ends: "14:48" / "1:05:00". */
fun countdownReadout(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** "15M 00S" / "1H 05M" pill under the countdown. */
fun durationPill(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%dH %02dM".format(h, m) else "%dM %02dS".format(m, s)
}

fun hmsToMillis(h: Int, m: Int, s: Int): Long = ((h * 3600L) + (m * 60L) + s) * 1000L

fun millisToHms(ms: Long): Triple<Int, Int, Int> {
    val total = ms / 1000
    return Triple((total / 3600).toInt().coerceAtMost(99), ((total % 3600) / 60).toInt(), (total % 60).toInt())
}
