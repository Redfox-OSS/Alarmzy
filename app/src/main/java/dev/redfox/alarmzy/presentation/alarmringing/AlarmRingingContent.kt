package dev.redfox.alarmzy.presentation.alarmringing

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.ui.theme.PoppinsFontFamily
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLOCK_TEXT_SIZE = 65.sp

/**
 * The full-screen alarm-ringing layout, shared by the real [dev.redfox.alarmzy.alarm.AlarmRingingActivity]
 * and the settings customization screen's live preview — keeping both in sync by construction.
 */
@Composable
fun AlarmRingingContent(
    time: String,
    label: String,
    backgroundImagePath: String?,
    buttonLayout: AlarmButtonLayout,
    clockPosition: AlarmClockPosition,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImage = backgroundImagePath != null
    val textColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onBackground
    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (backgroundImagePath != null) {
            AsyncImage(
                model = File(backgroundImagePath),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Guarantees text/button legibility over an arbitrary user photo — not optional.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.15f),
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Independent of clockPosition — always pinned near the top.
        if (label.isNotBlank()) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = textColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .padding(horizontal = 32.dp)
            )
        }

        // Clock + date move together between top/middle per clockPosition.
        Column(
            modifier = Modifier
                .align(if (clockPosition == AlarmClockPosition.TOP) Alignment.TopCenter else Alignment.Center)
                .padding(top = if (clockPosition == AlarmClockPosition.TOP) 140.dp else 0.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = time,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    fontFamily = PoppinsFontFamily,
                    fontSize = CLOCK_TEXT_SIZE,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    shadow = if (hasImage) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 16f) else null
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = currentDate,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (buttonLayout) {
                AlarmButtonLayout.BOTH -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Snooze")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Dismiss")
                    }
                }

                AlarmButtonLayout.SNOOZE_ONLY -> Button(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Snooze")
                }

                AlarmButtonLayout.DISMISS_ONLY -> Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.AlarmOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dismiss")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "It's currently $currentTime",
                fontSize = 18.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
