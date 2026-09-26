package dev.redfox.alarmzy.presentation.ringtone

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.presentation.alarmedit.BottomActionBar
import dev.redfox.alarmzy.ui.components.AzShapes
import dev.redfox.alarmzy.ui.components.DetailHeader
import dev.redfox.alarmzy.ui.components.DetailHeaderHeight
import dev.redfox.alarmzy.ui.components.PrimaryButton
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.SectionLabel
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

@Composable
fun RingtonePickerScreen(
    uiState: RingtonePickerUiState,
    onIntent: (RingtonePickerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AlarmzyTheme.colors
    val pickAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read access so the alarm can still play this file later
            // (across process death and reboots).
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onIntent(RingtonePickerIntent.AddCustomRingtone(uri.toString()))
        }
    }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailHeaderHeight
    val count = uiState.ringtones.size

    Box(modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().glassSource(),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = topInset + 8.dp, bottom = 128.dp)
        ) {
            item(key = "choose_from_device") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(pressedScale = 0.985f) { pickAudioLauncher.launch(arrayOf("audio/*")) }
                        .clip(AzShapes.CardLarge)
                        .background(colors.card)
                        .border(1.dp, colors.accentBorder, AzShapes.CardLarge)
                        .padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(AzShapes.Tile).background(colors.accentContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.accentLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Choose from device", style = AlarmzyTheme.typography.labelStrong, color = colors.textPrimary)
                        Text("Pick any audio file from your device", style = AlarmzyTheme.typography.body, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
                SectionLabel("Sounds")
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(uiState.ringtones, key = { _, it -> it.uri }) { index, ringtone ->
                // Rows render as one continuous card: round only the outer corners.
                val shape: Shape = when {
                    count == 1 -> AzShapes.CardLarge
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    index == count - 1 -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                RingtoneRow(
                    ringtone = ringtone,
                    isSelected = ringtone.uri == uiState.selectedUri,
                    isPlaying = ringtone.uri == uiState.playingUri && uiState.isPlaying,
                    shape = shape,
                    showDivider = index < count - 1,
                    onSelect = { onIntent(RingtonePickerIntent.SelectRingtone(ringtone.uri, ringtone.name)) },
                    onTogglePreview = { playing ->
                        if (playing) onIntent(RingtonePickerIntent.StopPreview)
                        else onIntent(RingtonePickerIntent.PreviewRingtone(ringtone.uri))
                    }
                )
            }
        }

        DetailHeader(title = "Ringtone", onBack = onNavigateBack, modifier = Modifier.align(Alignment.TopCenter))

        BottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PrimaryButton(text = "Use this sound", onClick = { onIntent(RingtonePickerIntent.Confirm) })
        }
    }
}

@Composable
private fun RingtoneRow(
    ringtone: RingtoneItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    showDivider: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: (playing: Boolean) -> Unit,
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    val ring by animateColorAsState(if (isSelected) colors.accent else colors.textDisabled, label = "radioRing")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .pressable(pressedScale = 1f, role = Role.RadioButton, onClick = onSelect)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).border(2.dp, ring, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Box(Modifier.size(10.dp).clip(CircleShape).background(colors.accent))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ringtone.name,
                    style = type.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isSelected) colors.accentLight else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ringtone.isCustom) {
                    Text("From device", style = type.label, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .pressable(pressedScale = 0.9f) { onTogglePreview(isPlaying) }
                    .clip(CircleShape)
                    .background(if (isPlaying) colors.accentContainerStrong else colors.tile),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Stop preview" else "Preview",
                    tint = if (isPlaying) colors.accentLight else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 55.dp, end = 17.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider)
            )
        }
    }
}
