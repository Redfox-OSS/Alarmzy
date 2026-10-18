package dev.redfox.alarmzy.presentation.alarms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.presentation.common.caption
import dev.redfox.alarmzy.presentation.common.clockTime
import dev.redfox.alarmzy.presentation.common.detailText
import dev.redfox.alarmzy.presentation.common.relativeDay
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsHomePrompt
import dev.redfox.obsidian.navigation.floatingNavClearance
import dev.redfox.obsidian.components.ObSwitch
import dev.redfox.obsidian.components.ObAlertDialog
import dev.redfox.obsidian.components.ObCheckbox
import dev.redfox.obsidian.components.ObDropdownMenu
import dev.redfox.obsidian.components.ObFab
import dev.redfox.obsidian.components.ObMenuItem
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.EmptyState
import dev.redfox.obsidian.components.HeaderIconButton
import dev.redfox.obsidian.components.LargeHeader
import dev.redfox.obsidian.components.LargeHeaderHeight
import dev.redfox.obsidian.theme.ScreenGutter
import dev.redfox.obsidian.components.SwitchSize
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import dev.redfox.obsidian.motion.ObMotion
import dev.redfox.obsidian.motion.HorizontalReveal
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.motion.HeaderActionsSwap
import dev.redfox.obsidian.motion.RollSwap
import androidx.activity.compose.BackHandler
import dev.redfox.alarmzy.alarm.RingingAlarm
import dev.redfox.obsidian.components.ActionPillButton
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.Brush
import dev.redfox.alarmzy.presentation.livealerts.FullScreenAlarmWarning
import dev.redfox.alarmzy.presentation.livealerts.rememberFullScreenAlarmsAllowed

@Composable
fun AlarmsScreen(
    uiState: AlarmsUiState,
    onIntent: (AlarmsIntent) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val fullScreenAllowed = rememberFullScreenAlarmsAllowed()

    // Back leaves selection mode instead of closing the app.
    BackHandler(enabled = uiState.selectionMode) { onIntent(AlarmsIntent.ClearSelection) }
    val navClearance = floatingNavClearance()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(ObsidianTheme.colors.background)) {
        when {
            uiState.isLoading -> Unit
            uiState.alarms.isEmpty() -> EmptyState(
                icon = R.drawable.ic_alarm,
                title = "No alarms yet",
                subtitle = "Tap + to create your first alarm",
                modifier = Modifier.glassSource().padding(top = topInset, bottom = navClearance)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().glassSource(),
                contentPadding = PaddingValues(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = topInset + 8.dp,
                    // Leave room for the FAB above the floating nav bar.
                    bottom = navClearance + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!uiState.selectionMode && !fullScreenAllowed) {
                    item(key = "full_screen_warning") {
                        FullScreenAlarmWarning(Modifier.animateItem())
                    }
                }
                val ringing = uiState.ringingAlarm
                if (!uiState.selectionMode && ringing != null) {
                    item(key = "ringing_now") {
                        RingingNowCard(
                            alarm = ringing,
                            onSnooze = { onIntent(AlarmsIntent.SnoozeRinging) },
                            onDismiss = { onIntent(AlarmsIntent.DismissRinging) },
                            modifier = Modifier.animateItem().revealOnEnter(0)
                        )
                    }
                }
                val next = uiState.nextAlarm
                if (!uiState.selectionMode && next != null && next.id != ringing?.id) {
                    item(key = "next_alarm_banner") {
                        NextAlarmBanner(alarm = next, modifier = Modifier.animateItem().revealOnEnter(0))
                    }
                }
                itemsIndexed(uiState.alarms, key = { _, it -> it.id }) { index, alarm ->
                    AlarmCard(
                        alarm = alarm,
                        selectionMode = uiState.selectionMode,
                        isSelected = alarm.id in uiState.selectedIds,
                        onClick = {
                            if (uiState.selectionMode) onIntent(AlarmsIntent.ToggleSelection(alarm.id))
                            else onAlarmClick(alarm.id)
                        },
                        onLongClick = { onIntent(AlarmsIntent.EnterSelection(alarm.id)) },
                        onToggle = { enabled -> onIntent(AlarmsIntent.ToggleAlarm(alarm.id, enabled)) },
                        modifier = Modifier.animateItem().revealOnEnter(index + 1)
                    )
                }
            }
        }

        LargeHeader(
            title = when {
                !uiState.selectionMode -> "Alarms"
                uiState.selectedCount == 0 -> "Select alarms"
                else -> "${uiState.selectedCount} selected"
            },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            AnimatedContent(
                targetState = uiState.selectionMode,
                transitionSpec = { HeaderActionsSwap },
                label = "alarmsHeaderActions"
            ) { selecting ->
                Row {
                    if (selecting) {
                        HeaderIconButton(Icons.Rounded.DoneAll, "Select all", onClick = { onIntent(AlarmsIntent.SelectAll) })
                        HeaderIconButton(
                            Icons.Rounded.DeleteOutline,
                            "Delete selected",
                            onClick = { showDeleteDialog = true },
                            enabled = uiState.selectedCount > 0,
                            tint = ObsidianTheme.colors.accentLight
                        )
                        HeaderIconButton(Icons.Rounded.Close, "Exit selection", onClick = { onIntent(AlarmsIntent.ClearSelection) })
                    } else {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            HeaderIconButton(R.drawable.ic_more_vert, "More options", onClick = { menuOpen = true })
                            ObDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                ObMenuItem(
                                    text = "Select alarms",
                                    onClick = {
                                        menuOpen = false
                                        onIntent(AlarmsIntent.StartSelection)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !uiState.selectionMode,
            enter = scaleIn(ObMotion.bouncy()) + fadeIn(),
            exit = scaleOut(ObMotion.snappy()) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ScreenGutter, bottom = navClearance + 16.dp)
        ) {
            ObFab(icon = R.drawable.ic_add, contentDescription = "Add alarm", onClick = onAddAlarm)
        }
    }

    if (!uiState.isLoading) {
        LiveAlertsHomePrompt(
            asked = uiState.liveAlertsPromptAsked,
            onMarkAsked = { onIntent(AlarmsIntent.MarkLiveAlertsPromptAsked) },
            onPermissionsChanged = { onIntent(AlarmsIntent.RestoreLiveAlerts) }
        )
    }

    if (showDeleteDialog) {
        val count = uiState.selectedCount
        ObAlertDialog(
            title = if (count == 1) "Delete alarm?" else "Delete $count alarms?",
            text = if (count == 1) "This alarm will be permanently deleted."
            else "These $count alarms will be permanently deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                onIntent(AlarmsIntent.DeleteSelected)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun NextAlarmBanner(alarm: Alarm, modifier: Modifier = Modifier) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val time = alarm.clockTime
    val day = if (alarm.nextTriggerTimeMillis > 0) relativeDay(alarm.nextTriggerTimeMillis) else null

    // The ambient glow breathes, and every few seconds the bell gives a little ring.
    val ambient = rememberInfiniteTransition(label = "bannerAmbient")
    val breathe by ambient.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val ring by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 5200
            0f at 4200
            -16f at 4270
            14f at 4350
            -10f at 4430
            7f at 4510
            -3f at 4600
            0f at 4700
        }),
        label = "bellRing"
    )

    Box(modifier.fillMaxWidth()) {
        // Subtle ambient accent glow bleeding out from behind the card.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .height(56.dp)
                .glow(colors.accent.copy(alpha = breathe), CircleShape, radius = 32.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ObShapes.Card)
                .background(colors.tile.copy(alpha = 0.8f))
                .border(1.dp, colors.borderStrong, ObShapes.Card)
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.accentContainer)
                    .border(1.dp, colors.accentBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_alarm),
                    contentDescription = null,
                    tint = colors.accentLight,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = ring
                        transformOrigin = TransformOrigin(0.5f, 0.15f)
                    }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NEXT ALARM", style = type.caption, color = colors.accentLight)
                // Animate the whole line so the time and its AM/PM stay on one baseline
                // (AnimatedContent doesn't expose a baseline to align against).
                AnimatedContent(
                    targetState = (if (day != null) "$day at ${time.digits}" else time.digits) to time.meridiem,
                    transitionSpec = { RollSwap },
                    contentAlignment = Alignment.CenterStart,
                    label = "nextAlarmTime"
                ) { (text, meridiem) ->
                    Row {
                        Text(
                            text,
                            style = type.title.copy(letterSpacing = (-0.5).sp),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).alignByBaseline()
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(meridiem, style = type.body, color = colors.textSecondary, modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val enabled = alarm.isEnabled
    val time = alarm.clockTime
    val border by animateColorAsState(
        if (isSelected) colors.accentBorder else colors.border.copy(alpha = 0f),
        ObMotion.snappy(),
        label = "cardBorder"
    )
    // Switching an alarm off lets the card sink back instead of snapping grey.
    val dim by animateFloatAsState(if (enabled || isSelected) 1f else 0.7f, ObMotion.gentle(), label = "cardDim")
    val cardBg by animateColorAsState(
        if (enabled) colors.cardGlass else colors.cardInner.copy(alpha = 0.5f),
        ObMotion.gentle(),
        label = "cardBg"
    )
    val timeColor by animateColorAsState(if (enabled) colors.textPrimary else colors.textSecondary, ObMotion.gentle(), label = "timeColor")
    val captionColor by animateColorAsState(if (enabled) colors.accentLight else colors.textSecondary, ObMotion.gentle(), label = "captionColor")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .graphicsLayer { alpha = dim; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .clip(ObShapes.Card)
            .background(cardBg)
            .border(1.dp, border, ObShapes.Card)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalReveal(visible = selectionMode) {
            Row {
                ObCheckbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(16.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            alarm.caption?.let { caption ->
                Text(
                    caption.uppercase(),
                    style = type.caption.copy(letterSpacing = 0.025.em),
                    color = captionColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Row {
                Text(
                    time.digits,
                    style = type.numericLarge,
                    color = timeColor,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    time.meridiem,
                    style = type.unit,
                    color = colors.textSecondary.copy(alpha = if (enabled) 1f else 0.7f),
                    modifier = Modifier.alignByBaseline()
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                alarm.repeatMode.detailText(),
                style = type.body,
                color = colors.textSecondary.copy(alpha = if (enabled) 1f else 0.7f)
            )
        }
        HorizontalReveal(visible = !selectionMode) {
            Row {
                Spacer(Modifier.width(12.dp))
                ObSwitch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    size = SwitchSize.Large,
                    showDot = true
                )
            }
        }
    }
}

/**
 * "Ringing now" hero card: the always-available way to stop a sounding alarm from inside the app,
 * even after the full-screen ringer was closed. The bell keeps shaking and the glow throbs.
 */
@Composable
private fun RingingNowCard(
    alarm: RingingAlarm,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val digits = alarm.time.substringBeforeLast(' ', alarm.time)
    val meridiem = alarm.time.substringAfterLast(' ', "").takeIf { it != alarm.time }.orEmpty()
    val shape = ObShapes.CardLarge

    val ambient = rememberInfiniteTransition(label = "ringingCard")
    val throb by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "throb"
    )
    val shake by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1400
            -18f at 60
            16f at 130
            -14f at 200
            12f at 270
            -8f at 340
            5f at 410
            0f at 500
        }),
        label = "shake"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glow(colors.accent.copy(alpha = 0.25f + 0.25f * throb), shape, radius = 22.dp + 10.dp * throb, offsetY = 6.dp)
            .clip(shape)
            .background(colors.card)
            .background(
                Brush.linearGradient(
                    listOf(colors.accent.copy(alpha = 0.28f), colors.accent.copy(alpha = 0.06f))
                )
            )
            .border(1.dp, colors.accent.copy(alpha = 0.35f + 0.25f * throb), shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.2f))
                    .border(1.dp, colors.accentBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_alarm),
                    contentDescription = null,
                    tint = colors.accentLight,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = shake
                        transformOrigin = TransformOrigin(0.5f, 0.15f)
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .graphicsLayer { alpha = 0.4f + 0.6f * throb }
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("RINGING NOW", style = type.caption, color = colors.accentLight)
                }
                Row {
                    Text(
                        digits,
                        style = type.numericLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (meridiem.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(meridiem, style = type.unit, color = colors.textSecondary, modifier = Modifier.alignByBaseline())
                    }
                }
                if (alarm.label.isNotBlank()) {
                    Text(
                        alarm.label,
                        style = type.body,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPillButton(
                text = "Snooze",
                icon = painterResource(R.drawable.ic_snooze),
                onClick = onSnooze,
                modifier = Modifier.weight(1f)
            )
            ActionPillButton(
                text = "Dismiss",
                icon = rememberVectorPainter(Icons.Rounded.AlarmOff),
                primary = true,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
