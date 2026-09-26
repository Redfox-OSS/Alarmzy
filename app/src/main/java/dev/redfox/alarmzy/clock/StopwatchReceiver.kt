package dev.redfox.alarmzy.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Handles the stopwatch notification buttons and its 60s Live Update TICK. */
@AndroidEntryPoint
class StopwatchReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stopwatchEngine: StopwatchEngine

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_LAP -> stopwatchEngine.lap()
            ACTION_PAUSE -> stopwatchEngine.pause()
            ACTION_RESUME -> stopwatchEngine.start()
            ACTION_RESET -> stopwatchEngine.reset()
            ACTION_TICK -> stopwatchEngine.refresh()
        }
    }

    companion object {
        const val ACTION_LAP = "dev.redfox.alarmzy.stopwatch.LAP"
        const val ACTION_PAUSE = "dev.redfox.alarmzy.stopwatch.PAUSE"
        const val ACTION_RESUME = "dev.redfox.alarmzy.stopwatch.RESUME"
        const val ACTION_RESET = "dev.redfox.alarmzy.stopwatch.RESET"
        const val ACTION_TICK = "dev.redfox.alarmzy.stopwatch.TICK"
    }
}
