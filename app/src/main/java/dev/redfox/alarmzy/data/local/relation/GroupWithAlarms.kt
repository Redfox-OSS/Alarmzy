package dev.redfox.alarmzy.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import dev.redfox.alarmzy.data.local.entity.AlarmEntity
import dev.redfox.alarmzy.data.local.entity.GroupEntity

data class GroupWithAlarms(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val alarms: List<AlarmEntity>
)
