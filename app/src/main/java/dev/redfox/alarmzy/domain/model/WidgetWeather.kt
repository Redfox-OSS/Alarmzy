package dev.redfox.alarmzy.domain.model

import kotlin.math.roundToInt

/**
 * What the widget shows for a reading. Each provider has its own table into it — Open-Meteo's
 * WMO codes ([fromOpenMeteo]) and OpenWeather's condition ids ([fromOpenWeather]), which are
 * finer for mist, haze, dust, squalls and tornadoes. Each maps to one Tabler glyph in
 * `ClockWidgetRenderer.iconFor`.
 */
enum class WeatherCondition(val displayName: String) {
    CLEAR("Clear"),
    MAINLY_CLEAR("Mainly clear"),
    PARTLY_CLOUDY("Partly cloudy"),
    OVERCAST("Overcast"),
    FOG("Fog"),
    MIST("Mist"),
    HAZE("Haze"),
    SMOKE("Smoke"),
    DUST("Dust"),
    SAND("Sand"),
    ASH("Volcanic ash"),
    DRIZZLE("Drizzle"),
    FREEZING_DRIZZLE("Freezing drizzle"),
    RAIN("Rain"),
    FREEZING_RAIN("Freezing rain"),
    SHOWERS("Rain showers"),
    SLEET("Sleet"),
    RAIN_AND_SNOW("Rain and snow"),
    SNOW("Snow"),
    SNOW_GRAINS("Snow grains"),
    SNOW_SHOWERS("Snow showers"),
    THUNDERSTORM("Thunderstorm"),
    THUNDERSTORM_HAIL("Thunderstorm with hail"),
    SQUALLS("Squalls"),
    TORNADO("Tornado"),
    WINDY("Windy");

    companion object {
        /** Sustained wind from which a dry sky reads as windy (NWS "windy": 20+ mph). */
        const val WINDY_KMH = 32.0

        private val DRY_SKIES = setOf(CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST)

        /** Open-Meteo `weather_code` (WMO 4677 subset). */
        fun fromOpenMeteo(wmoCode: Int, windKmh: Double): WeatherCondition = windy(
            when (wmoCode) {
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
            },
            windKmh
        )

        /** OpenWeather condition id (https://openweathermap.org/weather-conditions). */
        fun fromOpenWeather(id: Int, windKmh: Double): WeatherCondition = windy(
            when (id) {
                in 200..299 -> THUNDERSTORM
                in 300..399 -> DRIZZLE
                511 -> FREEZING_RAIN
                in 500..504 -> RAIN
                in 520..531 -> SHOWERS
                in 611..613 -> SLEET
                615, 616 -> RAIN_AND_SNOW
                in 620..622 -> SNOW_SHOWERS
                in 600..602 -> SNOW
                701 -> MIST
                711 -> SMOKE
                721 -> HAZE
                731, 761 -> DUST
                741 -> FOG
                751 -> SAND
                762 -> ASH
                771 -> SQUALLS
                781 -> TORNADO
                800 -> CLEAR
                801 -> MAINLY_CLEAR
                802 -> PARTLY_CLOUDY
                803, 804 -> OVERCAST
                else -> OVERCAST
            },
            windKmh
        )

        /** Like KWGT's WINDY: wind only takes over when nothing is falling from the sky. */
        private fun windy(sky: WeatherCondition, windKmh: Double): WeatherCondition =
            if (sky in DRY_SKIES && windKmh >= WINDY_KMH) WINDY else sky
    }
}

/** Last current-weather reading, cached for the widget. Temperature is always stored in °C. */
data class WidgetWeather(
    val temperatureC: Double,
    /** Which table [code] belongs to. */
    val source: WeatherProvider,
    /** The provider's raw condition code (WMO for Open-Meteo, condition id for OpenWeather). */
    val code: Int,
    val windKmh: Double,
    val isDay: Boolean,
    val fetchedAtMillis: Long,
    /** Coordinates the reading belongs to, so a city change invalidates it. */
    val latitude: Double,
    val longitude: Double
) {
    val condition: WeatherCondition
        get() = when (source) {
            WeatherProvider.OPEN_METEO -> WeatherCondition.fromOpenMeteo(code, windKmh)
            WeatherProvider.OPENWEATHER -> WeatherCondition.fromOpenWeather(code, windKmh)
        }

    fun temperature(fahrenheit: Boolean): Int =
        (if (fahrenheit) temperatureC * 9 / 5 + 32 else temperatureC).roundToInt()

    fun isFor(city: WeatherCity): Boolean = latitude == city.latitude && longitude == city.longitude
}
