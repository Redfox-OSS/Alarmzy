package dev.redfox.alarmzy.domain.model

/**
 * Category icon for an [AlarmGroup]. [key] is what's persisted, so never rename existing keys.
 * The drawable for each icon is resolved in the presentation layer.
 */
enum class GroupIcon(val key: String, val label: String) {
    MORNING("morning", "Morning"),
    FITNESS("fitness", "Fitness"),
    MEDICATION("medication", "Medication"),
    WORK("work", "Work"),
    COFFEE("coffee", "Coffee"),
    NIGHT("night", "Night"),
    STUDY("study", "Study"),
    TRANSIT("transit", "Travel"),
    COOKING("cooking", "Meals"),
    MINDFULNESS("mindfulness", "Mindfulness"),
    HOME("home", "Home"),
    HYDRATION("hydration", "Hydration"),
    DETOX("detox", "Screen break"),
    PET("pet", "Pet care"),
    TIMER("timer", "Focus"),
    SOCIAL("social", "Social"),
    GAMING("gaming", "Gaming"),
    MUSIC("music", "Audio");

    companion object {
        fun fromKey(key: String?): GroupIcon? = entries.firstOrNull { it.key == key }
    }
}
