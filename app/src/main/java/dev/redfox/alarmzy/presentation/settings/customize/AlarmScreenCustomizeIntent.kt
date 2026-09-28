package dev.redfox.alarmzy.presentation.settings.customize

import android.net.Uri
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition

sealed interface AlarmScreenCustomizeIntent {
    data class PickedBackgroundImage(val uri: Uri) : AlarmScreenCustomizeIntent
    data object RemoveBackgroundImage : AlarmScreenCustomizeIntent
    data class SetButtonLayout(val layout: AlarmButtonLayout) : AlarmScreenCustomizeIntent
    data class SetClockPosition(val position: AlarmClockPosition) : AlarmScreenCustomizeIntent
    data class SetShowQuote(val show: Boolean) : AlarmScreenCustomizeIntent
}
