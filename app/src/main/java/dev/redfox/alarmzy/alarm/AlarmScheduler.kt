package dev.redfox.alarmzy.alarm

import dev.redfox.alarmzy.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: Long)
    fun snooze(alarmId: Long, snoozeDurationMinutes: Int)
}
