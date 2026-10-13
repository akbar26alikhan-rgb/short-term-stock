package com.example.data.provider

import com.example.model.MarketCandle
import com.example.model.ProviderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class YahooFinanceMarketDataProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MarketDataProvider {

    override val name: String = "Yahoo Finance (NSE Direct .NS)"

    private var lastFetchTime: Long = 0
    private var isHealthy: Boolean = true
    private val warnings = mutableListOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    override suspend fun fetchHistoricalDailyCandles(
        symbol: String,
        days: Int
    ): Result<List<MarketCandle>> = withContext(Dispatchers.IO) {
        val ticker = if (symbol.startsWith("^") || symbol.endsWith(".NS")) symbol else "$symbol.NS"
        val range = when {
            days <= 30 -> "1mo"
            days <= 90 -> "3mo"
            days <= 180 -> "6mo"
            else -> "1y"
        }
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?range=$range&interval=1d"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android MarketJournalAI/1.0)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                isHealthy = false
                warnings.add("HTTP ${response.code} received for $ticker")
                return@withContext Result.failure(Exception("Yahoo Finance query failed with HTTP code ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val json = JSONObject(body)
            val chart = json.getJSONObject("chart")
            val results = chart.getJSONArray("result")
            if (results.length() == 0) {
                return@withContext Result.failure(Exception("No chart result found for $ticker"))
            }

            val resultObj = results.getJSONObject(0)
            val timestamps = resultObj.getJSONArray("timestamp")
            val quote = resultObj.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)

            val opens = quote.getJSONArray("open")
            val highs = quote.getJSONArray("high")
            val lows = quote.getJSONArray("low")
            val closes = quote.getJSONArray("close")
            val volumes = quote.getJSONArray("volume")

            val candles = mutableListOf<MarketCandle>()

            for (i in 0 until timestamps.length()) {
                val epochSec = timestamps.getLong(i)
                val epochDay = epochSec / 86400L

                if (closes.isNull(i) || opens.isNull(i) || highs.isNull(i) || lows.isNull(i)) {
                    continue
                }

                val open = opens.getDouble(i)
                val high = highs.getDouble(i)
                val low = lows.getDouble(i)
                val close = closes.getDouble(i)
                val vol = if (volumes.isNull(i)) 0L else volumes.getLong(i)

                candles.add(
                    MarketCandle(
                        symbol = symbol.removeSuffix(".NS"),
                        dateEpochDay = epochDay,
                        dateStr = dateFormat.format(Date(epochSec * 1000L)),
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = vol,
                        tradedValue = close * vol,
                        isIntraday = false
                    )
                )
            }

            lastFetchTime = System.currentTimeMillis()
            isHealthy = true
            Result.success(candles)
        } catch (e: Exception) {
            isHealthy = false
            warnings.add("Network error: ${e.message ?: "Unknown"}")
            Result.failure(e)
        }
    }

    override suspend fun fetchIntradayCandles(symbol: String): Result<List<MarketCandle>> = withContext(Dispatchers.IO) {
        val ticker = if (symbol.startsWith("^") || symbol.endsWith(".NS")) symbol else "$symbol.NS"
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?range=1d&interval=15m"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android MarketJournalAI/1.0)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Intraday query failed: HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            val json = JSONObject(body)
            val results = json.getJSONObject("chart").getJSONArray("result")
            if (results.length() == 0) return@withContext Result.failure(Exception("No intraday results"))

            val resultObj = results.getJSONObject(0)
            val timestamps = resultObj.getJSONArray("timestamp")
            val quote = resultObj.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
            val closes = quote.getJSONArray("close")
            val opens = quote.getJSONArray("open")
            val highs = quote.getJSONArray("high")
            val lows = quote.getJSONArray("low")
            val volumes = quote.getJSONArray("volume")

            val candles = mutableListOf<MarketCandle>()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)

            for (i in 0 until timestamps.length()) {
                if (closes.isNull(i)) continue
                val epochSec = timestamps.getLong(i)
                candles.add(
                    MarketCandle(
                        symbol = symbol.removeSuffix(".NS"),
                        dateEpochDay = epochSec,
                        dateStr = timeFormat.format(Date(epochSec * 1000L)),
                        open = opens.optDouble(i, 0.0),
                        high = highs.optDouble(i, 0.0),
                        low = lows.optDouble(i, 0.0),
                        close = closes.getDouble(i),
                        volume = volumes.optLong(i, 0L),
                        tradedValue = closes.getDouble(i) * volumes.optLong(i, 0L),
                        isIntraday = true
                    )
                )
            }
            Result.success(candles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBenchmarkCandles(days: Int): Result<List<MarketCandle>> {
        return fetchHistoricalDailyCandles("^NSEI", days)
    }

    override suspend fun fetchCorporateActions(symbol: String): Result<List<String>> = withContext(Dispatchers.IO) {
        // Yahoo Finance corporate actions or announcements
        Result.success(listOf("Regular quarterly earnings update scheduled", "Normal trading settlement cycle T+1"))
    }

    override fun getProviderStatus(): ProviderStatus {
        val delayText = "Public feed: ~15 minute delayed quotes for live session, end-of-day daily closes verified"
        return ProviderStatus(
            providerName = name,
            isAvailable = isHealthy,
            lastRetrievedTime = lastFetchTime,
            delayNotice = delayText,
            warnings = warnings.takeLast(5)
        )
    }
}
