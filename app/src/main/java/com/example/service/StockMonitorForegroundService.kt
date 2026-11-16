package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.StockApplication
import com.example.notification.StockNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StockMonitorForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITOR -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startMonitoringLoop()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForegroundService()
    }

    private fun startForegroundWithNotification() {
        val notification = StockNotificationHelper.buildForegroundNotification(this)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                StockNotificationHelper.FOREGROUND_NOTIFICATION_ID,
                notification,
                fgType,
            )
        } catch (_: Exception) {
            // Android 14+ ForegroundServiceStartNotAllowedException fallback
        }
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return

        monitorJob = serviceScope.launch {
            val app = applicationContext as? StockApplication ?: return@launch
            val repo = app.container.marketDataRepository
            val notifManager = getSystemService(NotificationManager::class.java)

            while (isActive) {
                try {
                    val summary = repo.refreshAllEnabledRules(sendNotifications = true)
                    val statusText = "盤中每分鐘巡檢：${summary.statusMessage}"
                    val updatedNotification = StockNotificationHelper.buildForegroundNotification(
                        context = this@StockMonitorForegroundService,
                        statusText = statusText,
                    )
                    notifManager?.notify(
                        StockNotificationHelper.FOREGROUND_NOTIFICATION_ID,
                        updatedNotification,
                    )
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Ignore transient network errors and continue next loop
                }

                // Poll every 60 seconds (1 minute interval)
                delay(60_000L)
            }
        }
    }

    private fun stopForegroundService() {
        monitorJob?.cancel()
        monitorJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_MONITOR = "com.example.action.START_FOREGROUND_MONITOR"
        const val ACTION_STOP_MONITOR = "com.example.action.STOP_FOREGROUND_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, StockMonitorForegroundService::class.java).apply {
                action = ACTION_START_MONITOR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StockMonitorForegroundService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            context.startService(intent)
        }
    }
}
