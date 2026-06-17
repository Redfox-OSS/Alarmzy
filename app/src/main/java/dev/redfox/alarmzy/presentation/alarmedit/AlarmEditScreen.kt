package dev.redfox.alarmzy.presentation.alarmedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    uiState: AlarmEditUiState,
    onIntent: (AlarmEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.hour,
        initialMinute = uiState.minute,
        is24Hour = false
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        if (timePickerState.hour != uiState.hour || timePickerState.minute != uiState.minute) {
            onIntent(AlarmEditIntent.SetTime(timePickerState.hour, timePickerState.minute))
        }
    }

    var showEndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { onIntent(AlarmEditIntent.Delete) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isNew) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !uiState.isSeries,
                        onClick = { onIntent(AlarmEditIntent.ToggleSeriesMode(false)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Single") }
                    SegmentedButton(
                        selected = uiState.isSeries,
                        onClick = { onIntent(AlarmEditIntent.ToggleSeriesMode(true)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Series") }
                }
            }

            if (uiState.isSeries) {
                Text(
                    "Start time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }

            if (uiState.isSeries) {
                SeriesValueTile(
                    icon = Icons.Default.Schedule,
                    label = "Ends at",
                    value = format12h(uiState.seriesEndHour, uiState.seriesEndMinute),
                    onClick = { showEndTimePicker = true }
                )

                IntervalPicker(
                    selectedMinutes = uiState.seriesIntervalMinutes,
                    onSelect = { onIntent(AlarmEditIntent.SetSeriesInterval(it)) }
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (uiState.canSave)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            !uiState.seriesRangeValid -> "End time must be after the start time."
                            !uiState.seriesWithinCap ->
                                "That's too many alarms (max ${AlarmEditUiState.MAX_SERIES_ALARMS}). Use a longer interval."
                            else -> "Creates ${uiState.seriesCount} alarms · " +
                                "${format12h(uiState.hour, uiState.minute)}–" +
                                "${format12h(uiState.seriesEndHour, uiState.seriesEndMinute)} " +
                                "every ${uiState.seriesIntervalMinutes} min"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.canSave)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Text("Repeat", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val presets = listOf(
                    "Once" to RepeatMode.OneTime,
                    "Daily" to RepeatMode.Daily,
                    "Weekdays" to RepeatMode.Weekdays,
                    "Weekends" to RepeatMode.Weekends
                )
                presets.forEach { (label, mode) ->
                    FilterChip(
                        selected = uiState.repeatMode == mode,
                        onClick = { onIntent(AlarmEditIntent.SetRepeatMode(mode)) },
                        label = { Text(label) }
                    )
                }
                val isCustom = uiState.repeatMode is RepeatMode.Custom
                FilterChip(
                    selected = isCustom,
                    onClick = {
                        if (!isCustom) {
                            onIntent(AlarmEditIntent.SetRepeatMode(RepeatMode.Custom(emptySet())))
                        }
                    },
                    label = { Text("Custom") }
                )
            }

            if (uiState.repeatMode is RepeatMode.Custom || uiState.repeatMode is RepeatMode.Custom) {
                val currentDays = uiState.repeatMode.toDays()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in currentDays,
                            onClick = {
                                val newDays = if (day in currentDays) currentDays - day else currentDays + day
                                onIntent(AlarmEditIntent.SetRepeatMode(RepeatMode.Custom(newDays)))
                            },
                            label = {
                                Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }
            }

            if (uiState.availableGroups.isNotEmpty()) {
                GroupPicker(
                    selectedGroupId = uiState.selectedGroupId,
                    groups = uiState.availableGroups,
                    onSelect = { onIntent(AlarmEditIntent.SetGroup(it)) }
                )
            }

            TextField(
                value = uiState.label,
                onValueChange = { onIntent(AlarmEditIntent.SetLabel(it)) },
                label = { Text("Label") },
                placeholder = { Text("Add a label") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onIntent(AlarmEditIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSave
            ) {
                Text(
                    when {
                        uiState.isSeries -> "Create ${uiState.seriesCount} alarms"
                        uiState.isNew -> "Create Alarm"
                        else -> "Save Changes"
                    }
                )
            }

            if (!uiState.isNew) {
                OutlinedButton(
                    onClick = { onIntent(AlarmEditIntent.Delete) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Alarm")
                }
            }
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "End time",
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
        AlertDialog(
            onDismissRequest = { onIntent(AlarmEditIntent.DismissDuplicateDialog) },
            title = { Text("Duplicate alarms") },
            text = {
                Text(
                    "Alarms already exist at ${uiState.duplicateTimes.joinToString(", ")}. " +
                        "Create the series anyway?"
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(AlarmEditIntent.ConfirmSave) }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(AlarmEditIntent.DismissDuplicateDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun parseColorOrNull(hex: String?): Color? = hex?.let {
    try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
}

@Composable
private fun ColorDot(color: Color?, modifier: Modifier = Modifier) {
    if (color != null) {
        Box(
            modifier = modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
    } else {
        Box(
            modifier = modifier
                .size(14.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
private fun GroupPicker(
    selectedGroupId: Long?,
    groups: List<AlarmGroup>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = groups.find { it.id == selectedGroupId }
    val selectedColor = parseColorOrNull(selected?.color)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            (selectedColor ?: MaterialTheme.colorScheme.primary)
                                .copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = selectedColor ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Group",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        selected?.name ?: "None",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            GroupMenuItem(
                name = "None",
                color = null,
                isSelected = selectedGroupId == null,
                onClick = { onSelect(null); expanded = false }
            )
            groups.forEach { group ->
                GroupMenuItem(
                    name = group.name,
                    color = parseColorOrNull(group.color),
                    isSelected = group.id == selectedGroupId,
                    onClick = { onSelect(group.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun GroupMenuItem(
    name: String,
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        leadingIcon = { ColorDot(color = color) },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

private fun format12h(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h12, minute, period)
}

@Composable
private fun TileIconCircle(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SeriesValueTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TileIconCircle(icon)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IntervalPicker(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(5, 10, 15, 20, 30, 60)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TileIconCircle(Icons.Default.Timelapse)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Interval",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$selectedMinutes min",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            options.forEach { minutes ->
                val selected = minutes == selectedMinutes
                DropdownMenuItem(
                    text = {
                        Text(
                            "$minutes min",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(minutes)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
