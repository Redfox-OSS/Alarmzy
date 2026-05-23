package dev.redfox.alarmzy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.redfox.alarmzy.data.local.entity.GroupEntity
import dev.redfox.alarmzy.data.local.relation.GroupWithAlarms
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` ORDER BY name")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Transaction
    @Query("SELECT * FROM `groups` ORDER BY name")
    fun getAllGroupsWithAlarms(): Flow<List<GroupWithAlarms>>

    @Query("SELECT * FROM `groups` WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("UPDATE `groups` SET isEnabled = :enabled WHERE id = :groupId")
    suspend fun setGroupEnabled(groupId: Long, enabled: Boolean)
}
