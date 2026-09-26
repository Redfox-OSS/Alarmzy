package dev.redfox.alarmzy.ui.theme

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
import dev.redfox.alarmzy.R

/** Inter, from a single variable TTF (google/fonts, OFL) instantiated at each weight we use. */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
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

/** Type scale of the Alarmzy Obsidian design system. */
@Immutable
data class AlarmzyTypography(
    /** Large-page header title ("Alarms", "Settings"). */
    val screenTitle: TextStyle,
    /** Big alarm time on the alarm list cards. */
    val timeLarge: TextStyle,
    /** Digits inside the hour/minute tiles of the time picker. */
    val timeTile: TextStyle,
    /** Child alarm time inside an expanded group. */
    val timeMedium: TextStyle,
    /** Time in compact rows (group editor checklist). */
    val timeSmall: TextStyle,
    /** AM/PM suffix next to a time. */
    val meridiem: TextStyle,
    /** Card titles (group name, "Next alarm" value). */
    val title: TextStyle,
    /** Detail-screen header title ("New Alarm"). */
    val headerTitle: TextStyle,
    /** Row titles in form cards ("Ends at", "Group"). */
    val bodyLarge: TextStyle,
    val body: TextStyle,
    /** Settings row title, chips, small buttons. */
    val label: TextStyle,
    val labelStrong: TextStyle,
    /** Tiny uppercase eyebrow text ("NEXT ALARM", "APPEARANCE", alarm card label). */
    val caption: TextStyle,
    /** Uppercase section label above a form section ("START TIME", "REPEAT"). */
    val sectionLabel: TextStyle,
    /** Full-width CTA label. */
    val button: TextStyle,
)

val DefaultAlarmzyTypography = AlarmzyTypography(
    screenTitle = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp),
    timeLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.05).em),
    timeTile = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.025).em),
    timeMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Light, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.025).em),
    timeSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.025).em),
    meridiem = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.02.em),
    title = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    headerTitle = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
    bodyLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    label = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelStrong = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    caption = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.05.em),
    sectionLabel = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.05.em),
    button = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
)

val LocalAlarmzyTypography = staticCompositionLocalOf { DefaultAlarmzyTypography }

/** Material 3 type scale on Inter, for the few stock M3 components still in use (dialogs, menus, text fields). */
private val base = Typography()
val MaterialTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = base.titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = base.labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = InterFontFamily),
)
