package dev.redfox.alarmzy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

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
fun AlarmzySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: SwitchSize = SwitchSize.Large,
    showDot: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = AlarmzyTheme.colors
    val track by animateColorAsState(if (checked) colors.accent else colors.switchTrackOff, label = "track")
    val thumb by animateColorAsState(if (checked) Color.White else colors.switchThumbOff, label = "thumb")
    val travel = size.width - size.thumb - size.padding * 2
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .then(if (checked) Modifier.glow(colors.accent.copy(alpha = 0.35f), CircleShape, radius = 6.dp) else Modifier)
            .size(size.width, size.height)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange
            )
            .padding(size.padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(size.thumb)
                .clip(CircleShape)
                .background(thumb),
            contentAlignment = Alignment.Center
        ) {
            if (showDot && checked) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
            }
        }
    }
}
