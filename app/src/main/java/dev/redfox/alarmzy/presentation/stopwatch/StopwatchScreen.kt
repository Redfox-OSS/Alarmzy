package dev.redfox.alarmzy.presentation.stopwatch

import android.graphics.BlurMaskFilter
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.StopwatchState
import dev.redfox.alarmzy.presentation.common.lapTime
import dev.redfox.alarmzy.presentation.common.rememberTicker
import dev.redfox.alarmzy.presentation.common.stopwatchReadout
import dev.redfox.alarmzy.presentation.navigation.floatingNavClearance
import dev.redfox.alarmzy.ui.components.ClockActionButton
import dev.redfox.alarmzy.ui.components.LargeHeader
import dev.redfox.alarmzy.ui.components.LargeHeaderHeight
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import dev.redfox.alarmzy.ui.theme.InterFontFamily

/** Tabular (fixed-width) digits so the running readout doesn't jitter. */
private const val Tnum = "tnum"
private val FastestColor = Color(0xFF4ADE80)

@Composable
fun StopwatchScreen(
    uiState: StopwatchUiState,
    onIntent: (StopwatchIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val sw = uiState.stopwatch
    val now = rememberTicker(active = sw.running) { SystemClock.elapsedRealtime() }
    val elapsed = sw.elapsedAt(now)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(AlarmzyTheme.colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().glassSource(),
            contentPadding = PaddingValues(
                start = ScreenGutter,
                end = ScreenGutter,
                top = topInset + 4.dp,
                bottom = floatingNavClearance() + 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "dial") {
                StopwatchDial(state = sw, elapsedMs = elapsed)
            }
            item(key = "controls") {
                Controls(state = sw, onIntent = onIntent)
            }
            if (sw.laps.isEmpty()) {
                item(key = "empty") { EmptyLaps(running = sw.running) }
            } else {
                item(key = "laps_header") { LapsHeader() }
                item(key = "lap_current") {
                    // The lap in progress, live.
                    LapRow(
                        number = sw.laps.size + 1,
                        split = elapsed - sw.laps.last(),
                        overall = elapsed,
                        current = true,
                        splitColor = AlarmzyTheme.colors.accent,
                    )
                }
                val splits = sw.lapSplits
                val fastest = if (splits.size >= 2) splits.min() else null
                val slowest = if (splits.size >= 2) splits.max() else null
                items(splits.indices.reversed().toList(), key = { "lap_$it" }) { i ->
                    LapRow(
                        number = i + 1,
                        split = splits[i],
                        overall = sw.laps[i],
                        current = false,
                        splitColor = when (splits[i]) {
                            fastest -> FastestColor
                            slowest -> AlarmzyTheme.colors.warning
                            else -> AlarmzyTheme.colors.textPrimary
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        LargeHeader(title = "Stopwatch", modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun StopwatchDial(state: StopwatchState, elapsedMs: Long) {
    val colors = AlarmzyTheme.colors
    val active = !state.isIdle
    val readout = stopwatchReadout(elapsedMs)
    val colonColor by animateColorAsState(
        if (state.running) colors.accent else Color.White.copy(alpha = 0.4f),
        label = "colon"
    )

    BoxWithConstraints(
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val dialSize = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            drawStopwatchRing(
                sweepFraction = if (active) (elapsedMs % 60_000L) / 60_000f else 0f,
                running = state.running,
                active = active,
                track = colors.card,
                ticks = colors.tile,
                accent = colors.accent,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val caption = when {
                state.running -> "LAP %02d".format(state.laps.size + 1) to "ACTIVE"
                active -> "LAP %02d".format(state.laps.size + 1) to "PAUSED"
                else -> null
            }
            Row(Modifier.height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                if (caption != null) {
                    val style = AlarmzyTheme.typography.label.copy(letterSpacing = 0.1.em)
                    Text(caption.first, style = style, color = colors.textSecondary.copy(alpha = 0.7f))
                    Text("  •  ", style = style, color = colors.textSecondary.copy(alpha = 0.7f))
                    Text(
                        caption.second,
                        style = style.copy(fontWeight = FontWeight.SemiBold),
                        color = if (state.running) colors.accent else colors.textSecondary
                    )
                }
            }
            val digitSize = (dialSize.value * if (readout.main.size == 3) 0.16f else 0.19f).sp
            val digits = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = digitSize,
                letterSpacing = (-0.4).sp,
                fontFeatureSettings = Tnum,
            )
            Row {
                readout.main.forEachIndexed { i, part ->
                    if (i > 0) Text(":", style = digits, color = colonColor, modifier = Modifier.alignByBaseline().padding(horizontal = 2.dp))
                    Text(part, style = digits, color = Color.White, modifier = Modifier.alignByBaseline())
                }
                Text(
                    ".${readout.tenths}",
                    style = digits.copy(fontSize = digitSize * 0.47f, fontWeight = FontWeight.Medium),
                    color = if (state.running) colors.textSecondary else colors.textTertiary,
                    modifier = Modifier.alignByBaseline().padding(start = 4.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawStopwatchRing(
    sweepFraction: Float,
    running: Boolean,
    active: Boolean,
    track: Color,
    ticks: Color,
    accent: Color,
) {
    val scale = size.minDimension / 320f
    val c = center
    // Soft inner glow while active; faint disc when idle.
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = if (running) 0.16f else 0.08f), Color.Transparent),
                center = c,
                radius = 144f * scale
            ),
            radius = 144f * scale,
            center = c
        )
    } else {
        drawCircle(Color.White.copy(alpha = 0.02f), radius = 144f * scale, center = c)
    }
    // Outer track
    drawCircle(track, radius = 140f * scale, center = c, style = Stroke(width = 7f * scale))
    // Dotted tick ring
    drawCircle(
        ticks,
        radius = 126f * scale,
        center = c,
        style = Stroke(width = 1.5f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * scale, 16f * scale)))
    )
    if (active && sweepFraction > 0f) {
        val r = 140f * scale
        val arcColor = if (running) accent else accent.copy(alpha = 0.5f)
        val topLeft = Offset(c.x - r, c.y - r)
        val arcSize = Size(r * 2, r * 2)
        // Glow pass
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 8f * scale
                strokeCap = android.graphics.Paint.Cap.ROUND
                color = arcColor.copy(alpha = if (running) 0.6f else 0.25f).toArgb()
                maskFilter = BlurMaskFilter(14f * scale, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawArc(topLeft.x, topLeft.y, topLeft.x + arcSize.width, topLeft.y + arcSize.height, -90f, 360f * sweepFraction, false, paint)
        }
        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = 360f * sweepFraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 7.5f * scale, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun Controls(state: StopwatchState, onIntent: (StopwatchIntent) -> Unit) {
    AnimatedContent(
        targetState = when {
            state.running -> 1
            state.isIdle -> 0
            else -> 2
        },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "stopwatchControls",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp)
    ) { mode ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            val play = rememberVectorPainter(Icons.Rounded.PlayArrow)
            when (mode) {
                0 -> ClockActionButton(
                    text = "Start",
                    icon = play,
                    primary = true,
                    onClick = { onIntent(StopwatchIntent.Start) },
                    modifier = Modifier.width(192.dp)
                )
                1 -> {
                    ClockActionButton(
                        text = "Lap",
                        icon = painterResource(R.drawable.ic_lap),
                        onClick = { onIntent(StopwatchIntent.Lap) },
                        modifier = Modifier.weight(1f)
                    )
                    ClockActionButton(
                        text = "Pause",
                        icon = painterResource(R.drawable.ic_pause),
                        primary = true,
                        onClick = { onIntent(StopwatchIntent.Pause) },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    ClockActionButton(
                        text = "Reset",
                        icon = rememberVectorPainter(Icons.Rounded.Replay),
                        onClick = { onIntent(StopwatchIntent.Reset) },
                        modifier = Modifier.weight(1f)
                    )
                    ClockActionButton(
                        text = "Resume",
                        icon = play,
                        primary = true,
                        onClick = { onIntent(StopwatchIntent.Start) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLaps(running: Boolean) {
    val colors = AlarmzyTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.cardInner.copy(alpha = 0.6f))
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_stopwatch_empty), contentDescription = null, tint = colors.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "No laps recorded",
            style = AlarmzyTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = colors.textSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (running) "Tap Lap to record a split" else "Press Start to begin timing",
            style = AlarmzyTheme.typography.label.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
            color = colors.textTertiary
        )
    }
}

private val LapNumberWidth: Dp = 56.dp
private val LapOverallWidth: Dp = 80.dp

@Composable
private fun LapsHeader() {
    val style = AlarmzyTheme.typography.caption.copy(fontSize = 11.sp, letterSpacing = 0.05.em)
    val color = AlarmzyTheme.colors.textTertiary
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp)) {
        Text("LAP", style = style, color = color, modifier = Modifier.width(LapNumberWidth))
        Text("LAP SPLIT", style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("OVERALL", style = style, color = color, modifier = Modifier.width(LapOverallWidth), textAlign = TextAlign.End)
    }
}

@Composable
private fun LapRow(
    number: Int,
    split: Long,
    overall: Long,
    current: Boolean,
    splitColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val base = AlarmzyTheme.typography.body.copy(fontSize = 13.sp, fontFeatureSettings = Tnum, letterSpacing = (-0.3).sp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(shape)
            .background(colors.cardInner.copy(alpha = if (current) 0.9f else 0.7f))
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.width(LapNumberWidth), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "%02d".format(number),
                style = base.copy(fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium),
                color = if (current) Color.White else colors.textSecondary
            )
            if (current) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .glow(colors.accent.copy(alpha = 0.9f), CircleShape, radius = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
        Text(
            lapTime(split),
            style = base.copy(fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal),
            color = splitColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            lapTime(overall),
            style = base.copy(fontSize = 12.sp, letterSpacing = 0.sp),
            color = if (current) Color.White.copy(alpha = 0.9f) else colors.textSecondary,
            modifier = Modifier.width(LapOverallWidth),
            textAlign = TextAlign.End
        )
    }
}
