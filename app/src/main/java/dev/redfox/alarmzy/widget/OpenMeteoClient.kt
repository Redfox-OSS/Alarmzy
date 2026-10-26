package dev.redfox.alarmzy.widget

import androidx.core.net.toUri
import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WidgetWeather
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Open-Meteo (open-meteo.com), the default provider: free, no API key, no account. Only two
 * requests ever leave the device: the typed city name (geocoding) and that city's coordinates
 * (current weather).
 */
@Singleton
class OpenMeteoClient @Inject constructor() : WeatherSource {

    override suspend fun searchCities(query: String): List<WeatherCity> {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toUri().buildUpon()
            .appendQueryParameter("name", query.trim())
            .appendQueryParameter("count", "10")
            .appendQueryParameter("language", Locale.getDefault().language)
            .appendQueryParameter("format", "json")
            .build().toString()
        val body = httpGet(url)
        return parsing {
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            (0 until results.length()).map { i ->
                val place = results.getJSONObject(i)
                val name = place.getString("name")
                WeatherCity(
                    name = name,
                    detail = listOf(place.optString("admin1"), place.optString("country"))
                        .filter { it.isNotBlank() && it != name }
                        .distinct()
                        .joinToString(", "),
                    latitude = place.getDouble("latitude"),
                    longitude = place.getDouble("longitude")
                )
            }
        }
    }

    override suspend fun currentWeather(city: WeatherCity): WidgetWeather {
        val url = "https://api.open-meteo.com/v1/forecast".toUri().buildUpon()
            .appendQueryParameter("latitude", city.latitude.toString())
            .appendQueryParameter("longitude", city.longitude.toString())
            .appendQueryParameter("current", "temperature_2m,weather_code,wind_speed_10m,is_day")
            .appendQueryParameter("temperature_unit", "celsius")
            .appendQueryParameter("wind_speed_unit", "kmh")
            .build().toString()
        val body = httpGet(url)
        return parsing {
            val current = JSONObject(body).getJSONObject("current")
            WidgetWeather(
                temperatureC = current.getDouble("temperature_2m"),
                weatherCode = current.getInt("weather_code"),
                windKmh = current.optDouble("wind_speed_10m", 0.0),
                isDay = current.optInt("is_day", 1) == 1,
                fetchedAtMillis = System.currentTimeMillis(),
                latitude = city.latitude,
                longitude = city.longitude
            )
        }
    }
}
