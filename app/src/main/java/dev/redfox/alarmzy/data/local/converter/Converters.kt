package dev.redfox.alarmzy.data.local.converter

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun toDayOfWeekSet(data: String): Set<DayOfWeek> =
        if (data.isBlank()) emptySet()
        else data.split(",").map { DayOfWeek.of(it.toInt()) }.toSet()
}
