package dev.redfox.alarmzy.presentation.alarmringing

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import dev.redfox.alarmzy.ui.theme.InterFontFamily
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import dev.redfox.alarmzy.ui.components.revealOnEnter

private val CLOCK_TEXT_SIZE = 84.sp

/**
 * The full-screen alarm-ringing layout, shared by the real [dev.redfox.alarmzy.alarm.AlarmRingingActivity]
 * and the settings customization screen's live preview — keeping both in sync by construction.
 */
@Composable
fun AlarmRingingContent(
    time: String,
    label: String,
    backgroundImagePath: String?,
    buttonLayout: AlarmButtonLayout,
    clockPosition: AlarmClockPosition,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Dismiss",
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val hasImage = backgroundImagePath != null
    val textColor = if (hasImage) Color.White else colors.textPrimary
    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }
    // "7:00 AM" -> digits + meridiem, styled separately like the alarm cards.
    val digits = time.substringBeforeLast(' ', time)
    val meridiem = time.substringAfterLast(' ', "").takeIf { it != time }.orEmpty()

    // Living screen: the bloom breathes, sonar rings pulse out of the clock, the CTA glows.
    val ambient = rememberInfiniteTransition(label = "ringAmbient")
    val breathe by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val sonar by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sonar"
    )
    val ringColor = if (hasImage) Color.White else colors.accent

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (backgroundImagePath != null) {
            AsyncImage(
                model = File(backgroundImagePath),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Guarantees text/button legibility over an arbitrary user photo — not optional.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.15f),
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
            )
        } else {
            // Ambient accent bloom behind the clock.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0f to colors.accent.copy(alpha = 0.22f + 0.1f * breathe),
                            0.55f to colors.accent.copy(alpha = 0.04f + 0.04f * breathe),
                            1f to Color.Transparent,
                            radius = 1250f + 250f * breathe
                        )
                    )
            )
        }

        // Independent of clockPosition — always pinned near the top.
        if (label.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .revealOnEnter(0, offsetY = (-16).dp)
                    .padding(top = 64.dp)
                    .padding(horizontal = 32.dp)
                    .clip(CircleShape)
                    .background(if (hasImage) Color.Black.copy(alpha = 0.35f) else colors.accentContainer)
                    .border(1.dp, if (hasImage) Color.White.copy(alpha = 0.2f) else colors.accentBorder, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    textAlign = TextAlign.Center,
                    style = type.sectionLabel.copy(letterSpacing = 0.1.em),
                    color = if (hasImage) Color.White else colors.accentLight,
                    maxLines = 1
                )
            }
        }

        // Clock + date move together between top/middle per clockPosition.
        Column(
            modifier = Modifier
                .align(if (clockPosition == AlarmClockPosition.TOP) Alignment.TopCenter else Alignment.Center)
                .padding(top = if (clockPosition == AlarmClockPosition.TOP) 140.dp else 0.dp)
                .padding(horizontal = 32.dp)
                .drawBehind {
                    // Three staggered rings expanding from behind the time, fading as they grow.
                    val base = size.minDimension * 0.55f
                    for (k in 0 until 3) {
                        val t = (sonar + k / 3f) % 1f
                        drawCircle(
                            ringColor.copy(alpha = 0.22f * (1f - t) * (1f - t)),
                            radius = base + size.maxDimension * 0.55f * t,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                .revealOnEnter(1, offsetY = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = digits,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = CLOCK_TEXT_SIZE,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-0.04).em,
                        color = textColor,
                        shadow = if (hasImage) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 16f) else null
                    )
                )
                if (meridiem.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = meridiem,
                        style = type.title.copy(fontWeight = FontWeight.Medium),
                        color = if (hasImage) Color.White.copy(alpha = 0.85f) else colors.accentLight,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            Text(
                text = currentDate,
                maxLines = 1,
                softWrap = false,
                style = type.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = textColor.copy(alpha = 0.75f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .revealOnEnter(3, offsetY = 40.dp)
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (buttonLayout) {
                AlarmButtonLayout.BOTH -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RingButton("Snooze", R.drawable.ic_snooze, primary = false, onClick = onSnooze, modifier = Modifier.weight(1f))
                    RingButton(dismissLabel, null, primary = true, onClick = onDismiss, glow = breathe, modifier = Modifier.weight(1f))
                }
                AlarmButtonLayout.SNOOZE_ONLY ->
                    RingButton("Snooze", R.drawable.ic_snooze, primary = true, onClick = onSnooze, glow = breathe, modifier = Modifier.fillMaxWidth())
                AlarmButtonLayout.DISMISS_ONLY ->
                    RingButton(dismissLabel, null, primary = true, onClick = onDismiss, glow = breathe, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "It's currently $currentTime",
                style = type.body,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

/** Large pill action: accent + glow when [primary], frosted neutral otherwise. */
@Composable
private fun RingButton(
    text: String,
    @DrawableRes icon: Int?,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 0..1 breathing phase for the primary glow. */
    glow: Float = 0.5f,
) {
    val colors = AlarmzyTheme.colors
    val content = if (primary) colors.onAccent else colors.textPrimary
    Row(
        modifier = modifier
            .then(
                if (primary) Modifier.glow(colors.accent.copy(alpha = 0.3f + 0.3f * glow), CircleShape, radius = 20.dp + 10.dp * glow, offsetY = 8.dp)
                else Modifier
            )
            .pressable(pressedScale = 0.94f, haptic = HapticFeedbackType.Confirm, onClick = onClick)
            .height(64.dp)
            .clip(CircleShape)
            .background(if (primary) colors.accent else colors.tile.copy(alpha = 0.85f))
            .border(1.dp, if (primary) Color.White.copy(alpha = 0.15f) else colors.borderStrong, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content)
        } else {
            Icon(Icons.Rounded.AlarmOff, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = AlarmzyTheme.typography.button, color = content)
    }
}
