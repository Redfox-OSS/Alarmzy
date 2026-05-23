package dev.redfox.alarmzy.alarm

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.Alarm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled || alarm.nextTriggerTimeMillis <= 0) return

        val intent = createAlarmIntent(alarm.id)
        val showIntent = createShowIntent()

        val clockInfo = AlarmClockInfo(alarm.nextTriggerTimeMillis, showIntent)
        alarmManager.setAlarmClock(clockInfo, intent)
    }

    override fun cancel(alarmId: Long) {
        val intent = createAlarmIntent(alarmId)
        alarmManager.cancel(intent)
    }

    override fun snooze(alarmId: Long, snoozeDurationMinutes: Int) {
        val triggerTime = System.currentTimeMillis() + snoozeDurationMinutes * 60 * 1000L
        val intent = createAlarmIntent(alarmId)
        val showIntent = createShowIntent()

        val clockInfo = AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(clockInfo, intent)
    }

    private fun createAlarmIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createShowIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
