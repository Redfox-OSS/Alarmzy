package dev.redfox.alarmzy.presentation.alarmedit

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import dev.redfox.alarmzy.ui.components.AzAlertDialog
import dev.redfox.alarmzy.ui.components.AzDropdownMenu
import dev.redfox.alarmzy.ui.components.AzMenuItem
import dev.redfox.alarmzy.ui.components.AzShapes
import dev.redfox.alarmzy.ui.components.ChoicePill
import dev.redfox.alarmzy.ui.components.DayChip
import dev.redfox.alarmzy.ui.components.DetailHeader
import dev.redfox.alarmzy.ui.components.DetailHeaderHeight
import dev.redfox.alarmzy.ui.components.HeaderIconButton
import dev.redfox.alarmzy.ui.components.IconTile
import dev.redfox.alarmzy.ui.components.PillButton
import dev.redfox.alarmzy.ui.components.PrimaryButton
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.SectionLabel
import dev.redfox.alarmzy.ui.components.SegmentOption
import dev.redfox.alarmzy.ui.components.SegmentedPill
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val SeriesIntervals = listOf(5, 10, 15, 20, 30, 60)

@Composable
fun AlarmEditScreen(
    uiState: AlarmEditUiState,
    onIntent: (AlarmEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AlarmzyTheme.colors
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailHeaderHeight

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        Column(
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
                        SegmentOption("Series", trailingIcon = if (uiState.isPro) null else Icons.Rounded.Lock)
                    ),
                    selectedIndex = if (uiState.isSeries) 1 else 0,
                    onSelect = { onIntent(AlarmEditIntent.ToggleSeriesMode(it == 1)) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            AnimatedVisibility(visible = uiState.isSeries) {
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "START TIME",
                        style = AlarmzyTheme.typography.sectionLabel,
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
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
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
                    AzDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AzMenuItem(
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
        AzAlertDialog(
            title = "Duplicate alarms",
            text = "Alarms already exist at ${uiState.duplicateTimes.joinToString(", ")}. Create the series anyway?",
            confirmLabel = "Create",
            onConfirm = { onIntent(AlarmEditIntent.ConfirmSave) },
            onDismiss = { onIntent(AlarmEditIntent.DismissDuplicateDialog) }
        )
    }

    if (showDeleteDialog) {
        AzAlertDialog(
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
    val bg = AlarmzyTheme.colors.background
    AnimatedVisibility(
        visible = !WindowInsets.isImeVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
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
    val colors = AlarmzyTheme.colors
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
            AzDropdownMenu(
                expanded = intervalMenuOpen,
                onDismissRequest = { intervalMenuOpen = false },
                modifier = Modifier.width(160.dp)
            ) {
                SeriesIntervals.forEach { minutes ->
                    val selected = minutes == uiState.seriesIntervalMinutes
                    AzMenuItem(
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
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val valid = uiState.seriesRangeValid && uiState.seriesWithinCap
    val tone = if (valid) colors.accentLight else colors.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzShapes.Card)
            .background(colors.cardInner.copy(alpha = 0.8f))
            .background(Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.1f), colors.accent.copy(alpha = 0f))))
            .border(1.dp, tone.copy(alpha = 0.2f), AzShapes.Card)
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
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val days = repeatMode.toDays()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.entries.forEach { day ->
                    DayChip(
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

@Composable
private fun GroupRow(selectedGroupId: Long?, groups: List<AlarmGroup>, onSelect: (Long?) -> Unit) {
    val colors = AlarmzyTheme.colors
    var open by remember { mutableStateOf(false) }
    val selected = groups.firstOrNull { it.id == selectedGroupId }
    Box {
        FormRow(icon = R.drawable.ic_folder, title = "Group", onClick = { open = true }) {
            RowValue(selected?.name ?: "None", muted = selected == null)
        }
        AzDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(220.dp)) {
            AzMenuItem(
                text = "None",
                selected = selectedGroupId == null,
                trailingIcon = if (selectedGroupId == null) R.drawable.ic_check else null,
                onClick = { onSelect(null); open = false }
            )
            groups.forEach { group ->
                val isSel = group.id == selectedGroupId
                AzMenuItem(
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
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
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
    val colors = AlarmzyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier)
            .clip(AzShapes.Card)
            .background(colors.cardInner)
            .border(1.dp, colors.border, AzShapes.Card)
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
            style = AlarmzyTheme.typography.bodyLarge,
            color = AlarmzyTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun RowValue(text: String, muted: Boolean = false) {
    val colors = AlarmzyTheme.colors
    Text(
        text,
        style = AlarmzyTheme.typography.bodyLarge.copy(fontWeight = if (muted) FontWeight.Normal else FontWeight.Medium),
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
    val colors = AlarmzyTheme.colors
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(AzShapes.CardLarge)
                .background(colors.card)
                .border(1.dp, colors.border, AzShapes.CardLarge)
                .padding(PaddingValues(top = 24.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = AlarmzyTheme.typography.title, color = colors.textPrimary)
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
