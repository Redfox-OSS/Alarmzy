package dev.redfox.alarmzy.domain.model

data class AlarmGroup(
    val id: Long = 0,
    val name: String = "",
    val isEnabled: Boolean = true,
    val alarms: List<Alarm> = emptyList()
) {
    val alarmCount: Int get() = alarms.size
}
