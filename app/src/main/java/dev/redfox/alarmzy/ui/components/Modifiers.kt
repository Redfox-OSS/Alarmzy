package dev.redfox.alarmzy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/**
 * Shared [HazeState] for the whole activity. Each screen marks its scrolling content with
 * [glassSource]; the screen header and the floating nav bar render it blurred via [glassEffect].
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
 * Clip the modifier chain to the surface's shape *before* this call.
 */
@Composable
fun Modifier.glassEffect(tint: Color, blurRadius: Dp = 12.dp): Modifier {
    val state = LocalHazeState.current ?: return this
    val background = AlarmzyTheme.colors.background
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

/**
 * Tactile click: the element dips to [pressedScale] while pressed instead of showing a ripple,
 * which reads better on the translucent glass surfaces.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    pressedScale: Float = 0.97f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick
        )
}
