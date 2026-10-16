package dev.redfox.alarmzy.presentation.timer

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus
import dev.redfox.alarmzy.presentation.common.countdownReadout
import dev.redfox.alarmzy.presentation.common.durationPill
import dev.redfox.alarmzy.presentation.common.hmsToMillis
import dev.redfox.alarmzy.presentation.common.millisToHms
import dev.redfox.alarmzy.presentation.common.rememberTicker
import dev.redfox.obsidian.navigation.floatingNavClearance
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.ActionPillButton
import dev.redfox.obsidian.components.LargeHeader
import dev.redfox.obsidian.components.LargeHeaderHeight
import dev.redfox.obsidian.theme.ScreenGutter
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.theme.InterFontFamily
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.graphics.graphicsLayer
import dev.redfox.obsidian.motion.ObMotion
import dev.redfox.obsidian.motion.RollingText
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.motion.popOnSelect

private const val Tnum = "tnum"

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val timer = uiState.timer
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(ObsidianTheme.colors.background)) {
        AnimatedContent(
            targetState = timer.status == TimerStatus.IDLE,
            transitionSpec = {
                if (targetState) {
                    // Back to the picker: it settles in as the ring shrinks away.
                    (fadeIn(tween(260, delayMillis = 60)) + scaleIn(ObMotion.gentle(), initialScale = 1.06f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 0.86f))
                } else {
                    // Start: the picker lifts away and the ring blooms in with a bounce.
                    (fadeIn(tween(240, delayMillis = 80)) + scaleIn(spring(dampingRatio = 0.62f, stiffness = 260f), initialScale = 0.8f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 1.08f))
                }
            },
            label = "timerMode",
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .padding(top = topInset, bottom = floatingNavClearance())
                .padding(horizontal = ScreenGutter)
        ) { idle ->
            if (idle) TimerSetup(timer, onIntent) else TimerRunning(timer, onIntent)
        }

        LargeHeader(title = "Timer", modifier = Modifier.align(Alignment.TopCenter))
    }
}

// ---------------------------------------------------------------------------------------------
// Idle: drum picker + presets + Start
// ---------------------------------------------------------------------------------------------

private val ColonWidth = 20.dp

@Composable
private fun TimerSetup(timer: TimerState, onIntent: (TimerIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val (h, m, s) = millisToHms(timer.pickerMs)
    val set = { hh: Int, mm: Int, ss: Int -> onIntent(TimerIntent.SetDuration(hmsToMillis(hh, mm, ss))) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        // Column headers aligned with the wheels below.
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).revealOnEnter(0)) {
            val label = ObsidianTheme.typography.label.copy(letterSpacing = 0.025.em)
            listOf("HOURS", "MINUTES", "SECONDS").forEachIndexed { i, text ->
                if (i > 0) Spacer(Modifier.width(ColonWidth))
                Text(text, style = label, color = colors.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }

        Box(Modifier.revealOnEnter(1), contentAlignment = Alignment.Center) {
            // Ambient accent glow behind the selected row.
            Box(
                Modifier
                    .width(256.dp)
                    .height(72.dp)
                    .glow(colors.accent.copy(alpha = 0.14f), CircleShape, radius = 36.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(range = 100, value = h, onValueChange = { set(it, m, s) }, description = "Hours", modifier = Modifier.weight(1f))
                WheelColon()
                NumberWheel(range = 60, value = m, onValueChange = { set(h, it, s) }, description = "Minutes", modifier = Modifier.weight(1f))
                WheelColon()
                NumberWheel(range = 60, value = s, onValueChange = { set(h, m, it) }, description = "Seconds", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            TimerUiState.Presets.forEachIndexed { i, preset ->
                PresetPill(
                    durationMs = preset,
                    selected = preset == timer.pickerMs,
                    onClick = { onIntent(TimerIntent.SetDuration(preset)) },
                    modifier = Modifier.weight(1f).widthIn(max = 105.dp).revealOnEnter(2 + i)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        ActionPillButton(
            text = "Start",
            primary = true,
            enabled = timer.pickerMs > 0,
            onClick = { onIntent(TimerIntent.Start) },
            modifier = Modifier.width(192.dp).revealOnEnter(2 + TimerUiState.Presets.size)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WheelColon() {
    Box(Modifier.width(ColonWidth), contentAlignment = Alignment.Center) {
        Text(
            ":",
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp),
            color = ObsidianTheme.colors.accent,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

private val WheelItemHeight = 80.dp
private val WheelHeight = 240.dp
private const val WheelLoops = 1000

/**
 * Looping drum picker. The centered item is the value; it snaps to center, ticks a haptic per
 * value, and reports the value once scrolling settles. External [value] changes (presets,
 * restored state) scroll the wheel to match.
 */
@Composable
private fun NumberWheel(
    range: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors
    val haptics = LocalHapticFeedback.current
    val count = range * WheelLoops
    val middle = remember(range) { (count / 2) - (count / 2) % range }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = middle + value)
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    val centerIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: (middle + currentValue)
        }
    }

    // Haptic tick as each value passes the center.
    LaunchedEffect(state) {
        snapshotFlow { centerIndex }.distinctUntilChanged().drop(1).collect {
            if (state.isScrollInProgress) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }
    // Report the settled value.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().filter { !it }.collect {
            val v = centerIndex % range
            if (v != currentValue) currentOnValueChange(v)
        }
    }
    // Follow external changes (presets / restored state).
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && centerIndex % range != value) {
            val base = centerIndex - centerIndex % range
            state.animateScrollToItem(base + value)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = (WheelHeight - WheelItemHeight) / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(WheelHeight)
            .semantics { contentDescription = "$description ${"%02d".format(value)}" }
    ) {
        items(count) { index ->
            Box(
                Modifier
                    .height(WheelItemHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        // A real drum: numbers curl away around a cylinder, shrinking and
                        // fading continuously with their distance from the center row.
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                        val d = if (item == null) 1.5f else {
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            ((item.offset + item.size / 2f) - center) / item.size
                        }
                        val a = abs(d).coerceAtMost(1.5f)
                        val scale = 1f - 0.3f * a.coerceAtMost(1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - 0.8f * a).coerceIn(0.08f, 1f)
                        rotationX = -d * 32f
                        cameraDistance = 10f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "%02d".format(index % range),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp,
                        letterSpacing = (-1.35).sp,
                        fontFeatureSettings = Tnum,
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PresetPill(durationMs: Long, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ObsidianTheme.colors
    val (h, m, s) = millisToHms(durationMs)
    Box(
        modifier = modifier
            .popOnSelect(selected)
            .then(if (selected) Modifier.glow(colors.accent.copy(alpha = 0.2f), CircleShape, radius = 6.dp) else Modifier)
            .pressable(pressedScale = 0.92f, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .clip(CircleShape)
            .background(colors.cardInner)
            .border(1.dp, if (selected) colors.accent.copy(alpha = 0.3f) else colors.border, CircleShape)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "%02d:%02d:%02d".format(h, m, s),
            style = ObsidianTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = Tnum, letterSpacing = (-0.3).sp),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Running / paused / finished: progress ring + controls
// ---------------------------------------------------------------------------------------------

@Composable
private fun TimerRunning(timer: TimerState, onIntent: (TimerIntent) -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val running = timer.status == TimerStatus.RUNNING
    val finished = timer.status == TimerStatus.FINISHED
    val now = rememberTicker(active = running) { System.currentTimeMillis() }
    val remaining = timer.remainingAt(now)
    val fraction = if (timer.durationMs > 0) (remaining.toFloat() / timer.durationMs).coerceIn(0f, 1f) else 0f

    // Paused and finished readouts pulse gently.
    val ambient = rememberInfiniteTransition(label = "timerPulse")
    val pulse by ambient.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    // "Time's up": the whole ring throbs like a heartbeat.
    val beat by ambient.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1100
            1.045f at 120
            1f at 300
            1.03f at 420
            1f at 640
        }),
        label = "beat"
    )
    // The ring draws itself in when the countdown screen appears.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1100, easing = ObMotion.EaseOutEmphasized)) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 288.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    val s = if (finished) beat else 1f
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center
        ) {
            val ringSize: Dp = maxWidth
            Canvas(Modifier.fillMaxSize()) {
                drawTimerRing(
                    fraction = (if (finished) 1f else fraction) * intro.value,
                    dim = !running && !finished,
                    track = colors.cardInner.copy(alpha = 0.8f),
                    accent = colors.accent,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingText(
                    countdownReadout(remaining),
                    rollUp = false,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (ringSize.value * if (remaining >= 3_600_000L) 0.15f else 0.1875f).sp,
                        letterSpacing = (-1.35).sp,
                        fontFeatureSettings = Tnum,
                    ),
                    color = if (finished) colors.accent else Color.White,
                    modifier = Modifier.alpha(if (running) 1f else pulse)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateContentSize(ObMotion.snappy())) {
                    when {
                        running -> {
                            Icon(painterResource(R.drawable.ic_bell_ring), contentDescription = null, tint = colors.accent)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ends at ${endsAt(timer.endAtMillis)}",
                                style = type.body.copy(fontSize = 13.sp, letterSpacing = 0.025.em),
                                color = colors.textTertiary
                            )
                        }
                        finished -> Text("Time's up", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.accentLight)
                        else -> Text("Paused", style = type.body.copy(fontSize = 13.sp, letterSpacing = 0.025.em), color = colors.textTertiary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.dp)
                ) {
                    Text(
                        durationPill(timer.durationMs),
                        style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.05.em),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            if (finished) {
                ActionPillButton(
                    text = "+1 min",
                    onClick = { onIntent(TimerIntent.AddMinute) },
                    modifier = Modifier.weight(1f)
                )
                ActionPillButton(
                    text = "Stop",
                    primary = true,
                    onClick = { onIntent(TimerIntent.Cancel) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActionPillButton(
                    text = "Cancel",
                    icon = painterResource(R.drawable.ic_cancel),
                    onClick = { onIntent(TimerIntent.Cancel) },
                    modifier = Modifier.weight(1f)
                )
                ActionPillButton(
                    text = if (running) "Pause" else "Resume",
                    icon = if (running) painterResource(R.drawable.ic_pause)
                    else rememberVectorPainter(Icons.Rounded.PlayArrow),
                    primary = true,
                    onClick = { onIntent(if (running) TimerIntent.Pause else TimerIntent.Resume) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun endsAt(endAtMillis: Long): String =
    Instant.ofEpochMilli(endAtMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun DrawScope.drawTimerRing(fraction: Float, dim: Boolean, track: Color, accent: Color) {
    val scale = size.minDimension / 288f
    val c = center
    val r = 123.4f * scale
    // Inner ambient glow
    drawCircle(
        brush = Brush.radialGradient(listOf(accent.copy(alpha = if (dim) 0.07f else 0.15f), Color.Transparent), center = c, radius = r),
        radius = r,
        center = c
    )
    drawCircle(track, radius = r, center = c, style = Stroke(width = 8f * scale))
    if (fraction <= 0f) return
    val arcColor = if (dim) accent.copy(alpha = 0.5f) else accent
    val topLeft = Offset(c.x - r, c.y - r)
    val arcSize = Size(r * 2, r * 2)
    val sweep = 360f * fraction
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 9f * scale
            strokeCap = android.graphics.Paint.Cap.ROUND
            color = arcColor.copy(alpha = if (dim) 0.25f else 0.6f).toArgb()
            maskFilter = BlurMaskFilter(16f * scale, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawArc(topLeft.x, topLeft.y, topLeft.x + arcSize.width, topLeft.y + arcSize.height, -90f, sweep, false, paint)
    }
    // Remaining time drains counter-clockwise from 12 o'clock.
    drawArc(
        color = arcColor,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = 8.2f * scale, cap = StrokeCap.Round)
    )
}
