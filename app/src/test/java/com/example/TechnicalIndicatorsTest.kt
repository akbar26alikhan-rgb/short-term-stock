package com.example

import com.example.analysis.PositionSizer
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.MarketCandle
import com.example.model.SetupType
import com.example.report.ReportService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalIndicatorsTest {

    private fun makeSampleCandles(closes: List<Double>): List<MarketCandle> {
        return closes.mapIndexed { i, close ->
            MarketCandle(
                symbol = "TEST",
                dateEpochDay = i.toLong(),
                dateStr = "2026-01-${String.format("%02d", i + 1)}",
                open = close * 0.99,
                high = close * 1.01,
                low = close * 0.98,
                close = close,
                volume = 100000L
            )
        }
    }

    @Test
    fun testRSICalculation_allGains() {
        val prices = (1..20).map { it.toDouble() * 10.0 }
        val candles = makeSampleCandles(prices)
        val rsi = TechnicalAnalysisEngine.calculateRSI(candles, 14)
        assertTrue("RSI for monotonic price increases should be 100", rsi >= 99.0)
    }

    @Test
    fun testRSICalculation_allLosses() {
        val prices = (20 downTo 1).map { it.toDouble() * 10.0 }
        val candles = makeSampleCandles(prices)
        val rsi = TechnicalAnalysisEngine.calculateRSI(candles, 14)
        assertTrue("RSI for monotonic price drops should be near 0", rsi <= 1.0)
    }

    @Test
    fun testEMACalculation() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0)
        val ema = TechnicalAnalysisEngine.calculateEMA(prices, 5)
        assertTrue("EMA should be weighted towards recent prices", ema > 14.0)
    }

    @Test
    fun testATRCalculation() {
        val closes = (1..20).map { 100.0 }
        val candles = closes.mapIndexed { i, c ->
            MarketCandle("TEST", i.toLong(), "2026-01-01", 100.0, 105.0, 95.0, 100.0, 50000L)
        }
        val atr = TechnicalAnalysisEngine.calculateATR(candles, 14)
        assertEquals(10.0, atr, 0.5)
    }

    @Test
    fun testVolumeMetrics_rvol() {
        val candles = (1..21).mapIndexed { i, _ ->
            val vol = if (i == 20) 200000L else 100000L
            MarketCandle("TEST", i.toLong(), "2026-01-01", 100.0, 102.0, 98.0, 100.0, vol)
        }
        val (avgVol, rvol) = TechnicalAnalysisEngine.calculateVolumeMetrics(candles)
        assertEquals(100000L, avgVol)
        assertEquals(2.0, rvol, 0.05)
    }

    @Test
    fun testRelativeStrengthVersusNifty() {
        // Stock goes from 100 to 120 (+20%)
        val stockCandles = (1..25).mapIndexed { i, _ ->
            val price = 100.0 + (i * 0.8)
            MarketCandle("STOCK", i.toLong(), "2026-01-01", price, price + 1, price - 1, price, 50000L)
        }
        // Benchmark goes from 100 to 105 (+5%)
        val benchCandles = (1..25).mapIndexed { i, _ ->
            val price = 100.0 + (i * 0.2)
            MarketCandle("^NSEI", i.toLong(), "2026-01-01", price, price + 1, price - 1, price, 500000L)
        }

        val rs = TechnicalAnalysisEngine.calculateRelativeStrength(stockCandles, benchCandles, 20)
        assertTrue("Stock relative strength should be positive", rs > 10.0)
    }

    @Test
    fun testPositionSizer() {
        val result = PositionSizer.calculate(
            capital = 100000.0,
            riskPercentage = 1.5, // ₹1,500 allowed risk
            entryPrice = 100.0,
            stopLossPrice = 95.0, // ₹5 risk per share
            target1 = 110.0,
            target2 = 120.0
        )

        // 1500 / 5 = 300 shares
        assertEquals(300, result.recommendedShares)
        assertEquals(1500.0, result.riskAmount, 0.01)
        assertEquals(30000.0, result.totalRequiredCapital, 0.01)
        assertEquals(30.0, result.capitalUtilizationPercent, 0.01)
        assertEquals(3000.0, result.potentialProfitTarget1, 0.01)
        assertEquals(2.0, result.riskToReward1, 0.01)
    }

    @Test
    fun testCsvParser() {
        val csv = """
            Date,Open,High,Low,Close,Volume
            2026-01-05,100.0,105.0,98.0,102.5,500000
            2026-01-06,102.5,108.0,101.0,107.0,750000
        """.trimIndent()

        val parsed = ReportService.parseCsvCandles(csv, "TEST")
        assertEquals(2, parsed.size)
        assertEquals(102.5, parsed[0].close, 0.01)
        assertEquals(107.0, parsed[1].close, 0.01)
        assertEquals(750000L, parsed[1].volume)
    }
}
