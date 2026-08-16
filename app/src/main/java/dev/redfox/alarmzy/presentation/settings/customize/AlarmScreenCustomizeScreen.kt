package dev.redfox.alarmzy.presentation.settings.customize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.presentation.alarmringing.AlarmRingingContent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreenCustomizeScreen(
    uiState: AlarmScreenCustomizeUiState,
    onIntent: (AlarmScreenCustomizeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onIntent(AlarmScreenCustomizeIntent.PickedBackgroundImage(uri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Screen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AlarmScreenPreview(
                backgroundImagePath = uiState.backgroundImagePath,
                buttonLayout = uiState.buttonLayout,
                clockPosition = uiState.clockPosition
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.backgroundImagePath != null) "Change photo" else "Choose photo")
                }
                if (uiState.backgroundImagePath != null) {
                    TextButton(
                        onClick = { onIntent(AlarmScreenCustomizeIntent.RemoveBackgroundImage) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove photo")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Buttons", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AlarmButtonLayout.entries.forEachIndexed { index, layout ->
                        SegmentedButton(
                            selected = layout == uiState.buttonLayout,
                            onClick = { onIntent(AlarmScreenCustomizeIntent.SetButtonLayout(layout)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AlarmButtonLayout.entries.size
                            )
                        ) { Text(layout.displayName) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Clock position", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AlarmClockPosition.entries.forEachIndexed { index, position ->
                        SegmentedButton(
                            selected = position == uiState.clockPosition,
                            onClick = { onIntent(AlarmScreenCustomizeIntent.SetClockPosition(position)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AlarmClockPosition.entries.size
                            )
                        ) { Text(position.displayName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmScreenPreview(
    backgroundImagePath: String?,
    buttonLayout: AlarmButtonLayout,
    clockPosition: AlarmClockPosition,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val realWidthDp = configuration.screenWidthDp.dp
    val realHeightDp = configuration.screenHeightDp.dp

    // Preview keeps the device's real width/height ratio, so it reads as a phone
    // mockup rather than a square crop. The whole ringing layout is rendered at its
    // true real-device size and then uniformly scaled down as one image — so every
    // element (buttons, padding, icons, clock) shrinks together in proportion,
    // instead of only the clock text shrinking while buttons stay full-size and wrap.
    val previewWidth = 260.dp
    val previewHeight = previewWidth * (realHeightDp.value / realWidthDp.value)
    val scale = previewWidth.value / realWidthDp.value

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Preview",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(24.dp))
        ) {
            AlarmRingingContent(
                time = "7:00 AM",
                label = "Alarm",
                backgroundImagePath = backgroundImagePath,
                buttonLayout = buttonLayout,
                clockPosition = clockPosition,
                onSnooze = {},
                onDismiss = {},
                modifier = Modifier.scaledMockup(realWidthDp, realHeightDp, scale)
            )
        }
    }
}

/**
 * Measures the content at its true [realWidth] x [realHeight] size, then draws that
 * whole layer scaled down by [scale] and reports the scaled size to the parent — so
 * every element inside (text, icons, padding, buttons) shrinks uniformly together,
 * instead of only individually-scaled values shrinking while the rest stay full-size.
 */
private fun Modifier.scaledMockup(realWidth: Dp, realHeight: Dp, scale: Float): Modifier =
    this.layout { measurable, _ ->
        val widthPx = realWidth.roundToPx()
        val heightPx = realHeight.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
        val scaledWidth = (widthPx * scale).roundToInt()
        val scaledHeight = (heightPx * scale).roundToInt()
        layout(scaledWidth, scaledHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
