package dev.redfox.alarmzy.domain

import dev.redfox.alarmzy.domain.model.WeatherCondition
import dev.redfox.alarmzy.widget.OpenWeatherClient
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun mapsWmoCodes() {
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
        expected.forEach { (code, condition) -> assertEquals("code $code", condition, WeatherCondition.from(code, 0.0)) }
    }

    @Test
    fun windOnlyOverridesDrySkies() {
        assertEquals(WeatherCondition.WINDY, WeatherCondition.from(0, 40.0))
        assertEquals(WeatherCondition.WINDY, WeatherCondition.from(3, WeatherCondition.WINDY_KMH))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.from(3, 31.9))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.from(63, 50.0))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.from(95, 60.0))
    }

    @Test
    fun unknownCodeFallsBackToOvercast() {
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.from(42, 0.0))
    }

    @Test
    fun mapsOpenWeatherIdsThroughWmo() {
        val expected = mapOf(
            211 to WeatherCondition.THUNDERSTORM,
            301 to WeatherCondition.DRIZZLE,
            500 to WeatherCondition.RAIN,
            511 to WeatherCondition.FREEZING_RAIN,
            521 to WeatherCondition.SHOWERS,
            601 to WeatherCondition.SNOW,
            611 to WeatherCondition.FREEZING_RAIN,
            616 to WeatherCondition.SNOW,
            621 to WeatherCondition.SNOW_SHOWERS,
            741 to WeatherCondition.FOG,
            800 to WeatherCondition.CLEAR,
            801 to WeatherCondition.MAINLY_CLEAR,
            802 to WeatherCondition.PARTLY_CLOUDY,
            804 to WeatherCondition.OVERCAST
        )
        expected.forEach { (id, condition) ->
            assertEquals("OpenWeather $id", condition, WeatherCondition.from(OpenWeatherClient.toWmo(id), 0.0))
        }
    }
}
