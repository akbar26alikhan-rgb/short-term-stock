package com.example.data.repository

import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.local.CandleEntity
import com.example.data.local.IndicatorSnapshotEntity
import com.example.data.local.JobExecutionLogEntity
import com.example.data.local.MarketDatabase
import com.example.data.local.NewsRecordEntity
import com.example.data.local.PerformanceTrackerEntity
import com.example.data.local.ScanResultEntity
import com.example.data.local.StockEntity
import com.example.data.provider.MarketDataProvider
import com.example.data.provider.NewsSourceProvider
import com.example.data.provider.RssNewsProvider
import com.example.data.provider.SampleMarketDataProvider
import com.example.data.provider.YahooFinanceMarketDataProvider
import com.example.model.MarketCandle
import com.example.model.NewsCategory
import com.example.model.ProviderStatus
import com.example.model.SetupType
import com.example.model.TradeSetup
import com.example.report.ReportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MarketRepository(
    private val database: MarketDatabase,
    var marketDataProvider: MarketDataProvider = YahooFinanceMarketDataProvider(),
    var newsProvider: NewsSourceProvider = RssNewsProvider()
) {

    private val backupSampleProvider = SampleMarketDataProvider()

    private val stockDao = database.stockDao()
    private val candleDao = database.candleDao()
    private val indicatorDao = database.indicatorDao()
    private val scanResultDao = database.scanResultDao()
    private val newsDao = database.newsDao()
    private val performanceDao = database.performanceDao()
    private val jobLogDao = database.jobLogDao()

    private val _currentProviderStatus = MutableStateFlow(marketDataProvider.getProviderStatus())
    val currentProviderStatus = _currentProviderStatus.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun getAllScanResults(): Flow<List<ScanResultEntity>> = scanResultDao.getAllScanResultsFlow()

    fun getAllNews(): Flow<List<NewsRecordEntity>> = newsDao.getAllNewsFlow()

    fun getAllPerformanceRecords(): Flow<List<PerformanceTrackerEntity>> = performanceDao.getAllPerformanceRecordsFlow()

    fun getJobLogs(): Flow<List<JobExecutionLogEntity>> = jobLogDao.getRecentLogsFlow()

    fun getCandlesForSymbol(symbol: String): Flow<List<CandleEntity>> = candleDao.getCandlesForSymbolFlow(symbol)

    fun setProvider(provider: MarketDataProvider) {
        marketDataProvider = provider
        _currentProviderStatus.value = provider.getProviderStatus()
    }

    suspend fun initializeUniverseIfEmpty() = withContext(Dispatchers.IO) {
        val count = stockDao.getCount()
        if (count == 0) {
            val universe = listOf(
                StockEntity("RELIANCE", "Reliance Industries Ltd", "Energy & Retail", true, "LARGE"),
                StockEntity("TCS", "Tata Consultancy Services", "IT Services", true, "LARGE"),
                StockEntity("INFY", "Infosys Ltd", "IT Services", true, "LARGE"),
                StockEntity("HDFCBANK", "HDFC Bank Ltd", "Banking & Finance", true, "LARGE"),
                StockEntity("ICICIBANK", "ICICI Bank Ltd", "Banking & Finance", true, "LARGE"),
                StockEntity("SBIN", "State Bank of India", "Banking & Finance", true, "LARGE"),
                StockEntity("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", true, "LARGE"),
                StockEntity("ITC", "ITC Ltd", "FMCG", true, "LARGE"),
                StockEntity("LT", "Larsen & Toubro Ltd", "Capital Goods", true, "LARGE"),
                StockEntity("TATAMOTORS", "Tata Motors Ltd", "Automobile", true, "LARGE"),
                StockEntity("MARUTI", "Maruti Suzuki India", "Automobile", true, "LARGE"),
                StockEntity("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", true, "LARGE"),
                StockEntity("SUNPHARMA", "Sun Pharmaceutical Ind", "Pharma", true, "LARGE"),
                StockEntity("TITAN", "Titan Company Ltd", "Consumer Durables", true, "LARGE"),
                StockEntity("AXISBANK", "Axis Bank Ltd", "Banking & Finance", true, "LARGE"),
                StockEntity("TATASTEEL", "Tata Steel Ltd", "Metals & Mining", true, "LARGE"),
                StockEntity("KOTAKBANK", "Kotak Mahindra Bank", "Banking & Finance", true, "LARGE"),
                StockEntity("ASIANPAINT", "Asian Paints Ltd", "Consumer / Paints", true, "LARGE"),
                StockEntity("HCLTECH", "HCL Technologies Ltd", "IT Services", true, "LARGE"),
                StockEntity("NTPC", "NTPC Ltd", "Power & Energy", true, "LARGE")
            )
            stockDao.insertStocks(universe)
        }
    }

    /**
     * Synchronizes historical market data from active exchange provider for universe + benchmark
     */
    suspend fun syncMarketData(forceRefresh: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            initializeUniverseIfEmpty()
            var totalCandlesLoaded = 0

            val stockSymbols = listOf(
                "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK",
                "SBIN", "BHARTIARTL", "ITC", "LT", "TATAMOTORS",
                "MARUTI", "BAJFINANCE", "SUNPHARMA", "TITAN", "AXISBANK",
                "TATASTEEL", "KOTAKBANK", "ASIANPAINT", "HCLTECH", "NTPC"
            )

            coroutineScope {
                // Fetch Nifty 50 benchmark
                val benchDeferred = async(Dispatchers.IO) {
                    val benchResult = marketDataProvider.fetchBenchmarkCandles(90)
                    if (benchResult.isSuccess && !benchResult.getOrNull().isNullOrEmpty()) {
                        benchResult.getOrNull()!!
                    } else {
                        backupSampleProvider.fetchBenchmarkCandles(90).getOrNull() ?: emptyList()
                    }
                }

                // Fetch Stock Universe in parallel
                val stockDeferreds = stockSymbols.map { sym ->
                    async(Dispatchers.IO) {
                        val res = marketDataProvider.fetchHistoricalDailyCandles(sym, 90)
                        val candles = if (res.isSuccess && !res.getOrNull().isNullOrEmpty()) {
                            res.getOrNull()!!
                        } else {
                            backupSampleProvider.fetchHistoricalDailyCandles(sym, 90).getOrNull() ?: emptyList()
                        }
                        Pair(sym, candles)
                    }
                }

                val benchCandles = benchDeferred.await()
                if (benchCandles.isNotEmpty()) {
                    candleDao.insertCandles(benchCandles.map { it.toEntity("^NSEI") })
                    totalCandlesLoaded += benchCandles.size
                }

                for (def in stockDeferreds) {
                    val (sym, candles) = def.await()
                    if (candles.isNotEmpty()) {
                        candleDao.insertCandles(candles.map { it.toEntity(sym) })
                        totalCandlesLoaded += candles.size
                    }
                }
            }

            _currentProviderStatus.value = marketDataProvider.getProviderStatus()
            Result.success(totalCandlesLoaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Runs full local technical scan across all stocked entities
     */
    suspend fun runFullScan(): Result<List<TradeSetup>> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        val startTime = System.currentTimeMillis()
        try {
            // Ensure data exists
            if (candleDao.getDistinctSymbolCount() < 2) {
                syncMarketData()
            }

            val benchCandleEntities = candleDao.getCandlesForSymbol("^NSEI")
            val benchmarkCandles = benchCandleEntities.map { it.toDomain() }

            val stockSymbols = listOf(
                "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK",
                "SBIN", "BHARTIARTL", "ITC", "LT", "TATAMOTORS",
                "MARUTI", "BAJFINANCE", "SUNPHARMA", "TITAN", "AXISBANK",
                "TATASTEEL", "KOTAKBANK", "ASIANPAINT", "HCLTECH", "NTPC"
            )

            val identifiedSetups = mutableListOf<TradeSetup>()
            val scanEntitiesToInsert = mutableListOf<ScanResultEntity>()

            for (sym in stockSymbols) {
                val stockEntity = stockDao.getStockBySymbol(sym)
                val candleEntities = candleDao.getCandlesForSymbol(sym)
                val candles = candleEntities.map { it.toDomain() }

                if (candles.size >= 15) {
                    val indicators = TechnicalAnalysisEngine.computeIndicators(sym, candles, benchmarkCandles)

                    // Persist indicator snapshot
                    indicatorDao.insertSnapshot(
                        IndicatorSnapshotEntity(
                            symbol = sym,
                            calculatedAt = System.currentTimeMillis(),
                            rsi = indicators.rsi,
                            macd = indicators.macd,
                            macdSignal = indicators.macdSignal,
                            macdHist = indicators.macdHist,
                            ema20 = indicators.ema20,
                            ema50 = indicators.ema50,
                            atr = indicators.atr,
                            avgVolume20 = indicators.avgVolume20,
                            rvol = indicators.rvol,
                            support20 = indicators.support20,
                            resistance20 = indicators.resistance20,
                            rsNifty50_20d = indicators.rsNifty50_20d,
                            rsNifty50_50d = indicators.rsNifty50_50d
                        )
                    )

                    val candidate = TechnicalAnalysisEngine.evaluateCandidate(
                        symbol = sym,
                        companyName = stockEntity?.name ?: sym,
                        sector = stockEntity?.sector ?: "NSE Equity",
                        candles = candles,
                        indicators = indicators,
                        dataSource = marketDataProvider.name,
                        isDemo = marketDataProvider is SampleMarketDataProvider
                    )

                    if (candidate != null) {
                        identifiedSetups.add(candidate)
                        scanEntitiesToInsert.add(
                            ScanResultEntity(
                                scanTimestamp = candidate.timestamp,
                                symbol = candidate.symbol,
                                companyName = candidate.companyName,
                                sector = candidate.sector,
                                setupType = candidate.setupType.name,
                                score = candidate.score.compositeScore,
                                trendScore = candidate.score.trendScore,
                                momentumScore = candidate.score.momentumScore,
                                volumeScore = candidate.score.volumeScore,
                                rsScore = candidate.score.rsScore,
                                riskRewardScore = candidate.score.riskRewardScore,
                                closePrice = candidate.closePrice,
                                entryPrice = candidate.entryPrice,
                                stopLoss = candidate.stopLoss,
                                target1 = candidate.target1,
                                target2 = candidate.target2,
                                riskRewardRatio = candidate.riskRewardRatio,
                                rationale = candidate.rationale,
                                dataSource = candidate.dataSource,
                                isDemo = candidate.isDemo,
                                status = "ACTIVE"
                            )
                        )
                    }
                }
            }

            // Sort setups by highest conviction score first
            identifiedSetups.sortByDescending { it.score.compositeScore }
            scanEntitiesToInsert.sortByDescending { it.score }

            // Save new scan results to database (replace previous)
            scanResultDao.clearAll()
            if (scanEntitiesToInsert.isNotEmpty()) {
                val insertedIds = scanResultDao.insertScanResults(scanEntitiesToInsert)

                // Initialize performance tracking entries
                val perfRecords = mutableListOf<PerformanceTrackerEntity>()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(System.currentTimeMillis())

                for ((idx, entity) in scanEntitiesToInsert.withIndex()) {
                    val scanId = insertedIds.getOrElse(idx) { 0L }
                    perfRecords.add(
                        PerformanceTrackerEntity(
                            scanId = scanId,
                            symbol = entity.symbol,
                            setupType = entity.setupType,
                            entryDate = todayStr,
                            entryPrice = entity.entryPrice,
                            stopLoss = entity.stopLoss,
                            target1 = entity.target1,
                            target2 = entity.target2,
                            outcomeStatus = "TRACKING"
                        )
                    )
                }
                performanceDao.insertRecords(perfRecords)
            }

            // Sync news research as well
            fetchNewsArticles()

            // Update performance tracking against available historical bars
            evaluateTrackedPerformance()

            // Log job execution
            val duration = System.currentTimeMillis() - startTime
            jobLogDao.insertLog(
                JobExecutionLogEntity(
                    jobName = "Market Research & Setup Scan",
                    triggerType = "MANUAL",
                    executedAt = System.currentTimeMillis(),
                    durationMs = duration,
                    status = "SUCCESS",
                    itemsProcessed = stockSymbols.size,
                    details = "Identified ${identifiedSetups.size} swing setups across ${stockSymbols.size} symbols"
                )
            )

            _isScanning.value = false
            Result.success(identifiedSetups)
        } catch (e: Exception) {
            _isScanning.value = false
            val duration = System.currentTimeMillis() - startTime
            jobLogDao.insertLog(
                JobExecutionLogEntity(
                    jobName = "Market Research & Setup Scan",
                    triggerType = "MANUAL",
                    executedAt = System.currentTimeMillis(),
                    durationMs = duration,
                    status = "FAILURE",
                    itemsProcessed = 0,
                    details = "Error during scanner run",
                    errorMessage = e.message
                )
            )
            Result.failure(e)
        }
    }

    suspend fun fetchNewsArticles(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val symbols = listOf("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN", "BHARTIARTL", "ITC", "LT", "TATAMOTORS")
            val newsResult = newsProvider.fetchNews(symbols)
            if (newsResult.isSuccess) {
                val articles = newsResult.getOrNull() ?: emptyList()
                val entities = articles.map {
                    NewsRecordEntity(
                        id = it.id,
                        title = it.title,
                        publisher = it.publisher,
                        publishedAt = it.publishedAt,
                        publishedAtFormatted = it.publishedAtFormatted,
                        retrievedAt = it.retrievalTime,
                        sourceUrl = it.sourceUrl,
                        linkedSymbolsCsv = it.linkedSymbols.joinToString(","),
                        category = it.category.name,
                        confidence = it.confidence,
                        aiSummary = it.aiSummary,
                        isUnverified = it.isUnverified
                    )
                }
                newsDao.insertNews(entities)
                Result.success(entities.size)
            } else {
                Result.failure(newsResult.exceptionOrNull() ?: Exception("Failed to fetch news"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluates subsequent stock performance for tracked setups across 1, 3, 5, 10, 20, 60, 120 sessions
     */
    suspend fun evaluateTrackedPerformance() = withContext(Dispatchers.IO) {
        val activeTracking = performanceDao.getActiveTrackingRecords()
        for (item in activeTracking) {
            val candles = candleDao.getCandlesForSymbol(item.symbol)
            if (candles.isEmpty()) continue

            val entryIdx = candles.indexOfFirst { it.dateStr >= item.entryDate }
            if (entryIdx == -1) continue

            val futureCandles = candles.drop(entryIdx + 1)
            if (futureCandles.isEmpty()) continue

            val pnl1 = futureCandles.getOrNull(0)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl3 = futureCandles.getOrNull(2)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl5 = futureCandles.getOrNull(4)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl10 = futureCandles.getOrNull(9)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl20 = futureCandles.getOrNull(19)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl60 = futureCandles.getOrNull(59)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }
            val pnl120 = futureCandles.getOrNull(119)?.let { ((it.close - item.entryPrice) / item.entryPrice) * 100.0 }

            val maxHigh = futureCandles.maxOfOrNull { it.high } ?: item.entryPrice
            val minLow = futureCandles.minOfOrNull { it.low } ?: item.entryPrice

            val maxFavExcursion = ((maxHigh - item.entryPrice) / item.entryPrice) * 100.0
            val maxAdvExcursion = ((minLow - item.entryPrice) / item.entryPrice) * 100.0

            var outcome = item.outcomeStatus
            if (minLow <= item.stopLoss) {
                outcome = "STOP_LOSS_HIT"
            } else if (maxHigh >= item.target1) {
                outcome = "PROFIT_TARGET_HIT"
            } else if (futureCandles.size >= 20) {
                outcome = "TIMEOUT"
            }

            val updated = item.copy(
                pnl1d = pnl1 ?: item.pnl1d,
                pnl3d = pnl3 ?: item.pnl3d,
                pnl5d = pnl5 ?: item.pnl5d,
                pnl10d = pnl10 ?: item.pnl10d,
                pnl20d = pnl20 ?: item.pnl20d,
                pnl60d = pnl60 ?: item.pnl60d,
                pnl120d = pnl120 ?: item.pnl120d,
                maxFavorableExcursion = maxFavExcursion,
                maxAdverseExcursion = maxAdvExcursion,
                outcomeStatus = outcome,
                lastEvaluatedAt = System.currentTimeMillis()
            )
            performanceDao.updateRecord(updated)
        }
    }

    suspend fun importCsvData(content: String, defaultSymbol: String): Int = withContext(Dispatchers.IO) {
        val parsedCandles = ReportService.parseCsvCandles(content, defaultSymbol)
        if (parsedCandles.isNotEmpty()) {
            candleDao.insertCandles(parsedCandles)
        }
        parsedCandles.size
    }

    private fun MarketCandle.toEntity(symbolName: String): CandleEntity {
        return CandleEntity(
            symbol = symbolName,
            dateEpochDay = this.dateEpochDay,
            dateStr = this.dateStr,
            open = this.open,
            high = this.high,
            low = this.low,
            close = this.close,
            volume = this.volume,
            tradedValue = this.tradedValue,
            isIntraday = this.isIntraday
        )
    }

    private fun CandleEntity.toDomain(): MarketCandle {
        return MarketCandle(
            symbol = this.symbol,
            dateEpochDay = this.dateEpochDay,
            dateStr = this.dateStr,
            open = this.open,
            high = this.high,
            low = this.low,
            close = this.close,
            volume = this.volume,
            tradedValue = this.tradedValue,
            isIntraday = this.isIntraday
        )
    }
}
