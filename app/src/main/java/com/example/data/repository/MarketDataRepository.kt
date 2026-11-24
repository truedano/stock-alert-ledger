package com.example.data.repository

import android.content.Context
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.CachedQuoteDao
import com.example.data.local.CachedQuoteEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistRuleEntity
import com.example.data.remote.SimulatedMarketProvider
import com.example.data.remote.YahooFinanceService
import com.example.domain.model.AlertCondition
import com.example.domain.model.ConditionEngine
import com.example.domain.model.ConditionEvaluationResult
import com.example.domain.model.DataSourceMode
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.MovingAverageTriggerMode
import com.example.domain.model.RefreshSummary
import com.example.domain.model.StockQuote
import com.example.domain.model.TaiwanFuturesChipSnapshot
import com.example.domain.model.TickerLookupResult
import com.example.domain.model.TickerSearchCandidate
import com.example.notification.StockNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MarketDataRepository(
    private val appContext: Context,
    private val yahooFinanceService: YahooFinanceService,
    private val twseMarginService: com.example.data.remote.TwseMarginService = com.example.data.remote.TwseMarginService(),
    private val fearAndGreedService: com.example.data.remote.FearAndGreedService = com.example.data.remote.FearAndGreedService(),
    private val tickerSearchService: com.example.data.remote.TickerSearchService? = null,
    private val taifexFuturesService: com.example.data.remote.TaifexFuturesService = com.example.data.remote.TaifexFuturesService(),
    private val valuationRemoteService: com.example.data.remote.ValuationRemoteService? = null,
    private val watchlistDao: WatchlistDao,
    private val cachedQuoteDao: CachedQuoteDao,
    private val alertLogDao: com.example.data.local.AlertLogDao? = null,
    private val preferencesRepository: AppPreferencesRepository,
) {
    val rulesFlow: Flow<List<WatchlistRuleEntity>> = watchlistDao.observeAllRules()
    val cachedQuotesFlow: Flow<List<CachedQuoteEntity>> = cachedQuoteDao.observeAllQuotes()
    val alertLogsFlow: Flow<List<com.example.data.local.AlertLogEntity>> = alertLogDao?.observeAllLogs()
        ?: kotlinx.coroutines.flow.flowOf(emptyList())

    private var latestMarginSnapshot: com.example.data.remote.MarketMarginSnapshot? = null
    private var stockMarginCache: Map<String, com.example.data.remote.StockMarginInfo> = emptyMap()
    private var latestFearAndGreed: com.example.domain.model.FearAndGreedData? = null
    private val candleHistoryCache = java.util.concurrent.ConcurrentHashMap<String, List<com.example.domain.model.CandleStickData>>()
    private val _futuresChipFlow = MutableStateFlow<TaiwanFuturesChipSnapshot?>(null)
    val futuresChipFlow: StateFlow<TaiwanFuturesChipSnapshot?> = _futuresChipFlow.asStateFlow()

    private val _valuationSnapshotFlow = MutableStateFlow<com.example.domain.model.ValuationRadarSnapshot?>(null)
    val valuationSnapshotFlow: StateFlow<com.example.domain.model.ValuationRadarSnapshot?> = _valuationSnapshotFlow.asStateFlow()

    suspend fun fetchValuationRadar(forceRefresh: Boolean = false): com.example.domain.model.ValuationRadarSnapshot? = withContext(Dispatchers.IO) {
        val snap = valuationRemoteService?.fetchValuationRadar(forceRefresh = forceRefresh)
        if (snap != null) {
            _valuationSnapshotFlow.value = snap
        }
        snap ?: _valuationSnapshotFlow.value
    }

    suspend fun fetchFearAndGreed(forceRefresh: Boolean = false): com.example.domain.model.FearAndGreedData? = withContext(Dispatchers.IO) {
        val data = fearAndGreedService.fetchFearAndGreed(forceRefresh)
        if (data != null) {
            latestFearAndGreed = data
        }
        latestFearAndGreed
    }

    fun getLatestFearAndGreed(): com.example.domain.model.FearAndGreedData? = latestFearAndGreed

    suspend fun fetchTwseMarginSnapshot(): com.example.data.remote.MarketMarginSnapshot? = withContext(Dispatchers.IO) {
        val snapshot = twseMarginService.fetchMarketMarginSnapshot()
        if (snapshot != null) {
            latestMarginSnapshot = snapshot
        }
        latestMarginSnapshot
    }

    fun getLatestMarginSnapshot(): com.example.data.remote.MarketMarginSnapshot? = latestMarginSnapshot

    suspend fun fetchTaifexFuturesSnapshot(forceRefresh: Boolean = false): TaiwanFuturesChipSnapshot? = withContext(Dispatchers.IO) {
        val snap = taifexFuturesService.fetchFuturesChipSnapshot(forceRefresh = forceRefresh)
        if (snap != null) {
            _futuresChipFlow.value = snap
        }
        snap ?: _futuresChipFlow.value
    }

    fun getLatestFuturesChipSnapshot(): TaiwanFuturesChipSnapshot? = _futuresChipFlow.value

    suspend fun fetchAllStockMargins(): Map<String, com.example.data.remote.StockMarginInfo> = withContext(Dispatchers.IO) {
        val margins = twseMarginService.fetchAllStockMargins()
        if (margins.isNotEmpty()) {
            stockMarginCache = margins
        }
        stockMarginCache
    }

    fun getStockMarginInfo(symbol: String): com.example.data.remote.StockMarginInfo? {
        val clean = symbol.substringBefore(".").trim().uppercase()
        return stockMarginCache[clean]
    }

    suspend fun clearAlertLogs() = withContext(Dispatchers.IO) {
        alertLogDao?.clearAllLogs()
    }

    suspend fun deleteAlertLog(id: Long) = withContext(Dispatchers.IO) {
        alertLogDao?.deleteLogById(id)
    }

    suspend fun getEnabledSymbols(): List<String> = withContext(Dispatchers.IO) {
        watchlistDao.getEnabledRules().map { it.symbol }.distinct()
    }

    suspend fun getEnabledRulesSnapshot(): List<WatchlistRuleEntity> = withContext(Dispatchers.IO) {
        watchlistDao.getEnabledRules()
    }

    suspend fun getPreferencesSnapshot() = withContext(Dispatchers.IO) {
        preferencesRepository.getSnapshot()
    }

    suspend fun searchTickers(query: String): List<TickerSearchCandidate> = withContext(Dispatchers.IO) {
        tickerSearchService?.search(query) ?: emptyList()
    }

    suspend fun lookupTicker(rawInput: String): TickerLookupResult = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.getSnapshot()
        if (prefs.dataSourceMode == DataSourceMode.SIMULATED) {
            SimulatedMarketProvider.findProfile(rawInput)?.let { profile ->
                val simQuote = SimulatedMarketProvider.quoteFor(profile.symbol, prefs)
                if (simQuote != null) {
                    return@withContext TickerLookupResult.Success(simQuote)
                }
            }
        }
        yahooFinanceService.resolveAndFetch(rawInput)
    }

    suspend fun saveRule(
        existingId: Long? = null,
        symbol: String,
        displaySymbol: String,
        name: String,
        currency: MarketCurrency,
        condition: AlertCondition,
        plannedBuyAmount: Double,
        initialQuote: StockQuote?,
    ): Long = withContext(Dispatchers.IO) {
        if (initialQuote != null) {
            upsertCachedQuote(initialQuote)
        }

        val existing = existingId?.let { watchlistDao.getRuleById(it) }
        val ruleId = if (existing != null) {
            watchlistDao.updateRule(
                existing.copy(
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    name = name,
                    currency = currency,
                    condition = condition,
                    plannedBuyAmount = plannedBuyAmount,
                    // Reset trigger if user changed condition parameters
                    isTriggered = false,
                    triggeredDateKey = null,
                    lastTriggeredAt = null,
                )
            )
            existing.id
        } else {
            watchlistDao.insertRule(
                WatchlistRuleEntity(
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    name = name,
                    currency = currency,
                    condition = condition,
                    plannedBuyAmount = plannedBuyAmount,
                    isEnabled = true,
                )
            )
        }

        // Immediately evaluate against cached quote if available
        val savedRule = watchlistDao.getRuleById(ruleId)
        val cached = cachedQuoteDao.getQuoteBySymbol(symbol)
        if (savedRule != null && cached != null && savedRule.isEnabled) {
            evaluateAndRecordTrigger(savedRule, cached.toStockQuote(), sendNotification = false)
        }

        notifyWidgetDataChanged()
        ruleId
    }

    suspend fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        watchlistDao.setEnabled(ruleId, enabled)
        notifyWidgetDataChanged()
    }

    suspend fun resetRuleTrigger(ruleId: Long) = withContext(Dispatchers.IO) {
        watchlistDao.resetTrigger(ruleId)
        notifyWidgetDataChanged()
    }

    suspend fun deleteRule(ruleId: Long) = withContext(Dispatchers.IO) {
        watchlistDao.deleteRuleById(ruleId)
        notifyWidgetDataChanged()
    }

    private fun notifyWidgetDataChanged() {
        try {
            val widgetManager = android.appwidget.AppWidgetManager.getInstance(appContext)
            val ids = widgetManager.getAppWidgetIds(
                android.content.ComponentName(appContext, com.example.widget.StockMonitorWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                com.example.widget.StockMonitorWidgetProvider.updateAllWidgets(appContext, widgetManager, ids)
            }
        } catch (_: Exception) {}
    }

    suspend fun refreshAllEnabledRules(sendNotifications: Boolean): RefreshSummary =
        withContext(Dispatchers.IO) {
            val prefs = preferencesRepository.getSnapshot()
            val rules = watchlistDao.getEnabledRules()
            if (rules.isEmpty()) {
                val now = System.currentTimeMillis()
                val msg = if (prefs.dataSourceMode == DataSourceMode.SIMULATED) {
                    "模擬模式待命中（尚無啟用監控項目）"
                } else {
                    "尚無啟用的監控項目"
                }
                preferencesRepository.updateSyncInfo(now, msg)
                return@withContext RefreshSummary(
                    updatedCount = 0,
                    cachedFallbackCount = 0,
                    triggeredCount = 0,
                    statusMessage = msg,
                    hasRateLimitOrNetworkIssue = false,
                )
            }

            // 僅在有設定「融資維持率條件」或啟用「台股」標的時，才向證交所抓取全市場維持率
            val hasMarginCondition = rules.any {
                it.condition is com.example.domain.model.MarginMaintenanceCondition ||
                it.currency == MarketCurrency.TWD
            }
            if (hasMarginCondition && prefs.dataSourceMode == DataSourceMode.REAL) {
                try {
                    fetchTwseMarginSnapshot()
                } catch (_: Exception) {
                    // non-fatal
                }
                try {
                    fetchAllStockMargins()
                } catch (_: Exception) {
                    // non-fatal
                }
            }

            // 更新美股恐懼與貪婪指數＆台指期籌碼（在真實數據模式下執行，各自內建快取保護）
            if (prefs.dataSourceMode == DataSourceMode.REAL) {
                try {
                    fetchFearAndGreed(forceRefresh = false)
                } catch (_: Exception) {
                    // non-fatal
                }
                try {
                    fetchTaifexFuturesSnapshot(forceRefresh = false)
                } catch (_: Exception) {
                    // non-fatal
                }
            }

            // Group rules by symbol so we fetch each ticker once per cycle
            val rulesBySymbol = rules.groupBy { it.symbol }
            var updatedCount = 0
            var cachedFallbackCount = 0
            var triggeredCount = 0
            var issueMessage: String? = null

            val symbols = rulesBySymbol.keys.toList()
            symbols.forEachIndexed { index, symbol ->
                val symbolRules = rulesBySymbol[symbol].orEmpty()
                val quote = fetchQuoteForMode(symbol, prefs.dataSourceMode, prefs)

                val effectiveQuote = when (quote) {
                    is TickerLookupResult.Success -> {
                        val baseQuote = quote.quote
                        val attachedQuote = attachMarginInfo(baseQuote)
                        upsertCachedQuote(attachedQuote)
                        updatedCount++
                        attachedQuote
                    }
                    is TickerLookupResult.RateLimited -> {
                        issueMessage = quote.message
                        cachedFallbackCount++
                        cachedQuoteDao.getQuoteBySymbol(symbol)?.toStockQuote()?.let { attachMarginInfo(it) }
                    }
                    is TickerLookupResult.Error -> {
                        issueMessage = quote.message
                        cachedFallbackCount++
                        cachedQuoteDao.getQuoteBySymbol(symbol)?.toStockQuote()?.let { attachMarginInfo(it) }
                    }
                    is TickerLookupResult.NotFound -> {
                        cachedFallbackCount++
                        cachedQuoteDao.getQuoteBySymbol(symbol)?.toStockQuote()?.let { attachMarginInfo(it) }
                    }
                }

                if (effectiveQuote != null) {
                    for (rule in symbolRules) {
                        val didTrigger = evaluateAndRecordTrigger(
                            rule = rule,
                            quote = effectiveQuote,
                            sendNotification = sendNotifications,
                        )
                        if (didTrigger) triggeredCount++
                    }
                }

                // Stagger requests by 300ms in REAL mode to avoid burst rate-limiting
                if (prefs.dataSourceMode == DataSourceMode.REAL && index < symbols.lastIndex) {
                    delay(300L)
                }
            }

            val now = System.currentTimeMillis()
            val summaryMessage = buildString {
                if (prefs.dataSourceMode == DataSourceMode.SIMULATED) {
                    append("模擬報價檢查完成（${updatedCount} 檔）")
                } else if (issueMessage != null) {
                    append(issueMessage)
                } else {
                    append("已更新 ${updatedCount} 檔最新報價")
                }
                if (triggeredCount > 0) {
                    append(" · 新觸發 $triggeredCount 項")
                }
            }
            preferencesRepository.updateSyncInfo(now, summaryMessage)

            // 通知桌面 Widget 更新最新報價
            try {
                val widgetManager = android.appwidget.AppWidgetManager.getInstance(appContext)
                val ids = widgetManager.getAppWidgetIds(
                    android.content.ComponentName(appContext, com.example.widget.StockMonitorWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    com.example.widget.StockMonitorWidgetProvider.updateAllWidgets(appContext, widgetManager, ids)
                }
            } catch (_: Exception) {}

            RefreshSummary(
                updatedCount = updatedCount,
                cachedFallbackCount = cachedFallbackCount,
                triggeredCount = triggeredCount,
                statusMessage = summaryMessage,
                hasRateLimitOrNetworkIssue = issueMessage != null,
            )
        }

    /**
     * Simulator action: updates simulated price, ensures a rule exists for the test stock
     * so users can immediately test the full notification -> quick-buy -> portfolio flow,
     * evaluates conditions, and optionally drops price below MA in one tap.
     */
    suspend fun updateSimulatedPriceAndEvaluate(
        symbol: String,
        newPrice: Double,
        sendNotifications: Boolean,
        ensurePeriodRuleIfMissing: Int? = null,
        fallbackName: String? = null,
        fallbackCurrency: MarketCurrency? = null,
    ): RefreshSummary = withContext(Dispatchers.IO) {
        val normalizedSymbol = symbol.trim().uppercase()
        preferencesRepository.setSimulatedPrice(normalizedSymbol, newPrice)

        if (ensurePeriodRuleIfMissing != null) {
            ensureRuleForSimulation(
                symbol = normalizedSymbol,
                periodDays = ensurePeriodRuleIfMissing,
                fallbackName = fallbackName,
                fallbackCurrency = fallbackCurrency,
            )
            // Reset trigger on matching rules for this symbol so user can re-test notification immediately
            val rulesForSymbol = watchlistDao.getRulesBySymbol(normalizedSymbol)
            for (rule in rulesForSymbol) {
                if (!rule.isEnabled) {
                    watchlistDao.setEnabled(rule.id, true)
                }
                if (rule.isTriggered) {
                    watchlistDao.resetTrigger(rule.id)
                }
            }
        }

        refreshAllEnabledRules(sendNotifications = sendNotifications)
    }

    private suspend fun ensureRuleForSimulation(
        symbol: String,
        periodDays: Int,
        fallbackName: String?,
        fallbackCurrency: MarketCurrency?,
    ): WatchlistRuleEntity? = withContext(Dispatchers.IO) {
        val existing = watchlistDao.getRulesBySymbol(symbol)
        val matchingRule = existing.firstOrNull { rule ->
            (rule.condition as? MovingAverageCondition)?.periodDays == periodDays
        }
        if (matchingRule != null) {
            return@withContext matchingRule
        }

        val cached = cachedQuoteDao.getQuoteBySymbol(symbol)?.toStockQuote()
        val profile = SimulatedMarketProvider.findProfile(symbol)
        val resolvedName = cached?.name ?: profile?.name ?: fallbackName ?: symbol
        val resolvedCurrency = cached?.currency ?: profile?.currency ?: fallbackCurrency
            ?: if (symbol.endsWith(".TW") || symbol.endsWith(".TWO")) MarketCurrency.TWD else MarketCurrency.USD
        val defaultAmount = if (resolvedCurrency == MarketCurrency.TWD) 30_000.0 else 500.0
        val prefs = preferencesRepository.getSnapshot()
        val quote = cached?.copy(
            currentPrice = prefs.simulatedPriceFor(symbol) ?: cached.currentPrice,
            isSimulated = true,
        ) ?: SimulatedMarketProvider.quoteFor(symbol, prefs)

        val id = saveRule(
            symbol = symbol,
            displaySymbol = cached?.displaySymbol ?: profile?.displaySymbol ?: symbol,
            name = resolvedName,
            currency = resolvedCurrency,
            condition = MovingAverageCondition(
                periodDays = periodDays,
                triggerMode = MovingAverageTriggerMode.BELOW_OR_EQUAL,
                tolerancePercent = 1.0,
            ),
            plannedBuyAmount = defaultAmount,
            initialQuote = quote,
        )
        watchlistDao.getRuleById(id)
    }

    fun evaluateRule(rule: WatchlistRuleEntity, quote: StockQuote?): ConditionEvaluationResult? {
        if (quote == null) return null
        val marginSnapshot = latestMarginSnapshot
        val snapshot = MarketSnapshot(
            symbol = quote.symbol,
            currentPrice = quote.currentPrice,
            historicalCloses = quote.closeHistory,
            marketKeepRate = marginSnapshot?.keepRate,
            recentMarketKeepRates = marginSnapshot?.recentKeepRates ?: emptyList(),
        )
        return ConditionEngine.evaluate(rule.condition, snapshot)
    }

    private suspend fun fetchQuoteForMode(
        symbol: String,
        mode: DataSourceMode,
        prefs: com.example.data.local.AppPreferencesState,
    ): TickerLookupResult {
        if (mode == DataSourceMode.SIMULATED) {
            val cached = cachedQuoteDao.getQuoteBySymbol(symbol)?.toStockQuote()
            val overridePrice = prefs.simulatedPriceFor(symbol)
            if (cached != null) {
                return TickerLookupResult.Success(
                    cached.copy(
                        currentPrice = overridePrice ?: cached.currentPrice,
                        isSimulated = true,
                    )
                )
            }
            val sim = SimulatedMarketProvider.quoteFor(symbol, prefs)
            if (sim != null) {
                return TickerLookupResult.Success(sim)
            }
            return TickerLookupResult.NotFound("模擬模式無 $symbol 歷史報價，請先查詢該股票")
        }
        return yahooFinanceService.fetchExactSymbol(symbol)
    }

    fun attachMarginInfo(quote: StockQuote): StockQuote {
        val quoteWithCandles = if (quote.candleHistory.isEmpty()) {
            val cachedCandles = candleHistoryCache[quote.symbol]
            if (!cachedCandles.isNullOrEmpty()) quote.copy(candleHistory = cachedCandles) else quote
        } else {
            quote
        }
        if (quoteWithCandles.marginInfo != null || quoteWithCandles.currency != MarketCurrency.TWD) {
            return quoteWithCandles
        }
        val cleanSymbol = quoteWithCandles.symbol.substringBefore(".").trim().uppercase()
        val twseMargin = stockMarginCache[cleanSymbol]
        val estimatedRate = com.example.domain.model.MovingAverageCalculator
            .calculateEstimatedMarginKeepRate(quoteWithCandles.currentPrice, quoteWithCandles.closeHistory)

        val mergedMargin = if (twseMargin != null) {
            twseMargin.copy(estimatedKeepRate = estimatedRate)
        } else if (estimatedRate != null) {
            com.example.data.remote.StockMarginInfo(
                symbol = quoteWithCandles.symbol,
                name = quoteWithCandles.name,
                marginBalance = 0L,
                marginChange = 0L,
                shortBalance = 0L,
                shortChange = 0L,
                estimatedKeepRate = estimatedRate,
            )
        } else null

        return quoteWithCandles.copy(marginInfo = mergedMargin)
    }

    private suspend fun upsertCachedQuote(quote: StockQuote) {
        if (quote.candleHistory.isNotEmpty()) {
            candleHistoryCache[quote.symbol] = quote.candleHistory
        }
        val existingCandles = if (quote.candleHistory.isNotEmpty()) {
            quote.candleHistory
        } else {
            candleHistoryCache[quote.symbol].orEmpty()
        }
        cachedQuoteDao.upsertQuote(
            CachedQuoteEntity(
                symbol = quote.symbol,
                displaySymbol = quote.displaySymbol,
                name = quote.name,
                currency = quote.currency,
                currentPrice = quote.currentPrice,
                previousClose = quote.previousClose,
                closeHistory = quote.closeHistory,
                lastUpdatedAt = quote.lastUpdatedAt,
                isSimulated = quote.isSimulated,
                candleHistory = existingCandles.takeLast(500),
            )
        )
    }

    private suspend fun evaluateAndRecordTrigger(
        rule: WatchlistRuleEntity,
        quote: StockQuote,
        sendNotification: Boolean,
    ): Boolean {
        val eval = evaluateRule(rule, quote) ?: return false
        if (!eval.isTriggered) return false

        val now = System.currentTimeMillis()
        val todayKey = Formatters.formatDateKey(now)
        // Anti-spam: do not notify again if already triggered on the same day or before manual reset
        if (rule.isTriggered || rule.triggeredDateKey == todayKey) {
            return false
        }

        watchlistDao.markTriggered(rule.id, todayKey, now)
        if (rule.condition is com.example.domain.model.PriceTargetCondition && rule.condition.autoDisableOnTrigger) {
            watchlistDao.setEnabled(rule.id, false)
        }

        // 記錄至警報歷史日誌
        alertLogDao?.insertLog(
            com.example.data.local.AlertLogEntity(
                ruleId = rule.id,
                symbol = rule.symbol,
                displaySymbol = rule.displaySymbol,
                name = rule.name,
                currency = rule.currency,
                triggerTag = eval.triggerTag,
                triggeredPrice = quote.currentPrice,
                triggeredTimestamp = now,
                triggeredDateKey = todayKey,
                note = eval.statusMessage,
            )
        )

        if (sendNotification) {
            StockNotificationHelper.showBuyAlertNotification(
                context = appContext,
                rule = rule,
                evaluation = eval,
                currentPrice = quote.currentPrice,
            )
        }
        return true
    }
}
