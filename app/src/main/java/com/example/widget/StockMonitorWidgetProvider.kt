package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.StockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockMonitorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val app = context.applicationContext as? StockApplication ?: return
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockMonitorWidgetProvider::class.java))
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    try {
                        app.container.marketDataRepository.refreshAllEnabledRules(sendNotifications = true)
                    } catch (_: Exception) {}
                    updateAllWidgetsInternal(context, widgetManager, ids)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.truedano.stockalertledger.widget.ACTION_REFRESH_WIDGET"
        const val ACTION_OPEN_RULE_FROM_WIDGET = "com.truedano.stockalertledger.widget.ACTION_OPEN_RULE_FROM_WIDGET"
        const val EXTRA_WIDGET_RULE_ID = "extra_widget_rule_id"

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                updateAllWidgetsInternal(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateAllWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            val app = context.applicationContext as? StockApplication ?: return
            val container = app.container

            val fng = container.marketDataRepository.getLatestFearAndGreed()
            val margin = container.marketDataRepository.getLatestMarginSnapshot()
            val timeStr = SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_stock_monitor)

                // Open App PendingIntent (點擊小工具標題列開啟 App)
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPi = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

                // Refresh Button PendingIntent
                val refreshIntent = Intent(context, StockMonitorWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPi = PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPi)

                // Market Mood Tag
                val moodText = when {
                    fng != null -> "美股恐貪: ${fng.score.toInt()} (${fng.localizedRating})"
                    margin?.keepRate != null -> "台股維持率: ${String.format(Locale.US, "%.1f", margin.keepRate)}%"
                    else -> "點擊可手動更新"
                }
                views.setTextViewText(R.id.widget_market_sentiment, moodText)

                // Setup Scrollable ListView via RemoteViewsService
                val serviceIntent = Intent(context, StockMonitorWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

                // ListView Item Click Template
                val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_RULE_FROM_WIDGET
                    `package` = context.packageName
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    com.example.notification.StockNotificationHelper.attachInternalToken(context, this)
                }
                val itemClickPi = PendingIntent.getActivity(
                    context,
                    0,
                    itemClickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPi)

                views.setTextViewText(R.id.widget_sync_time, "更新於 $timeStr")
                appWidgetManager.updateAppWidget(widgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view)
            }
        }
    }
}
