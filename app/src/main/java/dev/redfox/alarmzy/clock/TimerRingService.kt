package dev.redfox.alarmzy.clock

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.AndroidEntryPoint
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.alarm.AlarmPlayer
import dev.redfox.alarmzy.alarm.AlarmRingingActivity
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Foreground service that rings when a timer reaches zero. Mirrors [dev.redfox.alarmzy.alarm.AlarmService]:
 * high-priority full-screen notification, the shared ringing screen (Stop only), the user's default
 * ringtone/vibration, and an auto-stop after [AUTO_STOP_MILLIS]. Stopping goes through
 * [TimerEngine.cancel], which stops this service.
 */
@AndroidEntryPoint
class TimerRingService : Service() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var timerEngine: TimerEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: AlarmPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RING) {
            stopSelf()
            return START_NOT_STICKY
        }
        TimerNotifications.ensureChannels(this)

        val durationLabel = TimerNotifications.formatDuration(runBlocking { timerEngine.current() }.durationMs)
        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmRingingActivity.EXTRA_IS_TIMER, true)
            putExtra(AlarmRingingActivity.EXTRA_TIMER_LABEL, "Timer · $durationLabel")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreen = PendingIntent.getActivity(
            this, TimerNotifications.ID_RINGING, ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, TimerNotifications.CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentTitle("Time's up")
            .setContentText("$durationLabel timer")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(TimerNotifications.openTimerTab(this))
            .addAction(
                Notification.Action.Builder(null, "Stop", TimerNotifications.action(this, TimerReceiver.ACTION_STOP)).build()
            )
            .build()
        startForeground(TimerNotifications.ID_RINGING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        if (Settings.canDrawOverlays(this)) startActivity(ringingIntent)

        val prefs = runBlocking { dataStore.data.first() }
        player?.stop()
        player = AlarmPlayer(this).apply {
            play(
                ringtoneUri = prefs[SettingsViewModel.KEY_DEFAULT_RINGTONE_URI],
                vibrate = prefs[SettingsViewModel.KEY_DEFAULT_VIBRATION] ?: true,
                gradualVolume = false,
                scope = serviceScope
            )
        }

        serviceScope.launch {
            delay(AUTO_STOP_MILLIS)
            timerEngine.cancel()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.stop()
        player = null
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_RING = "dev.redfox.alarmzy.timer.RING"
        const val AUTO_STOP_MILLIS = 5L * 60 * 1000
    }
}
