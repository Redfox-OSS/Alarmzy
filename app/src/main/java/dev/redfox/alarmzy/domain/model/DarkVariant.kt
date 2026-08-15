package dev.redfox.alarmzy.domain.model

/**
 * Intensity of the dark theme's neutral surfaces. Only relevant when the
 * resolved theme is dark — [DARK] matches Material 3's default dark ramp,
 * [DARKER] is a dark-grey ramp, [DARKEST] is true AMOLED black.
 */
enum class DarkVariant(val displayName: String) {
    DARK("Dark"),
    DARKER("Darker"),
    DARKEST("Darkest")
}
