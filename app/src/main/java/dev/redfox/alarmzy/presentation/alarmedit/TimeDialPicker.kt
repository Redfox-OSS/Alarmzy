package dev.redfox.alarmzy.presentation.alarmedit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import dev.redfox.alarmzy.ui.components.AzMotion
import dev.redfox.alarmzy.ui.components.RollingText
import dev.redfox.alarmzy.ui.components.popOnSelect

private enum class TimeField { HOUR, MINUTE }

/**
 * Time picker from the design: tappable hour/minute squircle tiles, a vertical a.m./p.m.
 * pill, and a radial dial you tap or drag. Picking an hour on the dial advances to minutes.
 * [hour] is 0–23.
 */
@Composable
fun TimeDialPicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 232.dp,
) {
    var field by rememberSaveable { mutableStateOf(TimeField.HOUR) }
    val isPm = hour >= 12
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TimeTile(
                text = "%02d".format(hour12),
                active = field == TimeField.HOUR,
                description = "Hour",
                onClick = { field = TimeField.HOUR }
            )
            Text(
                ":",
                style = AlarmzyTheme.typography.timeTile.copy(fontSize = 32.sp),
                color = AlarmzyTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            TimeTile(
                text = "%02d".format(minute),
                active = field == TimeField.MINUTE,
                description = "Minute",
                onClick = { field = TimeField.MINUTE }
            )
            Spacer(Modifier.width(12.dp))
            MeridiemToggle(
                isPm = isPm,
                onChange = { pm -> onTimeChange(hour % 12 + if (pm) 12 else 0, minute) }
            )
        }
        Spacer(Modifier.height(20.dp))
        ClockDial(
            showMinutes = field == TimeField.MINUTE,
            hour12 = hour12,
            minute = minute,
            size = dialSize,
            onHourPicked = { h12 -> onTimeChange(h12 % 12 + if (isPm) 12 else 0, minute) },
            onMinutePicked = { m -> onTimeChange(hour, m) },
            onGestureEnd = { if (field == TimeField.HOUR) field = TimeField.MINUTE }
        )
    }
}

@Composable
private fun TimeTile(text: String, active: Boolean, description: String, onClick: () -> Unit) {
    val colors = AlarmzyTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val bg by animateColorAsState(if (active) colors.accentContainer else colors.tile, label = "tileBg")
    val fg by animateColorAsState(if (active) colors.accentLight else colors.textPrimary, label = "tileFg")
    Box(
        modifier = Modifier
            .semantics { contentDescription = "$description $text" }
            .popOnSelect(active, amount = 0.05f)
            .then(if (active) Modifier.glow(colors.accent.copy(alpha = 0.12f), shape, radius = 15.dp, offsetY = 8.dp) else Modifier)
            .pressable(pressedScale = 0.94f, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .size(94.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (active) colors.accentLight.copy(alpha = 0.4f) else colors.border, shape),
        contentAlignment = Alignment.Center
    ) {
        RollingText(text, style = AlarmzyTheme.typography.timeTile, color = fg)
    }
}

@Composable
private fun MeridiemToggle(isPm: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AlarmzyTheme.colors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardInner)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(false to "a.m.", true to "p.m.").forEach { (pm, label) ->
            val selected = pm == isPm
            val bg by animateColorAsState(if (selected) colors.accentLight else Color.Transparent, label = "meridiemBg")
            Box(
                modifier = Modifier
                    .popOnSelect(selected, amount = 0.1f)
                    .pressable(pressedScale = 0.9f, haptic = HapticFeedbackType.SegmentTick) { onChange(pm) }
                    .clip(CircleShape)
                    .background(bg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = AlarmzyTheme.typography.label.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    color = if (selected) colors.onAccentLight else colors.textSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ClockDial(
    showMinutes: Boolean,
    hour12: Int,
    minute: Int,
    size: Dp,
    onHourPicked: (Int) -> Unit,
    onMinutePicked: (Int) -> Unit,
    onGestureEnd: () -> Unit,
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val haptics = LocalHapticFeedback.current
    val nodeSize = 34.dp
    val numberInset = 24.dp

    // Continuous angle (degrees, 0 = 12 o'clock, clockwise) so the hand takes the short way round.
    val targetDeg = if (showMinutes) minute * 6f else (hour12 % 12) * 30f
    val unwrapped = remember { floatArrayOf(targetDeg) }
    val continuousDeg = remember(targetDeg) {
        unwrapped[0] += ((targetDeg - unwrapped[0]) % 360f + 540f) % 360f - 180f
        unwrapped[0]
    }
    val angle by animateFloatAsState(
        continuousDeg,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "dialAngle"
    )

    // pointerInput(Unit) outlives recompositions, so read the latest values/callbacks through these.
    val currentShowMinutes by rememberUpdatedState(showMinutes)
    val currentOnHourPicked by rememberUpdatedState(onHourPicked)
    val currentOnMinutePicked by rememberUpdatedState(onMinutePicked)
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val lastValue = remember { mutableFloatStateOf(-1f) }

    // Switching hours <-> minutes re-deals the numerals clockwise around the face.
    val deal = remember { Animatable(1f) }
    val firstFace = remember { booleanArrayOf(true) }
    LaunchedEffect(showMinutes) {
        if (firstFace[0]) {
            firstFace[0] = false
            return@LaunchedEffect
        }
        deal.snapTo(0f)
        deal.animateTo(1f, tween(DealMillis, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .glow(Color.Black.copy(alpha = 0.45f), CircleShape, radius = 25.dp, offsetY = 12.dp)
            .size(size)
            .clip(CircleShape)
            .background(colors.cardInner)
            .border(1.dp, colors.border, CircleShape)
            .pointerInput(Unit) {
                fun pick(pos: Offset) {
                    val dx = pos.x - this.size.width / 2f
                    val dy = pos.y - this.size.height / 2f
                    val deg = ((Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360.0) % 360.0).toFloat()
                    if (currentShowMinutes) {
                        val m = (deg / 6f).roundToInt() % 60
                        if (m.toFloat() != lastValue.floatValue) {
                            lastValue.floatValue = m.toFloat()
                            if (m % 5 == 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnMinutePicked(m)
                        }
                    } else {
                        val idx = (deg / 30f).roundToInt() % 12
                        val h = if (idx == 0) 12 else idx
                        if (h.toFloat() != lastValue.floatValue) {
                            lastValue.floatValue = h.toFloat()
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnHourPicked(h)
                        }
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    lastValue.floatValue = -1f
                    pick(down.position)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                pick(change.position)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    currentOnGestureEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val handColor = colors.accentLight.copy(alpha = 0.7f)
        Canvas(Modifier.size(size)) {
            val c = center
            val r = this.size.minDimension / 2f - numberInset.toPx()
            val rad = Math.toRadians(angle.toDouble())
            val end = Offset(c.x + (r * sin(rad)).toFloat(), c.y - (r * cos(rad)).toFloat())
            drawLine(handColor, c, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(colors.accentLight, radius = 5.dp.toPx(), center = c)
        }

        // Numerals
        val radius = size / 2 - numberInset
        for (i in 0 until 12) {
            val label = if (showMinutes) "%02d".format(i * 5) else (if (i == 0) "12" else "$i")
            val a = i * 30.0 * PI / 180.0
            DialLabel(
                text = label,
                x = radius * sin(a).toFloat(),
                y = -radius * cos(a).toFloat(),
                color = colors.textSecondary,
                modifier = Modifier.graphicsLayer {
                    val t = ((deal.value * DealMillis - i * 22f) / 300f).coerceIn(0f, 1f)
                    val e = AzMotion.EaseOutEmphasized.transform(t)
                    alpha = t
                    scaleX = 0.5f + 0.5f * e
                    scaleY = scaleX
                }
            )
        }

        // Selection node riding the hand
        val rad = angle * PI / 180.0
        Box(
            modifier = Modifier
                .offset { IntOffset((radius * sin(rad).toFloat()).roundToPx(), (-radius * cos(rad).toFloat()).roundToPx()) }
                .glow(colors.accentLight.copy(alpha = 0.3f), CircleShape, radius = 8.dp, offsetY = 3.dp)
                .size(nodeSize)
                .clip(CircleShape)
                .background(colors.accentLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (showMinutes) "%02d".format(minute) else "$hour12",
                style = type.labelStrong.copy(fontSize = 14.sp, letterSpacing = 0.sp),
                color = colors.onAccentLight
            )
        }
    }
}

private const val DealMillis = 560

@Composable
private fun DialLabel(text: String, x: Dp, y: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(Modifier.offset(x, y).size(28.dp).then(modifier), contentAlignment = Alignment.Center) {
        Text(text, style = AlarmzyTheme.typography.label.copy(letterSpacing = 0.sp), color = color)
    }
}
