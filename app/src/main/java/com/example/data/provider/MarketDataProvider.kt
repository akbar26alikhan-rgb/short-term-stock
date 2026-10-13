package com.example.data.provider

import com.example.model.MarketCandle
import com.example.model.ProviderStatus

interface MarketDataProvider {
    val name: String
    suspend fun fetchHistoricalDailyCandles(symbol: String, days: Int = 90): Result<List<MarketCandle>>
    suspend fun fetchIntradayCandles(symbol: String): Result<List<MarketCandle>>
    suspend fun fetchBenchmarkCandles(days: Int = 90): Result<List<MarketCandle>> // Nifty 50 (^NSEI)
    suspend fun fetchCorporateActions(symbol: String): Result<List<String>>
    fun getProviderStatus(): ProviderStatus
}
