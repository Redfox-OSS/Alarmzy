package dev.redfox.alarmzy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant

/**
 * Root theme. Screens read tokens from [AlarmzyTheme.colors] / [AlarmzyTheme.typography];
 * a matching Material 3 scheme is also installed so the few stock M3 components left
 * (dialogs, dropdown menus, text fields, bottom sheets) blend in.
 */
@Composable
fun AlarmzyTheme(
    accentColor: AccentColor = AccentColor.DEFAULT,
    darkVariant: DarkVariant = DarkVariant.DARK,
    content: @Composable () -> Unit
) {
    val colors = remember(accentColor, darkVariant) { alarmzyColors(accentColor, darkVariant) }
    val colorScheme = remember(colors) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.accentLight,
            secondary = colors.accentLight,
            onSecondary = colors.onAccentLight,
            secondaryContainer = colors.tileHigh,
            onSecondaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.background,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.tile,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = colors.background,
            surfaceContainerLow = colors.cardInner,
            surfaceContainer = colors.card,
            surfaceContainerHigh = colors.card,
            surfaceContainerHighest = colors.tile,
            outline = colors.borderStrong,
            outlineVariant = colors.border,
            error = colors.error,
        )
    }

    CompositionLocalProvider(
        LocalAlarmzyColors provides colors,
        LocalAlarmzyTypography provides DefaultAlarmzyTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            shapes = AlarmzyMaterialShapes,
            content = content
        )
    }
}

object AlarmzyTheme {
    val colors: AlarmzyColors
        @Composable @ReadOnlyComposable get() = LocalAlarmzyColors.current

    val typography: AlarmzyTypography
        @Composable @ReadOnlyComposable get() = LocalAlarmzyTypography.current
}

private val AlarmzyMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
