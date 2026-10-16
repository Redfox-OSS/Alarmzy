package dev.redfox.obsidian.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.IntOffset
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
import dev.redfox.obsidian.R
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.effects.glassEffect
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.theme.ScreenGutter

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
            .glassEffect(ObsidianTheme.colors.headerScrim)
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
            HeaderTitle(title, Modifier.weight(1f))
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
            .glassEffect(ObsidianTheme.colors.headerScrim.copy(alpha = 0.9f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DetailHeaderHeight)
                .padding(start = ScreenGutter - 12.dp, end = ScreenGutter - 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(icon = R.drawable.ob_ic_back, contentDescription = "Back", onClick = onBack)
            Spacer(Modifier.width(4.dp))
            HeaderTitle(title, Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/**
 * Header title that slides in on first show and rolls vertically when it changes
 * ("Alarms" -> "Select alarms" -> "2 selected").
 */
@Composable
private fun HeaderTitle(title: String, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = title,
        transitionSpec = {
            val spec = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 500f)
            (slideInVertically(spec) { it / 2 } + fadeIn(tween(180))) togetherWith
                (slideOutVertically(spec) { -it / 2 } + fadeOut(tween(120))) using SizeTransform(clip = false)
        },
        contentAlignment = Alignment.CenterStart,
        label = "headerTitle",
        modifier = modifier.revealOnEnter(offsetY = 10.dp)
    ) { text ->
        Text(
            text,
            style = ObsidianTheme.typography.screenTitle,
            color = ObsidianTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
