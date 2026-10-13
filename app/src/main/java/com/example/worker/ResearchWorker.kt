package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.JobExecutionLogEntity
import com.example.data.local.MarketDatabase
import com.example.data.repository.MarketRepository
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ResearchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = MarketDatabase.getInstance(applicationContext)
        val jobLogDao = database.jobLogDao()
        val startTime = System.currentTimeMillis()

        // 1. Check Indian Market Schedule & Holidays
        val kolkataTz = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(kolkataTz)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            jobLogDao.insertLog(
                JobExecutionLogEntity(
                    jobName = "Scheduled Indian Market Scan",
                    triggerType = "SCHEDULED",
                    executedAt = startTime,
                    durationMs = 50,
                    status = "SKIPPED_HOLIDAY",
                    itemsProcessed = 0,
                    details = "Weekend in Asia/Kolkata timezone. Markets closed."
                )
            )
            return Result.success()
        }

        return try {
            val repository = MarketRepository(database)
            val scanResult = repository.runFullScan()

            val setups = scanResult.getOrNull() ?: emptyList()
            val duration = System.currentTimeMillis() - startTime

            if (setups.isNotEmpty()) {
                val topCandidates = setups.take(3).joinToString(", ") { "${it.symbol} (${it.score.compositeScore})" }
                NotificationHelper.showScanCompleteNotification(
                    context = applicationContext,
                    title = "Market Journal AI: Scan Complete",
                    message = "${setups.size} swing setups identified. Top candidates: $topCandidates"
                )
            } else {
                NotificationHelper.showScanCompleteNotification(
                    context = applicationContext,
                    title = "Market Journal AI: Research Update",
                    message = "Market scan complete. 0 setups met strict filter criteria."
                )
            }

            jobLogDao.insertLog(
                JobExecutionLogEntity(
                    jobName = "Scheduled Indian Market Scan",
                    triggerType = "SCHEDULED",
                    executedAt = startTime,
                    durationMs = duration,
                    status = "SUCCESS",
                    itemsProcessed = setups.size,
                    details = "Completed scan. Found ${setups.size} setups."
                )
            )

            Result.success()
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            jobLogDao.insertLog(
                JobExecutionLogEntity(
                    jobName = "Scheduled Indian Market Scan",
                    triggerType = "SCHEDULED",
                    executedAt = startTime,
                    durationMs = duration,
                    status = "FAILURE",
                    itemsProcessed = 0,
                    details = "Worker execution exception",
                    errorMessage = e.message
                )
            )
            Result.retry()
        }
    }
}

object WorkScheduler {

    private const val PERIODIC_WORK_TAG = "market_journal_periodic_research"

    fun schedulePeriodicMarketScans(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Periodic work runs every 4 hours (WorkManager minimum is 15 minutes, 4 hours is optimal for swing research)
        val periodicRequest = PeriodicWorkRequestBuilder<ResearchWorker>(
            repeatInterval = 4,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerOneTimeImmediateScan(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ResearchWorker>()
            .setConstraints(constraints)
            .addTag("immediate_market_scan")
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }
}
