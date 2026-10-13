package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY symbol ASC")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun getCount(): Int
}

@Dao
interface CandleDao {
    @Query("SELECT * FROM candles WHERE symbol = :symbol ORDER BY dateEpochDay ASC")
    fun getCandlesForSymbolFlow(symbol: String): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE symbol = :symbol ORDER BY dateEpochDay ASC")
    suspend fun getCandlesForSymbol(symbol: String): List<CandleEntity>

    @Query("SELECT * FROM candles WHERE symbol = :symbol ORDER BY dateEpochDay DESC LIMIT :limit")
    suspend fun getRecentCandles(symbol: String, limit: Int): List<CandleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Query("DELETE FROM candles WHERE symbol = :symbol")
    suspend fun clearCandlesForSymbol(symbol: String)

    @Query("SELECT COUNT(DISTINCT symbol) FROM candles")
    suspend fun getDistinctSymbolCount(): Int
}

@Dao
interface IndicatorDao {
    @Query("SELECT * FROM indicator_snapshots WHERE symbol = :symbol ORDER BY calculatedAt DESC LIMIT 1")
    suspend fun getLatestSnapshot(symbol: String): IndicatorSnapshotEntity?

    @Query("SELECT * FROM indicator_snapshots ORDER BY calculatedAt DESC")
    fun getAllSnapshots(): Flow<List<IndicatorSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: IndicatorSnapshotEntity)
}

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY scanTimestamp DESC, score DESC")
    fun getAllScanResultsFlow(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE setupType = :setupType ORDER BY scanTimestamp DESC, score DESC")
    fun getScanResultsByTypeFlow(setupType: String): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE symbol = :symbol ORDER BY scanTimestamp DESC")
    fun getScanResultsForSymbol(symbol: String): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanResultById(id: Long): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResults(results: List<ScanResultEntity>): List<Long>

    @Update
    suspend fun updateScanResult(result: ScanResultEntity)

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_records ORDER BY publishedAt DESC")
    fun getAllNewsFlow(): Flow<List<NewsRecordEntity>>

    @Query("SELECT * FROM news_records WHERE category = :category ORDER BY publishedAt DESC")
    fun getNewsByCategoryFlow(category: String): Flow<List<NewsRecordEntity>>

    @Query("SELECT * FROM news_records WHERE linkedSymbolsCsv LIKE '%' || :symbol || '%' ORDER BY publishedAt DESC")
    fun getNewsForSymbolFlow(symbol: String): Flow<List<NewsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNews(news: List<NewsRecordEntity>)

    @Update
    suspend fun updateNews(news: NewsRecordEntity)

    @Query("SELECT COUNT(*) FROM news_records")
    suspend fun getCount(): Int
}

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performance_tracker ORDER BY id DESC")
    fun getAllPerformanceRecordsFlow(): Flow<List<PerformanceTrackerEntity>>

    @Query("SELECT * FROM performance_tracker WHERE symbol = :symbol ORDER BY id DESC")
    fun getRecordsForSymbol(symbol: String): Flow<List<PerformanceTrackerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<PerformanceTrackerEntity>)

    @Update
    suspend fun updateRecord(record: PerformanceTrackerEntity)

    @Query("SELECT * FROM performance_tracker WHERE outcomeStatus = 'TRACKING'")
    suspend fun getActiveTrackingRecords(): List<PerformanceTrackerEntity>
}

@Dao
interface JobLogDao {
    @Query("SELECT * FROM job_execution_logs ORDER BY executedAt DESC LIMIT 50")
    fun getRecentLogsFlow(): Flow<List<JobExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: JobExecutionLogEntity)

    @Query("SELECT * FROM job_execution_logs ORDER BY executedAt DESC LIMIT 1")
    suspend fun getLatestLog(): JobExecutionLogEntity?
}
