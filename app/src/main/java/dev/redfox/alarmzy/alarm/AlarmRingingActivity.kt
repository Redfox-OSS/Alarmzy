package dev.redfox.alarmzy.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.AndroidEntryPoint
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.alarmzy.presentation.alarmringing.AlarmRingingContent
import dev.redfox.alarmzy.presentation.settings.SettingsViewModel
import dev.redfox.alarmzy.presentation.settings.customize.AlarmScreenCustomizeViewModel
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import dev.redfox.alarmzy.clock.TimerEngine
import dev.redfox.alarmzy.domain.model.TimerStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AlarmRingingActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        keyguardManager.requestDismissKeyguard(this, null)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val isTimer = intent.getBooleanExtra(EXTRA_IS_TIMER, false)
        val alarmLabel = if (isTimer) intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"
        else intent.getStringExtra(AlarmService.EXTRA_ALARM_LABEL) ?: ""
        val alarmTime = if (isTimer) LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        else intent.getStringExtra(AlarmService.EXTRA_ALARM_TIME) ?: ""

        if (isTimer) {
            // Close once the timer is stopped from anywhere (notification action, auto-stop, the app).
            lifecycleScope.launch {
                // The ring can reach us before the engine flips to FINISHED; wait for that first
                // (bounded, in case this is a stale launch), then close when it leaves FINISHED.
                withTimeoutOrNull(5_000) { timerEngine.state.first { it.status == TimerStatus.FINISHED } }
                timerEngine.state.first { it.status != TimerStatus.FINISHED }
                finish()
            }
        }

        setContent {
            val prefs by dataStore.data.collectAsState(initial = null)

            val accentColor = AccentColor.fromName(prefs?.get(SettingsViewModel.KEY_ACCENT_COLOR))
            val darkVariant = DarkVariant.fromName(prefs?.get(SettingsViewModel.KEY_DARK_VARIANT))

            val backgroundImagePath = prefs?.get(AlarmScreenCustomizeViewModel.KEY_BG_IMAGE_PATH)
            val buttonLayout = runCatching {
                AlarmButtonLayout.valueOf(
                    prefs?.get(AlarmScreenCustomizeViewModel.KEY_BUTTON_LAYOUT) ?: AlarmButtonLayout.BOTH.name
                )
            }.getOrDefault(AlarmButtonLayout.BOTH)
            val clockPosition = runCatching {
                AlarmClockPosition.valueOf(
                    prefs?.get(AlarmScreenCustomizeViewModel.KEY_CLOCK_POSITION) ?: AlarmClockPosition.MIDDLE.name
                )
            }.getOrDefault(AlarmClockPosition.MIDDLE)

            AlarmzyTheme(
                accentColor = accentColor,
                darkVariant = darkVariant
            ) {
                AlarmRingingContent(
                    time = alarmTime,
                    label = alarmLabel,
                    backgroundImagePath = backgroundImagePath,
                    // Timers can't be snoozed: a single Stop button.
                    buttonLayout = if (isTimer) AlarmButtonLayout.DISMISS_ONLY else buttonLayout,
                    clockPosition = clockPosition,
                    onSnooze = { snooze() },
                    onDismiss = { if (isTimer) stopTimer() else dismiss() },
                    dismissLabel = if (isTimer) "Stop" else "Dismiss"
                )
            }
        }
    }

    private fun stopTimer() {
        timerEngine.cancel()
        finish()
    }

    private fun snooze() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
        }
        startService(intent)
        finish()
    }

    private fun dismiss() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(intent)
        finish()
    }

    companion object {
        const val EXTRA_IS_TIMER = "extra_is_timer"
        const val EXTRA_TIMER_LABEL = "extra_timer_label"
    }
}
