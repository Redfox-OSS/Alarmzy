package dev.redfox.alarmzy.presentation.paywall

data class PaywallUiState(
    val priceText: String? = null,
    val isPurchasing: Boolean = false,
    val isPro: Boolean = false,
    val errorMessage: String? = null
)
