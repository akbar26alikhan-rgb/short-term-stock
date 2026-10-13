package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StockEntity::class,
        CandleEntity::class,
        IndicatorSnapshotEntity::class,
        ScanResultEntity::class,
        NewsRecordEntity::class,
        PerformanceTrackerEntity::class,
        JobExecutionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun candleDao(): CandleDao
    abstract fun indicatorDao(): IndicatorDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun newsDao(): NewsDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun jobLogDao(): JobLogDao

    companion object {
        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getInstance(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketDatabase::class.java,
                    "market_journal_ai.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
