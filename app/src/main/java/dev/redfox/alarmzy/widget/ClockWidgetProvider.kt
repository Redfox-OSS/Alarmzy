package dev.redfox.alarmzy.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The `OCT27 / 00:02` home-screen widget. Re-renders when a widget is added or restored, on each
 * minute tick from [ClockWidgetTicker] (which also refreshes stale weather), and when the clock,
 * time zone or locale changes. [ClockWidgetStore] re-renders on every settings change.
 */
@AndroidEntryPoint
class ClockWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var store: ClockWidgetStore

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ClockWidgetTicker.ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            // An app update wipes the pending tick; restart it here rather than waiting for the app.
            Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAsync(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        ClockWidgetTicker.cancel(context)
    }

    private fun refreshAsync(context: Context) {
        // Arm the next minute first, synchronously: whatever happens to the render or the weather
        // fetch below (slow, crashed, killed), the clock keeps ticking.
        ClockWidgetTicker.scheduleNext(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                store.refreshWidgets()
                // Throttled inside: a fetch at most every 30 min (5 min after a failure). Ticks only
                // arrive while the screen is on, so weather never wakes the phone.
                store.refreshWeather()
            } finally {
                pending.finish()
            }
        }
    }
}
