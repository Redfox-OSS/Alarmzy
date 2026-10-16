package dev.redfox.alarmzy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.rememberHazeState
import dev.redfox.alarmzy.clock.TimerNotifications
import dev.redfox.alarmzy.presentation.navigation.AlarmzyNavHost
import dev.redfox.alarmzy.presentation.navigation.BottomNavBar
import dev.redfox.alarmzy.presentation.navigation.BottomNavScreen
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
import dev.redfox.obsidian.effects.LocalHazeState
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import dev.redfox.obsidian.theme.ObsidianTheme
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import dev.redfox.alarmzy.presentation.update.InAppUpdater
import dev.redfox.alarmzy.presentation.update.UpdateReadyBanner
import dev.redfox.obsidian.navigation.floatingNavClearance
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.dp
import javax.inject.Inject
import dev.redfox.alarmzy.domain.repository.BillingRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    var overlayPermissionGranted by mutableStateOf(false)
        private set

    private lateinit var inAppUpdater: InAppUpdater

    @Inject
    lateinit var billingRepository: BillingRepository

    /** Tab requested by a notification (e.g. the running timer); consumed once navigated. */
    private var pendingTab by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark: force light system-bar icons regardless of the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        inAppUpdater = InAppUpdater(this)
        requestNotificationPermissionIfNeeded()
        overlayPermissionGranted = Settings.canDrawOverlays(this)
        pendingTab = intent?.getStringExtra(TimerNotifications.EXTRA_OPEN_TAB)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            AlarmzyTheme(
                accentColor = settingsState.accentColor,
                darkVariant = settingsState.darkVariant
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = BottomNavScreen.isTabRoute(currentRoute)
                val hazeState = rememberHazeState()

                LaunchedEffect(pendingTab) {
                    val tab = BottomNavScreen.entries.firstOrNull { it.route == pendingTab } ?: return@LaunchedEffect
                    pendingTab = null
                    navController.navigate(tab.route) {
                        popUpTo(BottomNavScreen.Alarms.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                CompositionLocalProvider(LocalHazeState provides hazeState) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(ObsidianTheme.colors.background)
                    ) {
                        AlarmzyNavHost(
                            navController = navController,
                            overlayPermissionGranted = overlayPermissionGranted,
                            onRequestOverlayPermission = { requestOverlayPermission() },
                            modifier = Modifier.fillMaxSize()
                        )

                        // "Update ready · Restart" once a flexible Play update has downloaded.
                        AnimatedVisibility(
                            visible = inAppUpdater.readyToInstall,
                            enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (showBottomBar) floatingNavClearance() + 12.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp)
                        ) {
                            UpdateReadyBanner(onRestart = { inAppUpdater.completeUpdate() })
                        }

                        AnimatedVisibility(
                            visible = showBottomBar,
                            // Rises with a soft bounce when returning from a detail screen.
                            enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } +
                                fadeIn(tween(200)),
                            exit = slideOutVertically(spring(dampingRatio = 1f, stiffness = 500f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } +
                                fadeOut(tween(150)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(BottomNavScreen.Alarms.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(TimerNotifications.EXTRA_OPEN_TAB)?.let { pendingTab = it }
    }

    override fun onStart() {
        super.onStart()
        // Re-check Play purchases each time the app comes forward, so a refunded Pro is revoked.
        lifecycleScope.launch { runCatching { billingRepository.restorePurchases() } }
    }

    override fun onResume() {
        super.onResume()
        overlayPermissionGranted = Settings.canDrawOverlays(this)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
