package dev.redfox.alarmzy.presentation.groupedit

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.GroupIcon
import dev.redfox.alarmzy.presentation.alarmedit.BottomActionBar
import dev.redfox.alarmzy.presentation.common.clockTime
import dev.redfox.alarmzy.ui.components.AzAlertDialog
import dev.redfox.alarmzy.ui.components.AzCheckbox
import dev.redfox.alarmzy.ui.components.AzDropdownMenu
import dev.redfox.alarmzy.ui.components.AzMenuItem
import dev.redfox.alarmzy.ui.components.AzShapes
import dev.redfox.alarmzy.ui.components.DetailHeader
import dev.redfox.alarmzy.ui.components.DetailHeaderHeight
import dev.redfox.alarmzy.ui.components.GroupColorSwatches
import dev.redfox.alarmzy.ui.components.HeaderIconButton
import dev.redfox.alarmzy.ui.components.PrimaryButton
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.SectionLabel
import dev.redfox.alarmzy.ui.components.drawableRes
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import dev.redfox.alarmzy.ui.components.RevealColumn
import dev.redfox.alarmzy.ui.components.popOnSelect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import dev.redfox.alarmzy.ui.components.AzCard
import dev.redfox.alarmzy.ui.components.AzListRow
import dev.redfox.alarmzy.ui.components.PillButton
import dev.redfox.alarmzy.ui.components.ExpandEnter
import dev.redfox.alarmzy.ui.components.CollapseExit
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun GroupEditScreen(
    uiState: GroupEditUiState,
    onIntent: (GroupEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPickRingtone: () -> Unit = {},
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    var showDeleteDialog by remember { mutableStateOf(false) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailHeaderHeight

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(top = topInset + 12.dp, bottom = 128.dp)
        ) {
            SectionLabel("Group details", color = colors.textSecondary.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            NameField(name = uiState.name, onNameChange = { onIntent(GroupEditIntent.SetName(it)) })

            Spacer(Modifier.height(24.dp))
            SectionLabel(
                "Group icon",
                color = colors.textSecondary.copy(alpha = 0.7f),
                trailing = {
                    Text(
                        "Selected: ${uiState.icon?.label ?: "None"}",
                        style = type.label.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                        color = colors.textTertiary
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            IconGrid(
                selected = uiState.icon,
                tint = parseGroupColor(uiState.color) ?: colors.accent,
                onSelect = { onIntent(GroupEditIntent.SetIcon(it)) }
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("Group color", color = colors.textSecondary.copy(alpha = 0.7f))
            Spacer(Modifier.height(14.dp))
            ColorGrid(selected = uiState.color, onSelect = { onIntent(GroupEditIntent.SetColor(it)) })

            Spacer(Modifier.height(24.dp))
            SectionLabel("Alarm sound", color = colors.textSecondary.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            GroupRingtoneCard(
                uiState = uiState,
                onPick = onPickRingtone,
                onUseSettings = { onIntent(GroupEditIntent.UseSettingsRingtone) }
            )

            Spacer(Modifier.height(32.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select alarms for this group",
                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                if (uiState.selectedAlarmIds.isNotEmpty()) {
                    Text("${uiState.selectedAlarmIds.size} selected", style = type.labelStrong, color = colors.accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (uiState.allAlarms.isEmpty()) {
                Text(
                    "No alarms yet. Create an alarm first, then add it to a group.",
                    style = type.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.allAlarms.forEach { alarm ->
                        AlarmSelectionItem(
                            alarm = alarm,
                            editingGroupId = uiState.groupId,
                            isSelected = alarm.id in uiState.selectedAlarmIds,
                            onToggle = { onIntent(GroupEditIntent.ToggleAlarmSelection(alarm.id)) }
                        )
                    }
                }
            }
        }

        DetailHeader(
            title = if (uiState.isNew) "New Group" else "Edit Group",
            onBack = onNavigateBack,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (!uiState.isNew) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    HeaderIconButton(R.drawable.ic_more_vert, "Group options", onClick = { menuOpen = true })
                    AzDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AzMenuItem(
                            text = "Delete group",
                            color = colors.accentLight,
                            onClick = {
                                menuOpen = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        BottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PrimaryButton(
                text = if (uiState.isNew) "Create Group" else "Save Group",
                icon = R.drawable.ic_create_folder,
                enabled = uiState.name.isNotBlank() && !uiState.isSaving,
                onClick = { onIntent(GroupEditIntent.Save) }
            )
        }
    }

    if (showDeleteDialog) {
        AzAlertDialog(
            title = "Delete group?",
            text = "The group will be deleted. Its alarms will be kept and ungrouped.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                onIntent(GroupEditIntent.Delete)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(AzShapes.Card)
            .background(colors.cardInner)
            .border(1.dp, colors.borderStrong, AzShapes.Card)
            .padding(start = 17.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = type.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) {
                        Text("e.g. Work, Gym, Weekend", style = type.bodyLarge.copy(fontWeight = FontWeight.Normal), color = colors.textTertiary)
                    }
                    inner()
                }
            }
        )
        AnimatedVisibility(visible = name.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .pressable(pressedScale = 0.9f) { onNameChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_clear), contentDescription = "Clear name", tint = colors.textSecondary)
            }
        }
    }
}

private const val GridColumns = 6

@Composable
private fun IconGrid(selected: GroupIcon?, tint: Color, onSelect: (GroupIcon?) -> Unit) {
    val colors = AlarmzyTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val cell = (maxWidth - gap * (GridColumns - 1)) / GridColumns
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            GroupIcon.entries.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { icon ->
                        val isSel = icon == selected
                        val border by animateColorAsState(if (isSel) tint else colors.borderStrong, label = "iconBorder")
                        Box(Modifier.size(cell)) {
                            Box(
                                modifier = Modifier
                                    .semantics { contentDescription = icon.label }
                                    .popOnSelect(isSel, amount = 0.12f)
                                    .then(if (isSel) Modifier.glow(tint.copy(alpha = 0.25f), AzShapes.Tile, radius = 12.dp, offsetY = 6.dp) else Modifier)
                                    .pressable(pressedScale = 0.88f, role = Role.RadioButton, haptic = HapticFeedbackType.SegmentTick) { onSelect(if (isSel) null else icon) }
                                    .size(cell)
                                    .clip(AzShapes.Tile)
                                    .background(if (isSel) colors.tile else colors.cardInner)
                                    .border(if (isSel) 2.dp else 1.dp, border, AzShapes.Tile),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(icon.drawableRes),
                                    contentDescription = null,
                                    tint = if (isSel) tint else colors.textSecondary.copy(alpha = 0.85f)
                                )
                            }
                            if (isSel) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(4.dp, 4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(tint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_check_bold),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(selected: String?, onSelect: (String?) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        val gap = 14.dp
        val cell = (maxWidth - gap * (GridColumns - 1)) / GridColumns
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            GroupColorSwatches.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { hex ->
                        val color = parseGroupColor(hex) ?: Color.Gray
                        val isSel = hex.equals(selected, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .semantics { contentDescription = "Color $hex" }
                                .popOnSelect(isSel, amount = 0.14f)
                                .then(if (isSel) Modifier.glow(color.copy(alpha = 0.35f), CircleShape, radius = 12.dp, offsetY = 6.dp) else Modifier)
                                .pressable(pressedScale = 0.88f, role = Role.RadioButton, haptic = HapticFeedbackType.SegmentTick) { onSelect(if (isSel) null else hex) }
                                .size(cell)
                                .then(if (isSel) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .padding(if (isSel) 3.dp else 0.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(painterResource(R.drawable.ic_check_bold), contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSelectionItem(
    alarm: Alarm,
    editingGroupId: Long?,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val time = alarm.clockTime
    val inOtherGroup = alarm.groupId != null && alarm.groupId != editingGroupId
    val membership = when {
        alarm.groupId == null -> "Unassigned"
        !inOtherGroup -> "In this group"
        else -> "Currently in: ${alarm.groupName}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.985f, role = Role.Checkbox, onClick = onToggle)
            .clip(AzShapes.Card)
            .background(if (isSelected) colors.cardInner else colors.cardInner.copy(alpha = 0.7f))
            .border(1.dp, if (isSelected) colors.borderStrong else colors.border, AzShapes.Card)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzCheckbox(checked = isSelected, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    time.digits,
                    style = type.timeSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) colors.textPrimary else colors.textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    time.meridiem,
                    style = type.labelStrong.copy(letterSpacing = 0.sp),
                    color = colors.textSecondary.copy(alpha = if (isSelected) 1f else 0.7f),
                    modifier = Modifier.alignByBaseline()
                )
                if (alarm.label.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        alarm.label,
                        style = type.label,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(alarm.repeatMode.displayName, style = type.label.copy(letterSpacing = 0.sp), color = colors.textSecondary)
                Text(" • ", style = type.label, color = colors.textDisabled)
                Text(
                    membership,
                    style = type.label.copy(letterSpacing = 0.sp),
                    color = if (inOtherGroup) colors.warning.copy(alpha = 0.9f) else colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            painterResource(R.drawable.ic_alarm_small),
            contentDescription = null,
            tint = colors.textDisabled
        )
    }
}

/**
 * The group's alarm sound. Starts on "Same as Settings"; picking a sound overrides it for every
 * alarm in the group, and "Use Settings sound" reverts to following Settings.
 */
@Composable
private fun GroupRingtoneCard(uiState: GroupEditUiState, onPick: () -> Unit, onUseSettings: () -> Unit) {
    val colors = AlarmzyTheme.colors
    val custom = uiState.ringtoneUri != null
    AzCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        AzListRow(
            title = if (custom) uiState.ringtoneName ?: "Default" else uiState.settingsRingtoneName,
            subtitle = if (custom) "Only for this group" else "Same as Settings",
            icon = R.drawable.ic_bell,
            iconTint = if (custom) colors.accentLight else colors.textSecondary,
            onClick = onPick,
            trailing = {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.textSecondary)
            }
        )
        AnimatedVisibility(visible = custom, enter = ExpandEnter, exit = CollapseExit) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                PillButton(text = "Use Settings sound", onClick = onUseSettings)
            }
        }
    }
}
