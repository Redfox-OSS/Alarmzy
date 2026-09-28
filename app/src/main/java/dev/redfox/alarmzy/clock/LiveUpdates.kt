package dev.redfox.alarmzy.clock

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Shared plumbing for the timer/stopwatch Live Updates (Android 16 promoted notifications:
 * Pixel status chip, Samsung Now Bar, OnePlus/Oppo/Vivo "Live Alerts" pill).
 *
 * Promotion is silent when a requirement is missing, so every running notification must be:
 * ongoing, titled, on a non-MIN channel, not colorized, and carry BOTH
 * [NotificationCompat.Builder.setRequestPromotedOngoing] and a [NotificationCompat.ProgressStyle].
 */
object LiveUpdates {
    /** High importance (tested config for promotion) but silent: only [ensureChannel]'s first post peeks. */
    const val CHANNEL_LIVE = "clock_live"

    /** Brand crimson for the progress bar; the notification itself is never colorized (blocks promotion). */
    const val ACCENT = 0xFFFF535A.toInt()

    /** How often the promoted notification's static text (chip, title, progress) is refreshed. */
    const val TICK_INTERVAL_MILLIS = 60_000L

    private val LegacyChannels = listOf("timer_progress", "stopwatch_progress")

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // Channel importance can't be raised after creation, so the old LOW channels are replaced.
        LegacyChannels.forEach { runCatching { nm.deleteNotificationChannel(it) } }
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE, "Running timer & stopwatch", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Live countdown and stopwatch on your lock screen and status bar"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }

    /** Requests promotion on Android 16+ (both the flag and ProgressStyle are required on-device). */
    fun promote(builder: NotificationCompat.Builder, style: NotificationCompat.ProgressStyle) {
        if (Build.VERSION.SDK_INT >= 36) {
            builder.setRequestPromotedOngoing(true)
            builder.setStyle(style)
        }
    }

    /**
     * A single-segment bar. Must be styled-by-progress, or the OS draws it permanently full.
     * Lengths are in seconds so short timers still get a meaningful bar.
     */
    fun progressStyle(totalSeconds: Int, progressSeconds: Int): NotificationCompat.ProgressStyle {
        val total = totalSeconds.coerceAtLeast(1)
        return NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(total).setColor(ACCENT)))
            .setProgress(progressSeconds.coerceIn(0, total))
    }

    /**
     * Status chip / Live Alerts pill text while *paused* (≤ 7 chars), since a chronometer can't be
     * frozen: "0:07", "12:54", "1:02:03". While running, no short text is set at all, so the chip
     * shows the notification's own ticking chronometer (count-up or countdown) like the system
     * clock app does.
     */
    fun frozenChipText(ms: Long, roundUp: Boolean = false): String {
        // A countdown rounds up (0.4 s left still reads 0:01); a count-up rounds down.
        val totalSec = (ms.coerceAtLeast(0) + if (roundUp) 999 else 0) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /**
     * Re-arms a one-shot TICK so the progress bar stays current (the chip ticks by itself via the
     * chronometer). Plain exact-while-idle is fine: a late tick only leaves the bar a minute stale.
     */
    fun scheduleTick(context: Context, pendingIntent: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (!am.canScheduleExactAlarms()) return
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TICK_INTERVAL_MILLIS,
            pendingIntent
        )
    }

    fun cancelTick(context: Context, pendingIntent: PendingIntent) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
    }

    fun broadcast(context: Context, receiver: Class<*>, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
