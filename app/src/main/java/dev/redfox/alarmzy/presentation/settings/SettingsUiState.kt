package dev.redfox.alarmzy.presentation.settings

import dev.redfox.alarmzy.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultSnoozeDuration: Int = 5,
    val defaultVibration: Boolean = true,
    val defaultGradualVolume: Boolean = false
)
