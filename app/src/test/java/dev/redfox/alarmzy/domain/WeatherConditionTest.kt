package dev.redfox.alarmzy.domain

import dev.redfox.alarmzy.domain.model.WeatherCondition
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.domain.model.WidgetWeather
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun mapsOpenMeteoWmoCodes() {
        val expected = mapOf(
            0 to WeatherCondition.CLEAR,
            1 to WeatherCondition.MAINLY_CLEAR,
            2 to WeatherCondition.PARTLY_CLOUDY,
            3 to WeatherCondition.OVERCAST,
            45 to WeatherCondition.FOG,
            48 to WeatherCondition.FOG,
            53 to WeatherCondition.DRIZZLE,
            57 to WeatherCondition.FREEZING_DRIZZLE,
            63 to WeatherCondition.RAIN,
            67 to WeatherCondition.FREEZING_RAIN,
            75 to WeatherCondition.SNOW,
            77 to WeatherCondition.SNOW_GRAINS,
            81 to WeatherCondition.SHOWERS,
            86 to WeatherCondition.SNOW_SHOWERS,
            95 to WeatherCondition.THUNDERSTORM,
            99 to WeatherCondition.THUNDERSTORM_HAIL
        )
        expected.forEach { (code, condition) ->
            assertEquals("WMO $code", condition, WeatherCondition.fromOpenMeteo(code, 0.0))
        }
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromOpenMeteo(42, 0.0))
    }

    @Test
    fun mapsOpenWeatherIdsDirectly() {
        val expected = mapOf(
            211 to WeatherCondition.THUNDERSTORM,
            301 to WeatherCondition.DRIZZLE,
            500 to WeatherCondition.RAIN,
            511 to WeatherCondition.FREEZING_RAIN,
            521 to WeatherCondition.SHOWERS,
            601 to WeatherCondition.SNOW,
            611 to WeatherCondition.SLEET,
            616 to WeatherCondition.RAIN_AND_SNOW,
            621 to WeatherCondition.SNOW_SHOWERS,
            701 to WeatherCondition.MIST,
            711 to WeatherCondition.SMOKE,
            721 to WeatherCondition.HAZE,
            731 to WeatherCondition.DUST,
            741 to WeatherCondition.FOG,
            751 to WeatherCondition.SAND,
            761 to WeatherCondition.DUST,
            762 to WeatherCondition.ASH,
            771 to WeatherCondition.SQUALLS,
            781 to WeatherCondition.TORNADO,
            800 to WeatherCondition.CLEAR,
            801 to WeatherCondition.MAINLY_CLEAR,
            802 to WeatherCondition.PARTLY_CLOUDY,
            804 to WeatherCondition.OVERCAST
        )
        expected.forEach { (id, condition) ->
            assertEquals("OpenWeather $id", condition, WeatherCondition.fromOpenWeather(id, 0.0))
        }
    }

    @Test
    fun windOnlyOverridesDrySkies() {
        assertEquals(WeatherCondition.WINDY, WeatherCondition.fromOpenMeteo(0, 40.0))
        assertEquals(WeatherCondition.WINDY, WeatherCondition.fromOpenMeteo(3, WeatherCondition.WINDY_KMH))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromOpenMeteo(3, 31.9))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromOpenMeteo(63, 50.0))
        assertEquals(WeatherCondition.WINDY, WeatherCondition.fromOpenWeather(803, 40.0))
        assertEquals(WeatherCondition.MIST, WeatherCondition.fromOpenWeather(701, 40.0))
    }

    @Test
    fun readingUsesItsOwnProvidersTable() {
        // 800 is "clear" in OpenWeather's table but an unknown WMO code (→ overcast) in Open-Meteo's.
        fun reading(source: WeatherProvider, code: Int) =
            WidgetWeather(20.0, source, code, 0.0, isDay = true, fetchedAtMillis = 0L, latitude = 0.0, longitude = 0.0)
        assertEquals(WeatherCondition.CLEAR, reading(WeatherProvider.OPEN_METEO, 0).condition)
        assertEquals(WeatherCondition.CLEAR, reading(WeatherProvider.OPENWEATHER, 800).condition)
        assertEquals(WeatherCondition.MIST, reading(WeatherProvider.OPENWEATHER, 701).condition)
        assertEquals(WeatherCondition.OVERCAST, reading(WeatherProvider.OPEN_METEO, 800).condition)
    }
}
