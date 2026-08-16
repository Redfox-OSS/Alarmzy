package dev.redfox.alarmzy.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.alarmzy.domain.model.ThemeMode
import dev.redfox.alarmzy.presentation.paywall.PaywallBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    overlayPermissionGranted: Boolean = false,
    onRequestOverlayPermission: () -> Unit = {},
    onPickRingtone: () -> Unit = {},
    onCustomizeAlarmScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPaywall by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        modifier = modifier
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))

        SettingsSectionHeader("Alarmzy Pro")
        SettingsSection {
            if (uiState.isPro) {
                SettingsTile(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Pro unlocked",
                    subtitle = "Thanks for supporting Alarmzy"
                )
            } else {
                SettingsTile(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Unlock Alarmzy Pro",
                    subtitle = "Unlimited groups, unlimited alarms per group, alarm series",
                    onClick = { showPaywall = true },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Permissions section only appears while the overlay permission is missing.
        // Once granted, there's nothing actionable left here, so it's hidden.
        if (!overlayPermissionGranted) {
            SettingsSectionHeader("Permissions")
            SettingsSection {
                SettingsTile(
                    icon = Icons.Default.Warning,
                    title = "Display over other apps",
                    subtitle = "Required for the fullscreen alarm screen. Tap to grant.",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onRequestOverlayPermission,
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        SettingsSectionHeader("Appearance")
        SettingsSection {
            ThemeModeCard(
                selectedMode = uiState.themeMode,
                selectedVariant = uiState.darkVariant,
                onSelectMode = { onIntent(SettingsIntent.SetThemeMode(it)) },
                onSelectVariant = { onIntent(SettingsIntent.SetDarkVariant(it)) }
            )
            AccentColorTile(
                selected = uiState.accentColor,
                onSelect = { onIntent(SettingsIntent.SetAccentColor(it)) }
            )
        }

        SettingsSectionHeader("Alarm ringing screen")
        SettingsSection {
            SettingsTile(
                icon = Icons.Default.Fullscreen,
                title = "Customize alarm screen",
                subtitle = "Background, buttons, clock position and size",
                onClick = onCustomizeAlarmScreen,
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        SettingsSectionHeader("Default alarm settings")
        SettingsSection {
            SettingsTile(
                icon = Icons.Default.MusicNote,
                title = "Ringtone",
                subtitle = uiState.defaultRingtoneName,
                onClick = onPickRingtone,
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            SnoozeDurationPickerSetting(
                selectedMinutes = uiState.defaultSnoozeDuration,
                onSelect = { onIntent(SettingsIntent.SetDefaultSnoozeDuration(it)) }
            )
            SettingsTile(
                icon = Icons.Default.Vibration,
                title = "Vibration",
                subtitle = "Enable vibration for new alarms",
                trailing = {
                    Switch(
                        checked = uiState.defaultVibration,
                        onCheckedChange = { onIntent(SettingsIntent.SetDefaultVibration(it)) }
                    )
                }
            )
            SettingsTile(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Gradual volume increase",
                subtitle = "Gradually increase volume for new alarms",
                trailing = {
                    Switch(
                        checked = uiState.defaultGradualVolume,
                        onCheckedChange = { onIntent(SettingsIntent.SetDefaultGradualVolume(it)) }
                    )
                }
            )
        }

        SettingsSectionHeader("About")
        SettingsSection {
            SettingsTile(
                icon = Icons.Default.Info,
                title = "Alarmzy",
                subtitle = "Version 1.0"
            )
        }

        Spacer(Modifier.height(24.dp))
    }
    }

    if (showPaywall) {
        PaywallBottomSheet(onDismiss = { showPaywall = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentColorTile(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExpandableSettingsCard(
        icon = Icons.Default.Palette,
        title = "Accent color",
        valueSummary = selected.displayName,
        expanded = expanded,
        onToggleExpand = { expanded = !expanded }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccentColor.entries.forEach { accent ->
                val isSelected = accent == selected
                val swatch = Color(accent.swatch)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (accent == AccentColor.DYNAMIC)
                                MaterialTheme.colorScheme.surfaceVariant
                            else swatch,
                            CircleShape
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(accent) },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        accent == AccentColor.DYNAMIC -> Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = accent.displayName,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        isSelected -> Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun themeModeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun ThemeModeCard(
    selectedMode: ThemeMode,
    selectedVariant: DarkVariant,
    onSelectMode: (ThemeMode) -> Unit,
    onSelectVariant: (DarkVariant) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val summary = themeModeLabel(selectedMode) +
        if (selectedMode == ThemeMode.DARK) " · ${selectedVariant.displayName}" else ""

    ExpandableSettingsCard(
        icon = Icons.Default.Brightness6,
        title = "Theme",
        valueSummary = summary,
        expanded = expanded,
        onToggleExpand = { expanded = !expanded }
    ) {
        Column {
            ThemeMode.entries.forEach { mode ->
                SelectableOptionRow(
                    label = themeModeLabel(mode),
                    selected = mode == selectedMode,
                    onClick = { onSelectMode(mode) }
                )
            }
            AnimatedVisibility(
                visible = selectedMode == ThemeMode.DARK,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DarkVariant.entries.forEach { variant ->
                        SelectableOptionRow(
                            label = variant.displayName,
                            selected = variant == selectedVariant,
                            onClick = { onSelectVariant(variant) }
                        )
                    }
                }
            }
        }
    }
}

/** Inline expand/collapse card — header row with a trailing chevron, animated body below. */
@Composable
private fun ExpandableSettingsCard(
    icon: ImageVector,
    title: String,
    valueSummary: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        valueSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SnoozeDurationPickerSetting(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    SettingsDropdownTile(
        label = "Default snooze duration",
        value = "$selectedMinutes minutes",
        icon = Icons.Default.Snooze,
        options = listOf(5, 10, 15, 20, 30),
        optionLabel = { "$it minutes" },
        isSelected = { it == selectedMinutes },
        onSelect = onSelect
    )
}

@Composable
private fun <T> SettingsDropdownTile(
    label: String,
    value: String,
    icon: ImageVector,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            options.forEach { option ->
                val selected = isSelected(option)
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(option)
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
