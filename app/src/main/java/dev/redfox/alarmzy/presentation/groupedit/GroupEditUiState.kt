package dev.redfox.alarmzy.presentation.groupedit

import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.GroupIcon

data class GroupEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val isEnabled: Boolean = true,
    val color: String? = null,
    val icon: GroupIcon? = null,
    /**
     * Group-specific alarm sound: null = follow Settings, "" = the phone's default alarm sound,
     * otherwise a ringtone/file URI.
     */
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    /** Name of the Settings ringtone, shown while the group follows it. */
    val settingsRingtoneName: String = "Default",
    val settingsRingtoneUri: String? = null,
    val allAlarms: List<Alarm> = emptyList(),
    val selectedAlarmIds: Set<Long> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    /** Id of the group being edited (null when creating), to tell members from alarms in other groups. */
    val groupId: Long? = null
)
