package dev.redfox.alarmzy.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.ui.components.PillButton
import dev.redfox.alarmzy.ui.components.glassEffect
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/** Floating glass pill shown once a flexible update has downloaded. */
@Composable
fun UpdateReadyBanner(onRestart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AlarmzyTheme.colors
    Row(
        modifier = modifier
            .glow(Color.Black.copy(alpha = 0.35f), CircleShape, radius = 20.dp, offsetY = 8.dp)
            .clip(CircleShape)
            .glassEffect(colors.navScrim)
            .border(1.dp, colors.borderStrong, CircleShape)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_notif_reset),
            contentDescription = null,
            tint = colors.accentLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Update ready", style = AlarmzyTheme.typography.labelStrong, color = colors.textPrimary)
        Spacer(Modifier.width(12.dp))
        PillButton(
            text = "Restart",
            onClick = onRestart,
            containerColor = colors.accent,
            contentColor = colors.onAccent
        )
    }
}
