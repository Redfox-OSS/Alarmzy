package dev.redfox.alarmzy.presentation.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import dev.redfox.alarmzy.R

enum class BottomNavScreen(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int
) {
    Alarms("alarms", "Alarms", R.drawable.ic_nav_alarm),
    Timer("timer", "Timer", R.drawable.ic_nav_timer),
    Stopwatch("stopwatch", "Stopwatch", R.drawable.ic_nav_stopwatch),
    Groups("groups", "Groups", R.drawable.ic_nav_groups),
    Settings("settings", "Settings", R.drawable.ic_nav_settings);

    companion object {
        fun isTabRoute(route: String?): Boolean = entries.any { it.route == route }
    }
}

object Routes {
    const val ALARM_EDIT = "alarm_edit"
    const val ALARM_EDIT_WITH_ID = "alarm_edit/{alarmId}"
    const val GROUP_EDIT = "group_edit"
    const val GROUP_EDIT_WITH_ID = "group_edit/{groupId}"
    const val RINGTONE_PICKER = "ringtone_picker?uri={uri}&name={name}"
    const val CUSTOMIZE_ALARM_SCREEN = "customize_alarm_screen"

    fun alarmEdit(alarmId: Long? = null): String =
        if (alarmId != null) "alarm_edit/$alarmId" else ALARM_EDIT

    fun groupEdit(groupId: Long? = null): String =
        if (groupId != null) "group_edit/$groupId" else GROUP_EDIT

    fun ringtonePicker(uri: String?, name: String?): String =
        "ringtone_picker?uri=${Uri.encode(uri.orEmpty())}&name=${Uri.encode(name.orEmpty())}"
}
