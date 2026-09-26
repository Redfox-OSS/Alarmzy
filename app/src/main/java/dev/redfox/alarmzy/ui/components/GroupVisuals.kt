package dev.redfox.alarmzy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.GroupIcon

@get:DrawableRes
val GroupIcon?.drawableRes: Int
    get() = when (this) {
        null -> R.drawable.ic_folder
        GroupIcon.MORNING -> R.drawable.ic_group_morning
        GroupIcon.FITNESS -> R.drawable.ic_group_fitness
        GroupIcon.MEDICATION -> R.drawable.ic_group_medication
        GroupIcon.WORK -> R.drawable.ic_group_work
        GroupIcon.COFFEE -> R.drawable.ic_group_coffee
        GroupIcon.NIGHT -> R.drawable.ic_group_night
        GroupIcon.STUDY -> R.drawable.ic_group_study
        GroupIcon.TRANSIT -> R.drawable.ic_group_transit
        GroupIcon.COOKING -> R.drawable.ic_group_cooking
        GroupIcon.MINDFULNESS -> R.drawable.ic_group_mindfulness
        GroupIcon.HOME -> R.drawable.ic_group_home
        GroupIcon.HYDRATION -> R.drawable.ic_group_hydration
        GroupIcon.DETOX -> R.drawable.ic_group_detox
        GroupIcon.PET -> R.drawable.ic_group_pet
        GroupIcon.TIMER -> R.drawable.ic_group_timer
        GroupIcon.SOCIAL -> R.drawable.ic_group_social
        GroupIcon.GAMING -> R.drawable.ic_group_gaming
        GroupIcon.MUSIC -> R.drawable.ic_group_music
    }

/** Group color swatches from DESIGN.md (persisted as hex strings). */
val GroupColorSwatches: List<String> = listOf(
    "#5C6BC0", // Indigo Blue
    "#26A69A", // Teal Mint
    "#FF3B4E", // Crimson
    "#FB8C00", // Vibrant Orange
    "#4CAF50", // Emerald Green
    "#AB47BC", // Amethyst Purple
    "#FBC02D", // Solar Amber
    "#29B6F6", // Electric Sky
    "#EC407A", // Coral Pink
    "#8D6E63", // Warm Sand
)

/** Parses a "#RRGGBB" group color; null for missing/invalid values. */
fun parseGroupColor(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
