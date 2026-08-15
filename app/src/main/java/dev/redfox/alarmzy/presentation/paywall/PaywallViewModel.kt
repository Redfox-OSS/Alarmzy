package dev.redfox.alarmzy.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.redfox.alarmzy.domain.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                billingRepository.isProUnlocked(),
                billingRepository.proProductPrice()
            ) { isPro, price -> isPro to price }
                .collect { (isPro, price) ->
                    _uiState.value = _uiState.value.copy(isPro = isPro, priceText = price)
                }
        }
    }

    fun onIntent(intent: PaywallIntent) {
        when (intent) {
            is PaywallIntent.Purchase -> {
                _uiState.value = _uiState.value.copy(isPurchasing = true, errorMessage = null)
                viewModelScope.launch {
                    runCatching { billingRepository.launchPurchaseFlow(intent.activity) }
                        .onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                }
            }
            is PaywallIntent.Restore -> viewModelScope.launch {
                runCatching { billingRepository.restorePurchases() }
                    .onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            }
            is PaywallIntent.DismissError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
}
