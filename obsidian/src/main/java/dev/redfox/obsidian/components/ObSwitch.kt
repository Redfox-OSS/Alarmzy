package dev.redfox.obsidian.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.effects.ObGlows
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.motion.ObMotion

enum class SwitchSize(val width: Dp, val height: Dp, val thumb: Dp, val padding: Dp) {
    /** Alarm cards and group headers (52×30). */
    Large(52.dp, 30.dp, 24.dp, 3.dp),
    /** Settings rows (48×28). */
    Medium(48.dp, 28.dp, 24.dp, 2.dp),
    /** Child alarms inside an expanded group (46×26). */
    Small(46.dp, 26.dp, 22.dp, 2.dp),
}

/**
 * Pill switch from the design: accent track with a soft glow when on, grey track + grey thumb
 * when off. [showDot] adds the small accent dot in the thumb used on the alarm list cards.
 */
@Composable
fun ObSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: SwitchSize = SwitchSize.Large,
    showDot: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = ObsidianTheme.colors
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val track by animateColorAsState(if (checked) colors.accent else colors.switchTrackOff, ObMotion.snappy(), label = "track")
    val thumb by animateColorAsState(if (checked) Color.White else colors.switchThumbOff, ObMotion.snappy(), label = "thumb")
    val glowAlpha by animateFloatAsState(if (checked) 1f else 0f, ObMotion.gentle(), label = "glow")
    val travel = size.width - size.thumb - size.padding * 2
    // iOS-style squish: the thumb stretches while held, then snaps across with a little bounce.
    val stretch by animateDpAsState(
        targetValue = if (pressed && enabled) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "thumbStretch"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "thumbOffset"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (showDot && checked) 1f else 0f,
        animationSpec = ObMotion.bouncy(),
        label = "dot"
    )

    Box(
        modifier = modifier
            .glow(colors.accent.copy(alpha = glowAlpha), CircleShape, ObGlows.Switch)
            .size(size.width, size.height)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onCheckedChange(it)
                }
            )
            .padding(size.padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                // Grow towards the far side, so the thumb stays anchored where it rests.
                .offset(x = thumbOffset - stretch * (thumbOffset / travel))
                .size(size.thumb + stretch, size.thumb)
                .clip(CircleShape)
                .background(thumb),
            contentAlignment = Alignment.Center
        ) {
            if (dotScale > 0f) {
                Box(
                    Modifier
                        .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}
