package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.SetupBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.viewmodel.MarketViewModel

@Composable
fun JournalScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val performanceRecords by viewModel.performanceRecords.collectAsStateWithLifecycle()

    val totalRecords = performanceRecords.size
    val closedRecords = performanceRecords.filter { it.outcomeStatus != "TRACKING" }
    val targetHits = closedRecords.count { it.outcomeStatus == "PROFIT_TARGET_HIT" }
    val winRate = if (closedRecords.isNotEmpty()) (targetHits.toDouble() / closedRecords.size) * 100.0 else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("journal_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        DisclaimerBanner()
        Spacer(modifier = Modifier.height(12.dp))

        // Performance Statistics Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Forward Session Performance Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tracks candidates objectively across 1, 3, 5, 10, 20, 60, and 120 forward trading sessions, including losing trades, to prevent survivorship bias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("Tracked Setups", "$totalRecords", AccentCyan)
                    StatBox("Closed Trades", "${closedRecords.size}", MaterialTheme.colorScheme.onSurface)
                    StatBox("Target 1 Hits", "$targetHits", BullGreen)
                    StatBox("Win Rate", "${String.format("%.1f", winRate)}%", AccentGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (performanceRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracked candidates yet. Run a scan from the Dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(performanceRecords) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perf_card_${item.symbol}_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SetupBadge(item.setupType)
                                }

                                val (statusColor, statusLabel) = when (item.outcomeStatus) {
                                    "PROFIT_TARGET_HIT" -> Pair(BullGreen, "Target Hit")
                                    "STOP_LOSS_HIT" -> Pair(BearRed, "Stop Hit")
                                    "TIMEOUT" -> Pair(AccentGold, "Time Exit")
                                    else -> Pair(AccentCyan, "Tracking")
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Entry Date: ${item.entryDate} • Entry: ₹${String.format("%.2f", item.entryPrice)} • SL: ₹${String.format("%.2f", item.stopLoss)} • T1: ₹${String.format("%.2f", item.target1)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Forward Trading Session PnL Badges (T+1, T+3, T+5, T+10, T+20, T+60, T+120)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SessionPnlBadge("T+1", item.pnl1d)
                                SessionPnlBadge("T+3", item.pnl3d)
                                SessionPnlBadge("T+5", item.pnl5d)
                                SessionPnlBadge("T+10", item.pnl10d)
                                SessionPnlBadge("T+20", item.pnl20d)
                                SessionPnlBadge("T+60", item.pnl60d)
                                SessionPnlBadge("T+120", item.pnl120d)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Excursion extremes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Max Fav Excursion (MFE): ${item.maxFavorableExcursion?.let { String.format("%+.2f%%", it) } ?: "Pending"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BullGreen
                                )
                                Text(
                                    text = "Max Adv Excursion (MAE): ${item.maxAdverseExcursion?.let { String.format("%+.2f%%", it) } ?: "Pending"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BearRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SessionPnlBadge(label: String, pnl: Double?) {
    val (text, color) = when {
        pnl == null -> Pair("-", MaterialTheme.colorScheme.onSurfaceVariant)
        pnl > 0 -> Pair(String.format("%+.1f%%", pnl), BullGreen)
        pnl < 0 -> Pair(String.format("%+.1f%%", pnl), BearRed)
        else -> Pair("0.0%", MaterialTheme.colorScheme.onSurface)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
