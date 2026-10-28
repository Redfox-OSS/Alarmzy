package dev.redfox.alarmzy.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsSettingsFooter
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsSettingsRows
import dev.redfox.obsidian.navigation.floatingNavClearance
import dev.redfox.obsidian.components.ObSwitch
import dev.redfox.obsidian.components.ObDivider
import dev.redfox.obsidian.components.ObListRow
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.ChoicePill
import dev.redfox.obsidian.components.LargeHeader
import dev.redfox.obsidian.components.LargeHeaderHeight
import dev.redfox.obsidian.theme.ScreenGutter
import dev.redfox.obsidian.components.SwitchSize
import dev.redfox.obsidian.effects.glassSource
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.motion.RevealColumn
import dev.redfox.obsidian.motion.popOnSelect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import dev.redfox.obsidian.motion.ExpandEnter
import dev.redfox.obsidian.motion.CollapseExit

private val SnoozeOptions = listOf(5, 10, 15, 20, 30)

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    overlayPermissionGranted: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
    onPickRingtone: () -> Unit,
    onCustomizeAlarmScreen: () -> Unit = {},
    onCustomizeWidget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ObsidianTheme.colors
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + LargeHeaderHeight

    Box(modifier.fillMaxSize().background(colors.background)) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(top = topInset + 8.dp, bottom = floatingNavClearance() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Only shown while the overlay permission is missing — nothing actionable once granted.
            if (!overlayPermissionGranted) {
                SettingsSection("Permissions") {
                    SettingsCard {
                        Row(
                            Modifier.fillMaxWidth().pressable(pressedScale = 0.985f, onClick = onRequestOverlayPermission),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(ObShapes.Tile).background(colors.warning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Display over other apps", style = ObsidianTheme.typography.labelStrong, color = colors.textPrimary)
                                Text(
                                    "Required for the fullscreen alarm screen. Tap to grant.",
                                    style = ObsidianTheme.typography.body,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            ChevronRight()
                        }
                    }
                }
            }

            SettingsSection("Appearance") {
                SettingsCard {
                    ThemeVariantRow(
                        selected = uiState.darkVariant,
                        onSelect = { onIntent(SettingsIntent.SetDarkVariant(it)) }
                    )
                    ObDivider()
                    AccentColorPicker(
                        selected = uiState.accentColor,
                        onSelect = { onIntent(SettingsIntent.SetAccentColor(it)) }
                    )
                }
            }

            SettingsSection("Alarm settings") {
                SettingsCard {
                    ObListRow(
                        title = "Customize alarm screen",
                        subtitle = "Background, buttons, clock position and size",
                        icon = R.drawable.ic_tune,
                        onClick = onCustomizeAlarmScreen,
                        trailing = { ChevronRight() }
                    )
                    ObDivider()
                    ObListRow(
                        title = "Ringtone",
                        subtitle = uiState.defaultRingtoneName,
                        icon = R.drawable.ic_bell,
                        onClick = onPickRingtone,
                        trailing = { ChevronRight() }
                    )
                    ObDivider()
                    SnoozeRow(
                        selectedMinutes = uiState.defaultSnoozeDuration,
                        onSelect = { onIntent(SettingsIntent.SetDefaultSnoozeDuration(it)) }
                    )
                    ObDivider()
                    ObListRow(
                        title = "Vibration",
                        subtitle = "Vibrate when an alarm rings",
                        icon = R.drawable.ic_vibration,
                        onClick = { onIntent(SettingsIntent.SetDefaultVibration(!uiState.defaultVibration)) },
                        trailing = {
                            ObSwitch(
                                checked = uiState.defaultVibration,
                                onCheckedChange = { onIntent(SettingsIntent.SetDefaultVibration(it)) },
                                size = SwitchSize.Medium
                            )
                        }
                    )
                    ObDivider()
                    ObListRow(
                        title = "Gradual volume increase",
                        subtitle = "Start quiet and get louder",
                        icon = R.drawable.ic_volume,
                        onClick = { onIntent(SettingsIntent.SetDefaultGradualVolume(!uiState.defaultGradualVolume)) },
                        trailing = {
                            ObSwitch(
                                checked = uiState.defaultGradualVolume,
                                onCheckedChange = { onIntent(SettingsIntent.SetDefaultGradualVolume(it)) },
                                size = SwitchSize.Medium
                            )
                        }
                    )
                }
            }

            SettingsSection("Widget") {
                SettingsCard {
                    ObListRow(
                        title = "Home-screen widget",
                        subtitle = "Date, time and weather: layout, size, tap actions",
                        icon = R.drawable.ic_widgets,
                        onClick = onCustomizeWidget,
                        trailing = { ChevronRight() }
                    )
                }
            }

            SettingsSection("Live Alerts") {
                SettingsCard {
                    LiveAlertsSettingsRows(onPermissionsChanged = { onIntent(SettingsIntent.RestoreLiveAlerts) })
                }
                LiveAlertsSettingsFooter()
            }

            SettingsSection("About") {
                SettingsCard {
                    ObListRow(title = "Alarmzy", subtitle = "Version ${appVersionName()}", icon = R.drawable.ic_info)
                }
            }
        }

        LargeHeader(title = "Settings", modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = ObsidianTheme.typography.caption,
            color = ObsidianTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = ObsidianTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glow(Color.Black.copy(alpha = 0.25f), ObShapes.CardLarge, radius = 6.dp, offsetY = 4.dp)
            .clip(ObShapes.CardLarge)
            .background(colors.card)
            .border(1.dp, colors.border, ObShapes.CardLarge)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun ChevronRight() {
    Icon(
        painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = ObsidianTheme.colors.textSecondary
    )
}

@Composable
private fun ExpandChevron(expanded: Boolean) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "expandChevron")
    Icon(
        painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = ObsidianTheme.colors.textSecondary,
        modifier = Modifier.rotate(rotation)
    )
}

@Composable
private fun ThemeVariantRow(selected: DarkVariant, onSelect: (DarkVariant) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ObListRow(
            title = "Theme",
            subtitle = if (selected == DarkVariant.DARK) "Dark" else "Dark · ${selected.displayName}",
            icon = R.drawable.ic_palette,
            onClick = { expanded = !expanded },
            trailing = { ExpandChevron(expanded) }
        )
        AnimatedVisibility(visible = expanded, enter = ExpandEnter, exit = CollapseExit) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkVariant.entries.forEach { variant ->
                    ChoicePill(text = variant.displayName, selected = variant == selected, onClick = { onSelect(variant) })
                }
            }
        }
    }
}

@Composable
private fun AccentColorPicker(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ObListRow(
            title = "Accent color",
            subtitle = selected.displayName,
            icon = R.drawable.ic_format_paint,
            trailing = {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accentLight.copy(alpha = 0.2f))
                        .border(1.dp, colors.accentLight.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
                    Spacer(Modifier.width(6.dp))
                    Text("Active", style = type.labelStrong, color = colors.accentLight)
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AccentColor.entries.forEach { accent ->
                val isSel = accent == selected
                val swatch = Color(accent.swatch)
                Column(
                    modifier = Modifier
                        .semantics { contentDescription = "${accent.displayName} accent" }
                        .popOnSelect(isSel, amount = 0.1f)
                        .pressable(pressedScale = 0.9f, role = Role.RadioButton, haptic = HapticFeedbackType.SegmentTick) { onSelect(accent) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .then(if (isSel) Modifier.glow(swatch.copy(alpha = 0.35f), ObShapes.Tile, radius = 12.dp, offsetY = 6.dp) else Modifier)
                            .size(48.dp)
                            .alpha(if (isSel) 1f else 0.8f)
                            .clip(ObShapes.Tile)
                            .background(swatch)
                            .then(if (isSel) Modifier.border(2.dp, colors.accentLight.copy(alpha = 0.4f), ObShapes.Tile) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Icon(
                                painterResource(R.drawable.ic_check_bold),
                                contentDescription = null,
                                // Keep the check legible on the white Luminous swatch.
                                tint = if (accent == AccentColor.LUMINOUS) Color(0xFF1A1A1D) else Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        accent.displayName,
                        style = type.caption.copy(
                            fontWeight = if (isSel) FontWeight.Medium else FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = if (isSel) colors.textPrimary else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SnoozeRow(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ObListRow(
            title = "Snooze duration",
            subtitle = "$selectedMinutes minutes",
            icon = R.drawable.ic_snooze,
            onClick = { expanded = !expanded },
            trailing = { ExpandChevron(expanded) }
        )
        AnimatedVisibility(visible = expanded, enter = ExpandEnter, exit = CollapseExit) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnoozeOptions.forEach { minutes ->
                    ChoicePill(
                        text = "$minutes min",
                        selected = minutes == selectedMinutes,
                        onClick = { onSelect(minutes) }
                    )
                }
            }
        }
    }
}
