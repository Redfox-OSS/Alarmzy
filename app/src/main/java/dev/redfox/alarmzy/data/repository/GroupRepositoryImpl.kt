package dev.redfox.alarmzy.data.repository

import dev.redfox.alarmzy.alarm.AlarmScheduler
import dev.redfox.alarmzy.data.local.dao.AlarmDao
import dev.redfox.alarmzy.data.local.dao.GroupDao
import dev.redfox.alarmzy.data.local.entity.GroupEntity
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode
import dev.redfox.alarmzy.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) : GroupRepository {

    override fun getAllGroupsWithAlarms(): Flow<List<AlarmGroup>> =
        groupDao.getAllGroupsWithAlarms().map { groupsWithAlarms ->
            groupsWithAlarms.map { gwa ->
                AlarmGroup(
                    id = gwa.group.id,
                    name = gwa.group.name,
                    isEnabled = gwa.group.isEnabled,
                    alarms = gwa.alarms.map { entity ->
                        val days = AlarmRepositoryImpl.parseDays(entity.repeatDays)
                        Alarm(
                            id = entity.id,
                            hour = entity.hour,
                            minute = entity.minute,
                            label = entity.label,
                            isEnabled = entity.isEnabled,
                            repeatMode = RepeatMode.fromDays(days),
                            ringtoneUri = entity.ringtoneUri,
                            ringtoneName = entity.ringtoneName,
                            vibrationEnabled = entity.vibrationEnabled,
                            snoozeDurationMinutes = entity.snoozeDurationMinutes,
                            gradualVolumeIncrease = entity.gradualVolumeIncrease,
                            groupId = entity.groupId,
                            groupName = gwa.group.name,
                            nextTriggerTimeMillis = entity.nextTriggerTimeMillis
                        )
                    }
                )
            }
        }

    override fun getAllGroups(): Flow<List<AlarmGroup>> =
        groupDao.getAllGroups().map { groups ->
            groups.map { it.toDomain() }
        }

    override suspend fun getGroupById(id: Long): AlarmGroup? =
        groupDao.getGroupById(id)?.toDomain()

    override suspend fun saveGroup(group: AlarmGroup): Long {
        val entity = GroupEntity(
            id = group.id,
            name = group.name,
            isEnabled = group.isEnabled
        )
        return if (group.id == 0L) {
            groupDao.insertGroup(entity)
        } else {
            groupDao.updateGroup(entity)
            group.id
        }
    }

    override suspend fun deleteGroup(group: AlarmGroup) {
        groupDao.deleteGroup(
            GroupEntity(
                id = group.id,
                name = group.name,
                isEnabled = group.isEnabled
            )
        )
    }

    override suspend fun toggleGroup(groupId: Long, enabled: Boolean) {
        groupDao.setGroupEnabled(groupId, enabled)
        alarmDao.setAllAlarmsInGroupEnabled(groupId, enabled)
        val alarms = alarmDao.getAlarmsByGroupIdSync(groupId)
        for (alarm in alarms) {
            if (enabled) {
                val days = AlarmRepositoryImpl.parseDays(alarm.repeatDays)
                val triggerTime = AlarmRepositoryImpl.calculateNextTriggerTime(
                    alarm.hour, alarm.minute, days
                )
                alarmDao.updateNextTriggerTime(alarm.id, triggerTime)
                alarmScheduler.schedule(
                    Alarm(id = alarm.id, hour = alarm.hour, minute = alarm.minute,
                        isEnabled = true, nextTriggerTimeMillis = triggerTime)
                )
            } else {
                alarmScheduler.cancel(alarm.id)
            }
        }
    }

    private fun GroupEntity.toDomain() = AlarmGroup(
        id = id,
        name = name,
        isEnabled = isEnabled
    )
}
