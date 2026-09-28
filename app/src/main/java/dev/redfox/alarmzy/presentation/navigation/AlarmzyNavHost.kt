package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import dev.redfox.alarmzy.ui.components.RevealScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditScreen
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditSideEffect
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditViewModel
import dev.redfox.alarmzy.presentation.alarms.AlarmsScreen
import dev.redfox.alarmzy.presentation.alarms.AlarmsViewModel
import dev.redfox.alarmzy.presentation.stopwatch.StopwatchScreen
import dev.redfox.alarmzy.presentation.stopwatch.StopwatchViewModel
import dev.redfox.alarmzy.presentation.timer.TimerScreen
import dev.redfox.alarmzy.presentation.timer.TimerViewModel
import dev.redfox.alarmzy.presentation.groupedit.GroupEditScreen
import dev.redfox.alarmzy.presentation.groupedit.GroupEditSideEffect
import dev.redfox.alarmzy.presentation.groupedit.GroupEditViewModel
import dev.redfox.alarmzy.presentation.groups.GroupsScreen
import dev.redfox.alarmzy.presentation.groups.GroupsViewModel
import dev.redfox.alarmzy.presentation.paywall.PaywallBottomSheet
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerScreen
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerSideEffect
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerViewModel
import dev.redfox.alarmzy.presentation.settings.SettingsIntent
import dev.redfox.alarmzy.presentation.settings.SettingsScreen
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
import dev.redfox.alarmzy.presentation.settings.customize.AlarmScreenCustomizeScreen
import dev.redfox.alarmzy.presentation.settings.customize.AlarmScreenCustomizeViewModel
import dev.redfox.alarmzy.presentation.groupedit.GroupEditIntent

@Composable
fun AlarmzyNavHost(
    navController: NavHostController,
    overlayPermissionGranted: Boolean = false,
    onRequestOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Staggered entrances play when arriving at a screen, but not when returning to it from a
    // detail screen (back / save), where the parent should simply be there.
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            RevealPolicy.replay = RevealPolicy.lastRoute.let { it == null || BottomNavScreen.isTabRoute(it) }
            RevealPolicy.lastRoute = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Alarms.route,
        modifier = modifier,
        // Tabs glide sideways in the direction of travel; pushed screens slide in over a
        // receding, dimming parent (iOS-style parallax). All springs, so predictive back can
        // scrub them.
        enterTransition = {
            val dir = tabDirection()
            if (dir != null) {
                slideInHorizontally(TabSpring) { w -> dir * w / 9 } +
                    fadeIn(tween(240, delayMillis = 40)) + scaleIn(TabScaleSpring, initialScale = 0.97f)
            } else {
                slideInHorizontally(PushSpring) { it / 3 } + fadeIn(tween(220))
            }
        },
        exitTransition = {
            val dir = tabDirection()
            if (dir != null) {
                slideOutHorizontally(TabSpring) { w -> -dir * w / 12 } + fadeOut(tween(150))
            } else {
                slideOutHorizontally(PushSpring) { -it / 8 } + scaleOut(PushScaleSpring, targetScale = 0.93f) +
                    fadeOut(tween(300), targetAlpha = 0.25f)
            }
        },
        popEnterTransition = {
            val dir = tabDirection()
            if (dir != null) {
                slideInHorizontally(TabSpring) { w -> dir * w / 9 } + fadeIn(tween(240, delayMillis = 40))
            } else {
                slideInHorizontally(PushSpring) { -it / 8 } + scaleIn(PushScaleSpring, initialScale = 0.93f) +
                    fadeIn(tween(260), initialAlpha = 0.25f)
            }
        },
        popExitTransition = {
            val dir = tabDirection()
            if (dir != null) {
                slideOutHorizontally(TabSpring) { w -> -dir * w / 12 } + fadeOut(tween(150))
            } else {
                slideOutHorizontally(PushSpring) { it / 3 } + fadeOut(tween(200))
            }
        }
    ) {
        screen(BottomNavScreen.Alarms.route) {
            val viewModel: AlarmsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlarmsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onAddAlarm = { navController.navigate(Routes.ALARM_EDIT) },
                onAlarmClick = { alarmId -> navController.navigate(Routes.alarmEdit(alarmId)) }
            )
        }

        screen(BottomNavScreen.Timer.route) {
            val viewModel: TimerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TimerScreen(uiState = uiState, onIntent = viewModel::onIntent)
        }

        screen(BottomNavScreen.Stopwatch.route) {
            val viewModel: StopwatchViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            StopwatchScreen(uiState = uiState, onIntent = viewModel::onIntent)
        }

        screen(BottomNavScreen.Groups.route) {
            val viewModel: GroupsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showPaywall by remember { mutableStateOf(false) }

            GroupsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onAddGroup = {
                    if (uiState.canCreateGroup) navController.navigate(Routes.GROUP_EDIT)
                    else showPaywall = true
                },
                onEditGroup = { groupId -> navController.navigate(Routes.groupEdit(groupId)) }
            )

            if (showPaywall) {
                PaywallBottomSheet(onDismiss = { showPaywall = false })
            }
        }

        screen(BottomNavScreen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val ringtoneUri = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<String>("ringtone_uri")
            val ringtoneName = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<String>("ringtone_name")

            LaunchedEffect(ringtoneUri, ringtoneName) {
                if (ringtoneUri != null && ringtoneName != null) {
                    viewModel.onIntent(
                        SettingsIntent.SetDefaultRingtone(
                            uri = ringtoneUri.ifBlank { null },
                            name = ringtoneName
                        )
                    )
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>("ringtone_uri")
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>("ringtone_name")
                }
            }

            SettingsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                overlayPermissionGranted = overlayPermissionGranted,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onPickRingtone = {
                    navController.navigate(
                        Routes.ringtonePicker(uiState.defaultRingtoneUri, uiState.defaultRingtoneName)
                    )
                },
                onCustomizeAlarmScreen = { navController.navigate(Routes.CUSTOMIZE_ALARM_SCREEN) }
            )
        }

        screen(Routes.ALARM_EDIT) {
            AlarmEditDestination(navController)
        }

        screen(
            route = Routes.ALARM_EDIT_WITH_ID,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) {
            AlarmEditDestination(navController)
        }

        screen(Routes.GROUP_EDIT) {
            GroupEditDestination(navController, it)
        }

        screen(
            route = Routes.GROUP_EDIT_WITH_ID,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupEditDestination(navController, it)
        }

        screen(
            route = Routes.RINGTONE_PICKER,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            val viewModel: RingtonePickerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.sideEffect.collect { effect ->
                    when (effect) {
                        is RingtonePickerSideEffect.Confirmed -> {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("ringtone_uri", effect.uri ?: "")
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("ringtone_name", effect.name)
                            navController.popBackStack()
                        }
                    }
                }
            }

            RingtonePickerScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        screen(Routes.CUSTOMIZE_ALARM_SCREEN) {
            val viewModel: AlarmScreenCustomizeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AlarmScreenCustomizeScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private val TabSpring = spring(dampingRatio = 0.9f, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)
private val TabScaleSpring = spring(dampingRatio = 0.9f, stiffness = 420f, visibilityThreshold = 0.001f)
private val PushSpring = spring(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)
private val PushScaleSpring = spring(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = 0.001f)

/**
 * For a switch between two tabs, +1 when moving to a tab on the right, -1 to the left;
 * null when a detail screen is involved.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int? {
    val from = BottomNavScreen.entries.indexOfFirst { it.route == initialState.destination.route }
    val to = BottomNavScreen.entries.indexOfFirst { it.route == targetState.destination.route }
    if (from < 0 || to < 0) return null
    return if (to >= from) 1 else -1
}

/** [composable] whose content gets a fresh [RevealScope], so its staggered entrance plays on each visit. */
private fun NavGraphBuilder.screen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(route, arguments) { entry ->
    val replay = remember { RevealPolicy.replay }
    RevealScope(enabled = replay) { content(entry) }
}

private object RevealPolicy {
    var lastRoute: String? = null
    var replay: Boolean = true
}

@Composable
private fun AlarmEditDestination(navController: NavHostController) {
    val viewModel: AlarmEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AlarmEditSideEffect.Saved,
                is AlarmEditSideEffect.Deleted -> navController.popBackStack()
                is AlarmEditSideEffect.ShowPaywall -> showPaywall = true
            }
        }
    }

    AlarmEditScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = { navController.popBackStack() }
    )

    if (showPaywall) {
        PaywallBottomSheet(onDismiss = { showPaywall = false })
    }
}

@Composable
private fun GroupEditDestination(navController: NavHostController, entry: NavBackStackEntry) {
    val viewModel: GroupEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }

    // Result from the ringtone picker (it writes onto this entry's savedStateHandle).
    val pickedUri by entry.savedStateHandle.getStateFlow<String?>("ringtone_uri", null).collectAsStateWithLifecycle()
    val pickedName by entry.savedStateHandle.getStateFlow<String?>("ringtone_name", null).collectAsStateWithLifecycle()
    LaunchedEffect(pickedUri, pickedName) {
        val uri = pickedUri
        val name = pickedName
        if (uri != null && name != null) {
            viewModel.onIntent(GroupEditIntent.SetRingtone(uri = uri, name = name))
            entry.savedStateHandle.remove<String>("ringtone_uri")
            entry.savedStateHandle.remove<String>("ringtone_name")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupEditSideEffect.Saved,
                is GroupEditSideEffect.Deleted -> navController.popBackStack()
                is GroupEditSideEffect.ShowPaywall -> showPaywall = true
            }
        }
    }

    GroupEditScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = { navController.popBackStack() },
        onPickRingtone = {
            // Pre-select the sound the group currently plays.
            val (uri, name) = if (uiState.ringtoneUri != null) uiState.ringtoneUri to uiState.ringtoneName
            else uiState.settingsRingtoneUri to uiState.settingsRingtoneName
            navController.navigate(Routes.ringtonePicker(uri, name))
        }
    )

    if (showPaywall) {
        PaywallBottomSheet(onDismiss = { showPaywall = false })
    }
}
