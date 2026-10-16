package dev.redfox.alarmzy.ui.theme

import androidx.compose.runtime.Composable
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.obsidian.theme.ObsidianAccent
import dev.redfox.obsidian.theme.ObsidianNeutrals
import dev.redfox.obsidian.theme.ObsidianTheme

/**
 * Alarmzy's root theme: the Obsidian design system (`:obsidian` module) driven by the persisted
 * [AccentColor] / [DarkVariant] settings. Screens read tokens from `ObsidianTheme.colors` /
 * `ObsidianTheme.typography`.
 */
@Composable
fun AlarmzyTheme(
    accentColor: AccentColor = AccentColor.DEFAULT,
    darkVariant: DarkVariant = DarkVariant.DARK,
    content: @Composable () -> Unit
) {
    ObsidianTheme(
        accent = accentColor.toObsidian(),
        neutrals = darkVariant.toObsidian(),
        content = content
    )
}

fun AccentColor.toObsidian(): ObsidianAccent = when (this) {
    AccentColor.CRIMSON -> ObsidianAccent.Crimson
    AccentColor.ELECTRIC -> ObsidianAccent.Electric
    AccentColor.SOLAR -> ObsidianAccent.Solar
    AccentColor.EMERALD -> ObsidianAccent.Emerald
    AccentColor.LUMINOUS -> ObsidianAccent.Luminous
}

fun DarkVariant.toObsidian(): ObsidianNeutrals = when (this) {
    DarkVariant.DARK -> ObsidianNeutrals.Dark
    DarkVariant.DARKER -> ObsidianNeutrals.Darker
    DarkVariant.DARKEST -> ObsidianNeutrals.Amoled
}
