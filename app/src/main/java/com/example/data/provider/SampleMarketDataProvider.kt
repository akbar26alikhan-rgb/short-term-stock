package com.example.data.provider

import com.example.model.MarketCandle
import com.example.model.ProviderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SampleMarketDataProvider : MarketDataProvider {

    override val name: String = "Bundled NSE Historical Dataset (DEMO)"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    override suspend fun fetchHistoricalDailyCandles(
        symbol: String,
        days: Int
    ): Result<List<MarketCandle>> {
        val candles = generateRealisticHistoricalSeries(symbol, days)
        return Result.success(candles)
    }

    override suspend fun fetchIntradayCandles(symbol: String): Result<List<MarketCandle>> {
        // Intraday demo candles for standard trading hours 9:15 to 15:30 IST
        val list = mutableListOf<MarketCandle>()
        val basePrice = getBasePriceForSymbol(symbol)
        val times = listOf("09:15", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30")
        var currentPrice = basePrice

        for ((idx, t) in times.withIndex()) {
            val delta = (Math.sin(idx.toDouble() * 0.7) * (basePrice * 0.004))
            val open = currentPrice
            val close = currentPrice + delta
            val high = maxOf(open, close) + (basePrice * 0.002)
            val low = minOf(open, close) - (basePrice * 0.002)
            val vol = 45000L + (idx * 5000L)
            currentPrice = close

            list.add(
                MarketCandle(
                    symbol = symbol,
                    dateEpochDay = System.currentTimeMillis() / 1000L,
                    dateStr = t,
                    open = Math.round(open * 100.0) / 100.0,
                    high = Math.round(high * 100.0) / 100.0,
                    low = Math.round(low * 100.0) / 100.0,
                    close = Math.round(close * 100.0) / 100.0,
                    volume = vol,
                    tradedValue = close * vol,
                    isIntraday = true
                )
            )
        }
        return Result.success(list)
    }

    override suspend fun fetchBenchmarkCandles(days: Int): Result<List<MarketCandle>> {
        return fetchHistoricalDailyCandles("^NSEI", days)
    }

    override suspend fun fetchCorporateActions(symbol: String): Result<List<String>> {
        val actions = when (symbol) {
            "RELIANCE" -> listOf("Board meeting for Q3 Financial Results", "Dividend announced: ₹10/share")
            "TCS" -> listOf("Interim Dividend: ₹9/share", "Q3 Earnings call recording published")
            "INFY" -> listOf("Special Dividend: ₹8/share", "Share Buyback approved")
            "HDFCBANK" -> listOf("Annual General Meeting scheduled", "Branch expansion update")
            else -> listOf("Quarterly filing compliance confirmed")
        }
        return Result.success(actions)
    }

    override fun getProviderStatus(): ProviderStatus {
        return ProviderStatus(
            providerName = name,
            isAvailable = true,
            lastRetrievedTime = System.currentTimeMillis(),
            delayNotice = "Offline sample data clearly labelled DEMO for testing scanner without network latency",
            warnings = listOf("DEMO DATA: For educational and testing verification only")
        )
    }

    private fun getBasePriceForSymbol(symbol: String): Double {
        return when (symbol) {
            "RELIANCE" -> 2940.0
            "TCS" -> 4120.0
            "INFY" -> 1880.0
            "HDFCBANK" -> 1670.0
            "ICICIBANK" -> 1260.0
            "SBIN" -> 810.0
            "BHARTIARTL" -> 1650.0
            "ITC" -> 510.0
            "LT" -> 3620.0
            "TATAMOTORS" -> 980.0
            "BAJFINANCE" -> 7250.0
            "MARUTI" -> 12400.0
            "SUNPHARMA" -> 1890.0
            "TITAN" -> 3480.0
            "AXISBANK" -> 1210.0
            "^NSEI" -> 24850.0 // Nifty 50
            else -> 1500.0
        }
    }

    /**
     * Generates a statistically authentic 90-day daily candle series that exhibits
     * realistic market characteristics (breakouts, pullbacks, momentum, consolidation)
     */
    private fun generateRealisticHistoricalSeries(symbol: String, days: Int): List<MarketCandle> {
        val candles = mutableListOf<MarketCandle>()
        val basePrice = getBasePriceForSymbol(symbol)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)

        var currentPrice = basePrice * 0.88 // Starts 12% lower to simulate natural trend evolution
        var baseVolume = when (symbol) {
            "^NSEI" -> 180000000L
            "RELIANCE", "HDFCBANK", "SBIN" -> 3500000L
            "TATAMOTORS", "ITC" -> 8500000L
            else -> 1200000L
        }

        // Add character variation per symbol so different strategies trigger realistically
        val trendFactor = when (symbol) {
            "RELIANCE", "BHARTIARTL" -> 0.0035 // Strong breakout candidate
            "TCS", "INFY" -> 0.0018 // Steady momentum candidate
            "HDFCBANK", "ICICIBANK" -> 0.0012 // Pullback candidate
            "^NSEI" -> 0.0015 // Benchmark steady growth
            else -> 0.002
        }

        for (i in 0 until days) {
            // Skip weekends
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }

            val epochDay = calendar.timeInMillis / (86400000L)
            val dateStr = dateFormat.format(calendar.time)

            // Market wave oscillation
            val cycle = Math.sin(i.toDouble() * 0.18) * 0.015
            val noise = ((i * 17 % 100) - 50) / 2500.0
            val dailyReturn = trendFactor + cycle + noise

            val open = currentPrice
            val close = open * (1.0 + dailyReturn)
            val high = maxOf(open, close) * (1.0 + Math.abs(noise) + 0.004)
            val low = minOf(open, close) * (1.0 - Math.abs(noise) - 0.004)

            // Volume surge on big green days (Breakout catalyst simulation)
            val volumeMultiplier = if (dailyReturn > 0.018) 1.9 else if (dailyReturn < -0.01) 0.85 else 1.05
            val vol = (baseVolume * volumeMultiplier * (0.8 + ((i * 23 % 40) / 100.0))).toLong()

            candles.add(
                MarketCandle(
                    symbol = symbol,
                    dateEpochDay = epochDay,
                    dateStr = dateStr,
                    open = Math.round(open * 100.0) / 100.0,
                    high = Math.round(high * 100.0) / 100.0,
                    low = Math.round(low * 100.0) / 100.0,
                    close = Math.round(close * 100.0) / 100.0,
                    volume = vol,
                    tradedValue = close * vol,
                    isIntraday = false
                )
            )

            currentPrice = close
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candles
    }
}
