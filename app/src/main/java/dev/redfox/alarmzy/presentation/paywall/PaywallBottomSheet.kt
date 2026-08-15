package dev.redfox.alarmzy.presentation.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(uiState.isPro) {
        if (uiState.isPro) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("Unlock Alarmzy Pro", style = MaterialTheme.typography.headlineSmall)
            Text(
                "A one-time purchase, yours forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            PerkRow("Unlimited groups")
            PerkRow("Unlimited alarms per group")
            PerkRow("Alarm series generation")

            Button(
                onClick = { activity?.let { viewModel.onIntent(PaywallIntent.Purchase(it)) } },
                enabled = !uiState.isPurchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                if (uiState.isPurchasing) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(uiState.priceText?.let { "Unlock Pro — $it" } ?: "Unlock Pro")
            }

            uiState.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            TextButton(
                onClick = { viewModel.onIntent(PaywallIntent.Restore) },
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text("Restore purchases")
            }
        }
    }
}

@Composable
private fun PerkRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
