package dev.redfox.alarmzy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/** Squircle radii from DESIGN.md. */
object AzShapes {
    val Card = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(24.dp)
    val Tile = RoundedCornerShape(16.dp)
    val Small = RoundedCornerShape(12.dp)
    val Pill = CircleShape
}

/** Screen horizontal gutter used by every page in the design. */
val ScreenGutter = 20.dp

/**
 * Base card. [onClick]/[onLongClick] make it tactile via [pressable].
 */
@Composable
fun AzCard(
    modifier: Modifier = Modifier,
    shape: Shape = AzShapes.Card,
    color: Color = AlarmzyTheme.colors.card,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.pressable(onLongClick = onLongClick, onClick = onClick)
                else Modifier
            )
            .clip(shape)
            .background(color)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Rounded icon container (settings rows, group icons, form rows). */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = AzShapes.Tile,
    background: Color = AlarmzyTheme.colors.tile,
    tint: Color = AlarmzyTheme.colors.textSecondary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = tint)
    }
}

/** Uppercase eyebrow label above a section ("APPEARANCE", "REPEAT"). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AlarmzyTheme.colors.textSecondary.copy(alpha = 0.8f),
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text.uppercase(), style = AlarmzyTheme.typography.sectionLabel, color = color)
        trailing?.invoke(this)
    }
}

@Composable
fun AzDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(AlarmzyTheme.colors.divider))
}

/**
 * Standard row used in form and settings cards: icon tile, title/subtitle, trailing slot.
 */
@Composable
fun AzListRow(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    subtitle: String? = null,
    iconTileSize: Dp = 40.dp,
    iconTileShape: Shape = AzShapes.Tile,
    iconTint: Color = AlarmzyTheme.colors.textSecondary,
    titleStyleLarge: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = AlarmzyTheme.colors
    val type = AlarmzyTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(icon = icon, size = iconTileSize, shape = iconTileShape, tint = iconTint)
            Spacer(Modifier.size(if (titleStyleLarge) 12.dp else 16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (titleStyleLarge) type.bodyLarge else type.labelStrong,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = type.body,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

/** Empty list placeholder in the design language. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = AlarmzyTheme.colors
    // The badge floats and its halo breathes, so an empty screen still feels alive.
    val ambient = rememberInfiniteTransition(label = "emptyAmbient")
    val float by ambient.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    val breathe by ambient.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .revealOnEnter(0)
                .graphicsLayer { translationY = float * density }
                .glow(colors.accentGlow.copy(alpha = breathe), CircleShape, radius = 32.dp)
                .size(88.dp)
                .clip(CircleShape)
                .background(colors.accentContainer)
                .border(1.dp, colors.accentBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = colors.accentLight,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = AlarmzyTheme.typography.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.revealOnEnter(1)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = AlarmzyTheme.typography.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.revealOnEnter(2)
        )
    }
}
