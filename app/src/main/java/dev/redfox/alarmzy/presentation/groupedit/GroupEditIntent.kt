package dev.redfox.alarmzy.presentation.groupedit

import dev.redfox.alarmzy.domain.model.GroupIcon

sealed interface GroupEditIntent {
    data class SetName(val name: String) : GroupEditIntent
    data class SetColor(val color: String?) : GroupEditIntent
    data class SetIcon(val icon: GroupIcon?) : GroupEditIntent
    /** [uri] "" = the phone's default alarm sound. */
    data class SetRingtone(val uri: String, val name: String) : GroupEditIntent
    data object UseSettingsRingtone : GroupEditIntent
    data class ToggleAlarmSelection(val alarmId: Long) : GroupEditIntent
    data object Save : GroupEditIntent
    data object Delete : GroupEditIntent
}

sealed interface GroupEditSideEffect {
    data object Saved : GroupEditSideEffect
    data object Deleted : GroupEditSideEffect
}
