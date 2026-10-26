package dev.redfox.alarmzy.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Re-renders the clock widget at each minute boundary. The widget's text is drawn into bitmaps
 * (see [ClockWidgetRenderer]), so nothing on the home screen ticks by itself.
 *
 * Uses a *non-wakeup* exact alarm: while the screen is off it's held back and delivered as soon
 * as the device wakes, so the widget is current when it's seen without waking the phone every
 * minute. Exact delivery comes from `USE_EXACT_ALARM` (≤ SDK 32: `SCHEDULE_EXACT_ALARM`).
 */
object ClockWidgetTicker {

    const val ACTION_TICK = "dev.redfox.alarmzy.widget.ACTION_TICK"

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val nextMinute = now - now % 60_000L + 60_000L
        val pending = tickIntent(context)
        try {
            alarmManager.setExact(AlarmManager.RTC, nextMinute, pending)
        } catch (_: SecurityException) {
            // Exact alarms revoked (possible on SDK 31-32): settle for within a few seconds.
            alarmManager.setWindow(AlarmManager.RTC, nextMinute, 5_000L, pending)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(tickIntent(context))
    }

    private fun tickIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ClockWidgetProvider::class.java).setAction(ACTION_TICK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
