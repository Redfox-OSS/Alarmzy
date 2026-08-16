package dev.redfox.alarmzy.domain.model

enum class AlarmButtonLayout(val displayName: String) {
    BOTH("Snooze & Dismiss"),
    SNOOZE_ONLY("Snooze only"),
    DISMISS_ONLY("Dismiss only")
}
