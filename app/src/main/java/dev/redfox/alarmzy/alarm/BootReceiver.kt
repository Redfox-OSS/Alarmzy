package dev.redfox.alarmzy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import dev.redfox.alarmzy.data.repository.AlarmRepositoryImpl
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmDao: AlarmDao

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabledAlarms = alarmDao.getAllEnabledAlarms()
                for (entity in enabledAlarms) {
                    val days = AlarmRepositoryImpl.parseDays(entity.repeatDays)
                    val nextTrigger = AlarmRepositoryImpl.calculateNextTriggerTime(
                        entity.hour, entity.minute, days
                    )
                    alarmDao.updateNextTriggerTime(entity.id, nextTrigger)

                    val alarm = Alarm(
                        id = entity.id,
                        hour = entity.hour,
                        minute = entity.minute,
                        isEnabled = true,
                        repeatMode = RepeatMode.fromDays(days),
                        ringtoneUri = entity.ringtoneUri,
                        nextTriggerTimeMillis = nextTrigger
                    )
                    alarmScheduler.schedule(alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
