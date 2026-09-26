package dev.redfox.alarmzy.clock

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.redfox.alarmzy.MainActivity
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.StopwatchState
import dev.redfox.alarmzy.presentation.common.lapTime

/** Promoted Live Update mirroring the stopwatch: live count-up while running, frozen when paused. */
object StopwatchNotifications {
    const val ID = 2003
    const val TAB_STOPWATCH = "stopwatch"

    /** The stopwatch has no end, so its bar cycles once per hour (promotion needs a ProgressStyle). */
    private const val CYCLE_SECONDS = 3600

    fun show(context: Context, state: StopwatchState) {
        LiveUpdates.ensureChannel(context)
        val elapsed = state.elapsedAt(System.currentTimeMillis())
        val lapLabel = if (state.laps.isNotEmpty()) "Lap ${state.laps.size + 1}" else null

        val builder = NotificationCompat.Builder(context, LiveUpdates.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_nav_stopwatch)
            .setContentIntent(openTab(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setColor(LiveUpdates.ACCENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (state.running) {
            // Chronometer counts up from the wall-clock moment the stopwatch would have read 0.
            builder.setContentTitle(listOfNotNull("Stopwatch", lapLabel).joinToString(" · "))
                .setContentText(state.lapSplits.lastOrNull()?.let { "Last lap ${lapTime(it)}" } ?: "Running")
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
                .setWhen(System.currentTimeMillis() - elapsed)
                .setShowWhen(true)
                .setShortCriticalText(LiveUpdates.chipText(elapsed))
                .addAction(R.drawable.ic_lap, "Lap", action(context, StopwatchReceiver.ACTION_LAP))
                .addAction(R.drawable.ic_pause, "Pause", action(context, StopwatchReceiver.ACTION_PAUSE))
        } else {
            builder.setContentTitle("Paused • ${lapTime(elapsed)}")
                .setContentText(lapLabel ?: "Stopwatch")
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setShortCriticalText("Paused")
                .addAction(R.drawable.ic_nav_stopwatch, "Resume", action(context, StopwatchReceiver.ACTION_RESUME))
                .addAction(R.drawable.ic_cancel, "Reset", action(context, StopwatchReceiver.ACTION_RESET))
        }
        LiveUpdates.promote(
            builder,
            LiveUpdates.progressStyle(CYCLE_SECONDS, ((elapsed / 1000) % CYCLE_SECONDS).toInt())
        )
        context.getSystemService(NotificationManager::class.java).notify(ID, builder.build())
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }

    fun action(context: Context, action: String): PendingIntent =
        LiveUpdates.broadcast(context, StopwatchReceiver::class.java, action, action.hashCode())

    private fun openTab(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        ID,
        Intent(context, MainActivity::class.java)
            .putExtra(TimerNotifications.EXTRA_OPEN_TAB, TAB_STOPWATCH)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
