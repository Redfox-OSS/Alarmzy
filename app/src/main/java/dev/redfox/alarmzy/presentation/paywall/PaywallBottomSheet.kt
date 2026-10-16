package dev.redfox.alarmzy.presentation.paywall

import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.redfox.alarmzy.R
import dev.redfox.obsidian.theme.ObShapes
import dev.redfox.obsidian.components.PillButton
import dev.redfox.obsidian.components.PrimaryButton
import dev.redfox.obsidian.effects.glow
import dev.redfox.obsidian.theme.ObsidianTheme
import dev.redfox.obsidian.motion.RevealScope
import dev.redfox.obsidian.motion.revealOnEnter
import dev.redfox.obsidian.motion.popIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Always open fully: the paywall is short enough that a half-height peek just clips it.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activity = LocalContext.current as? Activity
    val colors = ObsidianTheme.colors
    val type = ObsidianTheme.typography

    LaunchedEffect(uiState.isPro) {
        if (uiState.isPro) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.textSecondary.copy(alpha = 0.3f))
            )
        },
        modifier = modifier
    ) {
      // Fresh scope: the sheet's content cascades in every time it opens.
      RevealScope {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Small screens / large font sizes: scroll rather than cut off the buttons.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .popIn(from = 0.4f)
                    .glow(colors.accent.copy(alpha = 0.35f), ObShapes.CardLarge, radius = 28.dp)
                    .size(72.dp)
                    .clip(ObShapes.CardLarge)
                    .background(colors.accentContainer)
                    .border(1.dp, colors.accentBorder, ObShapes.CardLarge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_pro_verified),
                    contentDescription = null,
                    tint = colors.accentLight,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Unlock Alarmzy Pro",
                style = type.screenTitle.copy(fontSize = type.screenTitle.fontSize * 1.2f),
                color = colors.textPrimary,
                modifier = Modifier.revealOnEnter(1)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A one-time purchase, yours forever.",
                style = type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.revealOnEnter(2)
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(3)
                    .clip(ObShapes.Card)
                    .background(colors.cardInner)
                    .border(1.dp, colors.border, ObShapes.Card)
                    .padding(horizontal = 17.dp, vertical = 8.dp)
            ) {
                PerkRow("Unlimited groups", Modifier.revealOnEnter(4, offsetY = 10.dp))
                PerkRow("Unlimited alarms per group", Modifier.revealOnEnter(5, offsetY = 10.dp))
                PerkRow("Alarm series generation", Modifier.revealOnEnter(6, offsetY = 10.dp))
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = when {
                    uiState.isPurchasing -> "Connecting…"
                    uiState.priceText != null -> "Unlock Pro — ${uiState.priceText}"
                    else -> "Unlock Pro"
                },
                enabled = !uiState.isPurchasing,
                onClick = { activity?.let { viewModel.onIntent(PaywallIntent.Purchase(it)) } },
                modifier = Modifier.revealOnEnter(6)
            )

            uiState.errorMessage?.let {
                Text(
                    it,
                    color = colors.error,
                    style = type.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "Restore purchases",
                onClick = { viewModel.onIntent(PaywallIntent.Restore) },
                containerColor = colors.card,
                contentColor = colors.textSecondary,
                modifier = Modifier.revealOnEnter(7)
            )
        }
      }
    }
}

@Composable
private fun PerkRow(text: String, modifier: Modifier = Modifier) {
    val colors = ObsidianTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(colors.accentContainerStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.accentLight)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = ObsidianTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}
