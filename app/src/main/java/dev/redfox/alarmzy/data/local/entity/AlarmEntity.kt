package dev.redfox.alarmzy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val gradualVolumeIncrease: Boolean = false,
    val groupId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerTimeMillis: Long = 0
)
