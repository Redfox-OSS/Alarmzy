package dev.redfox.alarmzy.domain.model

data class Alarm(
    val id: Long = 0,
    val hour: Int = 8,
    val minute: Int = 0,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.OneTime,
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val gradualVolumeIncrease: Boolean = false,
    val groupId: Long? = null,
    val groupName: String? = null,
    val groupColor: String? = null,
    val nextTriggerTimeMillis: Long = 0
) {
    val timeFormatted: String
        get() {
            val period = if (hour < 12) "AM" else "PM"
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return "%d:%02d %s".format(h12, minute, period)
        }
}
