package dev.redfox.alarmzy.widget

import androidx.core.net.toUri
import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WidgetWeather
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenWeather (openweathermap.org) with the user's own API key ([WeatherKeyStore]). Uses the
 * free-tier endpoints: Geocoding 1.0 and Current Weather 2.5. Its condition ids are translated to
 * WMO codes, so the widget's mapping and glyphs are shared with Open-Meteo.
 */
@Singleton
class OpenWeatherClient @Inject constructor(
    private val keyStore: WeatherKeyStore
) : WeatherSource {

    override suspend fun searchCities(query: String): List<WeatherCity> =
        searchCities(query, requireKey())

    override suspend fun currentWeather(city: WeatherCity): WidgetWeather {
        val url = "https://api.openweathermap.org/data/2.5/weather".toUri().buildUpon()
            .appendQueryParameter("lat", city.latitude.toString())
            .appendQueryParameter("lon", city.longitude.toString())
            .appendQueryParameter("units", "metric")
            .appendQueryParameter("appid", requireKey())
            .build().toString()
        val body = httpGet(url)
        return parsing {
            val json = JSONObject(body)
            val condition = json.getJSONArray("weather").getJSONObject(0)
            WidgetWeather(
                temperatureC = json.getJSONObject("main").getDouble("temp"),
                weatherCode = toWmo(condition.getInt("id")),
                // OpenWeather reports m/s in metric units.
                windKmh = (json.optJSONObject("wind")?.optDouble("speed", 0.0) ?: 0.0) * 3.6,
                // Icon codes end in "d" or "n" ("04d").
                isDay = !condition.optString("icon").endsWith("n"),
                fetchedAtMillis = System.currentTimeMillis(),
                latitude = city.latitude,
                longitude = city.longitude
            )
        }
    }

    /** Checks [key] with a one-result geocoding call. Throws [WeatherError.AUTH] when rejected. */
    suspend fun verify(key: String) {
        searchCities("London", key.trim(), limit = 1)
    }

    private suspend fun searchCities(query: String, key: String, limit: Int = 10): List<WeatherCity> {
        val url = "https://api.openweathermap.org/geo/1.0/direct".toUri().buildUpon()
            .appendQueryParameter("q", query.trim())
            .appendQueryParameter("limit", limit.toString())
            .appendQueryParameter("appid", key)
            .build().toString()
        val body = httpGet(url)
        val language = Locale.getDefault().language
        return parsing {
            val results = JSONArray(body)
            (0 until results.length()).map { i ->
                val place = results.getJSONObject(i)
                val name = place.optJSONObject("local_names")?.optString(language)?.takeIf { it.isNotBlank() }
                    ?: place.getString("name")
                WeatherCity(
                    name = name,
                    detail = listOf(place.optString("state"), countryName(place.optString("country")))
                        .filter { it.isNotBlank() && it != name }
                        .distinct()
                        .joinToString(", "),
                    latitude = place.getDouble("lat"),
                    longitude = place.getDouble("lon")
                )
            }.distinctBy { it.latitude to it.longitude }
        }
    }

    private suspend fun requireKey(): String =
        keyStore.current() ?: throw WeatherException(WeatherError.NO_KEY)

    private fun countryName(code: String): String =
        if (code.isBlank()) "" else Locale("", code).displayCountry.ifBlank { code }

    companion object {
        /**
         * OpenWeather condition id → the closest WMO code
         * (https://openweathermap.org/weather-conditions).
         */
        fun toWmo(id: Int): Int = when (id) {
            in 200..299 -> 95 // thunderstorm
            in 300..399 -> 53 // drizzle
            511 -> 66 // freezing rain
            in 500..504 -> 63 // rain
            in 520..531 -> 81 // shower rain
            in 611..613 -> 66 // sleet
            in 615..616 -> 73 // rain and snow
            in 620..622 -> 85 // snow showers
            in 600..699 -> 73 // snow
            in 700..799 -> 45 // mist, haze, fog, dust…
            800 -> 0 // clear
            801 -> 1 // few clouds
            802 -> 2 // scattered clouds
            in 803..804 -> 3 // broken / overcast
            else -> 3
        }
    }
}
