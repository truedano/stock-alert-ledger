package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_WORK_NAME = "stock_ma_periodic_check"

    fun syncPeriodicCheck(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<StockCheckWorker>(
                15L,
                TimeUnit.MINUTES,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } else {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
