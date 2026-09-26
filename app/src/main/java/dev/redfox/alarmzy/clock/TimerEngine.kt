package dev.redfox.alarmzy.clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide countdown timer. State is persisted to DataStore; every transition also drives its
 * side effects in one place ([applySideEffects]): the exact AlarmManager wake-up that fires
 * [TimerReceiver] at zero, the ongoing countdown notification, and the ring service.
 */
@Singleton
class TimerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val loaded = scope.async { load() }

    /** The current state once persisted state has been loaded (for cold starts from the alarm). */
    suspend fun current(): TimerState {
        loaded.await()
        return _state.value
    }

    /** Remembers the picker value while idle so it survives tab switches and restarts. */
    fun setPicker(durationMs: Long) = mutate { s ->
        if (s.status == TimerStatus.IDLE) s.copy(pickerMs = durationMs) else s
    }

    fun start(durationMs: Long) = mutate { s ->
        if (durationMs <= 0L || s.status == TimerStatus.RUNNING) s
        else s.copy(
            status = TimerStatus.RUNNING,
            durationMs = durationMs,
            endAtMillis = System.currentTimeMillis() + durationMs,
            pickerMs = durationMs,
        )
    }

    fun pause() = mutate { s ->
        if (s.status != TimerStatus.RUNNING) s
        else s.copy(status = TimerStatus.PAUSED, remainingMs = s.remainingAt(System.currentTimeMillis()))
    }

    fun resume() = mutate { s ->
        if (s.status != TimerStatus.PAUSED) s
        else s.copy(status = TimerStatus.RUNNING, endAtMillis = System.currentTimeMillis() + s.remainingMs)
    }

    /** Stops a running/paused timer without ringing, or silences a finished one. */
    fun cancel() = mutate { s -> s.copy(status = TimerStatus.IDLE, remainingMs = 0L, endAtMillis = 0L) }

    /** Called by [TimerReceiver] when the AlarmManager wake-up fires. */
    fun finish() = mutate { s ->
        if (s.status == TimerStatus.RUNNING) s.copy(status = TimerStatus.FINISHED) else s
    }

    /** Re-arms a running timer after reboot (AlarmManager alarms don't survive one). */
    fun rearmAfterBoot() = mutate(reapplySideEffects = true) { s ->
        when {
            s.status == TimerStatus.RUNNING && s.endAtMillis <= System.currentTimeMillis() ->
                s.copy(status = TimerStatus.IDLE) // expired while the phone was off
            else -> s
        }
    }

    /**
     * @param reapplySideEffects run [applySideEffects] even when the state is unchanged
     * (used after boot, when the AlarmManager wake-up and notification are gone).
     */
    private fun mutate(reapplySideEffects: Boolean = false, transform: (TimerState) -> TimerState) {
        scope.launch {
            loaded.await()
            mutex.withLock {
                val prev = _state.value
                val next = transform(prev)
                if (next != prev || reapplySideEffects) {
                    _state.value = next
                    persist(next)
                    applySideEffects(prev, next)
                }
            }
        }
    }

    private fun applySideEffects(prev: TimerState, next: TimerState) {
        when (next.status) {
            TimerStatus.RUNNING -> {
                schedule(next.endAtMillis)
                TimerNotifications.showCountdown(context, next)
            }
            TimerStatus.PAUSED -> {
                cancelSchedule()
                TimerNotifications.showCountdown(context, next)
            }
            TimerStatus.FINISHED -> {
                TimerNotifications.cancelCountdown(context)
            }
            TimerStatus.IDLE -> {
                cancelSchedule()
                TimerNotifications.cancelCountdown(context)
                if (prev.status == TimerStatus.FINISHED) {
                    context.stopService(Intent(context, TimerRingService::class.java))
                }
            }
        }
    }

    private fun schedule(endAtMillis: Long) {
        // setExactAndAllowWhileIdle (not setAlarmClock) so a timer doesn't show up as the
        // system "next alarm". USE_EXACT_ALARM is already declared for the alarm engine.
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, firePendingIntent())
    }

    private fun cancelSchedule() = alarmManager.cancel(firePendingIntent())

    private fun firePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_FIRE,
        Intent(context, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private suspend fun load() {
        val p = dataStore.data.first()
        val status = TimerStatus.entries.firstOrNull { it.name == p[KEY_STATUS] } ?: TimerStatus.IDLE
        val loaded = TimerState(
            status = status,
            durationMs = p[KEY_DURATION] ?: 0L,
            endAtMillis = p[KEY_END_AT] ?: 0L,
            remainingMs = p[KEY_REMAINING] ?: 0L,
            pickerMs = p[KEY_PICKER] ?: TimerState.DEFAULT_PICKER_MS,
        )
        // A run that expired while the process was dead and was never delivered shouldn't ring late.
        _state.value = if (loaded.status == TimerStatus.RUNNING && loaded.endAtMillis <= System.currentTimeMillis() - STALE_GRACE_MS) {
            loaded.copy(status = TimerStatus.IDLE)
        } else loaded
    }

    private suspend fun persist(s: TimerState) {
        dataStore.edit { p ->
            p[KEY_STATUS] = s.status.name
            p[KEY_DURATION] = s.durationMs
            p[KEY_END_AT] = s.endAtMillis
            p[KEY_REMAINING] = s.remainingMs
            p[KEY_PICKER] = s.pickerMs
        }
    }

    private companion object {
        const val REQUEST_FIRE = 71_001
        const val STALE_GRACE_MS = 60_000L
        val KEY_STATUS = stringPreferencesKey("timer_status")
        val KEY_DURATION = longPreferencesKey("timer_duration")
        val KEY_END_AT = longPreferencesKey("timer_end_at")
        val KEY_REMAINING = longPreferencesKey("timer_remaining")
        val KEY_PICKER = longPreferencesKey("timer_picker")
    }
}
