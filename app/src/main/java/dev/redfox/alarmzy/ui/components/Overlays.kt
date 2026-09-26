package dev.redfox.alarmzy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.ui.theme.AlarmzyTheme

/** Dropdown menu styled as a floating squircle card. */
@Composable
fun AzDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = AzShapes.Card,
        containerColor = AlarmzyTheme.colors.tile,
        border = BorderStroke(1.dp, AlarmzyTheme.colors.borderStrong),
        shadowElevation = 12.dp,
        content = content
    )
}

@Composable
fun AzMenuItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    color: Color = if (selected) AlarmzyTheme.colors.accentLight else AlarmzyTheme.colors.textPrimary,
    leading: (@Composable () -> Unit)? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    DropdownMenuItem(
        text = { Text(text, style = AlarmzyTheme.typography.body, color = color) },
        onClick = onClick,
        leadingIcon = leading,
        trailingIcon = trailingIcon?.let {
            { Icon(painterResource(it), contentDescription = null, tint = AlarmzyTheme.colors.accentLight) }
        }
    )
}

/** Confirmation dialog on the card surface. */
@Composable
fun AzAlertDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    val colors = AlarmzyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        shape = AzShapes.CardLarge,
        title = { Text(title, style = AlarmzyTheme.typography.title, color = colors.textPrimary) },
        text = { Text(text, style = AlarmzyTheme.typography.body, color = colors.textSecondary) },
        confirmButton = {
            Box(Modifier.padding(start = 8.dp)) {
                PillButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    containerColor = if (destructive) colors.accent else colors.accentContainerStrong,
                    contentColor = if (destructive) colors.onAccent else colors.accentLight
                )
            }
        },
        dismissButton = { PillButton(text = dismissLabel, onClick = onDismiss) }
    )
}
