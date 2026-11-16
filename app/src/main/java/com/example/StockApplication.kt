package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.notification.StockNotificationHelper
import com.example.service.StockMonitorForegroundService
import com.example.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StockApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        StockNotificationHelper.createNotificationChannel(this)
        appScope.launch {
            val prefs = container.preferencesRepository.getSnapshot()
            WorkScheduler.syncPeriodicCheck(this@StockApplication, prefs.backgroundCheckEnabled)
            if (prefs.highFrequencyCheckEnabled) {
                StockMonitorForegroundService.start(this@StockApplication)
            }
        }
    }
}
