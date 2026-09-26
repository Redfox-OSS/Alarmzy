package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.ui.components.ScreenGutter
import dev.redfox.alarmzy.ui.components.glassEffect
import dev.redfox.alarmzy.ui.components.glow
import dev.redfox.alarmzy.ui.components.pressable
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

private val NavBarHeight = 64.dp
private val NavBarBottomGap = 12.dp

/**
 * Vertical space tab screens must leave at the bottom of their scrolling content so the
 * last item can scroll clear of the floating nav bar.
 */
@Composable
fun floatingNavClearance(): Dp =
    NavBarHeight + NavBarBottomGap + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** OxygenOS-style floating frosted-glass pill nav with icon-only tabs. */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (BottomNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AlarmzyTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = NavBarBottomGap),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .glow(Color.Black.copy(alpha = 0.35f), CircleShape, radius = 24.dp, offsetY = 12.dp)
                .height(NavBarHeight)
                .clip(CircleShape)
                .glassEffect(colors.navScrim)
                .border(1.dp, colors.borderStrong, CircleShape)
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavScreen.entries.forEach { screen ->
                NavItem(
                    screen = screen,
                    selected = currentRoute == screen.route,
                    onClick = { onNavigate(screen) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(screen: BottomNavScreen, selected: Boolean, onClick: () -> Unit) {
    val colors = AlarmzyTheme.colors
    val selection by animateFloatAsState(if (selected) 1f else 0f, label = "navSelection")
    val tint by animateColorAsState(
        if (selected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.7f),
        label = "navTint"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .pressable(pressedScale = 0.88f, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Selected halo: tinted disc + hairline ring + accent glow, faded in with the selection.
        Box(
            Modifier
                .size(48.dp)
                .graphicsLayer { alpha = selection; scaleX = 0.8f + 0.2f * selection; scaleY = scaleX }
                .glow(colors.accentGlow, CircleShape, radius = 18.dp)
                .clip(CircleShape)
                .background(colors.accentContainer)
                .border(1.dp, colors.accentLight.copy(alpha = 0.4f), CircleShape)
        )
        Icon(
            painterResource(screen.icon),
            contentDescription = screen.label,
            tint = tint
        )
    }
}
