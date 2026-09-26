package dev.redfox.alarmzy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/** Height of the header bar below the status bar, for content top padding. */
val LargeHeaderHeight = 64.dp
val DetailHeaderHeight = 60.dp

/**
 * Frosted top header for the tab pages ("Alarms", "Alarm Groups", "Settings").
 * Place it over a [glassSource] list and pad the list by status bar + [LargeHeaderHeight].
 */
@Composable
fun LargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(AlarmzyTheme.colors.headerScrim)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LargeHeaderHeight)
                .padding(start = ScreenGutter, end = ScreenGutter - 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = AlarmzyTheme.typography.screenTitle,
                color = AlarmzyTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** Frosted header for pushed screens: back arrow, title, optional actions. */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(AlarmzyTheme.colors.headerScrim.copy(alpha = 0.9f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DetailHeaderHeight)
                .padding(start = ScreenGutter - 12.dp, end = ScreenGutter - 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(icon = R.drawable.ic_back, contentDescription = "Back", onClick = onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                style = AlarmzyTheme.typography.screenTitle,
                color = AlarmzyTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
