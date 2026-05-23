package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Alarms("alarms", "Alarms", Icons.Default.Alarm),
    Groups("groups", "Groups", Icons.Default.Folder),
    Settings("settings", "Settings", Icons.Default.Settings)
}

object Routes {
    const val ALARM_EDIT = "alarm_edit"
    const val ALARM_EDIT_WITH_ID = "alarm_edit/{alarmId}"
    const val GROUP_EDIT = "group_edit"
    const val GROUP_EDIT_WITH_ID = "group_edit/{groupId}"
    const val RINGTONE_PICKER = "ringtone_picker"

    fun alarmEdit(alarmId: Long? = null): String =
        if (alarmId != null) "alarm_edit/$alarmId" else ALARM_EDIT

    fun groupEdit(groupId: Long? = null): String =
        if (groupId != null) "group_edit/$groupId" else GROUP_EDIT
}
