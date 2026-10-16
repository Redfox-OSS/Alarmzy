package dev.redfox.obsidian.effects

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.redfox.obsidian.theme.ObsidianTheme

// ---------------------------------------------------------------------------------------------
// Glass (backdrop blur)
// ---------------------------------------------------------------------------------------------

/**
 * Shared [HazeState] for the whole activity. Provide one (`rememberHazeState()`) at the root;
 * each screen marks its scrolling content with [glassSource], and headers / the floating nav bar
 * render it blurred via [glassEffect]. Without a provider, both modifiers are no-ops.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Registers this layout as backdrop content for [glassEffect] surfaces. */
@Composable
fun Modifier.glassSource(): Modifier {
    val state = LocalHazeState.current ?: return this
    return hazeSource(state)
}

/**
 * Frosted-glass surface: blurs whatever [glassSource] content is behind it and paints [tint] on top.
 * Clip the modifier chain to the surface's shape *before* this call. A glass surface must be a
 * sibling of the source, never inside it.
 */
@Composable
fun Modifier.glassEffect(tint: Color, blurRadius: Dp = 12.dp): Modifier {
    val state = LocalHazeState.current ?: return this
    val background = ObsidianTheme.colors.background
    return hazeEffect(
        state = state,
        style = HazeStyle(
            backgroundColor = background,
            tints = listOf(HazeTint(tint)),
            blurRadius = blurRadius,
            noiseFactor = 0f,
        )
    )
}

// ---------------------------------------------------------------------------------------------
// Glow
// ---------------------------------------------------------------------------------------------

/** Soft colored glow behind a shape (the design's `drop-shadow(0 0 Npx accent/35%)`). */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    radius: Dp = 12.dp,
    spread: Dp = 0.dp,
    offsetY: Dp = 0.dp,
): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(radius = radius, color = color, spread = spread, offset = DpOffset(0.dp, offsetY))
)

/** A reusable glow recipe: [alpha] multiplies the color's own alpha. See [ObGlows]. */
@Immutable
data class GlowSpec(
    val alpha: Float,
    val radius: Dp,
    val offsetY: Dp = 0.dp,
    val spread: Dp = 0.dp,
)

/** [glow] from a [GlowSpec] preset: `Modifier.glow(colors.accent, CircleShape, ObGlows.Cta)`. */
fun Modifier.glow(color: Color, shape: Shape, spec: GlowSpec): Modifier =
    glow(color.copy(alpha = color.alpha * spec.alpha), shape, spec.radius, spec.spread, spec.offsetY)

/**
 * The glow vocabulary. Color is almost always `colors.accent`; [Elevation] is the only one meant
 * for black. Apply the glow *before* `clip` in the modifier chain (it draws outside the bounds).
 */
object ObGlows {
    /** Switch track when on — tight, close halo. */
    val Switch = GlowSpec(alpha = 0.35f, radius = 6.dp)
    /** Checked checkbox — a small lifted glow. */
    val Checkbox = GlowSpec(alpha = 0.25f, radius = 6.dp, offsetY = 2.dp)
    /** Secondary accent action (Start/Pause pills). */
    val Action = GlowSpec(alpha = 0.45f, radius = 12.dp)
    /** FAB — lifted and strong. */
    val Fab = GlowSpec(alpha = 0.45f, radius = 14.dp, offsetY = 8.dp)
    /** Full-width primary CTA — wide and lifted. */
    val Cta = GlowSpec(alpha = 0.30f, radius = 18.dp, offsetY = 8.dp)
    /** Selected indicator halo (nav bar bubble). Use with `colors.accentGlow` at alpha 1. */
    val Halo = GlowSpec(alpha = 1f, radius = 18.dp)
    /** Big ambient bloom (empty-state badge, hero). Pair with [breathingGlow]. */
    val Ambient = GlowSpec(alpha = 0.34f, radius = 32.dp)
    /** Floating-surface drop shadow. Use with `Color.Black`. */
    val Elevation = GlowSpec(alpha = 0.35f, radius = 24.dp, offsetY = 12.dp)
}

/**
 * Ambient glow whose intensity breathes between [minAlpha] and [maxAlpha] (2.6s per half-cycle),
 * so a static hero element still feels alive.
 */
@Composable
fun Modifier.breathingGlow(
    color: Color,
    shape: Shape,
    radius: Dp = ObGlows.Ambient.radius,
    minAlpha: Float = 0.16f,
    maxAlpha: Float = 0.34f,
    periodMillis: Int = 2600,
): Modifier {
    val alpha by rememberInfiniteTransition(label = "breathingGlow").animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    return glow(color.copy(alpha = alpha), shape, radius)
}

/** Slow vertical float of ±[amplitude] (2.6s per half-cycle); pairs with [breathingGlow]. */
@Composable
fun Modifier.ambientFloat(amplitude: Dp = 4.dp, periodMillis: Int = 2600): Modifier {
    val float by rememberInfiniteTransition(label = "ambientFloat").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    return graphicsLayer { translationY = float * amplitude.toPx() }
}

// ---------------------------------------------------------------------------------------------
// Tactile press
// ---------------------------------------------------------------------------------------------

/**
 * Tactile click: the element dips to [pressedScale] while pressed instead of showing a ripple,
 * which reads better on the translucent glass surfaces, then springs back with a small overshoot
 * on release. [haptic] ticks on click; a long click always gives the long-press haptic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    pressedScale: Float = 0.97f,
    haptic: HapticFeedbackType? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val down = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (down) pressedScale else 1f,
        // Sink fast and firm; release with a gentle bounce.
        animationSpec = if (down) spring(dampingRatio = 1f, stiffness = 900f)
        else spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick?.let { longClick ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    longClick()
                }
            },
            onClick = {
                haptic?.let(haptics::performHapticFeedback)
                onClick()
            }
        )
}
