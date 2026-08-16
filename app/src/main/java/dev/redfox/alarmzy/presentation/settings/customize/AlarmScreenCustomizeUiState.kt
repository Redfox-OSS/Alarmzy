package dev.redfox.alarmzy.presentation.settings.customize

import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition

data class AlarmScreenCustomizeUiState(
    val backgroundImagePath: String? = null,
    val buttonLayout: AlarmButtonLayout = AlarmButtonLayout.BOTH,
    val clockPosition: AlarmClockPosition = AlarmClockPosition.MIDDLE,
    val clockTextSizeSp: Int = 72
)
