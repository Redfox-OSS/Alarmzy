package dev.redfox.alarmzy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/** Rounded choice pill (repeat frequency, snooze duration…). */
@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    val bg by animateColorAsState(if (selected) colors.accentContainerStrong else colors.cardInner, label = "pillBg")
    val border by animateColorAsState(if (selected) colors.accentBorder else colors.border, label = "pillBorder")
    Box(
        modifier = modifier
            .popOnSelect(selected)
            .pressable(role = Role.RadioButton, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 17.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = AlarmzyTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = AlarmzyTheme.typography.body.letterSpacing
            ),
            color = if (selected) colors.accentLight else colors.textSecondary
        )
    }
}

/** Circular single-letter weekday toggle for custom repeat. */
@Composable
fun DayChip(
    letter: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    val bg by animateColorAsState(if (selected) colors.accentContainerStrong else colors.cardInner, label = "dayBg")
    Box(
        modifier = modifier
            .size(40.dp)
            .popOnSelect(selected, amount = 0.12f)
            .pressable(pressedScale = 0.88f, role = Role.Checkbox, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) colors.accentBorder else colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            style = AlarmzyTheme.typography.labelStrong,
            color = if (selected) colors.accentLight else colors.textSecondary
        )
    }
}

data class SegmentOption(val label: String, val trailingIcon: ImageVector? = null)

/**
 * Two-or-more option segmented pill (Single / Series). The selected segment slides
 * between positions and shows a check mark, as in the design.
 */
@Composable
fun SegmentedPill(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(colors.cardInner)
            .border(1.dp, colors.border, CircleShape)
            .padding(5.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val haptics = LocalHapticFeedback.current
        val position by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
            label = "segmentIndicator"
        )
        Box(
            Modifier
                .offset { IntOffset((segmentWidth * position).roundToPx(), 0) }
                .graphicsLayer {
                    // Liquid stretch while travelling between segments.
                    val stretch = abs(selectedIndex - position).coerceAtMost(1f)
                    scaleX = 1f + 0.18f * stretch
                    scaleY = 1f - 0.08f * stretch
                }
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.accentContainerStrong)
                .border(1.dp, colors.accentBorder, CircleShape)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(index)
                            }
                        ),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn(AzMotion.bouncy()) + fadeIn() + expandHorizontally(AzMotion.snappy()),
                        exit = scaleOut() + fadeOut() + shrinkHorizontally(AzMotion.snappy())
                    ) {
                        Row {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.accentLight
                            )
                            Box(Modifier.size(6.dp))
                        }
                    }
                    Text(
                        option.label,
                        style = AlarmzyTheme.typography.bodyLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) colors.accentLight else colors.textSecondary
                    )
                    if (option.trailingIcon != null) {
                        Box(Modifier.size(6.dp))
                        Icon(
                            option.trailingIcon,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Squircle checkbox from the group editor. */
@Composable
fun AzCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val bg by animateColorAsState(if (checked) colors.accent else Color.Transparent, AzMotion.snappy(), label = "checkBg")
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .popOnSelect(checked, amount = 0.14f)
            .then(if (checked) Modifier.glow(colors.accent.copy(alpha = 0.25f), shape, radius = 6.dp, offsetY = 2.dp) else Modifier)
            .size(24.dp)
            .clip(shape)
            .background(bg)
            .then(if (!checked) Modifier.border(2.dp, colors.textDisabled, shape) else Modifier)
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onValueChange = {
                        haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        onCheckedChange(it)
                    }
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(AzMotion.bouncy(), initialScale = 0.3f) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut()
        ) {
            Icon(painterResource(R.drawable.ic_check_bold), contentDescription = null, tint = colors.onAccent)
        }
    }
}
