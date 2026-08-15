package dev.redfox.alarmzy.presentation.paywall

import android.app.Activity

sealed interface PaywallIntent {
    data class Purchase(val activity: Activity) : PaywallIntent
    data object Restore : PaywallIntent
    data object DismissError : PaywallIntent
}
