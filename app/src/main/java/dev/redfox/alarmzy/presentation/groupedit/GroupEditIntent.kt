package dev.redfox.alarmzy.presentation.groupedit

sealed interface GroupEditIntent {
    data class SetName(val name: String) : GroupEditIntent
    data class ToggleAlarmSelection(val alarmId: Long) : GroupEditIntent
    data object Save : GroupEditIntent
    data object Delete : GroupEditIntent
}

sealed interface GroupEditSideEffect {
    data object Saved : GroupEditSideEffect
    data object Deleted : GroupEditSideEffect
}
