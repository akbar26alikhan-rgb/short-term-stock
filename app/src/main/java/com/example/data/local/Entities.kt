package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val sector: String,
    val isNifty50: Boolean,
    val marketCapTier: String, // "LARGE", "MID", "SMALL"
    val minLiquidityMet: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "candles",
    indices = [
        Index(value = ["symbol", "dateEpochDay"], unique = true),
        Index(value = ["symbol"])
    ]
)
data class CandleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val dateEpochDay: Long,
    val dateStr: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val tradedValue: Double,
    val isIntraday: Boolean = false
)

@Entity(
    tableName = "indicator_snapshots",
    indices = [Index(value = ["symbol", "calculatedAt"])]
)
data class IndicatorSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val calculatedAt: Long,
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

@Entity(
    tableName = "scan_results",
    indices = [Index(value = ["scanTimestamp"]), Index(value = ["symbol"])]
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanTimestamp: Long,
    val symbol: String,
    val companyName: String,
    val sector: String,
    val setupType: String, // "BREAKOUT", "PULLBACK", "MOMENTUM"
    val score: Int,
    val trendScore: Int,
    val momentumScore: Int,
    val volumeScore: Int,
    val rsScore: Int,
    val riskRewardScore: Int,
    val closePrice: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskRewardRatio: Double,
    val rationale: String,
    val dataSource: String,
    val isDemo: Boolean = false,
    val status: String = "ACTIVE" // "ACTIVE", "TARGET_REACHED", "STOP_TRIGGERED", "EXPIRED"
)

@Entity(tableName = "news_records")
data class NewsRecordEntity(
    @PrimaryKey val id: String,
    val title: String,
    val publisher: String,
    val publishedAt: Long,
    val publishedAtFormatted: String,
    val retrievedAt: Long,
    val sourceUrl: String,
    val linkedSymbolsCsv: String,
    val category: String,
    val confidence: Double,
    val aiSummary: String? = null,
    val isUnverified: Boolean = false
)

@Entity(
    tableName = "performance_tracker",
    indices = [Index(value = ["scanId"]), Index(value = ["symbol"])]
)
data class PerformanceTrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val symbol: String,
    val setupType: String,
    val entryDate: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val pnl1d: Double? = null,
    val pnl3d: Double? = null,
    val pnl5d: Double? = null,
    val pnl10d: Double? = null,
    val pnl20d: Double? = null,
    val pnl60d: Double? = null,
    val pnl120d: Double? = null,
    val maxFavorableExcursion: Double? = null,
    val maxAdverseExcursion: Double? = null,
    val outcomeStatus: String = "TRACKING", // "TRACKING", "PROFIT_TARGET_HIT", "STOP_LOSS_HIT", "TIMEOUT"
    val lastEvaluatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "job_execution_logs")
data class JobExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobName: String,
    val triggerType: String, // "SCHEDULED", "MANUAL"
    val executedAt: Long,
    val durationMs: Long,
    val status: String, // "SUCCESS", "FAILURE", "SKIPPED_HOLIDAY"
    val itemsProcessed: Int,
    val details: String,
    val errorMessage: String? = null
)
