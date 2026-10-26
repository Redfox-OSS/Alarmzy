package dev.redfox.alarmzy.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dev.redfox.alarmzy.MainActivity
import dev.redfox.alarmzy.domain.model.WidgetPart

/** An installed app with a launcher entry. */
data class LaunchableApp(val component: ComponentName, val label: String) {
    val key: String get() = component.flattenToString()
}

/**
 * Lists launchable apps and resolves which app each widget part opens. Package visibility for
 * these queries comes from the `<queries>` block in the manifest (launcher + calendar intents).
 */
object WidgetApps {
    // The int-flag PackageManager overloads are deprecated on 33+, but the Flags ones need 33
    // and minSdk is 31.

    fun launcherApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { info ->
                LaunchableApp(
                    component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                    label = info.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    /** The app [part] opens: the user's pick while it's still installed, else the part's default. */
    fun resolve(context: Context, part: WidgetPart, stored: String?): LaunchableApp? {
        stored?.let(ComponentName::unflattenFromString)?.let { component ->
            label(context, component)?.let { return LaunchableApp(component, it) }
        }
        return defaultFor(context, part)
    }

    fun defaultFor(context: Context, part: WidgetPart): LaunchableApp? = when (part) {
        WidgetPart.DATE -> calendarApp(context)
        WidgetPart.CLOCK -> alarmzy(context)
        WidgetPart.WEATHER -> launcherApps(context).firstOrNull {
            it.label.contains("weather", ignoreCase = true) ||
                it.component.packageName.contains("weather", ignoreCase = true)
        }
    }

    fun alarmzy(context: Context): LaunchableApp {
        val component = ComponentName(context, MainActivity::class.java)
        return LaunchableApp(component, label(context, component) ?: "Alarmzy")
    }

    fun launchIntent(component: ComponentName): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun calendarApp(context: Context): LaunchableApp? {
        val pm = context.packageManager
        val selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        // The preferred calendar first ("android" is the chooser, returned when there's no default),
        // then any app in the calendar category, then anything that looks like a calendar.
        val preferred = pm.resolveActivity(selector, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.takeIf { it != "android" }
        val calendars = pm.queryIntentActivities(selector, 0)
            .map { it.activityInfo.packageName }
        // Map onto launcher activities so the label/icon match the app drawer.
        val apps = launcherApps(context)
        return apps.firstOrNull { it.component.packageName == preferred }
            ?: apps.firstOrNull { it.component.packageName in calendars }
            ?: apps.firstOrNull { it.component.packageName.contains("calendar", ignoreCase = true) }
    }

    private fun label(context: Context, component: ComponentName): String? = runCatching {
        val pm = context.packageManager
        pm.getActivityInfo(component, 0)
            .takeIf { it.enabled && it.exported }
            ?.loadLabel(pm)?.toString()
    }.getOrNull()
}
