package dev.redfox.alarmzy.presentation.settings

import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant

data class SettingsUiState(
    val darkVariant: DarkVariant = DarkVariant.DARK,
    val accentColor: AccentColor = AccentColor.DEFAULT,
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneName: String = "Default",
    val defaultSnoozeDuration: Int = 5,
    val defaultVibration: Boolean = true,
    val defaultGradualVolume: Boolean = false,
    val isPro: Boolean = false
)
