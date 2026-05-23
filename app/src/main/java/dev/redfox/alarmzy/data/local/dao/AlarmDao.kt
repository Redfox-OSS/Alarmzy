package dev.redfox.alarmzy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.redfox.alarmzy.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY nextTriggerTimeMillis ASC LIMIT 1")
    fun getNextEnabledAlarm(): Flow<AlarmEntity?>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE groupId = :groupId")
    fun getAlarmsByGroupId(groupId: Long): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE groupId IS NULL ORDER BY hour, minute")
    fun getUngroupedAlarms(): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :alarmId")
    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE groupId = :groupId")
    suspend fun setAllAlarmsInGroupEnabled(groupId: Long, enabled: Boolean)

    @Query("UPDATE alarms SET groupId = :groupId WHERE id = :alarmId")
    suspend fun setAlarmGroup(alarmId: Long, groupId: Long?)

    @Query("UPDATE alarms SET nextTriggerTimeMillis = :triggerTime WHERE id = :alarmId")
    suspend fun updateNextTriggerTime(alarmId: Long, triggerTime: Long)

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getAllEnabledAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE groupId = :groupId")
    suspend fun getAlarmsByGroupIdSync(groupId: Long): List<AlarmEntity>
}
