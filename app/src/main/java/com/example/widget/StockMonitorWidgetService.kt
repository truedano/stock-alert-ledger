package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.StockApplication
import com.example.data.local.WatchlistRuleEntity
import com.example.domain.model.Formatters
import kotlinx.coroutines.runBlocking

class StockMonitorWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StockMonitorRemoteViewsFactory(applicationContext)
    }
}

class StockMonitorRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WatchlistRuleEntity> = emptyList()
    private val quotePrices = mutableMapOf<String, Double>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val app = context.applicationContext as? StockApplication ?: return
        val container = app.container

        runBlocking {
            try {
                items = container.database.watchlistDao().getEnabledRules()
                val quotes = container.database.cachedQuoteDao().observeAllQuotes()
                for (rule in items) {
                    val q = container.database.cachedQuoteDao().getQuoteBySymbol(rule.symbol)
                    if (q != null) {
                        quotePrices[rule.symbol] = q.currentPrice
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        items = emptyList()
        quotePrices.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null
        val rule = items[position]
        val price = quotePrices[rule.symbol]

        val views = RemoteViews(context.packageName, R.layout.widget_stock_item)

        // 簡潔名稱排版，避免過長英文名稱擠壓
        val displayName = when {
            rule.name.isNotBlank() && rule.name.any { it.code in 0x4e00..0x9fff } -> "${rule.displaySymbol} ${rule.name}"
            rule.displaySymbol.isNotBlank() -> rule.displaySymbol
            else -> rule.symbol.substringBefore(".")
        }

        views.setTextViewText(R.id.item_symbol, displayName)
        views.setTextViewText(
            R.id.item_price,
            price?.let { Formatters.formatPrice(it, rule.currency) } ?: "--"
        )

        val rawConditionText = when (val c = rule.condition) {
            is com.example.domain.model.CompositeCondition -> c.shortTriggerTag
            is com.example.domain.model.MovingAverageCondition -> c.shortTriggerTag
            is com.example.domain.model.PeakDrawdownCondition -> c.shortTriggerTag
            is com.example.domain.model.RsiCondition -> c.shortTriggerTag
            is com.example.domain.model.PriceTargetCondition -> c.shortTriggerTag
            is com.example.domain.model.MarginMaintenanceCondition -> c.shortTriggerTag
        }
        val safeConditionText = rawConditionText
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

        views.setTextViewText(R.id.item_status, safeConditionText)

        // 根據是否已觸發切換樣式：已觸發顯示紅色高亮警示膠囊
        if (rule.isTriggered) {
            views.setInt(R.id.item_status, "setBackgroundResource", R.drawable.bg_widget_tag_triggered)
            views.setTextColor(R.id.item_status, android.graphics.Color.parseColor("#F87171"))
        } else {
            views.setInt(R.id.item_status, "setBackgroundResource", R.drawable.bg_widget_tag)
            views.setTextColor(R.id.item_status, android.graphics.Color.parseColor("#38BDF8"))
        }

        // 點擊項目整列時將該規則 ID 傳給 MainActivity 精準定位
        val fillInIntent = Intent().apply {
            putExtra(StockMonitorWidgetProvider.EXTRA_WIDGET_RULE_ID, rule.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_row, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.id ?: position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
