package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.redfox.obsidian.navigation.FloatingNavBar
import dev.redfox.obsidian.navigation.FloatingNavItem
import dev.redfox.obsidian.navigation.NavIconMotion

/** Each tab's icon animation: the alarm rings, the hourglass flips, the stopwatch clicks, the stack wobbles, the gear turns. */
private val BottomNavScreen.motion: NavIconMotion
    get() = when (this) {
        BottomNavScreen.Alarms -> NavIconMotion.Shake
        BottomNavScreen.Timer -> NavIconMotion.Flip
        BottomNavScreen.Stopwatch -> NavIconMotion.Press
        BottomNavScreen.Groups -> NavIconMotion.Jelly
        BottomNavScreen.Settings -> NavIconMotion.Turn
    }

private val NavItems = BottomNavScreen.entries.map { FloatingNavItem(it.icon, it.label, it.motion) }

/** The app's tab bar: Obsidian's [FloatingNavBar] over the [BottomNavScreen] tabs. */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (BottomNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingNavBar(
        items = NavItems,
        selectedIndex = BottomNavScreen.entries.indexOfFirst { it.route == currentRoute },
        onSelect = { onNavigate(BottomNavScreen.entries[it]) },
        modifier = modifier
    )
}
