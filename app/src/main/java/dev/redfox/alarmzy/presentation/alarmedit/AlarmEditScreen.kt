package dev.redfox.alarmzy.presentation.alarmedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.domain.model.RepeatMode
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    uiState: AlarmEditUiState,
    onIntent: (AlarmEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onPickRingtone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.hour,
        initialMinute = uiState.minute,
        is24Hour = true
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        if (timePickerState.hour != uiState.hour || timePickerState.minute != uiState.minute) {
            onIntent(AlarmEditIntent.SetTime(timePickerState.hour, timePickerState.minute))
        }
    }

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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }

            OutlinedTextField(
                value = uiState.label,
                onValueChange = { onIntent(AlarmEditIntent.SetLabel(it)) },
                label = { Text("Label") },
                placeholder = { Text("Alarm label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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

            ListItem(
                headlineContent = { Text("Ringtone") },
                supportingContent = { Text(uiState.ringtoneName) },
                leadingContent = {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onPickRingtone)
            )

            ListItem(
                headlineContent = { Text("Vibration") },
                trailingContent = {
                    Switch(
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { onIntent(AlarmEditIntent.SetVibration(it)) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Gradual volume increase") },
                trailingContent = {
                    Switch(
                        checked = uiState.gradualVolumeIncrease,
                        onCheckedChange = { onIntent(AlarmEditIntent.SetGradualVolume(it)) }
                    )
                }
            )

            SnoozeDurationPicker(
                selectedMinutes = uiState.snoozeDurationMinutes,
                onSelect = { onIntent(AlarmEditIntent.SetSnoozeDuration(it)) }
            )

            if (uiState.availableGroups.isNotEmpty()) {
                GroupPicker(
                    selectedGroupId = uiState.selectedGroupId,
                    groups = uiState.availableGroups,
                    onSelect = { onIntent(AlarmEditIntent.SetGroup(it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onIntent(AlarmEditIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isNew) "Create Alarm" else "Save Changes")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozeDurationPicker(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30)
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "$selectedMinutes minutes",
            onValueChange = {},
            readOnly = true,
            label = { Text("Snooze duration") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("$minutes minutes") },
                    onClick = {
                        onSelect(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupPicker(
    selectedGroupId: Long?,
    groups: List<dev.redfox.alarmzy.domain.model.AlarmGroup>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = groups.find { it.id == selectedGroupId }?.name ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Group") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                        onSelect(group.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
