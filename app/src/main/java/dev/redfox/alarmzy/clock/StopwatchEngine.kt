package dev.redfox.alarmzy.clock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.redfox.alarmzy.domain.model.StopwatchState
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
 * Process-wide stopwatch — the single owner of stopwatch state. It's persisted to DataStore with
 * wall-clock instants so it keeps running across screen changes, process death and reboots. Each
 * transition re-posts the Live Update and (while running) re-arms its 60s TICK.
 */
@Singleton
class StopwatchEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private val loaded = scope.async { load() }

    fun start() = mutate { s ->
        if (s.running) s else s.copy(running = true, startedAt = now())
    }

    fun pause() = mutate { s ->
        if (!s.running) s else s.copy(running = false, accumulatedMs = s.elapsedAt(now()), startedAt = 0L)
    }

    fun lap() = mutate { s ->
        if (!s.running) s else s.copy(laps = s.laps + s.elapsedAt(now()))
    }

    fun reset() = mutate { StopwatchState() }

    /** Re-posts the Live Update and re-arms the TICK (TICK alarm, reboot, permission granted). */
    fun refresh() = mutate(reapplySideEffects = true) { it }

    /** The clock the UI must read too when calling [StopwatchState.elapsedAt]. */
    fun now(): Long = System.currentTimeMillis()

    private fun mutate(reapplySideEffects: Boolean = false, transform: (StopwatchState) -> StopwatchState) {
        scope.launch {
            loaded.await()
            mutex.withLock {
                val prev = _state.value
                val next = transform(prev)
                if (next != prev) {
                    _state.value = next
                    persist(next)
                }
                if (next != prev || reapplySideEffects) applySideEffects(next)
            }
        }
    }

    private fun applySideEffects(s: StopwatchState) {
        val tick = LiveUpdates.broadcast(context, StopwatchReceiver::class.java, StopwatchReceiver.ACTION_TICK, REQUEST_TICK)
        when {
            s.isIdle -> {
                LiveUpdates.cancelTick(context, tick)
                StopwatchNotifications.cancel(context)
            }
            s.running -> {
                StopwatchNotifications.show(context, s)
                LiveUpdates.scheduleTick(context, tick)
            }
            else -> {
                LiveUpdates.cancelTick(context, tick)
                StopwatchNotifications.show(context, s)
            }
        }
    }

    private suspend fun load() {
        val prefs = dataStore.data.first()
        _state.value = StopwatchState(
            running = prefs[KEY_RUNNING] ?: false,
            startedAt = prefs[KEY_STARTED_AT] ?: 0L,
            accumulatedMs = prefs[KEY_ACCUMULATED] ?: 0L,
            laps = prefs[KEY_LAPS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        )
    }

    /** One batched write with flat typed keys. */
    private suspend fun persist(s: StopwatchState) {
        dataStore.edit { p ->
            p[KEY_RUNNING] = s.running
            p[KEY_STARTED_AT] = s.startedAt
            p[KEY_ACCUMULATED] = s.accumulatedMs
            p[KEY_LAPS] = s.laps.joinToString(",")
        }
    }

    private companion object {
        const val REQUEST_TICK = 71_101
        // "_wall" keys: an earlier build stored elapsedRealtime under unsuffixed keys; those are ignored.
        val KEY_RUNNING = booleanPreferencesKey("stopwatch_running_wall")
        val KEY_STARTED_AT = longPreferencesKey("stopwatch_started_at_wall")
        val KEY_ACCUMULATED = longPreferencesKey("stopwatch_accumulated_wall")
        val KEY_LAPS = stringPreferencesKey("stopwatch_laps_wall")
    }
}
