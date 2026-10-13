package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScanResultEntity
import com.example.model.SetupType
import com.example.ui.components.CandidateCard
import com.example.ui.components.DisclaimerBanner
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MarketViewModel

@Composable
fun ScannerScreen(
    viewModel: MarketViewModel,
    onNavigateToCandidate: (ScanResultEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scanResults by viewModel.filteredScanResults.collectAsStateWithLifecycle()
    val allScanResults by viewModel.allScanResults.collectAsStateWithLifecycle()
    val selectedSetup by viewModel.selectedSetupFilter.collectAsStateWithLifecycle()
    val minScore by viewModel.minScoreFilter.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("scanner_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        DisclaimerBanner()
        Spacer(modifier = Modifier.height(10.dp))

        // Quick Exchange Collection & Re-Scan Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.collectDataFromExchange() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("scanner_collect_exchange_btn")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syncing...")
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Collect Exchange Data", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedButton(
                onClick = { viewModel.runScan() },
                enabled = !isScanning,
                modifier = Modifier
                    .weight(0.7f)
                    .testTag("scanner_rescan_btn")
            ) {
                Text("Re-Scan", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Setup filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSetup == null,
                onClick = { viewModel.selectedSetupFilter.value = null },
                label = { Text("All Strategies (${allScanResults.size})") },
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = selectedSetup == SetupType.BREAKOUT,
                onClick = { viewModel.selectedSetupFilter.value = SetupType.BREAKOUT },
                label = { Text("20D Breakouts") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentGold.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_breakout")
            )
            FilterChip(
                selected = selectedSetup == SetupType.PULLBACK,
                onClick = { viewModel.selectedSetupFilter.value = SetupType.PULLBACK },
                label = { Text("EMA Pullbacks") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentCyan.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_pullback")
            )
            FilterChip(
                selected = selectedSetup == SetupType.MOMENTUM,
                onClick = { viewModel.selectedSetupFilter.value = SetupType.MOMENTUM },
                label = { Text("Momentum") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BullGreen.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("filter_momentum")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Minimum score slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min Score Filter: $minScore/100",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${scanResults.size} showing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = minScore.toFloat(),
            onValueChange = { viewModel.minScoreFilter.value = it.toInt() },
            valueRange = 0f..85f,
            steps = 16,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("min_score_slider")
        )

        // Candidates LazyColumn
        if (scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No candidates match min score of $minScore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.minScoreFilter.value = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Show All Scanned Stocks (Min Score = 0)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.collectDataFromExchange() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Collect Latest Exchange Data Now")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scanResults) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onClick = {
                            viewModel.selectCandidate(candidate)
                            onNavigateToCandidate(candidate)
                        }
                    )
                }
            }
        }
    }
}
