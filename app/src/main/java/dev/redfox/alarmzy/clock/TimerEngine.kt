package dev.redfox.alarmzy.clock

import android.app.AlarmManager
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
 * Process-wide countdown timer — the single owner of timer state. Everything (UI, notification
 * buttons, alarm receiver, boot receiver) goes through here, and every transition drives its side
 * effects in one place ([applySideEffects]): the finish alarm, the 60s Live Update TICK, the
 * promoted notification, and the ring service.
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

    /** The current state once persisted state has been loaded (for cold starts from an alarm). */
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

    /** "+1 min" on a finished timer: silences it and runs a fresh one-minute timer. */
    fun addMinute() = mutate { s ->
        if (s.status != TimerStatus.FINISHED) s
        else s.copy(
            status = TimerStatus.RUNNING,
            durationMs = ONE_MINUTE,
            endAtMillis = System.currentTimeMillis() + ONE_MINUTE,
        )
    }

    /**
     * Called by [TimerReceiver] when the finish alarm fires. Ignores stale fires (the timer was
     * paused, cancelled or restarted since) and only then calls [onRing] to start ringing.
     */
    fun finish(onRing: () -> Unit) = mutate(onAccepted = onRing) { s ->
        val due = s.status == TimerStatus.RUNNING &&
            System.currentTimeMillis() >= s.endAtMillis - FIRE_TOLERANCE_MS
        if (due) s.copy(status = TimerStatus.FINISHED) else s
    }

    /** Re-posts the Live Update and re-arms the next TICK (TICK alarm, permission granted). */
    fun refresh() = mutate(reapplySideEffects = true) { it }

    /**
     * After a reboot, alarms and notifications are gone: re-arm and re-show a running/paused timer.
     * One that ended while the phone was off can't ring from boot, so it gets a silent "Time's up".
     */
    fun rearmAfterBoot() = mutate(
        reapplySideEffects = true,
        onAccepted = { TimerNotifications.showMissedFinish(context, _state.value.durationMs) },
    ) { s ->
        if (s.status == TimerStatus.RUNNING && s.endAtMillis <= System.currentTimeMillis()) {
            s.copy(status = TimerStatus.FINISHED)
        } else s
    }

    /**
     * @param reapplySideEffects run [applySideEffects] even when the state is unchanged. Never skip
     * on "nothing changed" for those callers: `notify()` / `AlarmManager.set*` are idempotent, and
     * skipping would leave the notification missing after a reboot or a permission grant.
     * @param onAccepted invoked (inside the lock) only when [transform] actually changed the state.
     */
    private fun mutate(
        reapplySideEffects: Boolean = false,
        onAccepted: (() -> Unit)? = null,
        transform: (TimerState) -> TimerState,
    ) {
        scope.launch {
            loaded.await()
            mutex.withLock {
                val prev = _state.value
                val next = transform(prev)
                if (next != prev) {
                    _state.value = next
                    persist(next)
                    onAccepted?.invoke()
                }
                if (next != prev || reapplySideEffects) applySideEffects(prev, next)
            }
        }
    }

    private fun applySideEffects(prev: TimerState, next: TimerState) {
        // Leaving FINISHED (Stop, +1 min, auto-stop) silences the ringer and the "Time's up" notice.
        if (prev.status == TimerStatus.FINISHED && next.status != TimerStatus.FINISHED) {
            context.stopService(Intent(context, TimerRingService::class.java))
            TimerNotifications.cancelFinished(context)
        }
        when (next.status) {
            TimerStatus.RUNNING -> {
                scheduleFinish(next.endAtMillis)
                TimerNotifications.showCountdown(context, next)
                LiveUpdates.scheduleTick(context, tickIntent())
            }
            TimerStatus.PAUSED -> {
                cancelFinish()
                LiveUpdates.cancelTick(context, tickIntent())
                TimerNotifications.showCountdown(context, next)
            }
            TimerStatus.FINISHED -> {
                // The ring service (or the boot path) owns the "Time's up" notification.
                cancelFinish()
                LiveUpdates.cancelTick(context, tickIntent())
                TimerNotifications.cancelCountdown(context)
            }
            TimerStatus.IDLE -> {
                cancelFinish()
                LiveUpdates.cancelTick(context, tickIntent())
                TimerNotifications.cancelCountdown(context)
            }
        }
    }

    /**
     * setAlarmClock: exempt from Doze throttling. The minute TICKs use the exact-while-idle budget
     * (roughly one per ~9 minutes in deep Doze), so the finish must not compete with them. The
     * trade-off is that a running timer shows as the system's "next alarm", as in most clock apps.
     */
    private fun scheduleFinish(endAtMillis: Long) {
        if (!alarmManager.canScheduleExactAlarms()) return
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(endAtMillis, TimerNotifications.openTab(context)),
            finishIntent()
        )
    }

    private fun cancelFinish() = alarmManager.cancel(finishIntent())

    private fun finishIntent() =
        LiveUpdates.broadcast(context, TimerReceiver::class.java, TimerReceiver.ACTION_FIRE, REQUEST_FIRE)

    private fun tickIntent() =
        LiveUpdates.broadcast(context, TimerReceiver::class.java, TimerReceiver.ACTION_TICK, REQUEST_TICK)

    private suspend fun load() {
        val p = dataStore.data.first()
        val status = TimerStatus.entries.firstOrNull { it.name == p[KEY_STATUS] } ?: TimerStatus.IDLE
        _state.value = TimerState(
            status = status,
            durationMs = p[KEY_DURATION] ?: 0L,
            endAtMillis = p[KEY_END_AT] ?: 0L,
            remainingMs = p[KEY_REMAINING] ?: 0L,
            pickerMs = p[KEY_PICKER] ?: TimerState.DEFAULT_PICKER_MS,
        )
    }

    /** One batched write with flat typed keys (one DataStore emission per transition). */
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
        const val REQUEST_TICK = 71_002
        const val ONE_MINUTE = 60_000L
        const val FIRE_TOLERANCE_MS = 2_000L
        val KEY_STATUS = stringPreferencesKey("timer_status")
        val KEY_DURATION = longPreferencesKey("timer_duration")
        val KEY_END_AT = longPreferencesKey("timer_end_at")
        val KEY_REMAINING = longPreferencesKey("timer_remaining")
        val KEY_PICKER = longPreferencesKey("timer_picker")
    }
}
