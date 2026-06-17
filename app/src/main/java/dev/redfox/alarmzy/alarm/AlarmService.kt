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
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var alarmDao: AlarmDao

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var alarmPlayer: AlarmPlayer? = null
    private var currentAlarmId: Long = -1

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
            putExtra(EXTRA_ALARM_TIME, "%02d:%02d".format(alarm.hour, alarm.minute))
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

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText(alarm.label.ifBlank { "%02d:%02d".format(alarm.hour, alarm.minute) })
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                Notification.Action.Builder(null, "Snooze", snoozePendingIntent).build()
            )
            .addAction(
                Notification.Action.Builder(null, "Dismiss", dismissPendingIntent).build()
            )
            .build()

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

        alarmPlayer?.stop()
        alarmPlayer = AlarmPlayer(this).apply {
            play(
                ringtoneUri = alarm.ringtoneUri,
                vibrate = alarm.vibrationEnabled,
                gradualVolume = alarm.gradualVolumeIncrease,
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

        serviceScope.launch {
            val alarm = alarmDao.getAlarmById(currentAlarmId)
            val snoozeDuration = alarm?.snoozeDurationMinutes ?: 5
            alarmScheduler.snooze(currentAlarmId, snoozeDuration)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleDismiss() {
        alarmPlayer?.stop()

        serviceScope.launch {
            val alarm = alarmDao.getAlarmById(currentAlarmId) ?: run {
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

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "dev.redfox.alarmzy.SNOOZE"
        const val ACTION_DISMISS = "dev.redfox.alarmzy.DISMISS"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_TIME = "extra_alarm_time"
        const val AUTO_DISMISS_MILLIS = 5L * 60 * 1000
    }
}
