package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditIntent
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditScreen
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditSideEffect
import dev.redfox.alarmzy.presentation.alarmedit.AlarmEditViewModel
import dev.redfox.alarmzy.presentation.alarms.AlarmsScreen
import dev.redfox.alarmzy.presentation.alarms.AlarmsViewModel
import dev.redfox.alarmzy.presentation.groupedit.GroupEditScreen
import dev.redfox.alarmzy.presentation.groupedit.GroupEditSideEffect
import dev.redfox.alarmzy.presentation.groupedit.GroupEditViewModel
import dev.redfox.alarmzy.presentation.groups.GroupsScreen
import dev.redfox.alarmzy.presentation.groups.GroupsViewModel
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerScreen
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerSideEffect
import dev.redfox.alarmzy.presentation.ringtone.RingtonePickerViewModel
import dev.redfox.alarmzy.presentation.settings.SettingsScreen
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel

@Composable
fun AlarmzyNavHost(
    navController: NavHostController,
    overlayPermissionGranted: Boolean = false,
    onRequestOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Alarms.route,
        modifier = modifier
    ) {
        composable(BottomNavScreen.Alarms.route) {
            val viewModel: AlarmsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlarmsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onAddAlarm = { navController.navigate(Routes.ALARM_EDIT) },
                onAlarmClick = { alarmId -> navController.navigate(Routes.alarmEdit(alarmId)) }
            )
        }

        composable(BottomNavScreen.Groups.route) {
            val viewModel: GroupsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            GroupsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onAddGroup = { navController.navigate(Routes.GROUP_EDIT) },
                onEditGroup = { groupId -> navController.navigate(Routes.groupEdit(groupId)) }
            )
        }

        composable(BottomNavScreen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                overlayPermissionGranted = overlayPermissionGranted,
                onRequestOverlayPermission = onRequestOverlayPermission
            )
        }

        composable(Routes.ALARM_EDIT) {
            AlarmEditDestination(navController)
        }

        composable(
            route = Routes.ALARM_EDIT_WITH_ID,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) {
            AlarmEditDestination(navController)
        }

        composable(Routes.GROUP_EDIT) {
            GroupEditDestination(navController)
        }

        composable(
            route = Routes.GROUP_EDIT_WITH_ID,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupEditDestination(navController)
        }

        composable(Routes.RINGTONE_PICKER) {
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
    }
}

@Composable
private fun AlarmEditDestination(navController: NavHostController) {
    val viewModel: AlarmEditViewModel = hiltViewModel()
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
                AlarmEditIntent.SetRingtone(
                    uri = ringtoneUri.ifBlank { null },
                    name = ringtoneName
                )
            )
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("ringtone_uri")
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("ringtone_name")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AlarmEditSideEffect.Saved,
                is AlarmEditSideEffect.Deleted -> navController.popBackStack()
            }
        }
    }

    AlarmEditScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = { navController.popBackStack() },
        onPickRingtone = { navController.navigate(Routes.RINGTONE_PICKER) }
    )
}

@Composable
private fun GroupEditDestination(navController: NavHostController) {
    val viewModel: GroupEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupEditSideEffect.Saved,
                is GroupEditSideEffect.Deleted -> navController.popBackStack()
            }
        }
    }

    GroupEditScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = { navController.popBackStack() }
    )
}
