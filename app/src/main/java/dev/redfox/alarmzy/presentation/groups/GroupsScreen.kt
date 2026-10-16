package dev.redfox.alarmzy.presentation.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.presentation.common.clockTime
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
import dev.redfox.obsidian.components.IconTile
import dev.redfox.obsidian.components.LargeHeader
import dev.redfox.obsidian.components.LargeHeaderHeight
import dev.redfox.obsidian.theme.ScreenGutter
import dev.redfox.obsidian.components.SwitchSize
import dev.redfox.alarmzy.ui.components.drawableRes
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.effects.glow
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import dev.redfox.obsidian.motion.ObMotion
import dev.redfox.obsidian.motion.HorizontalReveal
import dev.redfox.obsidian.motion.ExpandEnter
import dev.redfox.obsidian.motion.CollapseExit
import dev.redfox.obsidian.motion.RevealScope
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.motion.HeaderActionsSwap
import androidx.activity.compose.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun GroupsScreen(
    uiState: GroupsUiState,
    onIntent: (GroupsIntent) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Back leaves selection mode instead of closing the app.
    BackHandler(enabled = uiState.selectionMode) { onIntent(GroupsIntent.ClearSelection) }
    val navClearance = floatingNavClearance()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(ObsidianTheme.colors.background)) {
        when {
            uiState.isLoading -> Unit
            uiState.groups.isEmpty() -> EmptyState(
                icon = R.drawable.ic_nav_groups,
                title = "No groups yet",
                subtitle = "Group alarms to switch them all on or off at once",
                modifier = Modifier.glassSource().padding(top = topInset, bottom = navClearance)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().glassSource(),
                contentPadding = PaddingValues(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = topInset + 8.dp,
                    bottom = navClearance + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(uiState.groups, key = { _, it -> it.id }) { index, group ->
                    GroupCard(
                        group = group,
                        isExpanded = !uiState.selectionMode && group.id in uiState.expandedGroupIds,
                        selectionMode = uiState.selectionMode,
                        isSelected = group.id in uiState.selectedIds,
                        onCardClick = {
                            if (uiState.selectionMode) onIntent(GroupsIntent.ToggleSelection(group.id))
                            else onIntent(GroupsIntent.ToggleExpand(group.id))
                        },
                        onLongClick = { onIntent(GroupsIntent.EnterSelection(group.id)) },
                        onToggleGroup = { enabled -> onIntent(GroupsIntent.ToggleGroup(group.id, enabled)) },
                        onEditGroup = { onEditGroup(group.id) },
                        onToggleAlarm = { alarmId, enabled ->
                            onIntent(GroupsIntent.ToggleAlarmInGroup(alarmId, enabled))
                        },
                        modifier = Modifier.animateItem().revealOnEnter(index)
                    )
                }
            }
        }

        LargeHeader(
            title = when {
                !uiState.selectionMode -> "Alarm Groups"
                uiState.selectedCount == 0 -> "Select groups"
                else -> "${uiState.selectedCount} selected"
            },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            AnimatedContent(
                targetState = uiState.selectionMode,
                transitionSpec = { HeaderActionsSwap },
                label = "groupsHeaderActions"
            ) { selecting ->
                Row {
                    if (selecting) {
                        HeaderIconButton(Icons.Rounded.DoneAll, "Select all", onClick = { onIntent(GroupsIntent.SelectAll) })
                        HeaderIconButton(
                            Icons.Rounded.DeleteOutline,
                            "Delete selected",
                            onClick = { showDeleteDialog = true },
                            enabled = uiState.selectedCount > 0,
                            tint = ObsidianTheme.colors.accentLight
                        )
                        HeaderIconButton(Icons.Rounded.Close, "Exit selection", onClick = { onIntent(GroupsIntent.ClearSelection) })
                    } else {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            HeaderIconButton(R.drawable.ic_more_vert, "More options", onClick = { menuOpen = true })
                            ObDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                ObMenuItem(
                                    text = "Select groups",
                                    onClick = {
                                        menuOpen = false
                                        onIntent(GroupsIntent.StartSelection)
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
            ObFab(icon = R.drawable.ic_add, contentDescription = "Create group", onClick = onAddGroup)
        }
    }

    if (showDeleteDialog) {
        val count = uiState.selectedCount
        ObAlertDialog(
            title = if (count == 1) "Delete group?" else "Delete $count groups?",
            text = if (count == 1) "The group will be deleted. Its alarms will be kept and ungrouped."
            else "These $count groups will be deleted. Their alarms will be kept and ungrouped.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                onIntent(GroupsIntent.DeleteSelected)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun GroupCard(
    group: AlarmGroup,
    isExpanded: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleGroup: (Boolean) -> Unit,
    onEditGroup: () -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val enabled = group.isEnabled
    val groupColor = parseGroupColor(group.color)
    val chevronRotation by animateFloatAsState(
        if (isExpanded) 180f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "chevron"
    )
    val dim by animateFloatAsState(if (enabled || isSelected) 1f else 0.8f, ObMotion.gentle(), label = "groupDim")
    val cardColor by animateColorAsState(if (enabled) colors.card else colors.cardInner, ObMotion.gentle(), label = "groupBg")
    val border by animateColorAsState(
        if (isSelected) colors.accentBorder else colors.border.copy(alpha = 0f),
        label = "groupBorder"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.glow(colors.background.copy(alpha = 0.6f), ObShapes.Card, radius = 6.dp, offsetY = 4.dp) else Modifier)
            .graphicsLayer { alpha = dim; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .clip(ObShapes.Card)
            .background(cardColor)
            .border(1.dp, border, ObShapes.Card)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.985f, onLongClick = onLongClick, onClick = onCardClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalReveal(visible = selectionMode) {
                Row {
                    ObCheckbox(checked = isSelected, onCheckedChange = { onCardClick() })
                    Spacer(Modifier.width(12.dp))
                }
            }
            IconTile(
                icon = group.icon.drawableRes,
                size = 44.dp,
                background = if (enabled) colors.tile else colors.card,
                tint = (groupColor ?: colors.accentLight).copy(alpha = if (enabled) 1f else 0.6f)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = type.title,
                    color = colors.textPrimary.copy(alpha = if (enabled) 1f else 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (group.alarmCount == 1) "1 alarm" else "${group.alarmCount} alarms",
                    style = type.label,
                    color = colors.textSecondary.copy(alpha = if (enabled) 1f else 0.7f)
                )
            }
            // Edit sits in the header next to the group it edits, and only while the group is
            // open, so it's in thumb reach without scrolling past every alarm, and collapsed
            // cards stay clean.
            HorizontalReveal(visible = isExpanded && !selectionMode) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .pressable(pressedScale = 0.88f, haptic = HapticFeedbackType.ContextClick, onClick = onEditGroup)
                        .clip(CircleShape)
                        .background(colors.tile)
                        .border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_tune),
                        contentDescription = "Edit ${group.name}",
                        tint = colors.accentLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalReveal(visible = !selectionMode) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                ObSwitch(checked = enabled, onCheckedChange = onToggleGroup, size = SwitchSize.Large)
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .pressable(pressedScale = 0.9f, onClick = onCardClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_down),
                        contentDescription = if (isExpanded) "Collapse ${group.name}" else "Expand ${group.name}",
                        tint = colors.textSecondary,
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
              }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = ExpandEnter,
            exit = CollapseExit
        ) {
          // Fresh scope: the member alarms cascade in each time the group opens.
          RevealScope {
            Column {
                Spacer(Modifier.height(14.dp))
                if (group.alarms.isEmpty()) {
                    // Empty group: point straight at the next step instead of a dead end.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealOnEnter(1, offsetY = 12.dp)
                            .pressable(pressedScale = 0.98f, onClick = onEditGroup)
                            .clip(ObShapes.Card)
                            .background(colors.cardInner.copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No alarms in this group yet",
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Add alarms", style = type.labelStrong, color = colors.accentLight)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        group.alarms.forEachIndexed { i, alarm ->
                            GroupAlarmRow(
                                alarm = alarm,
                                onToggle = { onToggleAlarm(alarm.id, it) },
                                modifier = Modifier.revealOnEnter(i + 1, offsetY = 12.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
          }
        }
    }
}

@Composable
private fun GroupAlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val time = alarm.clockTime
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ObShapes.Card)
            .background(colors.cardInner.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f)) {
            Text(
                time.digits,
                style = type.timeMedium,
                color = if (alarm.isEnabled) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                time.meridiem,
                style = type.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
                color = if (alarm.isEnabled) colors.accentLight else colors.textSecondary,
                modifier = Modifier.alignByBaseline()
            )
            if (alarm.label.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    alarm.label,
                    style = type.body.copy(fontSize = 13.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        ObSwitch(checked = alarm.isEnabled, onCheckedChange = onToggle, size = SwitchSize.Small)
    }
}
