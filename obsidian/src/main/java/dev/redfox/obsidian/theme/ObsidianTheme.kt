package dev.redfox.obsidian.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

/**
 * Root theme. Screens read tokens from [ObsidianTheme.colors] / [ObsidianTheme.typography];
 * a matching Material 3 scheme is also installed so the few stock M3 components left
 * (dialogs, dropdown menus, text fields, bottom sheets) blend in.
 */
@Composable
fun ObsidianTheme(
    colors: ObsidianColors = obsidianColors(),
    typography: ObsidianTypography = ObsidianTypography.Default,
    content: @Composable () -> Unit,
) {
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
    val material = remember(typography) {
        materialTypography(typography.body.fontFamily ?: InterFontFamily)
    }

    CompositionLocalProvider(
        LocalObsidianColors provides colors,
        LocalObsidianTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = material,
            shapes = ObsidianMaterialShapes,
            content = content
        )
    }
}

/** Convenience overload: pick an accent + neutral ramp (both have presets on their companions). */
@Composable
fun ObsidianTheme(
    accent: ObsidianAccent,
    neutrals: ObsidianNeutrals = ObsidianNeutrals.Dark,
    typography: ObsidianTypography = ObsidianTypography.Default,
    content: @Composable () -> Unit,
) {
    val colors = remember(accent, neutrals) { obsidianColors(accent, neutrals) }
    ObsidianTheme(colors = colors, typography = typography, content = content)
}

object ObsidianTheme {
    val colors: ObsidianColors
        @Composable @ReadOnlyComposable get() = LocalObsidianColors.current

    val typography: ObsidianTypography
        @Composable @ReadOnlyComposable get() = LocalObsidianTypography.current
}

/** Squircle radii. */
object ObShapes {
    val Card = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(24.dp)
    val Tile = RoundedCornerShape(16.dp)
    val Small = RoundedCornerShape(12.dp)
    val Fab = RoundedCornerShape(18.dp)
    val Checkbox = RoundedCornerShape(8.dp)
    val Pill = CircleShape
}

/** Spacing scale. Every page uses [Gutter] as its horizontal edge padding. */
object ObSpacing {
    val XXS = 4.dp
    val XS = 6.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val Gutter = 20.dp
    val XL = 24.dp
    val XXL = 32.dp
}

/** Screen horizontal gutter used by every page. */
val ScreenGutter = ObSpacing.Gutter

private val ObsidianMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
