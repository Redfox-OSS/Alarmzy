package dev.redfox.alarmzy.presentation.update

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play in-app updates.
 *
 * - **Immediate** (blocking, full-screen Play UI) when the release's in-app update priority is
 *   [ImmediatePriority] or higher — for critical fixes. Priority can't be set in the Play Console
 *   UI; it's set per release through the Play Developer API (e.g. fastlane `in_app_update_priority`).
 * - **Flexible** (background download) otherwise; once downloaded, [readyToInstall] turns true and
 *   the app shows an "Update ready · Restart" pill that calls [completeUpdate].
 *
 * Only works for installs that came from Play (internal testing counts). Sideloaded/debug builds
 * get an API error, which is ignored.
 */
class InAppUpdater(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    private val manager = AppUpdateManagerFactory.create(activity)
    private val prefs = activity.getSharedPreferences("in_app_updates", android.content.Context.MODE_PRIVATE)

    /** A flexible update has finished downloading and needs a restart to install. */
    var readyToInstall by mutableStateOf(false)
        private set

    // Must be registered before the activity is STARTED: construct this in onCreate.
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Declined/cancelled: an immediate update is re-offered on the next resume. */ }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) readyToInstall = true
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        manager.registerListener(installListener)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            when {
                info.updatePriority() >= ImmediatePriority && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    start(info, AppUpdateType.IMMEDIATE)
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && flexiblePromptDue() -> {
                    prefs.edit().putLong(KeyLastFlexiblePrompt, System.currentTimeMillis()).apply()
                    start(info, AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            // Downloaded while the app was in the background.
            if (info.installStatus() == InstallStatus.DOWNLOADED) readyToInstall = true
            // An immediate update the user left mid-way must be resumed, not skipped.
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                start(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
    }

    /** Restarts the app into the downloaded update. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }
    }

    /** Don't nag: offer an optional update at most once a day. */
    private fun flexiblePromptDue(): Boolean =
        System.currentTimeMillis() - prefs.getLong(KeyLastFlexiblePrompt, 0L) > FlexiblePromptIntervalMillis

    private companion object {
        const val ImmediatePriority = 4
        const val KeyLastFlexiblePrompt = "last_flexible_prompt"
        const val FlexiblePromptIntervalMillis = 24L * 60 * 60 * 1000
    }
}
