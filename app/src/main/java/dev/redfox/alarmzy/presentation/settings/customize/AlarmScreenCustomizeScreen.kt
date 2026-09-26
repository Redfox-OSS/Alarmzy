package dev.redfox.alarmzy.presentation.settings.customize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.presentation.alarmringing.AlarmRingingContent
import dev.redfox.alarmzy.ui.components.ChoicePill
import dev.redfox.alarmzy.ui.components.DetailHeader
import dev.redfox.alarmzy.ui.components.DetailHeaderHeight
import dev.redfox.alarmzy.ui.components.PillButton
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.SectionLabel
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import kotlin.math.roundToInt

@Composable
fun AlarmScreenCustomizeScreen(
    uiState: AlarmScreenCustomizeUiState,
    onIntent: (AlarmScreenCustomizeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AlarmzyTheme.colors
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onIntent(AlarmScreenCustomizeIntent.PickedBackgroundImage(uri))
        }
    }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + DetailHeaderHeight

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(
                    top = topInset + 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AlarmScreenPreview(
                backgroundImagePath = uiState.backgroundImagePath,
                buttonLayout = uiState.buttonLayout,
                clockPosition = uiState.clockPosition
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Background")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        text = if (uiState.backgroundImagePath != null) "Change photo" else "Choose photo",
                        containerColor = colors.accentContainerStrong,
                        contentColor = colors.accentLight,
                        onClick = {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    if (uiState.backgroundImagePath != null) {
                        PillButton(
                            text = "Remove photo",
                            onClick = { onIntent(AlarmScreenCustomizeIntent.RemoveBackgroundImage) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Buttons")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlarmButtonLayout.entries.forEach { layout ->
                        ChoicePill(
                            text = layout.displayName,
                            selected = layout == uiState.buttonLayout,
                            onClick = { onIntent(AlarmScreenCustomizeIntent.SetButtonLayout(layout)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Clock position")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlarmClockPosition.entries.forEach { position ->
                        ChoicePill(
                            text = position.displayName,
                            selected = position == uiState.clockPosition,
                            onClick = { onIntent(AlarmScreenCustomizeIntent.SetClockPosition(position)) }
                        )
                    }
                }
            }
        }

        DetailHeader(title = "Alarm Screen", onBack = onNavigateBack, modifier = Modifier.align(Alignment.TopCenter))
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
        SectionLabel("Preview")
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .glow(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(28.dp), radius = 24.dp, offsetY = 12.dp)
                .width(previewWidth)
                .height(previewHeight)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, AlarmzyTheme.colors.borderStrong, RoundedCornerShape(28.dp))
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
