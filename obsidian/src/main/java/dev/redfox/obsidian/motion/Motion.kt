package dev.redfox.obsidian.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.AlignmentLine

/**
 * Motion vocabulary for the whole app. Everything interactive uses physics (springs) so it can be
 * interrupted mid-flight; only choreographed entrances use time-based easing.
 */
object ObMotion {
    /** Playful overshoot: nav indicator, pops, check marks. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = 380f)
    /** Quick and settled: toggles, pills, colour changes. */
    fun <T> snappy() = spring<T>(dampingRatio = 0.82f, stiffness = 700f)
    /** Soft, no overshoot: entrances and layout. */
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = 240f)

    /** Material "emphasized decelerate" — fast start, long soft landing. */
    val EaseOutEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Delay between consecutive items of a staggered entrance. */
    const val StaggerMillis = 38L
    /** Items past this index enter together with it, so long lists don't trickle in. */
    const val MaxStaggerIndex = 9
}

// ---------------------------------------------------------------------------------------------
// Staggered entrance
// ---------------------------------------------------------------------------------------------

/**
 * Remembers when a screen first appeared. Items only play their entrance while the screen is
 * fresh — scrolling a lazy list later must not replay it for recycled items.
 */
class RevealGate internal constructor(private val startNanos: Long) {
    internal fun isFresh(): Boolean = System.nanoTime() - startNanos < WindowNanos

    private companion object {
        const val WindowNanos = 650_000_000L
    }
}

val LocalRevealGate = staticCompositionLocalOf<RevealGate?> { null }

/**
 * Scope for a destination: its [revealOnEnter] items play once when it is shown. With
 * [enabled] false (e.g. returning to a screen via back) everything appears in place.
 */
@Composable
fun RevealScope(enabled: Boolean = true, content: @Composable () -> Unit) {
    val gate = remember { RevealGate(if (enabled) System.nanoTime() else Long.MIN_VALUE / 2) }
    CompositionLocalProvider(LocalRevealGate provides gate, content = content)
}

/**
 * Staggered entrance: the element rises [offsetY], scales up from 96% and fades in, [index]
 * steps after the screen appears. Cheap — it only touches the draw layer.
 */
@Composable
fun Modifier.revealOnEnter(index: Int = 0, offsetY: Dp = 22.dp): Modifier {
    val gate = LocalRevealGate.current
    val animate = remember { gate?.isFresh() ?: true }
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(min(index, ObMotion.MaxStaggerIndex) * ObMotion.StaggerMillis)
            progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 210f))
        }
    }
    val distance = with(LocalDensity.current) { offsetY.toPx() }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * distance
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
        // Keep glows (drawn outside the bounds) unclipped while fading.
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** One-shot springy scale-in for dialogs and sheets. */
@Composable
fun Modifier.popIn(from: Float = 0.88f): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f)) }
    return graphicsLayer {
        val p = progress.value
        val s = from + (1f - from) * p
        scaleX = s
        scaleY = s
        alpha = (p * 1.6f).coerceIn(0f, 1f)
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/**
 * Column whose direct children enter one after another (rise + fade), like iOS sheets settling.
 * A drop-in for the scrollable `Column`s of form/settings screens; one clock drives every child
 * so the stagger costs a single animation. Replays only when the screen is first shown.
 */
@Composable
fun RevealColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    offsetY: Dp = 26.dp,
    content: @Composable () -> Unit,
) {
    val gate = LocalRevealGate.current
    val animate = remember { gate?.isFresh() ?: true }
    val stepMs = ObMotion.StaggerMillis * 1.4f
    val itemMs = 520f
    val totalMs = stepMs * ObMotion.MaxStaggerIndex + itemMs
    val clock = remember { Animatable(if (animate) 0f else totalMs) }
    LaunchedEffect(Unit) {
        if (clock.value < totalMs) clock.animateTo(totalMs, tween(totalMs.toInt(), easing = LinearEasing))
    }
    val distance = with(LocalDensity.current) { offsetY.toPx() }

    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val sizes = IntArray(placeables.size) { placeables[it].height }
        val positions = IntArray(placeables.size)
        val contentHeight = with(verticalArrangement) {
            val spacing = spacing.roundToPx()
            sizes.sum() + spacing * max(0, sizes.size - 1)
        }
        val height = contentHeight.coerceIn(constraints.minHeight, max(constraints.minHeight, if (constraints.hasBoundedHeight) constraints.maxHeight else contentHeight))
        with(verticalArrangement) { arrange(height, sizes, positions) }
        val width = (placeables.maxOfOrNull { it.width } ?: 0)
            .coerceIn(constraints.minWidth, if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity)

        layout(width, height) {
            placeables.forEachIndexed { i, placeable ->
                val x = horizontalAlignment.align(placeable.width, width, layoutDirection)
                placeable.placeWithLayer(x, positions[i]) {
                    val start = min(i, ObMotion.MaxStaggerIndex) * stepMs
                    val linear = ((clock.value - start) / itemMs).coerceIn(0f, 1f)
                    val p = ObMotion.EaseOutEmphasized.transform(linear)
                    alpha = min(1f, linear * 2.2f)
                    translationY = (1f - p) * distance
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rolling digits
// ---------------------------------------------------------------------------------------------

/**
 * Text whose digits roll like an odometer when they change: up when the number grows, down when
 * it shrinks. Non-digit characters (":", " ", letters) stay still. Use for readouts and counters
 * that change at most a few times a second.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    /** Force a direction (a countdown always rolls down, even when 0 wraps to 9); null compares values. */
    rollUp: Boolean? = null,
) {
    val latestRollUp by rememberUpdatedState(rollUp)
    // AnimatedContent doesn't expose text baselines, so parents using alignByBaseline would
    // misplace the digits. A never-placed reference Text supplies the baseline instead.
    Layout(
        modifier = modifier,
        content = {
            RollingRow(text, style, color) { latestRollUp }
            Text(text, style = style, color = Color.Transparent, maxLines = 1)
        }
    ) { measurables, constraints ->
        val row = measurables[0].measure(constraints)
        val reference = measurables[1].measure(constraints)
        val baseline = reference[FirstBaseline]
        val lines = if (baseline == AlignmentLine.Unspecified) emptyMap()
        else mapOf<AlignmentLine, Int>(FirstBaseline to baseline, LastBaseline to baseline)
        layout(row.width, row.height, lines) { row.place(0, 0) }
    }
}

@Composable
private fun RollingRow(text: String, style: TextStyle, color: Color, rollUp: () -> Boolean?) {
    Row {
        // Digits roll individually; runs of other characters stay one Text so kerning holds.
        // Keyed by distance from the end so "9" -> "10" keeps the ones column in place.
        var i = 0
        while (i < text.length) {
            val fromEnd = text.length - i
            if (text[i].isDigit()) {
                val char = text[i]
                androidx.compose.runtime.key(fromEnd) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = { rollTransform(up = rollUp() ?: (targetState > initialState)) },
                        label = "rollingDigit"
                    ) { c -> Text(c.toString(), style = style, color = color) }
                }
                i++
            } else {
                var end = i
                while (end < text.length && !text[end].isDigit()) end++
                val run = text.substring(i, end)
                androidx.compose.runtime.key("run$fromEnd") { Text(run, style = style, color = color) }
                i = end
            }
        }
    }
}

private fun rollTransform(up: Boolean): ContentTransform {
    val spec: FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset> =
        spring(dampingRatio = 0.8f, stiffness = 520f)
    val enter = slideInVertically(spec) { h -> if (up) h else -h } + fadeIn(tween(160))
    val exit = slideOutVertically(spec) { h -> if (up) -h else h } + fadeOut(tween(120))
    return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = true))
}

/**
 * Springy "pop" each time [selected] turns true (not on first composition): the element swells
 * slightly and settles with a bounce. Pair with a colour change for chips and pills.
 */
@Composable
fun Modifier.popOnSelect(selected: Boolean, amount: Float = 0.08f): Modifier {
    val scale = remember { Animatable(1f) }
    val initial = remember { booleanArrayOf(true) }
    LaunchedEffect(selected) {
        if (initial[0]) {
            initial[0] = false
            return@LaunchedEffect
        }
        if (selected) {
            scale.animateTo(1f + amount, spring(dampingRatio = 1f, stiffness = 1400f))
            scale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 380f))
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Show/hide inline row content (selection checkboxes, trailing switches) by growing its width
 * while the content pops in, so siblings glide aside instead of jumping.
 */
@Composable
fun HorizontalReveal(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.expandHorizontally(
            spring(dampingRatio = 0.85f, stiffness = 500f, visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold),
            expandFrom = Alignment.Start
        ) + fadeIn(tween(160, delayMillis = 60)) +
            androidx.compose.animation.scaleIn(ObMotion.bouncy(), initialScale = 0.6f),
        exit = androidx.compose.animation.shrinkHorizontally(
            spring(dampingRatio = 1f, stiffness = 600f, visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold),
            shrinkTowards = Alignment.Start
        ) + fadeOut(tween(100)) + androidx.compose.animation.scaleOut(targetScale = 0.6f)
    ) { content() }
}

/** Expand/collapse for disclosure sections: springy height, content fades in slightly late. */
val ExpandEnter = androidx.compose.animation.expandVertically(
    spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold)
) + fadeIn(tween(220, delayMillis = 60))
val CollapseExit = androidx.compose.animation.shrinkVertically(
    spring(dampingRatio = 1f, stiffness = 500f, visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold)
) + fadeOut(tween(120))

/** Header action sets swapping (normal <-> selection mode): a quick scale-and-fade exchange. */
val HeaderActionsSwap: ContentTransform
    get() = ContentTransform(
        targetContentEnter = fadeIn(tween(200, delayMillis = 60)) +
            androidx.compose.animation.scaleIn(ObMotion.bouncy(), initialScale = 0.7f),
        initialContentExit = fadeOut(tween(100)) + androidx.compose.animation.scaleOut(targetScale = 0.7f),
        sizeTransform = SizeTransform(clip = false)
    )

/** Whole-text replacement that rolls up (e.g. the next-alarm time changing). */
val RollSwap: ContentTransform
    get() = rollTransform(up = true)
