package dev.redfox.alarmzy.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's OpenWeather API key. Kept in [Context.getNoBackupFilesDir] (app-private, never
 * included in cloud backups or device transfers) rather than the settings DataStore, which is.
 */
@Singleton
class WeatherKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val file = File(context.noBackupFilesDir, "openweather_api_key")
    private val _key = MutableStateFlow<String?>(null)
    @Volatile private var loaded = false

    /** The saved key, or null. Call [load] (or [current]) once before relying on it. */
    val key: StateFlow<String?> = _key.asStateFlow()

    suspend fun current(): String? {
        load()
        return _key.value
    }

    suspend fun load() {
        if (loaded) return
        _key.value = withContext(Dispatchers.IO) {
            file.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        }
        loaded = true
    }

    suspend fun save(key: String?) {
        val trimmed = key?.trim()?.takeIf { it.isNotEmpty() }
        withContext(Dispatchers.IO) {
            if (trimmed == null) file.delete() else file.writeText(trimmed)
        }
        _key.value = trimmed
        loaded = true
    }
}
