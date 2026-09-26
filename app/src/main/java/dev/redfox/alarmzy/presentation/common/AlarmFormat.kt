package dev.redfox.alarmzy.presentation.common

import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.RepeatMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** 12-hour clock split into the zero-padded digits ("07:00") and the meridiem ("AM"). */
data class ClockTime(val digits: String, val meridiem: String)

fun clockTime(hour: Int, minute: Int): ClockTime {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return ClockTime("%02d:%02d".format(h12, minute), if (hour < 12) "AM" else "PM")
}

val Alarm.clockTime: ClockTime get() = clockTime(hour, minute)

/** Compact form for summaries ("8:00 AM"). */
fun shortTime(hour: Int, minute: Int): String {
    val t = clockTime(hour, minute)
    return "${t.digits.trimStart('0')} ${t.meridiem}"
}

private fun DayOfWeek.short(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** Card subtitle: "Weekdays · Mon, Tue, Wed, Thu, Fri", "Once", "Mon, Wed, Fri"… */
fun RepeatMode.detailText(): String = when (this) {
    RepeatMode.OneTime -> "Once"
    RepeatMode.Daily -> "Every day"
    RepeatMode.Weekdays, RepeatMode.Weekends ->
        "$displayName · " + toDays().sortedBy { it.value }.joinToString(", ") { it.short() }
    is RepeatMode.Custom ->
        if (days.size == 7) "Every day" else days.sortedBy { it.value }.joinToString(", ") { it.short() }
}

/** Eyebrow shown above the time on alarm cards: group and/or label. */
val Alarm.caption: String?
    get() = listOfNotNull(groupName, label.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifBlank { null }

/** "Today", "Tomorrow", "Friday" or "Oct 3" for the next-alarm banner. */
fun relativeDay(epochMillis: Long, today: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days in 2..6 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}
