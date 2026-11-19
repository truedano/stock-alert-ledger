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
            val repo = app.container.marketDataRepository
            val prefs = repo.getPreferencesSnapshot()
            val isSimulated = prefs.dataSourceMode == com.example.domain.model.DataSourceMode.SIMULATED

            // During real mode, if all markets are closed, skip background worker execution to save battery
            if (!isSimulated) {
                val symbols = repo.getEnabledSymbols()
                val isMarketOpen = com.example.domain.model.MarketSessionChecker.isAnyMarketOpen(symbols)
                if (!isMarketOpen) {
                    return Result.success()
                }
            }

            repo.refreshAllEnabledRules(sendNotifications = true)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
