package dev.redfox.alarmzy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant

/**
 * Color tokens of the Alarmzy Obsidian design system (see the Stitch DESIGN.md / Figma).
 * Always dark; neutrals come from the [DarkVariant], accent roles from the [AccentColor].
 */
@Immutable
data class AlarmzyColors(
    // Surfaces
    val background: Color,
    /** Primary card surface (group cards, settings sections). */
    val card: Color,
    /** Translucent card surface used over blurred/scrolling content (alarm cards). */
    val cardGlass: Color,
    /** Inset rows inside a card and standalone form rows. */
    val cardInner: Color,
    /** Icon tiles, inactive time tiles. */
    val tile: Color,
    /** Highest neutral — selection pill backgrounds, pressed states. */
    val tileHigh: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    /** Tint painted over the blurred backdrop of the top header. */
    val headerScrim: Color,
    /** Tint painted over the blurred backdrop of the floating nav bar. */
    val navScrim: Color,

    // Content
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // Accent
    val accent: Color,
    val accentLight: Color,
    /** Content on [accent] (primary CTA label). */
    val onAccent: Color,
    /** Content on [accentLight] (selected dial node, a.m. pill). */
    val onAccentLight: Color,
    val accentContainer: Color,
    val accentContainerStrong: Color,
    val accentBorder: Color,
    val accentGlow: Color,

    // Controls & status
    val switchTrackOff: Color,
    val switchThumbOff: Color,
    val warning: Color,
    val error: Color,
)

private data class AccentPalette(
    val main: Color,
    val light: Color,
    val onMain: Color,
    val onLight: Color,
)

private fun AccentColor.palette(): AccentPalette = when (this) {
    AccentColor.CRIMSON -> AccentPalette(Color(0xFFFF535A), Color(0xFFFFB3B0), Color(0xFF5B000D), Color(0xFF680010))
    AccentColor.ELECTRIC -> AccentPalette(Color(0xFF3D9BFF), Color(0xFFA9CEFF), Color(0xFF002A55), Color(0xFF00315F))
    AccentColor.SOLAR -> AccentPalette(Color(0xFFFFB800), Color(0xFFFFD983), Color(0xFF3F2B00), Color(0xFF402D00))
    AccentColor.EMERALD -> AccentPalette(Color(0xFF21D07A), Color(0xFF8DF3B7), Color(0xFF00391B), Color(0xFF003920))
    AccentColor.LUMINOUS -> AccentPalette(Color(0xFFEDEDF0), Color(0xFFFFFFFF), Color(0xFF1A1A1D), Color(0xFF1A1A1D))
}

private data class Neutrals(
    val background: Color,
    val card: Color,
    val cardInner: Color,
    val tile: Color,
    val tileHigh: Color,
)

private fun DarkVariant.neutrals(): Neutrals = when (this) {
    DarkVariant.DARK -> Neutrals(Color(0xFF131316), Color(0xFF201F22), Color(0xFF1C1B1E), Color(0xFF2A2A2D), Color(0xFF353438))
    DarkVariant.DARKER -> Neutrals(Color(0xFF0E0E11), Color(0xFF1A191D), Color(0xFF161519), Color(0xFF242328), Color(0xFF2F2E33))
    DarkVariant.DARKEST -> Neutrals(Color(0xFF000000), Color(0xFF121114), Color(0xFF0D0C0F), Color(0xFF1E1D21), Color(0xFF29282D))
}

/** Neutral secondary-text base; blended with the accent so labels pick up a subtle tint (#E7BCBA for Crimson). */
private val SecondaryTextBase = Color(0xFFCBC7CC)

fun alarmzyColors(accent: AccentColor, variant: DarkVariant): AlarmzyColors {
    val a = accent.palette()
    val n = variant.neutrals()
    val textSecondary = lerp(SecondaryTextBase, a.light, 0.45f)
    return AlarmzyColors(
        background = n.background,
        card = n.card,
        cardGlass = n.card.copy(alpha = 0.7f),
        cardInner = n.cardInner,
        tile = n.tile,
        tileHigh = n.tileHigh,
        border = Color.White.copy(alpha = 0.05f),
        borderStrong = Color.White.copy(alpha = 0.10f),
        divider = n.tileHigh.copy(alpha = 0.4f),
        headerScrim = n.background.copy(alpha = 0.8f),
        navScrim = n.card.copy(alpha = 0.8f),
        textPrimary = Color(0xFFE5E1E5),
        textSecondary = textSecondary,
        textTertiary = textSecondary.copy(alpha = 0.7f),
        textDisabled = Color(0xFF52525B),
        accent = a.main,
        accentLight = a.light,
        onAccent = a.onMain,
        onAccentLight = a.onLight,
        accentContainer = a.main.copy(alpha = 0.20f),
        accentContainerStrong = a.main.copy(alpha = 0.25f),
        accentBorder = a.light.copy(alpha = 0.30f),
        accentGlow = a.main.copy(alpha = 0.40f),
        switchTrackOff = Color(0xFF474952),
        switchThumbOff = Color(0xFFB7B8C2),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFFFB4AB),
    )
}

val LocalAlarmzyColors = staticCompositionLocalOf {
    alarmzyColors(AccentColor.DEFAULT, DarkVariant.DARK)
}
