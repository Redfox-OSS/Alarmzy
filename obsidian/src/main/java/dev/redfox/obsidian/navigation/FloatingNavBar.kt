package dev.redfox.obsidian.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.redfox.obsidian.effects.ObGlows
import dev.redfox.obsidian.effects.glassEffect
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.ScreenGutter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private val NavBarHeight = 64.dp
private val NavBarBottomGap = 12.dp
private val IndicatorSize = 48.dp

/**
 * Vertical space tab screens must leave at the bottom of their scrolling content so the
 * last item can scroll clear of the [FloatingNavBar].
 */
@Composable
fun floatingNavClearance(): Dp =
    NavBarHeight + NavBarBottomGap + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** A tab of the [FloatingNavBar]. [motion] is the little animation its icon plays when selected. */
@Immutable
data class FloatingNavItem(
    @param:DrawableRes val icon: Int,
    val label: String,
    val motion: NavIconMotion = NavIconMotion.Pop,
)

/** Icon animations for [FloatingNavItem]. Every one starts with the shared dip-and-pop beat. */
enum class NavIconMotion {
    /** Just the shared dip-and-pop. */
    Pop,
    /** A decaying side-to-side shake (a ringing bell / alarm clock). */
    Shake,
    /** Half a turn (an hourglass). Use only for glyphs that look the same upside down. */
    Flip,
    /** Squashed down then hopping up with a twist (a stopwatch crown being pressed). */
    Press,
    /** A jelly wobble (stacks, folders). */
    Jelly,
    /** A slow half-turn (gears; symmetric glyphs only). */
    Turn,
}

/**
 * OxygenOS-style floating frosted-glass pill nav with icon-only tabs. Overlay it at the bottom
 * of a `Box` above the screen content, and pad scrolling content by [floatingNavClearance].
 *
 * A single "liquid" halo slides between tabs on a bouncy spring, stretching with its speed and
 * squashing as it lands; the newly selected icon plays its [FloatingNavItem.motion].
 * Pass `selectedIndex = -1` when no tab is selected (the halo hides).
 */
@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors

    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            position.animateTo(
                selectedIndex.toFloat(),
                spring(dampingRatio = 0.58f, stiffness = 300f)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = NavBarBottomGap),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .glow(Color.Black, CircleShape, ObGlows.Elevation)
                .height(NavBarHeight)
                .clip(CircleShape)
                .glassEffect(colors.navScrim)
                .border(1.dp, colors.borderStrong, CircleShape)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val slot = maxWidth / items.size
            if (selectedIndex >= 0) {
                LiquidIndicator(position = position, slot = slot)
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    NavItem(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * The selected-tab halo. It is one shared element (not one per tab) so it can travel: its width
 * grows with its velocity and it flattens slightly, like a drop of liquid being dragged.
 */
@Composable
private fun LiquidIndicator(position: Animatable<Float, AnimationVector1D>, slot: Dp) {
    val colors = ObsidianTheme.colors
    Box(
        Modifier
            .offset {
                val x = (slot * position.value).toPx() + (slot - IndicatorSize).toPx() / 2f
                IntOffset(x.toInt(), 0)
            }
            .graphicsLayer {
                position.value // re-run this block every frame of the slide
                // Velocity is in tabs per second; a fast hop stretches up to ~55% wider.
                val stretch = (abs(position.velocity) / 9f).coerceAtMost(0.55f)
                scaleX = 1f + stretch
                scaleY = 1f - stretch * 0.28f
            }
            .size(IndicatorSize)
            .glow(colors.accentGlow, CircleShape, ObGlows.Halo)
            .clip(CircleShape)
            .background(colors.accentContainer)
            .border(1.dp, colors.accentLight.copy(alpha = 0.4f), CircleShape)
    )
}

@Composable
private fun NavItem(
    item: FloatingNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors
    val haptics = LocalHapticFeedback.current
    val tint by animateColorAsState(
        if (selected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.7f),
        animationSpec = tween(220),
        label = "navTint"
    )

    // Replays on every selection, and on re-tapping the current tab.
    var plays by remember { mutableIntStateOf(0) }
    val initial = remember { booleanArrayOf(true) }
    LaunchedEffect(selected) {
        if (initial[0]) initial[0] = false
        else if (selected) plays++
    }

    val motion = remember { IconMotion() }
    LaunchedEffect(plays) {
        if (plays > 0) motion.play(item.motion)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .pressable(pressedScale = 0.86f, role = Role.Tab) {
                haptics.performHapticFeedback(
                    if (selected) HapticFeedbackType.ContextClick else HapticFeedbackType.SegmentTick
                )
                if (selected) plays++ else onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(item.icon),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = motion.scale.value * motion.squashX
                scaleY = motion.scale.value * motion.squashY
                rotationZ = motion.rotation.value
                translationY = motion.lift.value * density
            }
        )
    }
}

/** Per-icon transform channels, driven by [play]. */
private class IconMotion {
    val scale = Animatable(1f)
    val rotation = Animatable(0f)
    /** In dp; negative lifts the icon. */
    val lift = Animatable(0f)
    /** 1 = fully squashed (wide and short), 0 = at rest; overshoots for a jelly wobble. */
    val squash = Animatable(0f)

    val squashX get() = 1f + 0.26f * squash.value
    val squashY get() = 1f - 0.22f * squash.value

    suspend fun play(kind: NavIconMotion) = coroutineScope {
        // Shared beat: a quick dip, then an overshooting pop back to size.
        launch {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(dampingRatio = 0.36f, stiffness = 420f))
        }
        when (kind) {
            NavIconMotion.Pop -> Unit
            NavIconMotion.Shake -> launch {
                rotation.snapTo(0f)
                rotation.animateTo(0f, keyframes {
                    durationMillis = 720
                    -24f at 70
                    20f at 160
                    -16f at 250
                    12f at 340
                    -8f at 430
                    4f at 530
                    -2f at 620
                })
            }
            // Symmetric glyph, so it can reset invisibly.
            NavIconMotion.Flip -> launch {
                rotation.snapTo(0f)
                rotation.animateTo(180f, spring(dampingRatio = 0.5f, stiffness = 220f))
                rotation.snapTo(0f)
            }
            NavIconMotion.Press -> launch {
                launch {
                    lift.snapTo(0f)
                    lift.animateTo(2.5f, tween(70))
                    lift.animateTo(-4f, spring(dampingRatio = 1f, stiffness = 900f))
                    lift.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 400f))
                }
                rotation.snapTo(-28f)
                rotation.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f))
            }
            NavIconMotion.Jelly -> launch {
                squash.snapTo(1f)
                squash.animateTo(0f, spring(dampingRatio = 0.28f, stiffness = 360f))
            }
            // e.g. an 8-tooth gear: half a turn lands on an identical pose.
            NavIconMotion.Turn -> launch {
                rotation.snapTo(0f)
                rotation.animateTo(180f, spring(dampingRatio = 0.55f, stiffness = 110f))
                rotation.snapTo(0f)
            }
        }
    }
}
