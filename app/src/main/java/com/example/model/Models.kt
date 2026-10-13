package com.example.model

enum class SetupType(val label: String) {
    BREAKOUT("20-Day Breakout"),
    PULLBACK("EMA Pullback"),
    MOMENTUM("Trend Momentum")
}

enum class NewsCategory(val label: String) {
    RESULTS("Financial Results"),
    CORPORATE_DEVELOPMENTS("Corporate Development"),
    MANAGEMENT_CHANGES("Management Change"),
    REGULATORY("Regulatory / Legal"),
    NEGATIVE_NEWS("High Risk / Negative"),
    GENERAL("General Market")
}

data class MarketCandle(
    val symbol: String,
    val dateEpochDay: Long, // Epoch day for daily candles or epoch millis
    val dateStr: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val tradedValue: Double = close * volume,
    val isIntraday: Boolean = false
)

data class TechnicalIndicators(
    val symbol: String,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val macdHist: Double,
    val ema20: Double,
    val ema50: Double,
    val atr: Double,
    val avgVolume20: Long,
    val rvol: Double,
    val support20: Double,
    val resistance20: Double,
    val rsNifty50_20d: Double,
    val rsNifty50_50d: Double
)

data class CandidateScore(
    val compositeScore: Int, // 0 to 100
    val trendScore: Int,      // 0 to 25
    val momentumScore: Int,   // 0 to 20
    val volumeScore: Int,     // 0 to 20
    val rsScore: Int,         // 0 to 20
    val riskRewardScore: Int, // 0 to 15
    val notes: String
)

data class TradeSetup(
    val id: Long = 0,
    val symbol: String,
    val companyName: String,
    val sector: String,
    val setupType: SetupType,
    val closePrice: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskRewardRatio: Double,
    val score: CandidateScore,
    val indicators: TechnicalIndicators,
    val rationale: String,
    val timestamp: Long,
    val dataSource: String,
    val dataFreshness: String,
    val isDemo: Boolean = false
)

data class NewsArticle(
    val id: String,
    val title: String,
    val publisher: String,
    val publishedAt: Long,
    val publishedAtFormatted: String,
    val retrievalTime: Long,
    val sourceUrl: String,
    val linkedSymbols: List<String>,
    val category: NewsCategory,
    val confidence: Double, // 0.0 to 1.0
    val aiSummary: String? = null,
    val isUnverified: Boolean = false
)

data class ProviderStatus(
    val providerName: String,
    val isAvailable: Boolean,
    val lastRetrievedTime: Long,
    val delayNotice: String,
    val warnings: List<String>
)

data class PositionSizeResult(
    val capital: Double,
    val riskPercentage: Double,
    val riskAmount: Double,
    val entryPrice: Double,
    val stopLossPrice: Double,
    val riskPerShare: Double,
    val recommendedShares: Int,
    val totalRequiredCapital: Double,
    val capitalUtilizationPercent: Double,
    val target1: Double,
    val target2: Double,
    val potentialProfitTarget1: Double,
    val potentialProfitTarget2: Double,
    val riskToReward1: Double,
    val riskToReward2: Double
)

data class BacktestTrade(
    val symbol: String,
    val entryDate: String,
    val exitDate: String,
    val setupType: SetupType,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnlPercent: Double,
    val netPnlAfterCosts: Double,
    val outcome: String, // "TARGET_1", "TARGET_2", "STOP_LOSS", "TIME_STOP"
    val holdingPeriodDays: Int
)

data class BacktestSummary(
    val setupType: SetupType,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val profitFactor: Double,
    val totalReturnPercent: Double,
    val maxDrawdownPercent: Double,
    val estimatedTaxesAndSlippage: Double,
    val trades: List<BacktestTrade>
)
