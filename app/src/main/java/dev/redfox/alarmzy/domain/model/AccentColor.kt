package dev.redfox.alarmzy.domain.model

/**
 * Accent palette options. [DYNAMIC] follows the device wallpaper (Material You),
 * the rest are fixed Material 3 tonal palettes. Each swatch color is a vivid
 * representative hue shown in the picker — the actual scheme uses tonal values.
 */
enum class AccentColor(val displayName: String, val swatch: Long) {
    DYNAMIC("Dynamic", 0xFF8B8B8B),
    PURPLE("Purple", 0xFF7E57C2),
    BLUE("Blue", 0xFF4285F4),
    TEAL("Teal", 0xFF009688),
    GREEN("Green", 0xFF43A047),
    ORANGE("Orange", 0xFFFF7043),
    ROSE("Rose", 0xFFEC407A)
}
