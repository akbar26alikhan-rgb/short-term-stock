package com.example.report

import com.example.data.local.CandleEntity
import com.example.data.local.PerformanceTrackerEntity
import com.example.data.local.ScanResultEntity
import com.example.model.TradeSetup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportService {

    private val reportDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss z", Locale.ENGLISH)
    private val candleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    /**
     * Generates a comprehensive, human-readable TXT Market Research Report
     */
    fun generateTxtReport(
        setups: List<TradeSetup>,
        benchmarkTrend: String,
        activeNewsCount: Int,
        providerName: String,
        delayNotice: String
    ): String {
        val now = reportDateFormat.format(Date())
        val sb = StringBuilder()

        sb.appendLine("================================================================================")
        sb.appendLine("                         MARKET JOURNAL AI - RESEARCH REPORT                    ")
        sb.appendLine("                 Automated Indian Stock Market Swing Trading Analysis           ")
        sb.appendLine("================================================================================")
        sb.appendLine("Generated At    : $now")
        sb.appendLine("Data Provider   : $providerName")
        sb.appendLine("Data Freshness  : $delayNotice")
        sb.appendLine("Nifty 50 Trend  : $benchmarkTrend")
        sb.appendLine("Active Catalysts: $activeNewsCount news records reviewed")
        sb.appendLine("Total Setups    : ${setups.size} candidates identified")
        sb.appendLine("================================================================================")
        sb.appendLine()

        if (setups.isEmpty()) {
            sb.appendLine("No candidates currently satisfy the minimum liquidity, trend, or scoring criteria.")
            sb.appendLine("Consider running a scan after market close or checking data provider connectivity.")
        } else {
            val breakouts = setups.filter { it.setupType == com.example.model.SetupType.BREAKOUT }
            val pullbacks = setups.filter { it.setupType == com.example.model.SetupType.PULLBACK }
            val momentums = setups.filter { it.setupType == com.example.model.SetupType.MOMENTUM }

            sb.appendLine("--- STRATEGY SUMMARY ---")
            sb.appendLine("Breakout Candidates : ${breakouts.size}")
            sb.appendLine("Pullback Candidates : ${pullbacks.size}")
            sb.appendLine("Momentum Candidates : ${momentums.size}")
            sb.appendLine()

            sb.appendLine("================================================================================")
            sb.appendLine("                              CANDIDATE DETAILS                                 ")
            sb.appendLine("================================================================================")

            for ((index, s) in setups.withIndex()) {
                val ind = s.indicators
                sb.appendLine("[#${index + 1}] ${s.symbol} - ${s.companyName} (${s.sector})")
                sb.appendLine("  Setup Type   : ${s.setupType.label}")
                sb.appendLine("  Composite Score: ${s.score.compositeScore}/100 (Trend: ${s.score.trendScore}/25, Mom: ${s.score.momentumScore}/20, Vol: ${s.score.volumeScore}/20, RS: ${s.score.rsScore}/20, RR: ${s.score.riskRewardScore}/15)")
                sb.appendLine("  Current Close: ₹${String.format("%.2f", s.closePrice)}")
                sb.appendLine("  Entry Level  : ₹${String.format("%.2f", s.entryPrice)}")
                sb.appendLine("  Stop-Loss    : ₹${String.format("%.2f", s.stopLoss)} (${String.format("%.2f", ((s.closePrice - s.stopLoss) / s.closePrice) * 100)}% risk)")
                sb.appendLine("  Target 1     : ₹${String.format("%.2f", s.target1)} (1.6R, +${String.format("%.2f", ((s.target1 - s.closePrice) / s.closePrice) * 100)}%)")
                sb.appendLine("  Target 2     : ₹${String.format("%.2f", s.target2)} (2.6R, +${String.format("%.2f", ((s.target2 - s.closePrice) / s.closePrice) * 100)}%)")
                sb.appendLine("  Risk-to-Reward: ${String.format("%.2f", s.riskRewardRatio)} : 1")
                sb.appendLine("  Indicators   : RSI=${String.format("%.1f", ind.rsi)} | RVOL=${String.format("%.2f", ind.rvol)}x | 20 EMA=₹${String.format("%.2f", ind.ema20)} | 50 EMA=₹${String.format("%.2f", ind.ema50)}")
                sb.appendLine("  Key Levels   : 20d High (Res)=₹${String.format("%.2f", ind.resistance20)} | 20d Low (Sup)=₹${String.format("%.2f", ind.support20)}")
                sb.appendLine("  Rel. Strength: ${String.format("%+.2f", ind.rsNifty50_20d)}% vs Nifty 50 (20 sessions)")
                sb.appendLine("  Data Source  : ${s.dataSource} (${s.dataFreshness})")
                sb.appendLine("  Rationale    : ${s.rationale}")
                sb.appendLine("--------------------------------------------------------------------------------")
            }
        }

        sb.appendLine()
        sb.appendLine("================================================================================")
        sb.appendLine("                       STATUTORY DISCLAIMER & RISK POLICY                      ")
        sb.appendLine("================================================================================")
        sb.appendLine("• FOR RESEARCH AND EDUCATIONAL PURPOSES ONLY. NOT AN INVESTMENT RECOMMENDATION.")
        sb.appendLine("• The composite scores and targets are mathematical models, NOT a probability")
        sb.appendLine("  of profit. Past market performance does not guarantee future results.")
        sb.appendLine("• Swing trading equities involves capital risk. Always use strict stop-losses.")
        sb.appendLine("• Market Journal AI does not connect to any brokerage or execute live trades.")
        sb.appendLine("================================================================================")

        return sb.toString()
    }

    /**
     * Generates CSV format for candidate export
     */
    fun generateCsvReport(setups: List<TradeSetup>): String {
        val sb = StringBuilder()
        sb.appendLine("Symbol,CompanyName,Sector,SetupType,Score,ClosePrice,EntryPrice,StopLoss,Target1,Target2,RiskRewardRatio,RSI,RVOL,EMA20,EMA50,Support20,Resistance20,RS_Nifty50_20d,DataSource,Timestamp")

        for (s in setups) {
            val ind = s.indicators
            sb.append("${s.symbol},")
            sb.append("\"${s.companyName}\",")
            sb.append("\"${s.sector}\",")
            sb.append("${s.setupType.name},")
            sb.append("${s.score.compositeScore},")
            sb.append("${s.closePrice},")
            sb.append("${s.entryPrice},")
            sb.append("${s.stopLoss},")
            sb.append("${s.target1},")
            sb.append("${s.target2},")
            sb.append("${s.riskRewardRatio},")
            sb.append("${ind.rsi},")
            sb.append("${ind.rvol},")
            sb.append("${ind.ema20},")
            sb.append("${ind.ema50},")
            sb.append("${ind.support20},")
            sb.append("${ind.resistance20},")
            sb.append("${ind.rsNifty50_20d},")
            sb.append("\"${s.dataSource}\",")
            sb.append("${s.timestamp}\n")
        }

        return sb.toString()
    }

    /**
     * Generates CSV format for performance tracker
     */
    fun generatePerformanceCsv(records: List<PerformanceTrackerEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("ScanId,Symbol,SetupType,EntryDate,EntryPrice,StopLoss,Target1,Target2,PnL_T1,PnL_T3,PnL_T5,PnL_T10,PnL_T20,PnL_T60,PnL_T120,MaxGain,MaxLoss,OutcomeStatus")

        for (r in records) {
            sb.append("${r.scanId},")
            sb.append("${r.symbol},")
            sb.append("${r.setupType},")
            sb.append("${r.entryDate},")
            sb.append("${r.entryPrice},")
            sb.append("${r.stopLoss},")
            sb.append("${r.target1},")
            sb.append("${r.target2},")
            sb.append("${r.pnl1d ?: ""},")
            sb.append("${r.pnl3d ?: ""},")
            sb.append("${r.pnl5d ?: ""},")
            sb.append("${r.pnl10d ?: ""},")
            sb.append("${r.pnl20d ?: ""},")
            sb.append("${r.pnl60d ?: ""},")
            sb.append("${r.pnl120d ?: ""},")
            sb.append("${r.maxFavorableExcursion ?: ""},")
            sb.append("${r.maxAdverseExcursion ?: ""},")
            sb.append("${r.outcomeStatus}\n")
        }

        return sb.toString()
    }

    /**
     * Parses user-imported CSV data (Bhavcopy or standard OHLCV format)
     * Format 1: Symbol,Date,Open,High,Low,Close,Volume
     * Format 2: Date,Open,High,Low,Close,Volume (with symbol passed)
     */
    fun parseCsvCandles(csvContent: String, defaultSymbol: String = "CUSTOM"): List<CandleEntity> {
        val lines = csvContent.lines()
        val result = mutableListOf<CandleEntity>()

        for ((idx, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isBlank() || idx == 0 && (line.contains("Date", ignoreCase = true) || line.contains("Symbol", ignoreCase = true))) {
                continue
            }

            val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
            try {
                if (tokens.size >= 7) {
                    // Symbol,Date,Open,High,Low,Close,Volume
                    val symbol = tokens[0].uppercase()
                    val dateStr = tokens[1]
                    val open = tokens[2].toDouble()
                    val high = tokens[3].toDouble()
                    val low = tokens[4].toDouble()
                    val close = tokens[5].toDouble()
                    val volume = tokens[6].toLong()

                    val epochDay = try {
                        candleDateFormat.parse(dateStr)?.time?.div(86400000L) ?: System.currentTimeMillis() / 86400000L
                    } catch (e: Exception) {
                        System.currentTimeMillis() / 86400000L
                    }

                    result.add(
                        CandleEntity(
                            symbol = symbol,
                            dateEpochDay = epochDay,
                            dateStr = dateStr,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume,
                            tradedValue = close * volume,
                            isIntraday = false
                        )
                    )
                } else if (tokens.size >= 6) {
                    // Date,Open,High,Low,Close,Volume
                    val dateStr = tokens[0]
                    val open = tokens[1].toDouble()
                    val high = tokens[2].toDouble()
                    val low = tokens[3].toDouble()
                    val close = tokens[4].toDouble()
                    val volume = tokens[5].toLong()

                    val epochDay = try {
                        candleDateFormat.parse(dateStr)?.time?.div(86400000L) ?: System.currentTimeMillis() / 86400000L
                    } catch (e: Exception) {
                        System.currentTimeMillis() / 86400000L
                    }

                    result.add(
                        CandleEntity(
                            symbol = defaultSymbol,
                            dateEpochDay = epochDay,
                            dateStr = dateStr,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume,
                            tradedValue = close * volume,
                            isIntraday = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore malformed rows
            }
        }

        return result
    }
}
