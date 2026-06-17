package dev.redfox.alarmzy.presentation.alarmedit

import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode

data class AlarmEditUiState(
    val isNew: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0,
    val label: String = "",
    val repeatMode: RepeatMode = RepeatMode.OneTime,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "Default",
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val gradualVolumeIncrease: Boolean = false,
    val selectedGroupId: Long? = null,
    val availableGroups: List<AlarmGroup> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    // Series mode
    val isSeries: Boolean = false,
    val seriesEndHour: Int = 9,
    val seriesEndMinute: Int = 0,
    val seriesIntervalMinutes: Int = 10,
    // Duplicate confirmation
    val showDuplicateDialog: Boolean = false,
    val duplicateTimes: List<String> = emptyList()
) {
    private val startMinutes: Int get() = hour * 60 + minute
    private val endMinutes: Int get() = seriesEndHour * 60 + seriesEndMinute

    /** Times (minutes-of-day) the series would generate; empty when invalid. */
    val seriesTimes: List<Int>
        get() {
            if (!isSeries) return emptyList()
            if (seriesIntervalMinutes <= 0 || endMinutes <= startMinutes) return emptyList()
            val times = mutableListOf<Int>()
            var t = startMinutes
            while (t <= endMinutes && times.size <= MAX_SERIES_ALARMS) {
                times.add(t)
                t += seriesIntervalMinutes
            }
            // Always include the end time even if it's off the interval grid.
            if (times.lastOrNull() != endMinutes) times.add(endMinutes)
            return times
        }

    val seriesCount: Int get() = seriesTimes.size
    val seriesRangeValid: Boolean get() = endMinutes > startMinutes
    val seriesWithinCap: Boolean get() = seriesCount <= MAX_SERIES_ALARMS

    /** Whether the Save button can be pressed. */
    val canSave: Boolean
        get() = !isSaving && (!isSeries || (seriesRangeValid && seriesCount in 1..MAX_SERIES_ALARMS))

    companion object {
        const val MAX_SERIES_ALARMS = 100
    }
}
