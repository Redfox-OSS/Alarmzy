package dev.redfox.alarmzy.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.redfox.alarmzy.MainActivity
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus
import java.text.DateFormat
import java.util.Date

/** Notifications for the countdown timer: the promoted Live Update while running/paused, and "Time's up". */
object TimerNotifications {
    const val CHANNEL_ALERT = "timer_alert"
    const val ID_COUNTDOWN = 2001
    const val ID_RINGING = 2002

    /** Extra on the launch intent asking [MainActivity] to open a tab (its route). */
    const val EXTRA_OPEN_TAB = "open_tab"
    const val TAB_TIMER = "timer"

    fun ensureChannels(context: Context) {
        LiveUpdates.ensureChannel(context)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when a timer reaches zero"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Running: countdown chronometer + draining bar. Paused: frozen text. Both promoted on Android 16+. */
    fun showCountdown(context: Context, state: TimerState) {
        ensureChannels(context)
        val now = System.currentTimeMillis()
        val running = state.status == TimerStatus.RUNNING
        val remaining = state.remainingAt(now)
        val builder = NotificationCompat.Builder(context, LiveUpdates.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentIntent(openTab(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setColor(LiveUpdates.ACCENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (running) {
            builder.setContentTitle("Ends at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.endAtMillis))}")
                .setContentText("${formatDuration(state.durationMs)} timer")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(state.endAtMillis)
                .setShowWhen(true)
                .setShortCriticalText(LiveUpdates.chipText(remaining))
                .addAction(R.drawable.ic_notif_pause, "Pause", action(context, TimerReceiver.ACTION_PAUSE))
                .addAction(R.drawable.ic_notif_close, "Cancel", action(context, TimerReceiver.ACTION_CANCEL))
        } else {
            // A chronometer can't be frozen, so paused shows static text.
            builder.setContentTitle("Paused • ${formatDuration(remaining)} left")
                .setContentText("${formatDuration(state.durationMs)} timer")
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setShortCriticalText("Paused")
                .addAction(R.drawable.ic_notif_play, "Resume", action(context, TimerReceiver.ACTION_RESUME))
                .addAction(R.drawable.ic_notif_close, "Cancel", action(context, TimerReceiver.ACTION_CANCEL))
        }
        // The bar drains toward zero (a growing bar reads as "almost done").
        LiveUpdates.promote(
            builder,
            LiveUpdates.progressStyle((state.durationMs / 1000).toInt(), ((remaining + 999) / 1000).toInt())
        )
        context.getSystemService(NotificationManager::class.java).notify(ID_COUNTDOWN, builder.build())
    }

    fun cancelCountdown(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID_COUNTDOWN)
    }

    /** "Time's up" with Stop / +1 min. Used as the ring service's foreground notification. */
    fun buildFinished(context: Context, durationMs: Long, fullScreen: PendingIntent?): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentTitle("Time's up")
            .setContentText("${formatDuration(durationMs)} timer")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openTab(context))
            .apply { if (fullScreen != null) setFullScreenIntent(fullScreen, true) }
            .addAction(R.drawable.ic_notif_stop, "Stop", action(context, TimerReceiver.ACTION_STOP))
            .addAction(R.drawable.ic_notif_add, "+1 min", action(context, TimerReceiver.ACTION_ADD_MINUTE))
            .build()
    }

    /** Silent "Time's up" for a timer that ended while the phone was off (can't ring from boot). */
    fun showMissedFinish(context: Context, durationMs: Long) {
        context.getSystemService(NotificationManager::class.java)
            .notify(ID_RINGING, buildFinished(context, durationMs, fullScreen = null))
    }

    fun cancelFinished(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID_RINGING)
    }

    fun action(context: Context, action: String): PendingIntent =
        LiveUpdates.broadcast(context, TimerReceiver::class.java, action, action.hashCode())

    fun openTab(context: Context): PendingIntent = PendingIntent.getActivity(
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
