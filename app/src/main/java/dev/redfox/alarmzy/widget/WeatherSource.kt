package dev.redfox.alarmzy.widget

import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.domain.model.WidgetWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** A weather provider: city search plus current conditions. Both throw [WeatherException]. */
interface WeatherSource {
    suspend fun searchCities(query: String): List<WeatherCity>
    suspend fun currentWeather(city: WeatherCity): WidgetWeather
}

/** Why a weather request failed; persisted (by name) so the settings page can explain it. */
enum class WeatherError {
    /** Offline, timeout, server error or an unreadable reply. */
    NETWORK,
    /** OpenWeather selected but no API key saved. */
    NO_KEY,
    /** The provider rejected the API key (HTTP 401). */
    AUTH
}

class WeatherException(val error: WeatherError, cause: Throwable? = null) : Exception(error.name, cause)

@Singleton
class WeatherSources @Inject constructor(
    private val openMeteo: OpenMeteoClient,
    private val openWeather: OpenWeatherClient
) {
    fun forProvider(provider: WeatherProvider): WeatherSource = when (provider) {
        WeatherProvider.OPEN_METEO -> openMeteo
        WeatherProvider.OPENWEATHER -> openWeather
    }
}

/**
 * GETs [url] and returns the body. Plain `HttpURLConnection`: a couple of tiny GETs don't justify
 * an HTTP stack. Short timeouts, because refreshes run inside a widget broadcast's goAsync() window.
 */
internal suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val connection = try {
        URL(url).openConnection() as HttpURLConnection
    } catch (e: IOException) {
        throw WeatherException(WeatherError.NETWORK, e)
    }
    try {
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("Accept", "application/json")
        when (val code = connection.responseCode) {
            in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
            HttpURLConnection.HTTP_UNAUTHORIZED -> throw WeatherException(WeatherError.AUTH)
            else -> throw WeatherException(WeatherError.NETWORK, IOException("HTTP $code"))
        }
    } catch (e: IOException) {
        throw WeatherException(WeatherError.NETWORK, e)
    } finally {
        connection.disconnect()
    }
}

/** Runs [block] (JSON parsing), turning malformed replies into [WeatherError.NETWORK]. */
internal inline fun <T> parsing(block: () -> T): T = try {
    block()
} catch (e: WeatherException) {
    throw e
} catch (e: Exception) {
    throw WeatherException(WeatherError.NETWORK, e)
}
