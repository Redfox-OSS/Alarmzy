package dev.redfox.alarmzy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant

private fun light(
    primary: Long, onPrimary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, secondaryContainer: Long, onSecondaryContainer: Long,
    tertiary: Long, tertiaryContainer: Long, onTertiaryContainer: Long
): ColorScheme = lightColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer)
)

private fun dark(
    primary: Long, onPrimary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, secondaryContainer: Long, onSecondaryContainer: Long,
    tertiary: Long, tertiaryContainer: Long, onTertiaryContainer: Long
): ColorScheme = darkColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer)
)

private val PurpleLight = light(
    0xFF6750A4, 0xFFFFFFFF, 0xFFEADDFF, 0xFF21005D,
    0xFF625B71, 0xFFE8DEF8, 0xFF1D192B,
    0xFF7D5260, 0xFFFFD8E4, 0xFF31111D
)
private val PurpleDark = dark(
    0xFFD0BCFF, 0xFF381E72, 0xFF4F378B, 0xFFEADDFF,
    0xFFCCC2DC, 0xFF4A4458, 0xFFE8DEF8,
    0xFFEFB8C8, 0xFF633B48, 0xFFFFD8E4
)

private val BlueLight = light(
    0xFF415F91, 0xFFFFFFFF, 0xFFD6E3FF, 0xFF001B3E,
    0xFF565F71, 0xFFDAE2F9, 0xFF131C2B,
    0xFF705575, 0xFFFAD8FD, 0xFF28132E
)
private val BlueDark = dark(
    0xFFAAC7FF, 0xFF0A305F, 0xFF284777, 0xFFD6E3FF,
    0xFFBEC6DC, 0xFF3E4759, 0xFFDAE2F9,
    0xFFDDBCE0, 0xFF573E5C, 0xFFFAD8FD
)

private val TealLight = light(
    0xFF00696E, 0xFFFFFFFF, 0xFF6FF6FE, 0xFF002022,
    0xFF4A6364, 0xFFCCE8E9, 0xFF051F20,
    0xFF4B607C, 0xFFD3E4FF, 0xFF041C35
)
private val TealDark = dark(
    0xFF4CD9E0, 0xFF00373A, 0xFF004F53, 0xFF6FF6FE,
    0xFFB0CCCD, 0xFF324B4C, 0xFFCCE8E9,
    0xFFB3C8E8, 0xFF334863, 0xFFD3E4FF
)

private val GreenLight = light(
    0xFF386A20, 0xFFFFFFFF, 0xFFB7F397, 0xFF042100,
    0xFF55624C, 0xFFD9E7CB, 0xFF131F0D,
    0xFF19686A, 0xFFBCEBEC, 0xFF002021
)
private val GreenDark = dark(
    0xFF9CD67D, 0xFF0C3900, 0xFF205107, 0xFFB7F397,
    0xFFBDCBAF, 0xFF3E4A35, 0xFFD9E7CB,
    0xFFA0CFD0, 0xFF1E4E50, 0xFFBCEBEC
)

private val OrangeLight = light(
    0xFF8F4C34, 0xFFFFFFFF, 0xFFFFDBCF, 0xFF3A0B00,
    0xFF77574E, 0xFFFFDBCF, 0xFF2C150D,
    0xFF6C5D2F, 0xFFF5E1A7, 0xFF231B00
)
private val OrangeDark = dark(
    0xFFFFB59D, 0xFF561F0B, 0xFF723520, 0xFFFFDBCF,
    0xFFE7BDB1, 0xFF5D4036, 0xFFFFDBCF,
    0xFFD8C58D, 0xFF524419, 0xFFF5E1A7
)

private val RoseLight = light(
    0xFF8E4957, 0xFFFFFFFF, 0xFFFFD9DF, 0xFF3B0716,
    0xFF75565B, 0xFFFFD9DF, 0xFF2C151A,
    0xFF795831, 0xFFFFDDB7, 0xFF2B1700
)
private val RoseDark = dark(
    0xFFFFB2BE, 0xFF561D29, 0xFF72333F, 0xFFFFD9DF,
    0xFFE5BDC2, 0xFF5C3F43, 0xFFFFD9DF,
    0xFFEABF8F, 0xFF5E421D, 0xFFFFDDB7
)

private val RedLight = light(
    0xFFBA1A1A, 0xFFFFFFFF, 0xFFFFDAD6, 0xFF410002,
    0xFF775652, 0xFFFFDAD6, 0xFF2C1512,
    0xFF6D5D2F, 0xFFF6E1A6, 0xFF231B00
)
private val RedDark = dark(
    0xFFFFB4AB, 0xFF690005, 0xFF93000A, 0xFFFFDAD6,
    0xFFE7BDB8, 0xFF5D3F3B, 0xFFFFDAD6,
    0xFFD9C58D, 0xFF554500, 0xFFF6E1A6
)

private val IndigoLight = light(
    0xFF4655B3, 0xFFFFFFFF, 0xFFDFE0FF, 0xFF00105C,
    0xFF5B5D75, 0xFFE0E1FB, 0xFF181A2C,
    0xFF75546F, 0xFFFFD6F4, 0xFF2C122A
)
private val IndigoDark = dark(
    0xFFBEC2FF, 0xFF1B2088, 0xFF333A9F, 0xFFDFE0FF,
    0xFFC4C5DD, 0xFF434559, 0xFFE0E1FB,
    0xFFE4B9DA, 0xFF5C3C58, 0xFFFFD6F4
)

private val AmberLight = light(
    0xFF7C5800, 0xFFFFFFFF, 0xFFFFDEA6, 0xFF261900,
    0xFF6E5C3F, 0xFFF9E0BB, 0xFF251A04,
    0xFF3F6647, 0xFFC0EDC3, 0xFF00210A
)
private val AmberDark = dark(
    0xFFF7BD48, 0xFF412D00, 0xFF5E4200, 0xFFFFDEA6,
    0xFFDCC3A1, 0xFF554527, 0xFFF9E0BB,
    0xFFA5D0A6, 0xFF264F30, 0xFFC0EDC3
)

private val CyanLight = light(
    0xFF006874, 0xFFFFFFFF, 0xFF97F0FF, 0xFF001F24,
    0xFF4A6267, 0xFFCDE7EC, 0xFF051F23,
    0xFF525E7D, 0xFFDAE2FF, 0xFF0E1B37
)
private val CyanDark = dark(
    0xFF4DD8EA, 0xFF00363D, 0xFF004E58, 0xFF97F0FF,
    0xFFB1CBD0, 0xFF334B4F, 0xFFCDE7EC,
    0xFFBAC6EA, 0xFF3A4664, 0xFFDAE2FF
)

/** Returns the fixed scheme for an accent, or null for [AccentColor.DYNAMIC]. */
fun accentColorScheme(accent: AccentColor, darkTheme: Boolean): ColorScheme? = when (accent) {
    AccentColor.DYNAMIC -> null
    AccentColor.PURPLE -> if (darkTheme) PurpleDark else PurpleLight
    AccentColor.BLUE -> if (darkTheme) BlueDark else BlueLight
    AccentColor.TEAL -> if (darkTheme) TealDark else TealLight
    AccentColor.GREEN -> if (darkTheme) GreenDark else GreenLight
    AccentColor.ORANGE -> if (darkTheme) OrangeDark else OrangeLight
    AccentColor.ROSE -> if (darkTheme) RoseDark else RoseLight
    AccentColor.RED -> if (darkTheme) RedDark else RedLight
    AccentColor.INDIGO -> if (darkTheme) IndigoDark else IndigoLight
    AccentColor.AMBER -> if (darkTheme) AmberDark else AmberLight
    AccentColor.CYAN -> if (darkTheme) CyanDark else CyanLight
}

/**
 * Applies a darkness-intensity overlay on top of an already-resolved dark
 * [ColorScheme]. Only the neutral/surface roles are touched — accent hues
 * (primary/secondary/tertiary and their containers) are left untouched so
 * the chosen accent still shows through at any intensity. [DarkVariant.DARK]
 * returns [scheme] unchanged (Material 3's default dark ramp).
 */
fun applyDarkVariant(scheme: ColorScheme, variant: DarkVariant): ColorScheme = when (variant) {
    DarkVariant.DARK -> scheme
    DarkVariant.DARKER -> scheme.copy(
        background = Color(0xFF121212),
        surface = Color(0xFF121212),
        surfaceDim = Color(0xFF0A0A0A),
        surfaceBright = Color(0xFF383838),
        surfaceContainerLowest = Color(0xFF0A0A0A),
        surfaceContainerLow = Color(0xFF1A1A1A),
        surfaceContainer = Color(0xFF1E1E1E),
        surfaceContainerHigh = Color(0xFF282828),
        surfaceContainerHighest = Color(0xFF333333),
        surfaceVariant = Color(0xFF2A2A2A),
        onSurface = Color(0xFFE4E2E6),
        onSurfaceVariant = Color(0xFFC6C5C9),
        onBackground = Color(0xFFE4E2E6),
        outline = Color(0xFF8E8E93),
        outlineVariant = Color(0xFF444444)
    )
    DarkVariant.DARKEST -> scheme.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceBright = Color(0xFF2C2C2C),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1C1C1C),
        surfaceContainerHighest = Color(0xFF262626),
        surfaceVariant = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE4E2E6),
        onSurfaceVariant = Color(0xFFC6C5C9),
        onBackground = Color(0xFFE4E2E6),
        outline = Color(0xFF8E8E93),
        outlineVariant = Color(0xFF3A3A3A)
    )
}
