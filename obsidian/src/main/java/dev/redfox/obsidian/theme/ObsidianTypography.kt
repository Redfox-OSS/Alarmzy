package dev.redfox.obsidian.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.redfox.obsidian.R

/** Inter, from a single variable TTF (google/fonts, OFL) instantiated at each weight we use. */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.ob_inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val InterFontFamily = FontFamily(
    inter(FontWeight.Light),
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

/** Type scale of the Obsidian design system. */
@Immutable
data class ObsidianTypography(
    /** Large-page header title ("Alarms", "Settings"). */
    val screenTitle: TextStyle,
    /** Hero numeric readout (big time on a card). Light weight, tight tracking. */
    val timeLarge: TextStyle,
    /** Digits inside picker tiles. */
    val timeTile: TextStyle,
    /** Secondary numeric readout (nested rows). */
    val timeMedium: TextStyle,
    /** Numeric readout in compact rows. */
    val timeSmall: TextStyle,
    /** Unit / AM-PM suffix next to a numeric readout. */
    val meridiem: TextStyle,
    /** Card titles. */
    val title: TextStyle,
    /** Detail-screen header title. */
    val headerTitle: TextStyle,
    /** Row titles in form cards. */
    val bodyLarge: TextStyle,
    val body: TextStyle,
    /** Settings row title, chips, small buttons. */
    val label: TextStyle,
    val labelStrong: TextStyle,
    /** Tiny uppercase eyebrow text ("NEXT ALARM"). */
    val caption: TextStyle,
    /** Uppercase section label above a form section ("REPEAT"). */
    val sectionLabel: TextStyle,
    /** Full-width CTA label. */
    val button: TextStyle,
) {
    companion object {
        val Default = obsidianTypography()
    }
}

/** Builds the type scale on [fontFamily] (Inter by default) — swap the face, keep the rhythm. */
fun obsidianTypography(fontFamily: FontFamily = InterFontFamily) = ObsidianTypography(
    screenTitle = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp),
    timeLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.05).em),
    timeTile = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.025).em),
    timeMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.025).em),
    timeSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.025).em),
    meridiem = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.02.em),
    title = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    headerTitle = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    label = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelStrong = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    caption = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.05.em),
    sectionLabel = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.05.em),
    button = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
)

val LocalObsidianTypography = staticCompositionLocalOf { ObsidianTypography.Default }

/** Material 3 type scale on [fontFamily], for the few stock M3 components (dialogs, menus, text fields). */
internal fun materialTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}
