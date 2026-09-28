package dev.redfox.alarmzy.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import dagger.hilt.android.AndroidEntryPoint
import dev.redfox.alarmzy.R
import androidx.core.app.NotificationCompat
import dev.redfox.alarmzy.clock.LiveUpdates
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import dev.redfox.alarmzy.data.local.dao.GroupDao

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var alarmDao: AlarmDao

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var ringingTracker: RingingAlarmTracker

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var groupDao: GroupDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var alarmPlayer: AlarmPlayer? = null
    private var currentAlarmId: Long = -1
    /** The ringing notification, kept so it can be re-posted instantly if the user swipes it away. */
    private var ringingNotification: Notification? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1

        val action = intent?.action
        when (action) {
            ACTION_SNOOZE -> {
                handleSnooze()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                handleDismiss()
                return START_NOT_STICKY
            }
            ACTION_REPOST -> {
                // Android 14+ lets users swipe away even ongoing notifications. While the alarm is
                // still sounding, put it straight back so there is always a way to stop it.
                if (ringingTracker.ringing.value != null) {
                    ringingNotification?.let { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, it) }
                }
                return START_NOT_STICKY
            }
        }

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        currentAlarmId = alarmId
        val alarm = runBlocking { alarmDao.getAlarmById(alarmId) }

        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_TIME, formatTime12h(alarm.hour, alarm.minute))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId.toInt(), ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, alarmId.toInt() + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            this.action = ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, alarmId.toInt() + 20000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = formatTime12h(alarm.hour, alarm.minute)
        val repostPendingIntent = PendingIntent.getService(
            this, alarmId.toInt() + 30000,
            Intent(this, AlarmService::class.java).setAction(ACTION_REPOST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_alarm)
            .setContentTitle(alarm.label.ifBlank { "Alarm" })
            .setContentText(if (alarm.label.isBlank()) "Ringing · $timeText" else "$timeText · Ringing")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setColor(LiveUpdates.ACCENT)
            // Tapping the notification brings the full-screen ringer back.
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDeleteIntent(repostPendingIntent)
            // Status chip / Live Alerts pill text (≤ 7 chars): "12:30".
            .setShortCriticalText(timeText.substringBefore(' '))
            .addAction(R.drawable.ic_notif_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_notif_close, "Dismiss", dismissPendingIntent)
        // Android 16 Live Update (OnePlus Live Alerts / status chip): pinned and prominent.
        LiveUpdates.promote(builder, NotificationCompat.ProgressStyle().setProgressIndeterminate(true))

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        ringingNotification = notification
        ringingTracker.set(RingingAlarm(id = alarmId, label = alarm.label, time = timeText))

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        // Launch fullscreen alarm activity.
        // SYSTEM_ALERT_WINDOW permission allows us to reliably start the activity
        // from a background service even when the screen is on.
        // Without it, only the notification's fullScreenIntent fires (and only on lock screen).
        if (Settings.canDrawOverlays(this)) {
            startActivity(ringingIntent)
        }

        // Sound, vibration, gradual volume and snooze come from Settings *at ring time*: the
        // alarm editor has no per-alarm controls for them, and the copies stored on each alarm
        // row are only snapshots from when it was created (changing Settings later would
        // otherwise never reach existing alarms).
        val sound = currentSettings()
        // A group can override the sound for all its alarms; null means "use Settings".
        val groupRingtone = alarm.groupId?.let { runBlocking { groupDao.getGroupById(it) } }?.ringtoneUri
        alarmPlayer?.stop()
        alarmPlayer = AlarmPlayer(this).apply {
            play(
                ringtoneUri = when {
                    groupRingtone == null -> sound.ringtoneUri // follow Settings
                    groupRingtone.isBlank() -> null             // group chose the phone's default
                    else -> groupRingtone
                },
                vibrate = sound.vibrate,
                gradualVolume = sound.gradualVolume,
                scope = serviceScope
            )
        }

        serviceScope.launch {
            delay(AUTO_DISMISS_MILLIS)
            handleDismiss()
        }

        return START_NOT_STICKY
    }

    private fun handleSnooze() {
        alarmPlayer?.stop()
        ringingNotification = null

        serviceScope.launch {
            val snoozeDuration = dataStore.data.first()[SettingsViewModel.KEY_DEFAULT_SNOOZE] ?: 5
            alarmScheduler.snooze(currentAlarmId, snoozeDuration)
            ringingTracker.set(null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleDismiss() {
        alarmPlayer?.stop()
        ringingNotification = null

        serviceScope.launch {
            val alarm = alarmDao.getAlarmById(currentAlarmId) ?: run {
                ringingTracker.set(null)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val days = dev.redfox.alarmzy.data.repository.AlarmRepositoryImpl.parseDays(alarm.repeatDays)
            if (days.isEmpty()) {
                alarmDao.setAlarmEnabled(currentAlarmId, false)
            } else {
                val nextTrigger = dev.redfox.alarmzy.data.repository.AlarmRepositoryImpl
                    .calculateNextTriggerTime(alarm.hour, alarm.minute, days)
                alarmDao.updateNextTriggerTime(currentAlarmId, nextTrigger)
                val domainAlarm = dev.redfox.alarmzy.domain.model.Alarm(
                    id = alarm.id,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    isEnabled = true,
                    nextTriggerTimeMillis = nextTrigger
                )
                alarmScheduler.schedule(domainAlarm)
            }

            ringingTracker.set(null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private data class RingSettings(
        val ringtoneUri: String?,
        val vibrate: Boolean,
        val gradualVolume: Boolean,
        val snoozeMinutes: Int,
    )

    private fun currentSettings(): RingSettings {
        val prefs = runBlocking { dataStore.data.first() }
        return RingSettings(
            ringtoneUri = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_URI],
            vibrate = prefs[SettingsViewModel.KEY_DEFAULT_VIBRATION] ?: true,
            gradualVolume = prefs[SettingsViewModel.KEY_DEFAULT_GRADUAL_VOLUME] ?: false,
            snoozeMinutes = prefs[SettingsViewModel.KEY_DEFAULT_SNOOZE] ?: 5,
        )
    }

    private fun formatTime12h(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(h12, minute, period)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        alarmPlayer?.stop()
        ringingNotification = null
        ringingTracker.set(null)
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "dev.redfox.alarmzy.SNOOZE"
        const val ACTION_DISMISS = "dev.redfox.alarmzy.DISMISS"
        const val ACTION_REPOST = "dev.redfox.alarmzy.REPOST_RINGING"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
        const val AUTO_DISMISS_MILLIS = 5L * 60 * 1000
    }
}
