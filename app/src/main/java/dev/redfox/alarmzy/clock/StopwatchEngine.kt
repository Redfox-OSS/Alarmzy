package dev.redfox.alarmzy.clock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
 * Process-wide stopwatch. State lives here (not in a ViewModel) and is persisted to DataStore
 * so it keeps running across screen changes and process death. The UI derives the live
 * reading from [StopwatchState.elapsedAt] with `SystemClock.elapsedRealtime()`.
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

    private fun now() = SystemClock.elapsedRealtime()

    private fun mutate(transform: (StopwatchState) -> StopwatchState) {
        scope.launch {
            loaded.await()
            mutex.withLock {
                val next = transform(_state.value)
                if (next != _state.value) {
                    _state.value = next
                    persist(next)
                }
            }
        }
    }

    private suspend fun load() {
        val prefs = dataStore.data.first()
        // elapsedRealtime restarts at boot, so a stopwatch saved in an earlier boot is meaningless.
        if (prefs[KEY_BOOT_COUNT] != bootCount()) {
            _state.value = StopwatchState()
            return
        }
        _state.value = StopwatchState(
            running = prefs[KEY_RUNNING] ?: false,
            startedAt = prefs[KEY_STARTED_AT] ?: 0L,
            accumulatedMs = prefs[KEY_ACCUMULATED] ?: 0L,
            laps = prefs[KEY_LAPS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        )
    }

    private suspend fun persist(s: StopwatchState) {
        dataStore.edit { p ->
            p[KEY_RUNNING] = s.running
            p[KEY_STARTED_AT] = s.startedAt
            p[KEY_ACCUMULATED] = s.accumulatedMs
            p[KEY_LAPS] = s.laps.joinToString(",")
            p[KEY_BOOT_COUNT] = bootCount()
        }
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private companion object {
        val KEY_RUNNING = booleanPreferencesKey("stopwatch_running")
        val KEY_STARTED_AT = longPreferencesKey("stopwatch_started_at")
        val KEY_ACCUMULATED = longPreferencesKey("stopwatch_accumulated")
        val KEY_LAPS = stringPreferencesKey("stopwatch_laps")
        val KEY_BOOT_COUNT = intPreferencesKey("stopwatch_boot_count")
    }
}
