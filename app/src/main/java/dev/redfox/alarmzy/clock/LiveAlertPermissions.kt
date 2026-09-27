package dev.redfox.alarmzy.clock

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the timer/stopwatch Live Updates need, and where to send the user to fix each piece.
 * Exact alarms aren't listed: the app declares USE_EXACT_ALARM (auto-granted for clock apps).
 */
data class LiveAlertStatus(
    val notificationsAllowed: Boolean,
    /** The user can mute just the live channel while leaving notifications on. */
    val channelEnabled: Boolean,
    /** Android 16 per-app "Live Updates" toggle; null below API 36. */
    val liveUpdatesAllowed: Boolean?,
    /** BBK-family phone on Android 16+: the "Show Live Updates on Live Alerts" toggle can't be read. */
    val showBbkStep: Boolean,
    /**
     * Android 14+ "Full-screen notifications" access: without it a ringing alarm is only a banner,
     * never the full-screen alarm over the lock screen. Always true below API 34.
     */
    val fullScreenAllowed: Boolean = true,
) {
    val somethingMissing: Boolean
        get() = !notificationsAllowed || !channelEnabled || liveUpdatesAllowed == false || !fullScreenAllowed

    /** The home prompt also appears for the unreadable BBK toggle, which is off on a fresh install. */
    val worthPrompting: Boolean get() = somethingMissing || showBbkStep
}

object LiveAlertPermissions {
    private val BbkManufacturers = setOf("oneplus", "oppo", "vivo", "realme", "iqoo", "oplus")

    /** OnePlus/Oppo/Vivo/Realme/iQOO: their Live Alerts pill needs an extra per-app toggle. */
    fun isBbkDevice(): Boolean = Build.MANUFACTURER.lowercase() in BbkManufacturers

    fun status(context: Context): LiveAlertStatus {
        val nmc = NotificationManagerCompat.from(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(LiveUpdates.CHANNEL_LIVE)
        return LiveAlertStatus(
            notificationsAllowed = nmc.areNotificationsEnabled(),
            channelEnabled = channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE,
            liveUpdatesAllowed = if (Build.VERSION.SDK_INT >= 36) nmc.canPostPromotedNotifications() else null,
            showBbkStep = isBbkDevice() && Build.VERSION.SDK_INT >= 36,
            fullScreenAllowed = canUseFullScreen(context),
        )
    }

    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** The per-app "Full-screen notifications" switch (Android 14+). */
    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 34) return openNotificationSettings(context)
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openNotificationSettings(context) }
    }

    // OEM builds don't all resolve every Settings action, and ActivityNotFoundException is a crash:
    // every deep link falls back to a screen that always exists.

    fun openAppSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Also where the BBK "Show Live Updates on Live Alerts" toggle and a muted channel live. */
    fun openNotificationSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openAppSettings(context) }
    }

    fun openLiveUpdatesSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 36) return openNotificationSettings(context)
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openNotificationSettings(context) }
    }
}

/**
 * Re-posts the running timer/stopwatch Live Updates. Call after any permission change so a timer
 * that's already running gets promoted without restarting it.
 */
@Singleton
class ClockLiveAlerts @Inject constructor(
    private val timerEngine: TimerEngine,
    private val stopwatchEngine: StopwatchEngine,
) {
    fun restoreAll() {
        timerEngine.refresh()
        stopwatchEngine.refresh()
    }
}
