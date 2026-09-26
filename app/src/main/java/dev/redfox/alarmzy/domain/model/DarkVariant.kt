package dev.redfox.alarmzy.domain.model

/**
 * Intensity of the (always dark) theme's neutral surfaces. [DARK] is the design's base
 * #131316 ramp, [DARKER] the deeper #0E0E11 ramp, [DARKEST] true AMOLED black.
 */
enum class DarkVariant(val displayName: String) {
    DARK("Dark"),
    DARKER("Darker"),
    DARKEST("AMOLED");

    companion object {
        fun fromName(name: String?): DarkVariant = entries.firstOrNull { it.name == name } ?: DARK
    }
}
