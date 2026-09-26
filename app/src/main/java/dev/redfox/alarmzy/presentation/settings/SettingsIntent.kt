package dev.redfox.alarmzy.presentation.settings

import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant

sealed interface SettingsIntent {
    data class SetDarkVariant(val variant: DarkVariant) : SettingsIntent
    data class SetAccentColor(val accent: AccentColor) : SettingsIntent
    data class SetDefaultRingtone(val uri: String?, val name: String) : SettingsIntent
    data class SetDefaultSnoozeDuration(val minutes: Int) : SettingsIntent
    data class SetDefaultVibration(val enabled: Boolean) : SettingsIntent
    data class SetDefaultGradualVolume(val enabled: Boolean) : SettingsIntent
}
