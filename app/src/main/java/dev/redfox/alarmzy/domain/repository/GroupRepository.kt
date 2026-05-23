package dev.redfox.alarmzy.domain.repository

import dev.redfox.alarmzy.domain.model.AlarmGroup
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getAllGroupsWithAlarms(): Flow<List<AlarmGroup>>
    fun getAllGroups(): Flow<List<AlarmGroup>>
    suspend fun getGroupById(id: Long): AlarmGroup?
    suspend fun saveGroup(group: AlarmGroup): Long
    suspend fun deleteGroup(group: AlarmGroup)
    suspend fun toggleGroup(groupId: Long, enabled: Boolean)
}
