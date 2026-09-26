package dev.redfox.alarmzy.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry point for the timer from outside the UI: the finish alarm ([ACTION_FIRE]), the 60s Live
 * Update TICK, and the notification buttons. Everything is delegated to [TimerEngine].
 */
@AndroidEntryPoint
class TimerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_FIRE -> timerEngine.finish {
                // Only for a genuine (non-stale) finish. Allowed from an exact-alarm broadcast.
                appContext.startForegroundService(
                    Intent(appContext, TimerRingService::class.java).setAction(TimerRingService.ACTION_RING)
                )
            }
            ACTION_TICK -> timerEngine.refresh()
            ACTION_PAUSE -> timerEngine.pause()
            ACTION_RESUME -> timerEngine.resume()
            ACTION_ADD_MINUTE -> timerEngine.addMinute()
            ACTION_CANCEL, ACTION_STOP -> timerEngine.cancel()
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.redfox.alarmzy.timer.FIRE"
        const val ACTION_TICK = "dev.redfox.alarmzy.timer.TICK"
        const val ACTION_PAUSE = "dev.redfox.alarmzy.timer.PAUSE"
        const val ACTION_RESUME = "dev.redfox.alarmzy.timer.RESUME"
        const val ACTION_ADD_MINUTE = "dev.redfox.alarmzy.timer.ADD_MINUTE"
        const val ACTION_CANCEL = "dev.redfox.alarmzy.timer.CANCEL"
        const val ACTION_STOP = "dev.redfox.alarmzy.timer.STOP"
    }
}
