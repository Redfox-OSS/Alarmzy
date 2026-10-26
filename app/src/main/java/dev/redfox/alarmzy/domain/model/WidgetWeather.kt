package dev.redfox.alarmzy.domain.model

import kotlin.math.roundToInt

/**
 * What the widget shows for a reading, mapped from Open-Meteo's WMO `weather_code` (plus wind
 * speed for [WINDY]). Each maps to one Tabler glyph in `ClockWidgetRenderer.iconFor`.
 */
enum class WeatherCondition(val displayName: String) {
    CLEAR("Clear"),
    MAINLY_CLEAR("Mainly clear"),
    PARTLY_CLOUDY("Partly cloudy"),
    OVERCAST("Overcast"),
    FOG("Fog"),
    DRIZZLE("Drizzle"),
    FREEZING_DRIZZLE("Freezing drizzle"),
    RAIN("Rain"),
    FREEZING_RAIN("Freezing rain"),
    SHOWERS("Rain showers"),
    SNOW("Snow"),
    SNOW_GRAINS("Snow grains"),
    SNOW_SHOWERS("Snow showers"),
    THUNDERSTORM("Thunderstorm"),
    THUNDERSTORM_HAIL("Thunderstorm with hail"),
    WINDY("Windy");

    companion object {
        /** Sustained wind from which a dry sky reads as windy (NWS "windy": 20+ mph). */
        const val WINDY_KMH = 32.0

        fun from(wmoCode: Int, windKmh: Double): WeatherCondition {
            val sky = when (wmoCode) {
                0 -> CLEAR
                1 -> MAINLY_CLEAR
                2 -> PARTLY_CLOUDY
                3 -> OVERCAST
                45, 48 -> FOG
                51, 53, 55 -> DRIZZLE
                56, 57 -> FREEZING_DRIZZLE
                61, 63, 65 -> RAIN
                66, 67 -> FREEZING_RAIN
                71, 73, 75 -> SNOW
                77 -> SNOW_GRAINS
                80, 81, 82 -> SHOWERS
                85, 86 -> SNOW_SHOWERS
                95 -> THUNDERSTORM
                96, 99 -> THUNDERSTORM_HAIL
                else -> OVERCAST
            }
            // Like KWGT's WINDY: wind only takes over when nothing is falling from the sky.
            val dry = sky in setOf(CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST)
            return if (dry && windKmh >= WINDY_KMH) WINDY else sky
        }
    }
}

/** Last current-weather reading, cached for the widget. Temperature is always stored in °C. */
data class WidgetWeather(
    val temperatureC: Double,
    /** Raw WMO code, so a finer mapping later needs no refetch. */
    val weatherCode: Int,
    val windKmh: Double,
    val isDay: Boolean,
    val fetchedAtMillis: Long,
    /** Coordinates the reading belongs to, so a city change invalidates it. */
    val latitude: Double,
    val longitude: Double
) {
    val condition: WeatherCondition get() = WeatherCondition.from(weatherCode, windKmh)

    fun temperature(fahrenheit: Boolean): Int =
        (if (fahrenheit) temperatureC * 9 / 5 + 32 else temperatureC).roundToInt()

    fun isFor(city: WeatherCity): Boolean = latitude == city.latitude && longitude == city.longitude
}
