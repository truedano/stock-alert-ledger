package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.StockApplication

class StockCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StockApplication ?: return Result.failure()
        return try {
            app.container.marketDataRepository.refreshAllEnabledRules(sendNotifications = true)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
