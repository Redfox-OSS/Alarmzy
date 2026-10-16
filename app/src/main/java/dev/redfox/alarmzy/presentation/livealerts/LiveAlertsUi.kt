package dev.redfox.alarmzy.presentation.livealerts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.clock.LiveAlertPermissions
import dev.redfox.alarmzy.clock.LiveAlertStatus
import dev.redfox.obsidian.components.ObDivider
import dev.redfox.obsidian.components.ObListRow
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.motion.popIn
import dev.redfox.obsidian.effects.pressable
import dev.redfox.obsidian.theme.ObsidianTheme
import androidx.compose.material.icons.rounded.WarningAmber

/** Live permission status plus the actions to fix each piece. Shared by the home prompt and Settings. */
class LiveAlertsController internal constructor(
    private val statusState: MutableState<LiveAlertStatus>,
    private val onRequestNotifications: () -> Unit,
    private val context: Context,
) {
    val status: LiveAlertStatus get() = statusState.value

    fun fixNotifications() {
        if (!status.notificationsAllowed && Build.VERSION.SDK_INT >= 33) onRequestNotifications()
        else LiveAlertPermissions.openNotificationSettings(context) // allowed but channel muted
    }

    fun fixLiveUpdates() = LiveAlertPermissions.openLiveUpdatesSettings(context)
    fun fixFullScreen() = LiveAlertPermissions.openFullScreenSettings(context)
    fun openBbkSettings() = LiveAlertPermissions.openNotificationSettings(context)
}

/**
 * Reads [LiveAlertStatus] and re-reads it on every ON_RESUME: every step except the runtime
 * notification dialog happens in a system Settings screen that returns no result.
 * [onPermissionsChanged] fires whenever it changes, so running timers get (re-)promoted.
 */
@Composable
fun rememberLiveAlertsController(onPermissionsChanged: () -> Unit): LiveAlertsController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val onChanged by rememberUpdatedState(onPermissionsChanged)

    val statusState = remember { mutableStateOf(LiveAlertPermissions.status(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Permanently denied (no rationale on a denial): the dialog won't show again, go to Settings.
        if (!granted && activity != null && Build.VERSION.SDK_INT >= 33 &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) LiveAlertPermissions.openNotificationSettings(context)
        statusState.value = LiveAlertPermissions.status(context)
        onChanged()
    }
    val controller = remember {
        LiveAlertsController(
            statusState = statusState,
            onRequestNotifications = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            context = context,
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val fresh = LiveAlertPermissions.status(context)
                if (fresh != statusState.value) {
                    statusState.value = fresh
                    onChanged()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val BbkCopy =
    "On this phone, also turn on \"Show Live Updates on Live Alerts\" in notification settings so timers appear as a Live Alert."

// ---------------------------------------------------------------------------------------------
// Home prompt (Alarms tab) — asked once; Settings → Live Alerts is the way back.
// ---------------------------------------------------------------------------------------------

@Composable
fun LiveAlertsHomePrompt(
    asked: Boolean,
    onMarkAsked: () -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    val controller = rememberLiveAlertsController(onPermissionsChanged)
    // Decide once per composition entry, so the dialog doesn't vanish mid-flow as steps get ticked.
    var visible by remember { mutableStateOf(!asked && controller.status.worthPrompting) }
    if (!visible) return
    val status = controller.status
    val dismiss = {
        visible = false
        onMarkAsked()
    }
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography

    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .popIn()
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(ObShapes.CardLarge)
                .background(colors.card)
                .border(1.dp, colors.border, ObShapes.CardLarge)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .glow(colors.accent.copy(alpha = 0.3f), ObShapes.Tile, radius = 20.dp)
                    .size(56.dp)
                    .clip(ObShapes.Tile)
                    .background(colors.accentContainer)
                    .border(1.dp, colors.accentBorder, ObShapes.Tile),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_nav_timer), contentDescription = null, tint = colors.accentLight)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "See your timers without opening the app",
                style = type.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Running timers and the stopwatch can live on your lock screen and status bar. A few settings are needed:",
                style = type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (Build.VERSION.SDK_INT >= 34) {
                    PromptStep(
                        icon = painterResource(R.drawable.ic_alarm),
                        title = "Allow full-screen alarms",
                        subtitle = "So a ringing alarm takes over the lock screen",
                        done = status.fullScreenAllowed,
                        onClick = controller::fixFullScreen
                    )
                }
                if (Build.VERSION.SDK_INT >= 33 || !status.channelEnabled) {
                    PromptStep(
                        icon = painterResource(R.drawable.ic_bell),
                        title = "Allow notifications",
                        done = status.notificationsAllowed && status.channelEnabled,
                        onClick = controller::fixNotifications
                    )
                }
                status.liveUpdatesAllowed?.let { allowed ->
                    PromptStep(
                        icon = painterResource(R.drawable.ic_bell_ring),
                        title = "Allow Live Updates",
                        done = allowed,
                        onClick = controller::fixLiveUpdates
                    )
                }
                if (status.showBbkStep) {
                    PromptStep(
                        icon = rememberVectorPainter(Icons.Rounded.PhoneAndroid),
                        title = "Turn on \"Show Live Updates on Live Alerts\"",
                        subtitle = "In notification settings — needed on this phone for the Live Alert pill",
                        done = null,
                        onClick = controller::openBbkSettings
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            PillButton(
                text = if (status.somethingMissing) "Not now" else "Done",
                onClick = dismiss,
                containerColor = if (status.somethingMissing) colors.tile else colors.accentContainerStrong,
                contentColor = if (status.somethingMissing) colors.textPrimary else colors.accentLight
            )
        }
    }
}

/**
 * One checklist step. [done] = null means the state can't be read (BBK toggle): it renders a
 * "Check" action and never a tick.
 */
@Composable
private fun PromptStep(
    icon: Painter,
    title: String,
    done: Boolean?,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val colors = ObsidianTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = done != true, pressedScale = 0.98f, onClick = onClick)
            .clip(ObShapes.Card)
            .background(colors.cardInner)
            .border(1.dp, colors.border, ObShapes.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(colors.tile),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = ObsidianTheme.typography.labelStrong, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = ObsidianTheme.typography.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
            }
        }
        Spacer(Modifier.width(10.dp))
        when (done) {
            true -> Box(
                Modifier.size(26.dp).clip(CircleShape).background(colors.accentContainerStrong),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Done", tint = colors.accentLight, modifier = Modifier.size(16.dp))
            }
            false -> StepPill("Allow", filled = true)
            null -> StepPill("Check", filled = false)
        }
    }
}

@Composable
private fun StepPill(text: String, filled: Boolean) {
    val colors = ObsidianTheme.colors
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (filled) colors.accent else colors.accentContainer)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = ObsidianTheme.typography.labelStrong,
            color = if (filled) colors.onAccent else colors.accentLight
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Settings → "Live Alerts" section content (rows go inside the Settings card)
// ---------------------------------------------------------------------------------------------

private enum class RowStatus { Allowed, Off, Check }

@Composable
fun LiveAlertsSettingsRows(onPermissionsChanged: () -> Unit) {
    val controller = rememberLiveAlertsController(onPermissionsChanged)
    val status = controller.status
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (Build.VERSION.SDK_INT >= 34) {
            ObListRow(
                title = "Full-screen alarms",
                subtitle = "Ringing alarm over the lock screen",
                icon = R.drawable.ic_alarm,
                onClick = controller::fixFullScreen,
                trailing = { StatusLabel(if (status.fullScreenAllowed) RowStatus.Allowed else RowStatus.Off) }
            )
            ObDivider()
        }
        ObListRow(
            title = "Notifications",
            subtitle = "Timer and stopwatch notifications",
            icon = R.drawable.ic_bell,
            onClick = controller::fixNotifications,
            trailing = { StatusLabel(if (status.notificationsAllowed && status.channelEnabled) RowStatus.Allowed else RowStatus.Off) }
        )
        status.liveUpdatesAllowed?.let { allowed ->
            ObDivider()
            ObListRow(
                title = "Live Updates",
                subtitle = "Lock screen, status bar and Now Bar",
                icon = R.drawable.ic_bell_ring,
                onClick = controller::fixLiveUpdates,
                trailing = { StatusLabel(if (allowed) RowStatus.Allowed else RowStatus.Off) }
            )
        }
        if (status.showBbkStep) {
            ObDivider()
            ObListRow(
                title = "Show on Live Alerts",
                subtitle = "\"Show Live Updates on Live Alerts\" toggle",
                icon = R.drawable.ic_nav_timer,
                onClick = controller::openBbkSettings,
                trailing = { StatusLabel(RowStatus.Check) }
            )
        }
    }
}

/** Footer under the Live Alerts card. */
@Composable
fun LiveAlertsSettingsFooter() {
    Text(
        "Running timers and the stopwatch show on your lock screen and status bar." +
            if (LiveAlertPermissions.isBbkDevice()) " $BbkCopy" else "",
        style = ObsidianTheme.typography.label.copy(fontWeight = FontWeight.Normal),
        color = ObsidianTheme.colors.textTertiary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun StatusLabel(status: RowStatus) {
    val colors = ObsidianTheme.colors
    val (text, color) = when (status) {
        RowStatus.Allowed -> "Allowed" to colors.textSecondary
        RowStatus.Off -> "Off" to colors.warning // warning, not grey, so it stands out
        RowStatus.Check -> "Check" to colors.accentLight // unknowable — never "Enabled"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = ObsidianTheme.typography.labelStrong, color = color)
        Spacer(Modifier.width(6.dp))
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = if (status == RowStatus.Off) colors.warning else colors.textSecondary
        )
    }
}

/**
 * Persistent Alarms-tab warning while full-screen alarms are blocked (Android 14+). Without that
 * access a ringing alarm is only a banner — easy to miss or lose in the morning — so unlike the
 * one-time prompt this stays until it's fixed. Re-checks whenever the app resumes.
 */
@Composable
fun FullScreenAlarmWarning(modifier: Modifier = Modifier) {
    val controller = rememberLiveAlertsController(onPermissionsChanged = {})
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = controller::fixFullScreen)
            .clip(ObShapes.Card)
            .background(colors.warning.copy(alpha = 0.12f))
            .border(1.dp, colors.warning.copy(alpha = 0.35f), ObShapes.Card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            rememberVectorPainter(Icons.Rounded.WarningAmber),
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Alarms won't open full screen", style = type.labelStrong, color = colors.textPrimary)
            Text(
                "Allow full-screen notifications so ringing alarms appear over the lock screen.",
                style = type.label,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Text("Fix", style = type.labelStrong, color = colors.warning)
    }
}

/** Whether full-screen alarms are allowed; re-read every time the app resumes. */
@Composable
fun rememberFullScreenAlarmsAllowed(): Boolean =
    rememberLiveAlertsController(onPermissionsChanged = {}).status.fullScreenAllowed
