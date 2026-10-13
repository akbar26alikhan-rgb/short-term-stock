package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SetupType
import com.example.ui.components.CandlestickChart
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.ScoreBadge
import com.example.ui.components.SetupBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit,
    onNavigateToBacktest: (SetupType) -> Unit,
    modifier: Modifier = Modifier
) {
    val candidate by viewModel.selectedCandidate.collectAsStateWithLifecycle()
    val candles by viewModel.stockCandles.collectAsStateWithLifecycle()
    val aiExplanation by viewModel.aiExplanation.collectAsStateWithLifecycle()
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsStateWithLifecycle()
    val positionResult by viewModel.positionSizeResult.collectAsStateWithLifecycle()

    val capitalStr by viewModel.userCapital.collectAsStateWithLifecycle()
    val riskStr by viewModel.userRiskPercent.collectAsStateWithLifecycle()

    if (candidate == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stock candidate selected")
        }
        return
    }

    val item = candidate!!
    val setupTypeEnum = try {
        SetupType.valueOf(item.setupType)
    } catch (e: Exception) {
        SetupType.BREAKOUT
    }

    Column(modifier = modifier.fillMaxSize().testTag("stock_detail_screen")) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SetupBadge(item.setupType)
                    }
                    Text(
                        text = item.companyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                ScoreBadge(item.score)
                Spacer(modifier = Modifier.width(12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DisclaimerBanner()
            }

            // Interactive Candlestick Chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("candlestick_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Candlestick Chart (40 Sessions)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Close: ₹${String.format("%.2f", item.closePrice)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BullGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        CandlestickChart(
                            candles = candles,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            supportLevel = item.stopLoss,
                            resistanceLevel = item.target1
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(BullGreen))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Support / SL (Green)", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(BearRed))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Target 1 (Red)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Trade Setup Levels Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hypothetical Trade Planning Levels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LevelBadge("Entry", "₹${String.format("%.2f", item.entryPrice)}", MaterialTheme.colorScheme.primary)
                            val riskPct = ((item.closePrice - item.stopLoss) / item.closePrice) * 100
                            LevelBadge("Stop-Loss", "₹${String.format("%.2f", item.stopLoss)} (-${String.format("%.1f", riskPct)}%)", BearRed)
                            val t1Pct = ((item.target1 - item.closePrice) / item.closePrice) * 100
                            LevelBadge("Target 1 (1.6R)", "₹${String.format("%.2f", item.target1)} (+${String.format("%.1f", t1Pct)}%)", BullGreen)
                            val t2Pct = ((item.target2 - item.closePrice) / item.closePrice) * 100
                            LevelBadge("Target 2 (2.6R)", "₹${String.format("%.2f", item.target2)} (+${String.format("%.1f", t2Pct)}%)", AccentGold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Calculated Risk-to-Reward Ratio: ${String.format("%.2f", item.riskRewardRatio)} : 1",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    }
                }
            }

            // Rule-based Technical Setup Rationale
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Technical Setup Rationale",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.rationale,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Interactive Position Sizing Calculator
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("position_sizer_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Position Sizing & Risk Calculator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = capitalStr,
                                onValueChange = {
                                    viewModel.userCapital.value = it
                                    viewModel.recalculatePositionSize()
                                },
                                label = { Text("Trading Capital (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("capital_input")
                            )

                            OutlinedTextField(
                                value = riskStr,
                                onValueChange = {
                                    viewModel.userRiskPercent.value = it
                                    viewModel.recalculatePositionSize()
                                },
                                label = { Text("Max Risk (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("risk_pct_input")
                            )
                        }

                        positionResult?.let { res ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Recommended Shares:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${res.recommendedShares} shares", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BullGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Required Capital:", style = MaterialTheme.typography.bodySmall)
                                        Text("₹${String.format("%.2f", res.totalRequiredCapital)} (${String.format("%.1f", res.capitalUtilizationPercent)}% utilized)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Max Risk on Stop Loss:", style = MaterialTheme.typography.bodySmall, color = BearRed)
                                        Text("₹${String.format("%.2f", res.riskAmount)}", style = MaterialTheme.typography.bodySmall, color = BearRed, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Potential Profit (Target 1):", style = MaterialTheme.typography.bodySmall, color = BullGreen)
                                        Text("₹${String.format("%.2f", res.potentialProfitTarget1)}", style = MaterialTheme.typography.bodySmall, color = BullGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Optional Gemini AI Explanation Module
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_explainer_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Research Explainer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Uses Gemini 3.5 Flash to synthesize market structure, key invalidation triggers, and execution discipline for this candidate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.triggerAiResearch(item) },
                            enabled = !isGeneratingAi,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_ai_explanation_button")
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing with Gemini...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate AI Research Rationale")
                            }
                        }

                        aiExplanation?.let { explanation ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Security Note: Embedded client API keys can be extracted from APKs. Use with caution or configured secrets.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Backtest Button
            item {
                Button(
                    onClick = { onNavigateToBacktest(setupTypeEnum) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backtest_strategy_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backtest ${item.setupType} Strategy")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LevelBadge(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
