package dev.redfox.alarmzy.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    fun isProUnlocked(): Flow<Boolean>
    fun proProductPrice(): Flow<String?>
    suspend fun launchPurchaseFlow(activity: Activity)
    suspend fun restorePurchases()
}
