package dev.redfox.alarmzy.domain.model

/** A tappable segment of the home-screen clock widget; each one launches its own app. */
enum class WidgetPart(val displayName: String) {
    DATE("Date"),
    CLOCK("Clock"),
    WEATHER("Weather")
}

enum class WidgetHorizontalAlign(val displayName: String) {
    START("Left"),
    CENTER("Center"),
    END("Right")
}

enum class WidgetVerticalAlign(val displayName: String) {
    TOP("Top"),
    CENTER("Middle"),
    BOTTOM("Bottom")
}

/** Where the widget's weather comes from. */
enum class WeatherProvider(val displayName: String, val needsApiKey: Boolean) {
    /** Free, no key: the default. */
    OPEN_METEO("Open-Meteo", needsApiKey = false),
    /** The user's own OpenWeather API key. */
    OPENWEATHER("OpenWeather", needsApiKey = true)
}

/** A place picked from the weather provider's city search. */
data class WeatherCity(
    val name: String,
    /** Region and country, for telling same-named places apart ("West Bengal, India"). */
    val detail: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String get() = if (detail.isBlank()) name else "$name, $detail"
}

/**
 * Settings of the `OCT27 / 00:02` home-screen widget. App assignments are flattened
 * `ComponentName`s; `null` means the part's default (calendar / Alarmzy / a weather app).
 */
data class ClockWidgetConfig(
    val scale: Float = DEFAULT_SCALE,
    val horizontalAlign: WidgetHorizontalAlign = WidgetHorizontalAlign.CENTER,
    val verticalAlign: WidgetVerticalAlign = WidgetVerticalAlign.CENTER,
    val use24Hour: Boolean = true,
    val boldTime: Boolean = false,
    /** Off by default: weather is the widget's only network use. */
    val showWeather: Boolean = false,
    /** Typed by the user and geocoded once; Alarmzy never reads the device location. */
    val weatherCity: WeatherCity? = null,
    val useFahrenheit: Boolean = false,
    val weatherProvider: WeatherProvider = WeatherProvider.OPEN_METEO,
    val dateApp: String? = null,
    val clockApp: String? = null,
    val weatherApp: String? = null
) {
    fun appFor(part: WidgetPart): String? = when (part) {
        WidgetPart.DATE -> dateApp
        WidgetPart.CLOCK -> clockApp
        WidgetPart.WEATHER -> weatherApp
    }

    fun withApp(part: WidgetPart, component: String?): ClockWidgetConfig = when (part) {
        WidgetPart.DATE -> copy(dateApp = component)
        WidgetPart.CLOCK -> copy(clockApp = component)
        WidgetPart.WEATHER -> copy(weatherApp = component)
    }

    companion object {
        const val DEFAULT_SCALE = 1f
        const val MIN_SCALE = 0.6f
        const val MAX_SCALE = 2f
    }
}
