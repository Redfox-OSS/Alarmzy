package dev.redfox.alarmzy.presentation.settings

import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.ThemeMode

sealed interface SettingsIntent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
    data class SetAccentColor(val accent: AccentColor) : SettingsIntent
    data class SetDefaultRingtone(val uri: String?, val name: String) : SettingsIntent
    data class SetDefaultSnoozeDuration(val minutes: Int) : SettingsIntent
    data class SetDefaultVibration(val enabled: Boolean) : SettingsIntent
    data class SetDefaultGradualVolume(val enabled: Boolean) : SettingsIntent
}
