package com.example.notification

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.StockApplication
import com.example.widget.StockMonitorWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles background notification action buttons (e.g., pausing a rule or dismissing a notification)
 * without launching the main UI.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (!StockNotificationHelper.isInternalTokenValid(context, intent)) return

        val notificationId = intent.getIntExtra(StockNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            StockNotificationHelper.ACTION_DISMISS_NOTIFICATION -> {
                StockNotificationHelper.cancelNotification(context, notificationId)
            }

            StockNotificationHelper.ACTION_PAUSE_RULE_FROM_NOTIFICATION -> {
                val ruleId = intent.getLongExtra(StockNotificationHelper.EXTRA_RULE_ID, -1L)
                StockNotificationHelper.cancelNotification(context, notificationId)
                if (ruleId <= 0L) return

                val app = context.applicationContext as? StockApplication ?: return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.container.database.watchlistDao().setEnabled(ruleId, false)
                        val widgetManager = AppWidgetManager.getInstance(context)
                        val ids = widgetManager.getAppWidgetIds(
                            ComponentName(context, StockMonitorWidgetProvider::class.java),
                        )
                        if (ids.isNotEmpty()) {
                            StockMonitorWidgetProvider.updateAllWidgets(context, widgetManager, ids)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
