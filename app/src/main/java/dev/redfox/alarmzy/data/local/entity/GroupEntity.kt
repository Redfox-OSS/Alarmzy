package dev.redfox.alarmzy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val color: String? = null,
    /** [dev.redfox.alarmzy.domain.model.GroupIcon.key]; null = generic folder icon. */
    val icon: String? = null,
    /** Group-specific alarm sound; null = follow the Settings ringtone. */
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
