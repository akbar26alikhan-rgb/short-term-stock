package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CandleEntity
import com.example.data.local.ScanResultEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import kotlin.math.max
import kotlin.math.min

@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("disclaimer_banner"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Research Disclaimer",
                tint = AccentGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RESEARCH & STUDY ONLY: Mathematical setup scanner. Not brokerage or investment advice. Scores are rule metrics, not profit probabilities.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SetupBadge(setupType: String) {
    val (color, label) = when (setupType.uppercase()) {
        "BREAKOUT" -> Pair(AccentGold, "20D Breakout")
        "PULLBACK" -> Pair(AccentCyan, "EMA Pullback")
        "MOMENTUM" -> Pair(BullGreen, "Momentum")
        else -> Pair(MaterialTheme.colorScheme.primary, setupType)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> BullGreen
        score >= 65 -> AccentGold
        else -> AccentCyan
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CandidateCard(
    candidate: ScanResultEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("candidate_card_${candidate.symbol}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        text = candidate.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SetupBadge(candidate.setupType)
                    if (candidate.isDemo) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DEMO",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                ScoreBadge(candidate.score)
            }

            Text(
                text = "${candidate.companyName} • ${candidate.sector}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Levels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Close / Entry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format("%.2f", candidate.closePrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Stop-Loss", style = MaterialTheme.typography.labelSmall, color = BearRed)
                    val riskPct = ((candidate.closePrice - candidate.stopLoss) / candidate.closePrice) * 100
                    Text("₹${String.format("%.2f", candidate.stopLoss)} (-${String.format("%.1f", riskPct)}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BearRed)
                }
                Column {
                    Text("Target 1 (1.6R)", style = MaterialTheme.typography.labelSmall, color = BullGreen)
                    val rewardPct = ((candidate.target1 - candidate.closePrice) / candidate.closePrice) * 100
                    Text("₹${String.format("%.2f", candidate.target1)} (+${String.format("%.1f", rewardPct)}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BullGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick technical indicators footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "R:R ${String.format("%.2f", candidate.riskRewardRatio)}:1",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGold,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Score: ${candidate.score}/100",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap for Chart & Risk",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Custom High-Resolution Candlestick and EMA Overlay Chart
 */
@Composable
fun CandlestickChart(
    candles: List<CandleEntity>,
    modifier: Modifier = Modifier,
    supportLevel: Double? = null,
    resistanceLevel: Double? = null
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No candle data available for chart", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // Display up to last 40 daily bars
    val displayCandles = candles.takeLast(40)
    val maxPrice = max(
        displayCandles.maxOf { it.high },
        resistanceLevel ?: 0.0
    ) * 1.01
    val minPrice = min(
        displayCandles.minOf { it.low },
        supportLevel ?: Double.MAX_VALUE
    ) * 0.99
    val priceRange = max(1.0, maxPrice - minPrice)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / displayCandles.size.toFloat()
        val candleBodyWidth = max(2f, barWidth * 0.65f)

        fun priceToY(price: Double): Float {
            return (height - ((price - minPrice) / priceRange * height)).toFloat()
        }

        // Draw horizontal grid lines
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = height * (i.toFloat() / gridSteps)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw Support Line if present
        supportLevel?.let { sup ->
            val y = priceToY(sup)
            drawLine(
                color = BullGreen.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 2f
            )
        }

        // Draw Resistance Line if present
        resistanceLevel?.let { res ->
            val y = priceToY(res)
            drawLine(
                color = BearRed.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 2f
            )
        }

        // Draw Candlesticks
        for ((index, c) in displayCandles.withIndex()) {
            val xCenter = (index * barWidth) + (barWidth / 2f)
            val openY = priceToY(c.open)
            val closeY = priceToY(c.close)
            val highY = priceToY(c.high)
            val lowY = priceToY(c.low)

            val isBullish = c.close >= c.open
            val color = if (isBullish) BullGreen else BearRed

            // Draw high-low wick line
            drawLine(
                color = color,
                start = Offset(xCenter, highY),
                end = Offset(xCenter, lowY),
                strokeWidth = 1.5f
            )

            // Draw candle real body
            val topY = min(openY, closeY)
            val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))

            drawRect(
                color = color,
                topLeft = Offset(xCenter - (candleBodyWidth / 2f), topY),
                size = Size(candleBodyWidth, bodyHeight)
            )
        }
    }
}
