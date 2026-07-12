package dev.redfox.alarmzy.presentation.ringtone

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerScreen(
    uiState: RingtonePickerUiState,
    onIntent: (RingtonePickerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Ringtone") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(RingtonePickerIntent.Confirm) }) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item(key = "choose_from_device") {
                ListItem(
                    headlineContent = { Text("Choose from device") },
                    supportingContent = { Text("Pick any audio file from your device") },
                    leadingContent = {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        pickAudioLauncher.launch(arrayOf("audio/*"))
                    }
                )
                HorizontalDivider()
            }

            items(uiState.ringtones) { ringtone ->
                val isSelected = ringtone.uri == uiState.selectedUri
                val isPlaying = ringtone.uri == uiState.playingUri && uiState.isPlaying

                ListItem(
                    headlineContent = {
                        Text(
                            ringtone.name,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = if (ringtone.isCustom) {
                        { Text("From device") }
                    } else null,
                    leadingContent = {
                        Icon(
                            if (isSelected) Icons.Default.RadioButtonChecked
                            else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                if (isPlaying) onIntent(RingtonePickerIntent.StopPreview)
                                else onIntent(RingtonePickerIntent.PreviewRingtone(ringtone.uri))
                            }
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Preview"
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        onIntent(RingtonePickerIntent.SelectRingtone(ringtone.uri, ringtone.name))
                    }
                )
            }
        }
    }
}
