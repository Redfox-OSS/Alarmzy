package dev.redfox.alarmzy.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry point for the timer from outside the UI: the AlarmManager wake-up at zero ([ACTION_FIRE])
 * and the notification buttons (pause/resume/cancel/stop).
 */
@AndroidEntryPoint
class TimerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                timerEngine.finish()
                // Allowed from an exact-alarm broadcast even while the app is in the background.
                context.startForegroundService(
                    Intent(context, TimerRingService::class.java).setAction(TimerRingService.ACTION_RING)
                )
            }
            ACTION_PAUSE -> timerEngine.pause()
            ACTION_RESUME -> timerEngine.resume()
            ACTION_CANCEL, ACTION_STOP -> timerEngine.cancel()
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.redfox.alarmzy.timer.FIRE"
        const val ACTION_PAUSE = "dev.redfox.alarmzy.timer.PAUSE"
        const val ACTION_RESUME = "dev.redfox.alarmzy.timer.RESUME"
        const val ACTION_CANCEL = "dev.redfox.alarmzy.timer.CANCEL"
        const val ACTION_STOP = "dev.redfox.alarmzy.timer.STOP"
    }
}
