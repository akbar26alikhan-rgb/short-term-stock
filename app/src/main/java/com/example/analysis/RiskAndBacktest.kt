package com.example.analysis

import com.example.model.BacktestSummary
import com.example.model.BacktestTrade
import com.example.model.MarketCandle
import com.example.model.PositionSizeResult
import com.example.model.SetupType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object PositionSizer {

    fun calculate(
        capital: Double,
        riskPercentage: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        target1: Double,
        target2: Double
    ): PositionSizeResult {
        val safeCapital = max(1000.0, capital)
        val safeRiskPct = riskPercentage.coerceIn(0.1, 10.0)
        val riskAmount = safeCapital * (safeRiskPct / 100.0)

        val riskPerShare = max(0.01, entryPrice - stopLossPrice)
        val theoreticalShares = floor(riskAmount / riskPerShare).toInt()
        // Ensure not exceeding total capital
        val maxAffordableShares = floor(safeCapital / entryPrice).toInt()
        val recommendedShares = max(1, min(theoreticalShares, maxAffordableShares))

        val totalRequiredCapital = recommendedShares * entryPrice
        val capitalUtilization = (totalRequiredCapital / safeCapital) * 100.0

        val profit1PerShare = max(0.0, target1 - entryPrice)
        val profit2PerShare = max(0.0, target2 - entryPrice)

        val potentialProfit1 = recommendedShares * profit1PerShare
        val potentialProfit2 = recommendedShares * profit2PerShare

        val rr1 = if (riskPerShare > 0) profit1PerShare / riskPerShare else 0.0
        val rr2 = if (riskPerShare > 0) profit2PerShare / riskPerShare else 0.0

        return PositionSizeResult(
            capital = safeCapital,
            riskPercentage = safeRiskPct,
            riskAmount = riskAmount,
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            riskPerShare = riskPerShare,
            recommendedShares = recommendedShares,
            totalRequiredCapital = totalRequiredCapital,
            capitalUtilizationPercent = capitalUtilization,
            target1 = target1,
            target2 = target2,
            potentialProfitTarget1 = potentialProfit1,
            potentialProfitTarget2 = potentialProfit2,
            riskToReward1 = rr1,
            riskToReward2 = rr2
        )
    }
}

object BacktestEngine {

    /**
     * Estimated round-trip friction for Indian equity delivery:
     * - STT: 0.1% on buy and 0.1% on sell (0.2% total)
     * - Exchange transaction charge, GST, SEBI fee, Stamp duty: ~0.08%
     * - Brokerage: ~0.05%
     * - Execution slippage: ~0.1% each way (0.2% total)
     * Total estimated round-trip cost & slippage = ~0.53%
     */
    private const val ROUND_TRIP_COST_PERCENT = 0.53

    fun runBacktest(
        symbol: String,
        candles: List<MarketCandle>,
        benchmarkCandles: List<MarketCandle>,
        strategy: SetupType,
        maxHoldingDays: Int = 15
    ): BacktestSummary {
        val trades = mutableListOf<BacktestTrade>()
        if (candles.size < 60) {
            return BacktestSummary(
                setupType = strategy,
                totalTrades = 0,
                winningTrades = 0,
                losingTrades = 0,
                winRatePercent = 0.0,
                profitFactor = 0.0,
                totalReturnPercent = 0.0,
                maxDrawdownPercent = 0.0,
                estimatedTaxesAndSlippage = 0.0,
                trades = emptyList()
            )
        }

        var inTrade = false
        var entryPrice = 0.0
        var stopLoss = 0.0
        var target1 = 0.0
        var entryIndex = 0

        // Step through history simulating realistic non-lookahead conditions
        for (i in 40 until candles.size) {
            if (!inTrade) {
                val historicalSublist = candles.subList(0, i + 1)
                val benchSublist = benchmarkCandles.take(min(benchmarkCandles.size, i + 1))
                val indicators = TechnicalAnalysisEngine.computeIndicators(symbol, historicalSublist, benchSublist)

                val setup = TechnicalAnalysisEngine.evaluateCandidate(
                    symbol = symbol,
                    companyName = symbol,
                    sector = "NSE Equities",
                    candles = historicalSublist,
                    indicators = indicators,
                    dataSource = "Backtest Engine"
                )

                if (setup != null && setup.setupType == strategy) {
                    inTrade = true
                    entryPrice = setup.entryPrice
                    stopLoss = setup.stopLoss
                    target1 = setup.target1
                    entryIndex = i
                }
            } else {
                val current = candles[i]
                val daysHeld = i - entryIndex

                var exitPrice: Double? = null
                var outcome: String? = null

                // Check intraday extremes: Did it hit stop loss or target first?
                if (current.low <= stopLoss) {
                    exitPrice = stopLoss
                    outcome = "STOP_LOSS"
                } else if (current.high >= target1) {
                    exitPrice = target1
                    outcome = "TARGET_1"
                } else if (daysHeld >= maxHoldingDays) {
                    exitPrice = current.close
                    outcome = "TIME_STOP"
                }

                if (exitPrice != null && outcome != null) {
                    val grossPnlPct = ((exitPrice - entryPrice) / entryPrice) * 100.0
                    val netPnlPct = grossPnlPct - ROUND_TRIP_COST_PERCENT

                    trades.add(
                        BacktestTrade(
                            symbol = symbol,
                            entryDate = candles[entryIndex].dateStr,
                            exitDate = current.dateStr,
                            setupType = strategy,
                            entryPrice = entryPrice,
                            exitPrice = exitPrice,
                            pnlPercent = grossPnlPct,
                            netPnlAfterCosts = netPnlPct,
                            outcome = outcome,
                            holdingPeriodDays = daysHeld
                        )
                    )

                    inTrade = false
                }
            }
        }

        val totalTrades = trades.size
        val winningTrades = trades.count { it.netPnlAfterCosts > 0 }
        val losingTrades = trades.count { it.netPnlAfterCosts <= 0 }
        val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades) * 100.0 else 0.0

        val totalGains = trades.filter { it.netPnlAfterCosts > 0 }.sumOf { it.netPnlAfterCosts }
        val totalLosses = abs(trades.filter { it.netPnlAfterCosts < 0 }.sumOf { it.netPnlAfterCosts })
        val profitFactor = if (totalLosses > 0) totalGains / totalLosses else if (totalGains > 0) 9.9 else 0.0

        val totalReturn = trades.sumOf { it.netPnlAfterCosts }

        // Calculate max drawdown
        var peak = 0.0
        var cumulative = 0.0
        var maxDd = 0.0
        for (trade in trades) {
            cumulative += trade.netPnlAfterCosts
            if (cumulative > peak) peak = cumulative
            val dd = peak - cumulative
            if (dd > maxDd) maxDd = dd
        }

        val totalCost = totalTrades * ROUND_TRIP_COST_PERCENT

        return BacktestSummary(
            setupType = strategy,
            totalTrades = totalTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRatePercent = winRate,
            profitFactor = profitFactor,
            totalReturnPercent = totalReturn,
            maxDrawdownPercent = maxDd,
            estimatedTaxesAndSlippage = totalCost,
            trades = trades
        )
    }
}
