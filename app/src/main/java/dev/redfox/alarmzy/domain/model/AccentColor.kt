package dev.redfox.alarmzy.domain.model

/**
 * Accent palette options from the Alarmzy Obsidian design system. [swatch] is the vivid
 * representative hue shown in the picker; the full tonal palette lives in `ui/theme`.
 */
enum class AccentColor(val displayName: String, val swatch: Long) {
    CRIMSON("Crimson", 0xFFFF2A42),
    ELECTRIC("Electric", 0xFF0084FF),
    SOLAR("Solar", 0xFFFFB800),
    EMERALD("Emerald", 0xFF00E676),
    LUMINOUS("Luminous", 0xFFFFFFFF);

    companion object {
        val DEFAULT = CRIMSON

        /** Parses a persisted name, falling back to [DEFAULT] for removed/unknown accents. */
        fun fromName(name: String?): AccentColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
