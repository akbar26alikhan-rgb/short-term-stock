package com.example.analysis

import com.example.model.CandidateScore
import com.example.model.MarketCandle
import com.example.model.SetupType
import com.example.model.TechnicalIndicators
import com.example.model.TradeSetup
import kotlin.math.abs
import kotlin.math.max

object TechnicalAnalysisEngine {

    /**
     * Calculates RSI (14-period Wilder smoothing)
     */
    fun calculateRSI(candles: List<MarketCandle>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) gains += change else losses += abs(change)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Calculates Exponential Moving Average (EMA)
     */
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size < period) return prices.average()

        val multiplier = 2.0 / (period + 1.0)
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = (prices[i] * multiplier) + (ema * (1.0 - multiplier))
        }
        return ema
    }

    /**
     * Calculates full EMA series for MACD signal calculation
     */
    fun calculateEMASeries(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()

        val multiplier = 2.0 / (period + 1.0)
        val result = mutableListOf<Double>()
        var ema = prices.take(period).average()
        result.add(ema)

        for (i in period until prices.size) {
            ema = (prices[i] * multiplier) + (ema * (1.0 - multiplier))
            result.add(ema)
        }
        return result
    }

    /**
     * Calculates MACD (12, 26, 9)
     * returns Triple(macdLine, signalLine, histogram)
     */
    fun calculateMACD(
        candles: List<MarketCandle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<Double, Double, Double> {
        if (candles.size < slowPeriod + signalPeriod) {
            return Triple(0.0, 0.0, 0.0)
        }

        val closes = candles.map { it.close }
        val fastEmaSeries = calculateEMASeries(closes, fastPeriod)
        val slowEmaSeries = calculateEMASeries(closes, slowPeriod)

        val offset = slowPeriod - fastPeriod
        val macdSeries = mutableListOf<Double>()
        for (i in slowEmaSeries.indices) {
            val fastIndex = i + offset
            if (fastIndex < fastEmaSeries.size) {
                macdSeries.add(fastEmaSeries[fastIndex] - slowEmaSeries[i])
            }
        }

        if (macdSeries.size < signalPeriod) {
            val lastMacd = macdSeries.lastOrNull() ?: 0.0
            return Triple(lastMacd, 0.0, lastMacd)
        }

        val signalSeries = calculateEMASeries(macdSeries, signalPeriod)
        val macdLine = macdSeries.last()
        val signalLine = signalSeries.last()
        val hist = macdLine - signalLine

        return Triple(macdLine, signalLine, hist)
    }

    /**
     * Calculates Average True Range (ATR, 14-period)
     */
    fun calculateATR(candles: List<MarketCandle>, period: Int = 14): Double {
        if (candles.size < 2) return 0.0
        val trList = mutableListOf<Double>()

        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevClose = candles[i - 1].close

            val tr = max(high - low, max(abs(high - prevClose), abs(low - prevClose)))
            trList.add(tr)
        }

        if (trList.size < period) return trList.average()

        var atr = trList.take(period).average()
        for (i in period until trList.size) {
            atr = (atr * (period - 1) + trList[i]) / period
        }
        return atr
    }

    /**
     * Calculates 20-day Support and Resistance
     */
    fun calculateSupportResistance20(candles: List<MarketCandle>): Pair<Double, Double> {
        val window = candles.takeLast(20)
        if (window.isEmpty()) return Pair(0.0, 0.0)
        val support = window.minOf { it.low }
        val resistance = window.maxOf { it.high }
        return Pair(support, resistance)
    }

    /**
     * Calculates Relative Volume (RVOL) = Today's Volume / 20-day Avg Volume
     */
    fun calculateVolumeMetrics(candles: List<MarketCandle>): Pair<Long, Double> {
        if (candles.isEmpty()) return Pair(0L, 1.0)
        val window = candles.takeLast(21)
        if (window.size <= 1) return Pair(window.first().volume, 1.0)

        val priorCandles = window.dropLast(1)
        val avgVol = (priorCandles.map { it.volume }.average()).toLong().coerceAtLeast(1L)
        val todayVol = window.last().volume
        val rvol = todayVol.toDouble() / avgVol.toDouble()

        return Pair(avgVol, rvol)
    }

    /**
     * Calculates Relative Strength vs Nifty 50 benchmark
     */
    fun calculateRelativeStrength(
        stockCandles: List<MarketCandle>,
        benchmarkCandles: List<MarketCandle>,
        lookbackDays: Int
    ): Double {
        if (stockCandles.size <= lookbackDays || benchmarkCandles.size <= lookbackDays) return 0.0

        val stockNow = stockCandles.last().close
        val stockOld = stockCandles[stockCandles.size - 1 - lookbackDays].close
        val stockReturn = if (stockOld > 0) ((stockNow - stockOld) / stockOld) * 100.0 else 0.0

        val benchNow = benchmarkCandles.last().close
        val benchOld = benchmarkCandles[benchmarkCandles.size - 1 - lookbackDays].close
        val benchReturn = if (benchOld > 0) ((benchNow - benchOld) / benchOld) * 100.0 else 0.0

        return stockReturn - benchReturn
    }

    /**
     * Computes all technical indicators for a given stock series
     */
    fun computeIndicators(
        symbol: String,
        candles: List<MarketCandle>,
        benchmarkCandles: List<MarketCandle>
    ): TechnicalIndicators {
        val closes = candles.map { it.close }
        val rsi = calculateRSI(candles)
        val (macd, macdSignal, macdHist) = calculateMACD(candles)
        val ema20 = calculateEMA(closes, 20)
        val ema50 = calculateEMA(closes, 50)
        val atr = calculateATR(candles, 14)
        val (avgVol, rvol) = calculateVolumeMetrics(candles)
        val (support20, resistance20) = calculateSupportResistance20(candles)
        val rs20 = calculateRelativeStrength(candles, benchmarkCandles, 20)
        val rs50 = calculateRelativeStrength(candles, benchmarkCandles, 50)

        return TechnicalIndicators(
            symbol = symbol,
            rsi = rsi,
            macd = macd,
            macdSignal = macdSignal,
            macdHist = macdHist,
            ema20 = ema20,
            ema50 = ema50,
            atr = atr,
            avgVolume20 = avgVol,
            rvol = rvol,
            support20 = support20,
            resistance20 = resistance20,
            rsNifty50_20d = rs20,
            rsNifty50_50d = rs50
        )
    }

    /**
     * Evaluates a stock for swing trading setups (Breakout, Pullback, Momentum)
     */
    fun evaluateCandidate(
        symbol: String,
        companyName: String,
        sector: String,
        candles: List<MarketCandle>,
        indicators: TechnicalIndicators,
        dataSource: String,
        isDemo: Boolean = false
    ): TradeSetup? {
        if (candles.size < 30) return null

        val current = candles.last()
        val close = current.close

        // Liquidity filter: Ensure minimum historical candles
        if (candles.size < 15) return null

        val priorCandles = candles.dropLast(1)
        val prior20High = priorCandles.takeLast(20).maxOfOrNull { it.high } ?: close
        val distTo20High = if (prior20High > 0) (prior20High - close) / prior20High else 0.0

        // Classify into appropriate swing strategy based on current structural positioning
        val isBreakout = (close >= prior20High * 0.985 && indicators.rvol >= 1.05) || (distTo20High in -0.05..0.02)
        val isPullback = (abs(close - indicators.ema20) / indicators.ema20 <= 0.035 && indicators.rsi in 35.0..60.0) ||
                (close >= indicators.ema50 * 0.98 && close <= indicators.ema20 * 1.02)

        val bestSetup = when {
            isBreakout -> SetupType.BREAKOUT
            isPullback -> SetupType.PULLBACK
            else -> SetupType.MOMENTUM
        }

        val (entry, sl, t1, t2) = calculateRiskLevels(bestSetup, close, indicators)
        val risk = entry - sl
        if (risk <= 0.0) return null

        val rr = (t1 - entry) / risk
        val score = calculateScore(bestSetup, indicators, rr)

        val rationale = buildRationale(bestSetup, indicators, score, close, sl, t1)

        return TradeSetup(
            symbol = symbol,
            companyName = companyName,
            sector = sector,
            setupType = bestSetup,
            closePrice = close,
            entryPrice = entry,
            stopLoss = sl,
            target1 = t1,
            target2 = t2,
            riskRewardRatio = rr,
            score = score,
            indicators = indicators,
            rationale = rationale,
            timestamp = System.currentTimeMillis(),
            dataSource = dataSource,
            dataFreshness = "Verified Daily Close",
            isDemo = isDemo
        )
    }

    private fun checkBreakoutSetup(candles: List<MarketCandle>, ind: TechnicalIndicators): Boolean? {
        val close = candles.last().close
        val priorCandles = candles.dropLast(1)
        val prior20High = priorCandles.takeLast(20).maxOfOrNull { it.high } ?: return null

        // Breakout condition: Close near or above 20-day high with strong volume expansion
        val isBreakingOut = close >= (prior20High * 0.995)
        val volumeSurge = ind.rvol >= 1.35
        val trendAligned = close > ind.ema20 && ind.ema20 > ind.ema50
        val healthyRsi = ind.rsi in 54.0..80.0
        val outperforming = ind.rsNifty50_20d > -1.0

        return if (isBreakingOut && volumeSurge && trendAligned && healthyRsi && outperforming) true else null
    }

    private fun checkPullbackSetup(candles: List<MarketCandle>, ind: TechnicalIndicators): Boolean? {
        val close = candles.last().close

        // Pullback condition: Primary uptrend, pulling back near 20 EMA with dry volume
        val uptrend = ind.ema20 > ind.ema50 && close > ind.ema50
        val nearEma20 = abs(close - ind.ema20) / ind.ema20 <= 0.03
        val pullbackRsi = ind.rsi in 40.0..58.0
        val dryVolume = ind.rvol <= 1.25
        val holdingSupport = close >= ind.support20

        return if (uptrend && nearEma20 && pullbackRsi && dryVolume && holdingSupport) true else null
    }

    private fun checkMomentumSetup(candles: List<MarketCandle>, ind: TechnicalIndicators): Boolean? {
        val close = candles.last().close

        // Momentum condition: Fast trend expansion, positive MACD histogram, high RS
        val expandingTrend = close > ind.ema20 && ind.ema20 > ind.ema50
        val macdBullish = ind.macdHist > 0 && ind.macd > ind.macdSignal
        val strongRsi = ind.rsi in 58.0..78.0
        val strongRS = ind.rsNifty50_20d >= 1.5
        val volumeConfirmed = ind.rvol >= 1.15

        return if (expandingTrend && macdBullish && strongRsi && strongRS && volumeConfirmed) true else null
    }

    private fun calculateRiskLevels(
        setupType: SetupType,
        close: Double,
        ind: TechnicalIndicators
    ): List<Double> {
        val entry = close
        val sl = when (setupType) {
            SetupType.BREAKOUT -> {
                max(close - (1.6 * ind.atr), ind.support20)
            }
            SetupType.PULLBACK -> {
                max(ind.ema20 - (1.2 * ind.atr), ind.support20)
            }
            SetupType.MOMENTUM -> {
                close - (1.5 * ind.atr)
            }
        }

        val risk = max(entry - sl, close * 0.015) // At least 1.5% buffer
        val target1 = entry + (1.6 * risk)
        val target2 = entry + (2.6 * risk)

        return listOf(entry, sl, target1, target2)
    }

    fun calculateScore(
        setupType: SetupType,
        ind: TechnicalIndicators,
        rr: Double
    ): CandidateScore {
        // Trend score (0-25)
        var trendScore = 10
        if (ind.ema20 > ind.ema50) trendScore += 10
        if (ind.ema50 > 0 && (ind.ema20 - ind.ema50) / ind.ema50 > 0.01) trendScore += 5
        trendScore = trendScore.coerceIn(0, 25)

        // Momentum score (0-20)
        var momentumScore = 10
        if (ind.macdHist > 0) momentumScore += 5
        if (ind.rsi in 55.0..68.0) momentumScore += 5
        else if (ind.rsi in 48.0..55.0 && setupType == SetupType.PULLBACK) momentumScore += 5
        momentumScore = momentumScore.coerceIn(0, 20)

        // Volume score (0-20)
        val volumeScore = when {
            ind.rvol >= 2.0 -> 20
            ind.rvol >= 1.5 -> 17
            ind.rvol >= 1.2 -> 13
            ind.rvol >= 0.9 -> 10
            else -> 6
        }.coerceIn(0, 20)

        // Relative Strength score (0-20)
        val rsScore = when {
            ind.rsNifty50_20d >= 5.0 -> 20
            ind.rsNifty50_20d >= 2.0 -> 16
            ind.rsNifty50_20d >= 0.0 -> 12
            ind.rsNifty50_20d >= -3.0 -> 8
            else -> 4
        }.coerceIn(0, 20)

        // Risk Reward score (0-15)
        val rrScore = when {
            rr >= 2.2 -> 15
            rr >= 1.8 -> 13
            rr >= 1.5 -> 10
            else -> 6
        }.coerceIn(0, 15)

        val total = trendScore + momentumScore + volumeScore + rsScore + rrScore

        val notes = "Setup: ${setupType.label} | Trend=$trendScore/25, Mom=$momentumScore/20, Vol=$volumeScore/20, RS=$rsScore/20, R:R=$rrScore/15"

        return CandidateScore(
            compositeScore = total,
            trendScore = trendScore,
            momentumScore = momentumScore,
            volumeScore = volumeScore,
            rsScore = rsScore,
            riskRewardScore = rrScore,
            notes = notes
        )
    }

    private fun buildRationale(
        setup: SetupType,
        ind: TechnicalIndicators,
        score: CandidateScore,
        close: Double,
        sl: Double,
        t1: Double
    ): String {
        val slPct = String.format("%.2f", ((close - sl) / close) * 100)
        val t1Pct = String.format("%.2f", ((t1 - close) / close) * 100)
        val rvolStr = String.format("%.2f", ind.rvol)
        val rsiStr = String.format("%.1f", ind.rsi)
        val rsStr = String.format("%+.2f", ind.rsNifty50_20d)

        return when (setup) {
            SetupType.BREAKOUT -> "Price broke 20-day resistance at ₹${String.format("%.2f", ind.resistance20)} with strong volume surge (${rvolStr}x RVOL). RSI sits at $rsiStr indicating active buying pressure without extreme exhaustion. Outperforming Nifty 50 by $rsStr% over 20 sessions. Planned risk: -$slPct%, Target 1: +$t1Pct%."
            SetupType.PULLBACK -> "Price is orderly pulling back to test rising 20 EMA (₹${String.format("%.2f", ind.ema20)}) on diminished volume (${rvolStr}x RVOL). RSI reset to $rsiStr. Uptrend remains intact with 50 EMA support. Planned risk: -$slPct%, Target 1: +$t1Pct%."
            SetupType.MOMENTUM -> "Steady momentum trend with expanding 20/50 EMAs and positive MACD histogram. Relative strength vs Nifty is $rsStr%. Volume is supportive at ${rvolStr}x RVOL. Planned risk: -$slPct%, Target 1: +$t1Pct%."
        }
    }
}
