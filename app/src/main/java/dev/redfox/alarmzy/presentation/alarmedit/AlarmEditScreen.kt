package dev.redfox.alarmzy.presentation.alarmedit

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode
import dev.redfox.alarmzy.presentation.common.shortTime
import dev.redfox.obsidian.components.ObAlertDialog
import dev.redfox.obsidian.components.ObDropdownMenu
import dev.redfox.obsidian.components.ObMenuItem
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.ChoicePill
import dev.redfox.obsidian.components.DayChip
import dev.redfox.obsidian.components.DetailHeader
import dev.redfox.obsidian.components.DetailHeaderHeight
import dev.redfox.obsidian.components.HeaderIconButton
import dev.redfox.obsidian.components.IconTile
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.obsidian.theme.ScreenGutter
import dev.redfox.obsidian.components.SectionLabel
import dev.redfox.obsidian.components.SegmentOption
import dev.redfox.obsidian.components.SegmentedPill
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import dev.redfox.obsidian.motion.popIn
import dev.redfox.obsidian.motion.RevealColumn
import dev.redfox.obsidian.motion.ExpandEnter
import dev.redfox.obsidian.motion.CollapseExit
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import dev.redfox.obsidian.motion.RevealScope
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.motion.ObMotion
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize

private val SeriesIntervals = listOf(5, 10, 15, 20, 30, 60)

@Composable
fun AlarmEditScreen(
    uiState: AlarmEditUiState,
    onIntent: (AlarmEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ObsidianTheme.colors
    var showEndTimePicker by remember { mutableStateOf(false) }
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
                .padding(top = topInset + 4.dp, bottom = 128.dp)
        ) {
            if (uiState.isNew) {
                SegmentedPill(
                    options = listOf(
                        SegmentOption("Single"),
                        SegmentOption("Series")
                    ),
                    selectedIndex = if (uiState.isSeries) 1 else 0,
                    onSelect = { onIntent(AlarmEditIntent.ToggleSeriesMode(it == 1)) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            AnimatedVisibility(visible = uiState.isSeries, enter = ExpandEnter, exit = CollapseExit) {
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "START TIME",
                        style = ObsidianTheme.typography.sectionLabel,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            TimeDialPicker(
                hour = uiState.hour,
                minute = uiState.minute,
                onTimeChange = { h, m -> onIntent(AlarmEditIntent.SetTime(h, m)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = uiState.isSeries,
                enter = ExpandEnter,
                exit = CollapseExit
            ) {
                SeriesSection(
                    uiState = uiState,
                    onEditEnd = { showEndTimePicker = true },
                    onIntervalSelected = { onIntent(AlarmEditIntent.SetSeriesInterval(it)) }
                )
            }

            RepeatSection(
                repeatMode = uiState.repeatMode,
                onRepeatChange = { onIntent(AlarmEditIntent.SetRepeatMode(it)) }
            )
            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.availableGroups.isNotEmpty()) {
                    GroupRow(
                        selectedGroupId = uiState.selectedGroupId,
                        groups = uiState.availableGroups,
                        onSelect = { onIntent(AlarmEditIntent.SetGroup(it)) }
                    )
                }
                LabelRow(
                    label = uiState.label,
                    onLabelChange = { onIntent(AlarmEditIntent.SetLabel(it)) }
                )
            }
        }

        DetailHeader(
            title = if (uiState.isNew) "New Alarm" else "Edit Alarm",
            onBack = onNavigateBack,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            if (!uiState.isNew) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    HeaderIconButton(R.drawable.ic_more_vert, "Alarm options", onClick = { menuOpen = true })
                    ObDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ObMenuItem(
                            text = "Delete alarm",
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
                text = when {
                    uiState.isSeries -> "Create ${uiState.seriesCount} alarms"
                    uiState.isNew -> "Create Alarm"
                    else -> "Save Changes"
                },
                icon = if (uiState.isSeries) R.drawable.ic_alarm_add else null,
                enabled = uiState.canSave,
                onClick = { onIntent(AlarmEditIntent.Save) }
            )
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Ends at",
            initialHour = uiState.seriesEndHour,
            initialMinute = uiState.seriesEndMinute,
            onConfirm = { h, m ->
                onIntent(AlarmEditIntent.SetSeriesEnd(h, m))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    if (uiState.showDuplicateDialog) {
        ObAlertDialog(
            title = "Duplicate alarms",
            text = "Alarms already exist at ${uiState.duplicateTimes.joinToString(", ")}. Create the series anyway?",
            confirmLabel = "Create",
            onConfirm = { onIntent(AlarmEditIntent.ConfirmSave) },
            onDismiss = { onIntent(AlarmEditIntent.DismissDuplicateDialog) }
        )
    }

    if (showDeleteDialog) {
        ObAlertDialog(
            title = "Delete alarm?",
            text = "This alarm will be permanently deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                onIntent(AlarmEditIntent.Delete)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * Gradient-faded footer holding the screen's primary CTA above the gesture bar.
 * Slides away while the keyboard is open so it never covers the field being edited.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val bg = ObsidianTheme.colors.background
    AnimatedVisibility(
        visible = !WindowInsets.isImeVisible,
        enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 360f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } + fadeIn(),
        exit = slideOutVertically(spring(dampingRatio = 1f, stiffness = 600f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } + fadeOut(),
        modifier = modifier
    ) {
        BottomActionBarContent(bg, content)
    }
}

@Composable
private fun BottomActionBarContent(bg: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to bg.copy(alpha = 0f), 0.35f to bg.copy(alpha = 0.95f), 1f to bg))
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(start = ScreenGutter, end = ScreenGutter, top = 20.dp, bottom = 12.dp)
    ) {
        content()
    }
}

@Composable
private fun SeriesSection(
    uiState: AlarmEditUiState,
    onEditEnd: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
) {
    val colors = ObsidianTheme.colors
    Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormRow(
            icon = R.drawable.ic_ends_at,
            title = "Ends at",
            onClick = onEditEnd
        ) {
            RowValue(shortTime(uiState.seriesEndHour, uiState.seriesEndMinute))
        }

        var intervalMenuOpen by remember { mutableStateOf(false) }
        Box {
            FormRow(
                icon = R.drawable.ic_interval,
                title = "Interval",
                onClick = { intervalMenuOpen = true }
            ) {
                RowValue("${uiState.seriesIntervalMinutes} min")
            }
            ObDropdownMenu(
                expanded = intervalMenuOpen,
                onDismissRequest = { intervalMenuOpen = false },
                modifier = Modifier.width(160.dp)
            ) {
                SeriesIntervals.forEach { minutes ->
                    val selected = minutes == uiState.seriesIntervalMinutes
                    ObMenuItem(
                        text = "Every $minutes min",
                        selected = selected,
                        trailingIcon = if (selected) R.drawable.ic_check else null,
                        onClick = {
                            onIntervalSelected(minutes)
                            intervalMenuOpen = false
                        }
                    )
                }
            }
        }

        SeriesSummary(uiState)
    }
}

@Composable
private fun SeriesSummary(uiState: AlarmEditUiState) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    val valid = uiState.seriesRangeValid && uiState.seriesWithinCap
    val tone by animateColorAsState(if (valid) colors.accentLight else colors.error, ObMotion.gentle(), label = "summaryTone")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(ObMotion.snappy())
            .clip(ObShapes.Card)
            .background(colors.cardInner.copy(alpha = 0.8f))
            .background(Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.1f), colors.accent.copy(alpha = 0f))))
            .border(1.dp, tone.copy(alpha = 0.2f), ObShapes.Card)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(tone.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_info_small), contentDescription = null, tint = tone)
        }
        Spacer(Modifier.width(12.dp))
        val body = type.label.copy(letterSpacing = type.body.letterSpacing, lineHeight = type.label.lineHeight)
        when {
            !uiState.seriesRangeValid -> Text("End time must be after the start time", style = body, color = colors.error)
            !uiState.seriesWithinCap -> Text(
                "That's ${uiState.seriesCount} alarms — the limit is ${AlarmEditUiState.MAX_SERIES_ALARMS}. Try a longer interval.",
                style = body,
                color = colors.error
            )
            else -> Text(
                buildAnnotatedString {
                    append("Creates ")
                    withStyle(SpanStyle(color = colors.accentLight, fontWeight = FontWeight.SemiBold)) {
                        append("${uiState.seriesCount} alarms")
                    }
                    append(
                        " · ${shortTime(uiState.hour, uiState.minute)}–" +
                            "${shortTime(uiState.seriesEndHour, uiState.seriesEndMinute)} every ${uiState.seriesIntervalMinutes} min"
                    )
                },
                style = body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun RepeatSection(repeatMode: RepeatMode, onRepeatChange: (RepeatMode) -> Unit) {
    Column {
        SectionLabel("Repeat")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Once" to RepeatMode.OneTime,
                "Daily" to RepeatMode.Daily,
                "Weekdays" to RepeatMode.Weekdays,
                "Weekends" to RepeatMode.Weekends
            ).forEach { (label, mode) ->
                ChoicePill(text = label, selected = repeatMode == mode, onClick = { onRepeatChange(mode) })
            }
            val isCustom = repeatMode is RepeatMode.Custom
            ChoicePill(
                text = "Custom",
                selected = isCustom,
                onClick = {
                    if (!isCustom) onRepeatChange(RepeatMode.Custom(repeatMode.toDays()))
                }
            )
        }
        AnimatedVisibility(
            visible = repeatMode is RepeatMode.Custom,
            enter = ExpandEnter,
            exit = CollapseExit
        ) {
          // Fresh scope so the days deal in left to right each time Custom opens.
          RevealScope {
            val days = repeatMode.toDays()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.entries.forEachIndexed { i, day ->
                    DayChip(
                        modifier = Modifier.revealOnEnter(i, offsetY = 10.dp),
                        letter = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        selected = day in days,
                        onClick = {
                            val newDays = if (day in days) days - day else days + day
                            onRepeatChange(RepeatMode.Custom(newDays))
                        }
                    )
                }
            }
          }
        }
    }
}

@Composable
private fun GroupRow(selectedGroupId: Long?, groups: List<AlarmGroup>, onSelect: (Long?) -> Unit) {
    val colors = ObsidianTheme.colors
    var open by remember { mutableStateOf(false) }
    val selected = groups.firstOrNull { it.id == selectedGroupId }
    Box {
        FormRow(icon = R.drawable.ic_folder, title = "Group", onClick = { open = true }) {
            RowValue(selected?.name ?: "None", muted = selected == null)
        }
        ObDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(220.dp)) {
            ObMenuItem(
                text = "None",
                selected = selectedGroupId == null,
                trailingIcon = if (selectedGroupId == null) R.drawable.ic_check else null,
                onClick = { onSelect(null); open = false }
            )
            groups.forEach { group ->
                val isSel = group.id == selectedGroupId
                ObMenuItem(
                    text = group.name,
                    selected = isSel,
                    leading = {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(parseGroupColor(group.color) ?: colors.textSecondary)
                        )
                    },
                    trailingIcon = if (isSel) R.drawable.ic_check else null,
                    onClick = { onSelect(group.id); open = false }
                )
            }
        }
    }
}

@Composable
private fun LabelRow(label: String, onLabelChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    FormRowContainer {
        IconTile(icon = R.drawable.ic_label, size = 36.dp, shape = CircleShape)
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = label,
            onValueChange = onLabelChange,
            singleLine = true,
            textStyle = type.bodyLarge.copy(fontWeight = FontWeight.Normal, color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (label.isEmpty()) {
                        Text("Label", style = type.bodyLarge.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary.copy(alpha = 0.8f))
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun FormRowContainer(
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ObsidianTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier)
            .clip(ObShapes.Card)
            .background(colors.cardInner)
            .border(1.dp, colors.border, ObShapes.Card)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Squircle form card: circular icon tile, title, trailing value. */
@Composable
private fun FormRow(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    FormRowContainer(onClick = onClick) {
        IconTile(icon = icon, size = 36.dp, shape = CircleShape)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = ObsidianTheme.typography.bodyLarge,
            color = ObsidianTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun RowValue(text: String, muted: Boolean = false) {
    val colors = ObsidianTheme.colors
    Text(
        text,
        style = ObsidianTheme.typography.bodyLarge.copy(fontWeight = if (muted) FontWeight.Normal else FontWeight.Medium),
        color = if (muted) colors.textSecondary.copy(alpha = 0.8f) else colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.width(6.dp))
    Icon(
        painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = colors.textSecondary.copy(alpha = 0.7f)
    )
}

@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ObsidianTheme.colors
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .popIn()
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(ObShapes.CardLarge)
                .background(colors.card)
                .border(1.dp, colors.border, ObShapes.CardLarge)
                .padding(PaddingValues(top = 24.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = ObsidianTheme.typography.title, color = colors.textPrimary)
            Spacer(Modifier.height(20.dp))
            TimeDialPicker(
                hour = hour,
                minute = minute,
                onTimeChange = { h, m -> hour = h; minute = m },
                dialSize = 224.dp
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton(text = "Cancel", onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                PillButton(
                    text = "Done",
                    onClick = { onConfirm(hour, minute) },
                    containerColor = colors.accentContainerStrong,
                    contentColor = colors.accentLight
                )
            }
        }
    }
}
