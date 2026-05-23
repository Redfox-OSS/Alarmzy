package dev.redfox.alarmzy.domain.model

import java.time.DayOfWeek

sealed interface RepeatMode {
    data object OneTime : RepeatMode
    data object Daily : RepeatMode
    data object Weekdays : RepeatMode
    data object Weekends : RepeatMode
    data class Custom(val days: Set<DayOfWeek>) : RepeatMode

    fun toDays(): Set<DayOfWeek> = when (this) {
        is OneTime -> emptySet()
        is Daily -> DayOfWeek.entries.toSet()
        is Weekdays -> setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        is Weekends -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        is Custom -> days
    }

    val displayName: String
        get() = when (this) {
            is OneTime -> "Once"
            is Daily -> "Every day"
            is Weekdays -> "Weekdays"
            is Weekends -> "Weekends"
            is Custom -> {
                if (days.size == 7) "Every day"
                else days.sortedBy { it.value }
                    .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
            }
        }

    companion object {
        fun fromDays(days: Set<DayOfWeek>): RepeatMode = when {
            days.isEmpty() -> OneTime
            days.size == 7 -> Daily
            days == setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            ) -> Weekdays
            days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> Weekends
            else -> Custom(days)
        }
    }
}
