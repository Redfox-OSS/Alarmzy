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
import dev.redfox.alarmzy.presentation.navigation.floatingNavClearance
import dev.redfox.alarmzy.ui.components.AlarmzySwitch
import dev.redfox.alarmzy.ui.components.AzAlertDialog
import dev.redfox.alarmzy.ui.components.AzCheckbox
import dev.redfox.alarmzy.ui.components.AzDropdownMenu
import dev.redfox.alarmzy.ui.components.AzFab
import dev.redfox.alarmzy.ui.components.AzMenuItem
import dev.redfox.alarmzy.ui.components.AzShapes
import dev.redfox.alarmzy.ui.components.EmptyState
import dev.redfox.alarmzy.ui.components.HeaderIconButton
import dev.redfox.alarmzy.ui.components.LargeHeader
import dev.redfox.alarmzy.ui.components.LargeHeaderHeight
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.SwitchSize
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

@Composable
fun AlarmsScreen(
    uiState: AlarmsUiState,
    onIntent: (AlarmsIntent) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val navClearance = floatingNavClearance()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(AlarmzyTheme.colors.background)) {
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
                val next = uiState.nextAlarm
                if (!uiState.selectionMode && next != null) {
                    item(key = "next_alarm_banner") {
                        NextAlarmBanner(alarm = next, modifier = Modifier.animateItem())
                    }
                }
                items(uiState.alarms, key = { it.id }) { alarm ->
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
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        if (uiState.selectionMode) {
            LargeHeader(
                title = if (uiState.selectedCount == 0) "Select alarms" else "${uiState.selectedCount} selected",
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                HeaderIconButton(Icons.Rounded.DoneAll, "Select all", onClick = { onIntent(AlarmsIntent.SelectAll) })
                HeaderIconButton(
                    Icons.Rounded.DeleteOutline,
                    "Delete selected",
                    onClick = { showDeleteDialog = true },
                    enabled = uiState.selectedCount > 0,
                    tint = AlarmzyTheme.colors.accentLight
                )
                HeaderIconButton(Icons.Rounded.Close, "Exit selection", onClick = { onIntent(AlarmsIntent.ClearSelection) })
            }
        } else {
            LargeHeader(title = "Alarms", modifier = Modifier.align(Alignment.TopCenter)) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    HeaderIconButton(R.drawable.ic_more_vert, "More options", onClick = { menuOpen = true })
                    AzDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AzMenuItem(
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

        AnimatedVisibility(
            visible = !uiState.selectionMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ScreenGutter, bottom = navClearance + 16.dp)
        ) {
            AzFab(icon = R.drawable.ic_add, contentDescription = "Add alarm", onClick = onAddAlarm)
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
        AzAlertDialog(
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
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val time = alarm.clockTime
    val day = if (alarm.nextTriggerTimeMillis > 0) relativeDay(alarm.nextTriggerTimeMillis) else null

    Box(modifier.fillMaxWidth()) {
        // Subtle ambient accent glow bleeding out from behind the card.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .height(56.dp)
                .glow(colors.accent.copy(alpha = 0.22f), CircleShape, radius = 32.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AzShapes.Card)
                .background(colors.tile.copy(alpha = 0.8f))
                .border(1.dp, colors.borderStrong, AzShapes.Card)
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
                Icon(painterResource(R.drawable.ic_alarm), contentDescription = null, tint = colors.accentLight)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NEXT ALARM", style = type.caption, color = colors.accentLight)
                Row {
                    Text(
                        if (day != null) "$day at ${time.digits}" else time.digits,
                        style = type.title.copy(letterSpacing = (-0.5).sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).alignByBaseline()
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(time.meridiem, style = type.body, color = colors.textSecondary, modifier = Modifier.alignByBaseline())
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
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val enabled = alarm.isEnabled
    val time = alarm.clockTime
    val border by animateColorAsState(
        if (isSelected) colors.accentBorder else colors.border.copy(alpha = 0f),
        label = "cardBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .alpha(if (enabled || isSelected) 1f else 0.7f)
            .clip(AzShapes.Card)
            .background(if (enabled) colors.cardGlass else colors.cardInner.copy(alpha = 0.5f))
            .border(1.dp, border, AzShapes.Card)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = selectionMode) {
            Row {
                AzCheckbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(16.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            alarm.caption?.let { caption ->
                Text(
                    caption.uppercase(),
                    style = type.caption.copy(letterSpacing = 0.025.em),
                    color = if (enabled) colors.accentLight else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Row {
                Text(
                    time.digits,
                    style = type.timeLarge,
                    color = if (enabled) colors.textPrimary else colors.textSecondary,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    time.meridiem,
                    style = type.meridiem,
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
        if (!selectionMode) {
            Spacer(Modifier.width(12.dp))
            AlarmzySwitch(
                checked = enabled,
                onCheckedChange = onToggle,
                size = SwitchSize.Large,
                showDot = true
            )
        }
    }
}
