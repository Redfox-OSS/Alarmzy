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

@AndroidEntryPoint
class AlarmRingingActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        keyguardManager.requestDismissKeyguard(this, null)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val alarmLabel = intent.getStringExtra(AlarmService.EXTRA_ALARM_LABEL) ?: ""
        val alarmTime = intent.getStringExtra(AlarmService.EXTRA_ALARM_TIME) ?: ""

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
                    buttonLayout = buttonLayout,
                    clockPosition = clockPosition,
                    onSnooze = { snooze() },
                    onDismiss = { dismiss() }
                )
            }
        }
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
}
