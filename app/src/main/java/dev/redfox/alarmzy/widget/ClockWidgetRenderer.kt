package dev.redfox.alarmzy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WidgetHorizontalAlign
import dev.redfox.alarmzy.domain.model.WidgetLayout
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.domain.model.WidgetVerticalAlign
import dev.redfox.alarmzy.domain.model.WeatherCondition
import dev.redfox.alarmzy.domain.model.WidgetWeather
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.core.graphics.withRotation
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds the clock widget's [RemoteViews] from a [ClockWidgetConfig].
 *
 * Every text part is drawn into a bitmap here, in our own (unrestricted) process: launchers
 * inflate widgets in a restricted context, where TextView silently drops bundled fonts and the
 * line would fall back to Roboto. The price is that the time doesn't tick by itself —
 * [ClockWidgetTicker] re-renders at each minute boundary. The same views back the in-app live
 * preview, so the two can't drift apart.
 */
object ClockWidgetRenderer {

    /** Base text size at scale 1. */
    private const val BASE_TEXT_SP = 16f

    // Proportions measured off the KWGT original, in ems of the text size.
    /** Letter tracking. */
    private const val TRACKING_EM = 0.27f
    /** Ink-to-ink gap on each side of a divider. */
    private const val SEPARATOR_GAP_EM = 0.87f
    /** The divider is a shape, not a "/": a 2 x 18 capsule tilted 22° (text size 19 in KWGT). */
    private const val DIVIDER_WIDTH_EM = 2f / 19f
    private const val DIVIDER_HEIGHT_EM = 18f / 19f
    private const val DIVIDER_ANGLE_DEG = 22f
    /**
     * Bold: Be Vietnam Pro Medium's stems are 1.52x Light's (0.114 vs 0.075 em), so the divider
     * thickens by the same factor and the glyphs switch to their stroke-3 (vs 2) variants.
     */
    private const val BOLD_STROKE_SCALE = 1.52f
    /** Vertical layout: extra space between stacked lines, on top of each line's own box. */
    private const val STACK_GAP_EM = 0.4f
    /** KWGT's small degree is Be Vietnam Pro's ring "˚", nudged down to sit level with the caps. */
    private const val DEGREE = "\u02DA"
    private const val DEGREE_DROP_EM = 0.065f
    /** Weather glyph: Tabler icon box size, and ink gap after the degree. */
    private const val ICON_BOX_EM = 1.2f
    private const val ICON_GAP_EM = 0.54f

    /** View ids that make up each tappable part. */
    fun partViews(part: WidgetPart): IntArray = when (part) {
        WidgetPart.DATE -> intArrayOf(R.id.widget_date)
        WidgetPart.CLOCK -> intArrayOf(R.id.widget_time)
        WidgetPart.WEATHER -> intArrayOf(R.id.widget_temp)
    }

    /** @param withClicks false for the in-app preview, which wires its own click listeners. */
    fun build(
        context: Context,
        config: ClockWidgetConfig,
        weather: WidgetWeather?,
        now: ZonedDateTime = ZonedDateTime.now(),
        withClicks: Boolean = true
    ): RemoteViews {
        val stacked = config.layout == WidgetLayout.VERTICAL
        val views = RemoteViews(context.packageName, layoutFor(config))
        val metrics = context.resources.displayMetrics
        val textSp = BASE_TEXT_SP * config.scale.coerceIn(ClockWidgetConfig.MIN_SCALE, ClockWidgetConfig.MAX_SCALE)
        val textPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp, metrics)
        val text = paint(context, if (config.bold) R.font.be_vietnam_pro_medium else R.font.be_vietnam_pro_light, textPx)
        val locale = Locale.getDefault()

        val date = DateTimeFormatter.ofPattern("MMMdd", locale).format(now).uppercase(locale)
        val time = DateTimeFormatter.ofPattern(if (config.use24Hour) "HH:mm" else "h:mm", locale).format(now)
        val line = LineBox(text, metrics.densityDpi, strokeScale = if (config.bold) BOLD_STROKE_SCALE else 1f)
        views.setImageViewBitmap(R.id.widget_date, line.text(date, text))
        // Dividers only exist in the single-line layout.
        if (!stacked) views.setImageViewBitmap(R.id.widget_sep_date, line.separator(text))
        views.setImageViewBitmap(R.id.widget_time, line.text(time, text))
        views.setContentDescription(R.id.widget_date, DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(now))
        views.setContentDescription(R.id.widget_time, DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(now))

        views.setInt(R.id.widget_root, "setGravity", gravityOf(config))
        if (stacked) {
            val gap = STACK_GAP_EM * textPx
            listOf(R.id.widget_time, R.id.widget_weather).forEach {
                views.setViewLayoutMargin(it, RemoteViews.MARGIN_TOP, gap, TypedValue.COMPLEX_UNIT_PX)
            }
        }

        // Weather needs a city; until the first reading arrives it shows "--°".
        val showWeather = config.showWeather && config.weatherCity != null
        views.setViewVisibility(R.id.widget_weather, if (showWeather) View.VISIBLE else View.GONE)
        if (showWeather) {
            val temperature = weather?.temperature(config.useFahrenheit)?.toString() ?: "--"
            if (!stacked) views.setImageViewBitmap(R.id.widget_sep_weather, line.separator(text))
            views.setImageViewBitmap(
                R.id.widget_temp,
                line.temperature(temperature, text, weather?.let { context.getDrawable(iconFor(it, config.bold)) })
            )
            views.setContentDescription(
                R.id.widget_temp,
                weather?.let { "$temperature° · ${it.condition.displayName}" } ?: "Weather loading"
            )
        }

        if (withClicks) {
            WidgetPart.entries.forEach { part ->
                val app = WidgetApps.resolve(context, part, config.appFor(part)) ?: WidgetApps.alarmzy(context)
                val pending = PendingIntent.getActivity(
                    context,
                    part.ordinal,
                    WidgetApps.launchIntent(app.component),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                partViews(part).forEach { views.setOnClickPendingIntent(it, pending) }
            }
        }
        return views
    }

    /** The layout [build] inflates; the in-app preview re-inflates when it changes. */
    @LayoutRes
    fun layoutFor(config: ClockWidgetConfig): Int = when (config.layout) {
        WidgetLayout.HORIZONTAL -> R.layout.widget_clock
        WidgetLayout.VERTICAL -> R.layout.widget_clock_vertical
    }

    fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))

    /**
     * Re-renders every placed clock widget and arms the next minute tick (or cancels it when none
     * are placed). Does package lookups and drawing, so call it off the main thread.
     */
    fun updateAll(context: Context, config: ClockWidgetConfig, weather: WidgetWeather?) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) {
            ClockWidgetTicker.cancel(context)
            return
        }
        AppWidgetManager.getInstance(context).updateAppWidget(ids, build(context, config, weather))
        ClockWidgetTicker.scheduleNext(context)
    }

    private fun paint(context: Context, fontRes: Int, textPx: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
            textSize = textPx
            color = Color.WHITE
        }

    /**
     * The Tabler glyph for [weather], following the KWGT formula (sun/moon, cloud, cloud-rain,
     * cloud-bolt, cloud-snow, cloud-fog, wind) plus the finer conditions only OpenWeather reports
     * (mist, haze-like particulates, tornado). [bold] picks the stroke-3 variants.
     */
    @DrawableRes
    fun iconFor(weather: WidgetWeather, bold: Boolean = false): Int {
        val (regular, heavy) = when (weather.condition) {
            WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR ->
                if (weather.isDay) R.drawable.ic_weather_sun to R.drawable.ic_weather_sun_bold
                else R.drawable.ic_weather_moon to R.drawable.ic_weather_moon_bold
            WeatherCondition.PARTLY_CLOUDY, WeatherCondition.OVERCAST ->
                R.drawable.ic_weather_cloud to R.drawable.ic_weather_cloud_bold
            WeatherCondition.FOG -> R.drawable.ic_weather_cloud_fog to R.drawable.ic_weather_cloud_fog_bold
            WeatherCondition.MIST -> R.drawable.ic_weather_mist to R.drawable.ic_weather_mist_bold
            WeatherCondition.HAZE, WeatherCondition.SMOKE, WeatherCondition.DUST, WeatherCondition.SAND,
            WeatherCondition.ASH -> R.drawable.ic_weather_haze to R.drawable.ic_weather_haze_bold
            WeatherCondition.DRIZZLE, WeatherCondition.FREEZING_DRIZZLE, WeatherCondition.RAIN,
            WeatherCondition.FREEZING_RAIN, WeatherCondition.SHOWERS ->
                R.drawable.ic_weather_cloud_rain to R.drawable.ic_weather_cloud_rain_bold
            WeatherCondition.SLEET, WeatherCondition.RAIN_AND_SNOW, WeatherCondition.SNOW,
            WeatherCondition.SNOW_SHOWERS -> R.drawable.ic_weather_cloud_snow to R.drawable.ic_weather_cloud_snow_bold
            WeatherCondition.SNOW_GRAINS -> R.drawable.ic_weather_snowflake to R.drawable.ic_weather_snowflake_bold
            WeatherCondition.THUNDERSTORM, WeatherCondition.THUNDERSTORM_HAIL ->
                R.drawable.ic_weather_cloud_bolt to R.drawable.ic_weather_cloud_bolt_bold
            WeatherCondition.WINDY, WeatherCondition.SQUALLS -> R.drawable.ic_weather_wind to R.drawable.ic_weather_wind_bold
            WeatherCondition.TORNADO -> R.drawable.ic_weather_tornado to R.drawable.ic_weather_tornado_bold
        }
        return if (bold) heavy else regular
    }

    /**
     * Draws the parts of the line. Every bitmap shares one height and baseline, so the pieces line
     * up when the row centres them, and is trimmed to its ink horizontally, so the gaps between
     * parts are exactly the ones set here (the separator carries them). Characters are placed one
     * by one with explicit tracking rather than Paint.letterSpacing, whose half-spacing at the
     * ends would leak into those gaps. Bitmap density makes ImageViews show them at true size.
     */
    private class LineBox(paint: Paint, private val densityDpi: Int, private val strokeScale: Float) {
        private val em = paint.textSize
        private val tracking = TRACKING_EM * em
        private val iconBox = ICON_BOX_EM * em
        private val metrics = paint.fontMetrics
        // Tabler glyphs (24-unit box, stroke 2) put their ink bottom at 19 units: sit it on the baseline.
        private val iconTop = -iconBox * 19f / 24f
        private val top = minOf(metrics.ascent, iconTop)
        private val bottom = maxOf(metrics.descent, iconTop + iconBox)
        private val baseline = -top
        private val height = ceil(bottom - top).toInt().coerceAtLeast(1)
        private val bounds = Rect()
        private val capHeight = Rect().also { paint.getTextBounds("H", 0, 1, it) }.height().toFloat()

        fun text(text: String, paint: Paint): Bitmap {
            val inkLeft = inkLeft(text, paint)
            val width = advance(text, paint) - inkLeft - rightBearing(text, paint)
            return draw(width) { drawTracked(this, text, -inkLeft, paint) }
        }

        /** The tilted capsule divider, centred on the caps, with [SEPARATOR_GAP_EM] on each side. */
        fun separator(paint: Paint): Bitmap {
            val barWidth = DIVIDER_WIDTH_EM * em * strokeScale
            val barHeight = DIVIDER_HEIGHT_EM * em
            val angle = Math.toRadians(DIVIDER_ANGLE_DEG.toDouble())
            val inkWidth = (barHeight * sin(angle) + barWidth * cos(angle)).toFloat()
            val gap = SEPARATOR_GAP_EM * em
            return draw(gap * 2 + inkWidth) {
                val cx = gap + inkWidth / 2
                val cy = baseline - capHeight / 2
                withRotation(DIVIDER_ANGLE_DEG, cx, cy) {
                    drawRoundRect(
                        cx - barWidth / 2, cy - barHeight / 2, cx + barWidth / 2, cy + barHeight / 2,
                        barWidth / 2, barWidth / 2, paint
                    )
                }
            }
        }

        /** "20" + the small degree + the condition glyph (or nothing while loading). */
        fun temperature(value: String, paint: Paint, icon: Drawable?): Bitmap {
            val inkLeft = inkLeft(value, paint)
            val degreeX = advance(value, paint) - inkLeft + tracking
            paint.getTextBounds(DEGREE, 0, DEGREE.length, bounds)
            val degreeInkRight = degreeX + bounds.right
            // Tabler ink starts 1 unit into the box (2-unit stroke centred on x = 2).
            val iconLeft = degreeInkRight + ICON_GAP_EM * em - iconBox / 24f
            val width = if (icon != null) iconLeft + iconBox * 23f / 24f else degreeInkRight
            return draw(width) {
                drawTracked(this, value, -inkLeft, paint)
                drawText(DEGREE, degreeX, baseline + DEGREE_DROP_EM * em, paint)
                icon?.mutate()?.let {
                    it.setTint(paint.color)
                    val y = (baseline + iconTop).toInt()
                    it.setBounds(iconLeft.toInt(), y, (iconLeft + iconBox).toInt(), (y + iconBox).toInt())
                    it.draw(this)
                }
            }
        }

        private fun drawTracked(canvas: Canvas, text: String, startX: Float, paint: Paint) {
            var x = startX
            forEachChar(text) { ch ->
                canvas.drawText(ch, x, baseline, paint)
                x += paint.measureText(ch) + tracking
            }
        }

        /** Advance of [text] with tracking between (not after) characters. */
        private fun advance(text: String, paint: Paint): Float {
            var total = 0f
            var count = 0
            forEachChar(text) { total += paint.measureText(it); count++ }
            return total + tracking * (count - 1).coerceAtLeast(0)
        }

        private fun inkLeft(text: String, paint: Paint): Float {
            val first = text.substring(0, text.offsetByCodePoints(0, 1))
            paint.getTextBounds(first, 0, first.length, bounds)
            return bounds.left.toFloat()
        }

        private fun rightBearing(text: String, paint: Paint): Float {
            val last = text.substring(text.offsetByCodePoints(text.length, -1))
            paint.getTextBounds(last, 0, last.length, bounds)
            return paint.measureText(last) - bounds.right
        }

        private inline fun forEachChar(text: String, block: (String) -> Unit) {
            var i = 0
            while (i < text.length) {
                val next = text.offsetByCodePoints(i, 1)
                block(text.substring(i, next))
                i = next
            }
        }

        private inline fun draw(width: Float, block: Canvas.() -> Unit): Bitmap =
            createBitmap(ceil(width).toInt().coerceAtLeast(1), height).apply {
                density = densityDpi
                Canvas(this).block()
            }
    }

    private fun gravityOf(config: ClockWidgetConfig): Int {
        val horizontal = when (config.horizontalAlign) {
            WidgetHorizontalAlign.START -> Gravity.START
            WidgetHorizontalAlign.CENTER -> Gravity.CENTER_HORIZONTAL
            WidgetHorizontalAlign.END -> Gravity.END
        }
        val vertical = when (config.verticalAlign) {
            WidgetVerticalAlign.TOP -> Gravity.TOP
            WidgetVerticalAlign.CENTER -> Gravity.CENTER_VERTICAL
            WidgetVerticalAlign.BOTTOM -> Gravity.BOTTOM
        }
        return horizontal or vertical
    }
}
