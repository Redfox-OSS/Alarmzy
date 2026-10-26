package dev.redfox.alarmzy.presentation.settings.widget

import dev.redfox.alarmzy.domain.model.WeatherCity
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.domain.model.WidgetHorizontalAlign
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.domain.model.WidgetVerticalAlign

sealed interface WidgetCustomizeIntent {
    /** Live slider value: updates the preview only. */
    data class DragScale(val scale: Float) : WidgetCustomizeIntent
    /** Slider released: persists the scale and updates placed widgets. */
    data object CommitScale : WidgetCustomizeIntent
    data object ResetScale : WidgetCustomizeIntent
    data class SetHorizontalAlign(val align: WidgetHorizontalAlign) : WidgetCustomizeIntent
    data class SetVerticalAlign(val align: WidgetVerticalAlign) : WidgetCustomizeIntent
    data class SetUse24Hour(val use24Hour: Boolean) : WidgetCustomizeIntent
    data class SetBoldTime(val bold: Boolean) : WidgetCustomizeIntent
    data class OpenPicker(val part: WidgetPart) : WidgetCustomizeIntent
    data object ClosePicker : WidgetCustomizeIntent
    data class SetQuery(val query: String) : WidgetCustomizeIntent
    /** Assigns [component] (a flattened ComponentName) to the open picker's part; null = default. */
    data class AssignApp(val component: String?) : WidgetCustomizeIntent
    data object AddToHomeScreen : WidgetCustomizeIntent
    /** Turning it on without a city opens the city search first. */
    data class SetShowWeather(val show: Boolean) : WidgetCustomizeIntent
    data class SetFahrenheit(val fahrenheit: Boolean) : WidgetCustomizeIntent
    data object OpenCitySearch : WidgetCustomizeIntent
    data object CloseCitySearch : WidgetCustomizeIntent
    data class SetCityQuery(val query: String) : WidgetCustomizeIntent
    data class PickCity(val city: WeatherCity) : WidgetCustomizeIntent
    data object RefreshWeather : WidgetCustomizeIntent
    /** Choosing a key-based provider without a saved key opens the key sheet. */
    data class SetProvider(val provider: WeatherProvider) : WidgetCustomizeIntent
    data object OpenApiKeySheet : WidgetCustomizeIntent
    data object CloseApiKeySheet : WidgetCustomizeIntent
    data class SetApiKeyDraft(val key: String) : WidgetCustomizeIntent
    /** Verifies, then saves; [skipCheck] saves a key OpenWeather rejected or couldn't check. */
    data class SaveApiKey(val skipCheck: Boolean = false) : WidgetCustomizeIntent
    data object RemoveApiKey : WidgetCustomizeIntent
    /** Screen resumed: the user may have added or removed widgets, or (un)installed apps. */
    data object Refresh : WidgetCustomizeIntent
}
