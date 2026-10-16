package dev.redfox.obsidian.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.effects.ObGlows
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.motion.RollSwap
import dev.redfox.obsidian.motion.RollingText
import dev.redfox.obsidian.motion.popIn
import dev.redfox.obsidian.theme.ObShapes

/** Full-width signature pill CTA ("Create 7 alarms", "Create Group"). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = ObsidianTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .then(
                if (enabled) Modifier.glow(colors.accent, CircleShape, ObGlows.Cta)
                else Modifier
            )
            .pressable(enabled = enabled, pressedScale = 0.96f, haptic = HapticFeedbackType.Confirm, onClick = onClick)
            .clip(CircleShape)
            .background(colors.accent)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onAccent)
            Box(Modifier.size(8.dp))
        }
        // Counts in the label ("Create 7 alarms") roll as they change.
        RollingText(text, style = ObsidianTheme.typography.button, color = colors.onAccent)
    }
}

/**
 * 56dp action pill used by the Stopwatch/Timer controls: accent + glow when [primary]
 * (Pause, Resume, Start), dark neutral otherwise (Lap, Reset, Cancel).
 */
@Composable
fun ActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = ObsidianTheme.colors
    val content = if (primary) {
        if (colors.accent.luminance() > 0.6f) colors.onAccent else Color.White
    } else colors.textPrimary
    Row(
        modifier = modifier
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .then(
                if (primary && enabled) Modifier.glow(colors.accent, CircleShape, ObGlows.Action)
                else Modifier
            )
            .pressable(enabled = enabled, pressedScale = 0.93f, haptic = HapticFeedbackType.ContextClick, onClick = onClick)
            .clip(CircleShape)
            .background(if (primary) colors.accent else colors.cardInner)
            .border(1.dp, if (primary) Color.Transparent else colors.border, CircleShape)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label + glyph morph when the action changes (Pause <-> Resume).
        AnimatedContent(
            targetState = text to icon,
            contentKey = { it.first },
            transitionSpec = { RollSwap },
            label = "clockActionLabel"
        ) { (label, glyph) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (glyph != null) {
                    Icon(glyph, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                    Box(Modifier.size(8.dp))
                }
                Text(
                    label,
                    style = ObsidianTheme.typography.bodyLarge.copy(
                        fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = content
                )
            }
        }
    }
}

/** Small neutral pill button ("Edit Group", "Add custom sound"). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    contentColor: Color = ObsidianTheme.colors.textPrimary,
    containerColor: Color = ObsidianTheme.colors.tile,
) {
    Row(
        modifier = modifier
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
            Box(Modifier.size(6.dp))
        }
        Text(text, style = ObsidianTheme.typography.label, color = contentColor)
    }
}

/** OxygenOS-style squircle FAB with the accent gradient and glow. */
@Composable
fun ObFab(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors
    val shape = ObShapes.Fab
    // Arrives with a pop while the glyph twists into place.
    val spin = remember { Animatable(-135f) }
    LaunchedEffect(Unit) { spin.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
    Box(
        modifier = modifier
            .popIn(from = 0.5f)
            .glow(colors.accent, shape, ObGlows.Fab)
            .pressable(pressedScale = 0.88f, haptic = HapticFeedbackType.ContextClick, onClick = onClick)
            .size(56.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(colors.accent, lerp(colors.accent, colors.accentLight, 0.25f)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon),
            contentDescription = contentDescription,
            tint = if (colors.accent.luminance() > 0.6f) colors.onAccent else Color.White,
            modifier = Modifier.graphicsLayer { rotationZ = spin.value }
        )
    }
}

/** 44dp round tap target for header actions (back, overflow). */
@Composable
fun HeaderIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ObsidianTheme.colors.textPrimary,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = tint)
    }
}

/**
 * [HeaderIconButton] for actions whose glyph isn't in the Figma icon set yet
 * (delete, select-all, close) — uses the Material Symbols-like Rounded vectors.
 */
@Composable
fun HeaderIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ObsidianTheme.colors.textPrimary,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(enabled = enabled, pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}
