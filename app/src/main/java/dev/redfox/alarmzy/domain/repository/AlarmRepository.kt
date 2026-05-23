package dev.redfox.alarmzy.domain.repository

import dev.redfox.alarmzy.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    fun getNextEnabledAlarm(): Flow<Alarm?>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun saveAlarm(alarm: Alarm): Long
    suspend fun deleteAlarm(alarm: Alarm)
    suspend fun toggleAlarm(id: Long, enabled: Boolean)
    suspend fun setAlarmGroup(alarmId: Long, groupId: Long?)
    suspend fun getAllEnabledAlarms(): List<Alarm>
}
