package dev.redfox.alarmzy.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.redfox.alarmzy.MainActivity
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus

/** Notification channels and builders for the countdown timer. */
object TimerNotifications {
    const val CHANNEL_PROGRESS = "timer_progress"
    const val CHANNEL_ALERT = "timer_alert"
    const val ID_COUNTDOWN = 2001
    const val ID_RINGING = 2002

    /** Extra on the launch intent asking [MainActivity] to open the Timer tab. */
    const val EXTRA_OPEN_TAB = "open_tab"
    const val TAB_TIMER = "timer"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the countdown while a timer is running"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when a timer reaches zero"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Ongoing countdown (running) or frozen remaining time (paused), with Pause/Resume + Cancel. */
    fun showCountdown(context: Context, state: TimerState) {
        ensureChannels(context)
        val running = state.status == TimerStatus.RUNNING
        val builder = Notification.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentTitle(if (running) "Timer" else "Timer paused")
            .setContentIntent(openTimerTab(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .addAction(
                Notification.Action.Builder(
                    null,
                    if (running) "Pause" else "Resume",
                    action(context, if (running) TimerReceiver.ACTION_PAUSE else TimerReceiver.ACTION_RESUME)
                ).build()
            )
            .addAction(Notification.Action.Builder(null, "Cancel", action(context, TimerReceiver.ACTION_CANCEL)).build())
        if (running) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(state.endAtMillis)
                .setShowWhen(true)
        } else {
            builder.setContentText("${formatDuration(state.remainingMs)} left").setShowWhen(false)
        }
        context.getSystemService(NotificationManager::class.java).notify(ID_COUNTDOWN, builder.build())
    }

    fun cancelCountdown(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID_COUNTDOWN)
    }

    fun action(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, TimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun openTimerTab(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        ID_COUNTDOWN,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_TAB, TAB_TIMER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** "1h 05m", "15m 00s", "45s". */
    fun formatDuration(ms: Long): String {
        val total = (ms + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return when {
            h > 0 -> "%dh %02dm".format(h, m)
            m > 0 -> "%dm %02ds".format(m, s)
            else -> "${s}s"
        }
    }
}
