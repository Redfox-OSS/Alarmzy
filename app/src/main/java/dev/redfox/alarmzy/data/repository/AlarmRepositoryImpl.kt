package dev.redfox.alarmzy.data.repository

import dev.redfox.alarmzy.alarm.AlarmScheduler
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import dev.redfox.alarmzy.data.local.dao.GroupDao
import dev.redfox.alarmzy.data.local.entity.AlarmEntity
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.RepeatMode
import dev.redfox.alarmzy.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao,
    private val groupDao: GroupDao,
    private val alarmScheduler: AlarmScheduler
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> =
        alarmDao.getAllAlarms().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getNextEnabledAlarm(): Flow<Alarm?> =
        alarmDao.getNextEnabledAlarm().map { it?.toDomain() }

    override suspend fun getAlarmById(id: Long): Alarm? =
        alarmDao.getAlarmById(id)?.toDomain()

    override suspend fun saveAlarm(alarm: Alarm): Long {
        val entity = alarm.toEntity()
        val triggerTime = calculateNextTriggerTime(entity.hour, entity.minute, alarm.repeatMode.toDays())
        val entityWithTrigger = entity.copy(nextTriggerTimeMillis = triggerTime)
        val id = if (alarm.id == 0L) {
            alarmDao.insertAlarm(entityWithTrigger)
        } else {
            alarmDao.updateAlarm(entityWithTrigger)
            alarm.id
        }
        val savedAlarm = alarm.copy(id = id, nextTriggerTimeMillis = triggerTime)
        if (savedAlarm.isEnabled) {
            alarmScheduler.schedule(savedAlarm)
        } else {
            alarmScheduler.cancel(id)
        }
        return id
    }

    override suspend fun saveAlarms(alarms: List<Alarm>) {
        alarms.forEach { saveAlarm(it) }
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmScheduler.cancel(alarm.id)
        alarmDao.deleteAlarm(alarm.toEntity())
    }

    override suspend fun toggleAlarm(id: Long, enabled: Boolean) {
        alarmDao.setAlarmEnabled(id, enabled)
        if (enabled) {
            val alarm = alarmDao.getAlarmById(id) ?: return
            val days = parseDays(alarm.repeatDays)
            val triggerTime = calculateNextTriggerTime(alarm.hour, alarm.minute, days)
            alarmDao.updateNextTriggerTime(id, triggerTime)
            alarmScheduler.schedule(
                Alarm(id = id, hour = alarm.hour, minute = alarm.minute,
                    isEnabled = true, nextTriggerTimeMillis = triggerTime)
            )
        } else {
            alarmScheduler.cancel(id)
        }
    }

    override suspend fun setAlarmGroup(alarmId: Long, groupId: Long?) {
        alarmDao.setAlarmGroup(alarmId, groupId)
    }

    override suspend fun getAllEnabledAlarms(): List<Alarm> =
        alarmDao.getAllEnabledAlarms().map { it.toDomain() }

    private suspend fun AlarmEntity.toDomain(): Alarm {
        val group = groupId?.let { groupDao.getGroupById(it) }
        val days = parseDays(repeatDays)
        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            label = label,
            isEnabled = isEnabled,
            repeatMode = RepeatMode.fromDays(days),
            ringtoneUri = ringtoneUri,
            ringtoneName = ringtoneName,
            vibrationEnabled = vibrationEnabled,
            snoozeDurationMinutes = snoozeDurationMinutes,
            gradualVolumeIncrease = gradualVolumeIncrease,
            groupId = groupId,
            groupName = group?.name,
            groupColor = group?.color,
            nextTriggerTimeMillis = nextTriggerTimeMillis
        )
    }

    private fun Alarm.toEntity(): AlarmEntity {
        val daysString = repeatMode.toDays()
            .joinToString(",") { it.value.toString() }
        return AlarmEntity(
            id = id,
            hour = hour,
            minute = minute,
            label = label,
            isEnabled = isEnabled,
            repeatDays = daysString,
            ringtoneUri = ringtoneUri,
            ringtoneName = ringtoneName,
            vibrationEnabled = vibrationEnabled,
            snoozeDurationMinutes = snoozeDurationMinutes,
            gradualVolumeIncrease = gradualVolumeIncrease,
            groupId = groupId,
            nextTriggerTimeMillis = nextTriggerTimeMillis
        )
    }

    companion object {
        fun parseDays(daysString: String): Set<DayOfWeek> =
            if (daysString.isBlank()) emptySet()
            else daysString.split(",").map { DayOfWeek.of(it.trim().toInt()) }.toSet()

        fun calculateNextTriggerTime(hour: Int, minute: Int, days: Set<DayOfWeek>): Long {
            val now = java.time.ZonedDateTime.now()
            val todayAlarmTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

            if (days.isEmpty()) {
                return if (todayAlarmTime.isAfter(now)) {
                    todayAlarmTime.toInstant().toEpochMilli()
                } else {
                    todayAlarmTime.plusDays(1).toInstant().toEpochMilli()
                }
            }

            for (daysAhead in 0L..7L) {
                val candidate = todayAlarmTime.plusDays(daysAhead)
                if (candidate.dayOfWeek in days && candidate.isAfter(now)) {
                    return candidate.toInstant().toEpochMilli()
                }
            }

            return todayAlarmTime.plusDays(7).toInstant().toEpochMilli()
        }
    }
}
