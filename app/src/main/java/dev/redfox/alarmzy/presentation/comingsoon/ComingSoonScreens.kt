package dev.redfox.alarmzy.presentation.comingsoon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.presentation.navigation.floatingNavClearance
import dev.redfox.alarmzy.ui.components.EmptyState
import dev.redfox.alarmzy.ui.components.LargeHeader
import dev.redfox.alarmzy.ui.components.glassSource
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

// Placeholders for the Timer and Stopwatch tabs; their designs and functionality come next.

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    ComingSoonScreen(
        title = "Timer",
        icon = R.drawable.ic_nav_timer,
        subtitle = "Countdown timers are on their way.",
        modifier = modifier
    )
}

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    ComingSoonScreen(
        title = "Stopwatch",
        icon = R.drawable.ic_nav_stopwatch,
        subtitle = "Lap timing is on its way.",
        modifier = modifier
    )
}

@Composable
private fun ComingSoonScreen(
    title: String,
    @DrawableRes icon: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AlarmzyTheme.colors.background)) {
        Box(
            Modifier
                .fillMaxSize()
                .glassSource()
                .padding(bottom = floatingNavClearance()),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(icon = icon, title = "Coming soon", subtitle = subtitle)
        }
        LargeHeader(title = title, modifier = Modifier.align(Alignment.TopCenter))
    }
}
