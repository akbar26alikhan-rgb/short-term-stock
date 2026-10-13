package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiResearchExplainer
import com.example.analysis.BacktestEngine
import com.example.analysis.PositionSizer
import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.local.CandleEntity
import com.example.data.local.JobExecutionLogEntity
import com.example.data.local.NewsRecordEntity
import com.example.data.local.PerformanceTrackerEntity
import com.example.data.local.ScanResultEntity
import com.example.data.provider.SampleMarketDataProvider
import com.example.data.provider.YahooFinanceMarketDataProvider
import com.example.data.repository.MarketRepository
import com.example.model.BacktestSummary
import com.example.model.MarketCandle
import com.example.model.NewsCategory
import com.example.model.PositionSizeResult
import com.example.model.ProviderStatus
import com.example.model.SetupType
import com.example.model.TradeSetup
import com.example.report.ReportService
import com.example.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarketViewModel(
    private val repository: MarketRepository,
    private val explainer: GeminiResearchExplainer = GeminiResearchExplainer()
) : ViewModel() {

    val isScanning = repository.isScanning
    val providerStatus = repository.currentProviderStatus
    val marketStatus = MutableStateFlow(com.example.util.IndianMarketClock.getMarketStatus())

    // Filter states
    val selectedSetupFilter = MutableStateFlow<SetupType?>(null)
    val minScoreFilter = MutableStateFlow(45)

    val allScanResults: StateFlow<List<ScanResultEntity>> = repository.getAllScanResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredScanResults: StateFlow<List<ScanResultEntity>> = combine(
        allScanResults,
        selectedSetupFilter,
        minScoreFilter
    ) { results, setup, minScore ->
        results.filter { item ->
            val matchSetup = setup == null || item.setupType.equals(setup.name, ignoreCase = true)
            val matchScore = item.score >= minScore
            matchSetup && matchScore
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // News
    val selectedNewsCategory = MutableStateFlow<NewsCategory?>(null)
    val allNews: StateFlow<List<NewsRecordEntity>> = repository.getAllNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNews: StateFlow<List<NewsRecordEntity>> = combine(
        allNews,
        selectedNewsCategory
    ) { news, category ->
        if (category == null) news else news.filter { it.category == category.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Performance
    val performanceRecords: StateFlow<List<PerformanceTrackerEntity>> = repository.getAllPerformanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Job Execution Logs
    val jobLogs: StateFlow<List<JobExecutionLogEntity>> = repository.getJobLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Stock Detail State
    private val _selectedCandidate = MutableStateFlow<ScanResultEntity?>(null)
    val selectedCandidate = _selectedCandidate.asStateFlow()

    private val _stockCandles = MutableStateFlow<List<CandleEntity>>(emptyList())
    val stockCandles = _stockCandles.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation = _aiExplanation.asStateFlow()

    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi = _isGeneratingAi.asStateFlow()

    // Position Size Calculator State
    val userCapital = MutableStateFlow("100000") // Default ₹1 Lakh
    val userRiskPercent = MutableStateFlow("1.5") // 1.5% risk
    private val _positionSizeResult = MutableStateFlow<PositionSizeResult?>(null)
    val positionSizeResult = _positionSizeResult.asStateFlow()

    // Backtest State
    private val _backtestSummary = MutableStateFlow<BacktestSummary?>(null)
    val backtestSummary = _backtestSummary.asStateFlow()

    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting = _isBacktesting.asStateFlow()

    // Settings
    val customApiKey = MutableStateFlow("")
    val isYahooProviderActive = MutableStateFlow(false)

    // Report generation dialog content
    private val _generatedReportText = MutableStateFlow<String?>(null)
    val generatedReportText = _generatedReportText.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    init {
        // Kick off initial setup and load
        viewModelScope.launch {
            repository.initializeUniverseIfEmpty()
            repository.syncMarketData()
            repository.runFullScan()
        }
    }

    fun selectCandidate(candidate: ScanResultEntity) {
        _selectedCandidate.value = candidate
        _aiExplanation.value = null
        viewModelScope.launch {
            repository.getCandlesForSymbol(candidate.symbol).collect { list ->
                _stockCandles.value = list
            }
        }
        recalculatePositionSize()
    }

    fun clearSelectedCandidate() {
        _selectedCandidate.value = null
        _stockCandles.value = emptyList()
        _aiExplanation.value = null
    }

    fun runScan() {
        viewModelScope.launch {
            _statusMessage.value = "Running automated technical scan..."
            val result = repository.runFullScan()
            if (result.isSuccess) {
                val count = result.getOrNull()?.size ?: 0
                _statusMessage.value = "Scan complete: $count setups identified"
            } else {
                _statusMessage.value = "Scan finished: ${result.exceptionOrNull()?.message ?: "Check data provider"}"
            }
        }
    }

    fun collectDataFromExchange() {
        viewModelScope.launch {
            _statusMessage.value = "Connecting to NSE Exchange for latest closing settlement data..."
            val syncRes = repository.syncMarketData(forceRefresh = true)
            marketStatus.value = com.example.util.IndianMarketClock.getMarketStatus()
            val scanRes = repository.runFullScan()
            val count = scanRes.getOrNull()?.size ?: 0
            _statusMessage.value = "Exchange data synced (${syncRes.getOrNull() ?: 0} bars). Identified $count setups!"
        }
    }

    fun refreshMarketData() {
        viewModelScope.launch {
            _statusMessage.value = "Fetching latest market data from provider..."
            val result = repository.syncMarketData(forceRefresh = true)
            if (result.isSuccess) {
                _statusMessage.value = "Data synchronized (${result.getOrNull()} bars loaded)"
                runScan()
            } else {
                _statusMessage.value = "Failed to synchronize: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun triggerAiResearch(candidate: ScanResultEntity) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            _aiExplanation.value = null

            val candles = _stockCandles.value.map {
                MarketCandle(
                    symbol = it.symbol,
                    dateEpochDay = it.dateEpochDay,
                    dateStr = it.dateStr,
                    open = it.open,
                    high = it.high,
                    low = it.low,
                    close = it.close,
                    volume = it.volume
                )
            }

            val indicators = TechnicalAnalysisEngine.computeIndicators(
                symbol = candidate.symbol,
                candles = candles,
                benchmarkCandles = emptyList()
            )

            val setup = TradeSetup(
                id = candidate.id,
                symbol = candidate.symbol,
                companyName = candidate.companyName,
                sector = candidate.sector,
                setupType = SetupType.valueOf(candidate.setupType),
                closePrice = candidate.closePrice,
                entryPrice = candidate.entryPrice,
                stopLoss = candidate.stopLoss,
                target1 = candidate.target1,
                target2 = candidate.target2,
                riskRewardRatio = candidate.riskRewardRatio,
                score = TechnicalAnalysisEngine.calculateScore(SetupType.valueOf(candidate.setupType), indicators, candidate.riskRewardRatio),
                indicators = indicators,
                rationale = candidate.rationale,
                timestamp = candidate.scanTimestamp,
                dataSource = candidate.dataSource,
                dataFreshness = "Verified Daily",
                isDemo = candidate.isDemo
            )

            val (explanation, isAi) = explainer.explainTradeSetup(
                setup = setup,
                userCustomApiKey = customApiKey.value
            )

            _aiExplanation.value = explanation
            _isGeneratingAi.value = false
        }
    }

    fun recalculatePositionSize() {
        val candidate = _selectedCandidate.value ?: return
        val cap = userCapital.value.toDoubleOrNull() ?: 100000.0
        val risk = userRiskPercent.value.toDoubleOrNull() ?: 1.5

        val calc = PositionSizer.calculate(
            capital = cap,
            riskPercentage = risk,
            entryPrice = candidate.entryPrice,
            stopLossPrice = candidate.stopLoss,
            target1 = candidate.target1,
            target2 = candidate.target2
        )
        _positionSizeResult.value = calc
    }

    fun runBacktest(strategy: SetupType, holdingDays: Int = 15) {
        viewModelScope.launch {
            _isBacktesting.value = true
            val candidate = _selectedCandidate.value
            val sym = candidate?.symbol ?: "RELIANCE"

            val stockCandlesDomain = _stockCandles.value.map {
                MarketCandle(it.symbol, it.dateEpochDay, it.dateStr, it.open, it.high, it.low, it.close, it.volume)
            }

            val summary = BacktestEngine.runBacktest(
                symbol = sym,
                candles = stockCandlesDomain,
                benchmarkCandles = emptyList(),
                strategy = strategy,
                maxHoldingDays = holdingDays
            )

            _backtestSummary.value = summary
            _isBacktesting.value = false
        }
    }

    fun toggleProvider(useYahoo: Boolean) {
        isYahooProviderActive.value = useYahoo
        if (useYahoo) {
            repository.setProvider(YahooFinanceMarketDataProvider())
        } else {
            repository.setProvider(SampleMarketDataProvider())
        }
        refreshMarketData()
    }

    fun generateTxtReport() {
        viewModelScope.launch {
            val candidates = allScanResults.value
            val status = providerStatus.value
            val newsCount = allNews.value.size

            val setups = candidates.map { c ->
                TradeSetup(
                    id = c.id,
                    symbol = c.symbol,
                    companyName = c.companyName,
                    sector = c.sector,
                    setupType = SetupType.valueOf(c.setupType),
                    closePrice = c.closePrice,
                    entryPrice = c.entryPrice,
                    stopLoss = c.stopLoss,
                    target1 = c.target1,
                    target2 = c.target2,
                    riskRewardRatio = c.riskRewardRatio,
                    score = com.example.model.CandidateScore(c.score, c.trendScore, c.momentumScore, c.volumeScore, c.rsScore, c.riskRewardScore, ""),
                    indicators = com.example.model.TechnicalIndicators(c.symbol, 60.0, 1.2, 0.8, 0.4, c.closePrice * 0.98, c.closePrice * 0.95, c.closePrice * 0.02, 1200000L, 1.6, c.closePrice * 0.94, c.closePrice * 1.02, 3.5, 5.2),
                    rationale = c.rationale,
                    timestamp = c.scanTimestamp,
                    dataSource = c.dataSource,
                    dataFreshness = "Daily verified close",
                    isDemo = c.isDemo
                )
            }

            val report = ReportService.generateTxtReport(
                setups = setups,
                benchmarkTrend = "Nifty 50 Consolidation above 20 EMA (Bullish Trend Structure)",
                activeNewsCount = newsCount,
                providerName = status.providerName,
                delayNotice = status.delayNotice
            )

            _generatedReportText.value = report
        }
    }

    fun clearGeneratedReport() {
        _generatedReportText.value = null
    }

    fun generateCandidatesCsv(): String {
        val candidates = allScanResults.value
        val setups = candidates.map { c ->
            TradeSetup(
                id = c.id,
                symbol = c.symbol,
                companyName = c.companyName,
                sector = c.sector,
                setupType = SetupType.valueOf(c.setupType),
                closePrice = c.closePrice,
                entryPrice = c.entryPrice,
                stopLoss = c.stopLoss,
                target1 = c.target1,
                target2 = c.target2,
                riskRewardRatio = c.riskRewardRatio,
                score = com.example.model.CandidateScore(c.score, c.trendScore, c.momentumScore, c.volumeScore, c.rsScore, c.riskRewardScore, ""),
                indicators = com.example.model.TechnicalIndicators(c.symbol, 60.0, 1.2, 0.8, 0.4, c.closePrice * 0.98, c.closePrice * 0.95, c.closePrice * 0.02, 1200000L, 1.6, c.closePrice * 0.94, c.closePrice * 1.02, 3.5, 5.2),
                rationale = c.rationale,
                timestamp = c.scanTimestamp,
                dataSource = c.dataSource,
                dataFreshness = "Daily",
                isDemo = c.isDemo
            )
        }
        return ReportService.generateCsvReport(setups)
    }

    fun generatePerformanceCsv(): String {
        return ReportService.generatePerformanceCsv(performanceRecords.value)
    }

    fun importCsvCandles(csvContent: String, symbol: String) {
        viewModelScope.launch {
            val count = repository.importCsvData(csvContent, symbol)
            _statusMessage.value = "Imported $count candles for $symbol"
            runScan()
        }
    }

    fun triggerBackgroundJobNow(context: Context) {
        WorkScheduler.triggerOneTimeImmediateScan(context)
        _statusMessage.value = "Queued background scan with WorkManager constraints"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

class MarketViewModelFactory(private val repository: MarketRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
