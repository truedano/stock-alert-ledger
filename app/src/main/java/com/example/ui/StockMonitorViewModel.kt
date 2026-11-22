package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.StockApplication
import com.example.data.local.AppPreferencesState
import com.example.data.local.BuyRecordEntity
import com.example.data.local.CachedQuoteEntity
import com.example.data.local.WatchlistRuleEntity
import com.example.data.remote.SimulatedMarketProvider
import com.example.data.remote.SimulatedStockProfile
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.TriggerTagCount
import com.example.domain.model.AlertCondition
import com.example.domain.model.ConditionEvaluationResult
import com.example.domain.model.DataSourceMode
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCalculator
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.PortfolioSummary
import com.example.domain.model.StockQuote
import com.example.domain.model.TickerLookupResult
import com.example.notification.QuickBuyPrefill
import com.example.notification.StockNotificationHelper
import com.example.service.StockMonitorForegroundService
import com.example.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class WatchlistCombineState(
    val rules: List<WatchlistRuleEntity>,
    val cachedQuotes: List<CachedQuoteEntity>,
    val prefs: AppPreferencesState,
    val refreshing: Boolean,
    val rateWarning: Boolean,
)

data class WatchlistItemUi(
    val rule: WatchlistRuleEntity,
    val quote: StockQuote?,
    val evaluation: ConditionEvaluationResult?,
    val estimatedShares: Double?,
)

data class SimulatedTargetLevel(
    val periodDays: Int,
    val label: String,
    val referencePrice: Double,
)

data class SimulatedStockUi(
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val currentPrice: Double,
    val minSliderPrice: Double,
    val maxSliderPrice: Double,
    val targetLevels: List<SimulatedTargetLevel>,
    val hasWatchlistRule: Boolean,
    val isFromWatchlist: Boolean,
)

data class WatchlistUiState(
    val dataSourceMode: DataSourceMode = DataSourceMode.REAL,
    val backgroundCheckEnabled: Boolean = true,
    val highFrequencyCheckEnabled: Boolean = false,
    val highFrequencyIntervalSeconds: Int = 60,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "尚未同步報價",
    val isRefreshing: Boolean = false,
    val hasRateLimitWarning: Boolean = false,
    val items: List<WatchlistItemUi> = emptyList(),
    val watchlistSimulatedStocks: List<SimulatedStockUi> = emptyList(),
    val builtInSimulatedStocks: List<SimulatedStockUi> = emptyList(),
    val marketKeepRate: Double? = null,
    val marketCallAccNum: Long? = null,
    val marketExeAccNum: Long? = null,
    val fearAndGreed: com.example.domain.model.FearAndGreedData? = null,
    val futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
    val alertLogs: List<com.example.data.local.AlertLogEntity> = emptyList(),
)

data class PortfolioUiState(
    val selectedTriggerFilter: String? = null,
    val totalRecordCount: Int = 0,
    val triggerTagCounts: List<TriggerTagCount> = emptyList(),
    val summary: PortfolioSummary = PortfolioSummary(0.0, 0.0, emptyList()),
    val filteredRecords: List<BuyRecordEntity> = emptyList(),
)

sealed interface LookupUiState {
    data object Idle : LookupUiState
    data object Loading : LookupUiState
    data class Verified(val quote: StockQuote) : LookupUiState
    data class Failed(val message: String) : LookupUiState
}

class StockMonitorViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as StockApplication
    private val container = app.container
    private val marketRepo = container.marketDataRepository
    private val portfolioRepo = container.portfolioRepository
    private val prefsRepo = container.preferencesRepository
    private val backupRepo = container.backupRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _hasRateLimitWarning = MutableStateFlow(false)
    private val _selectedTriggerFilter = MutableStateFlow<String?>(null)
    private val _lookupState = MutableStateFlow<LookupUiState>(LookupUiState.Idle)
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()

    private val _quickBuyDialogPrefill = MutableStateFlow<QuickBuyPrefill?>(null)
    val quickBuyDialogPrefill: StateFlow<QuickBuyPrefill?> = _quickBuyDialogPrefill.asStateFlow()

    private val _highlightedRuleId = MutableStateFlow<Long?>(null)
    val highlightedRuleId: StateFlow<Long?> = _highlightedRuleId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val preferencesState: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesState(),
        )

    private val _marginSnapshotFlow = MutableStateFlow<com.example.data.remote.MarketMarginSnapshot?>(null)
    private val _fearAndGreedFlow = MutableStateFlow<com.example.domain.model.FearAndGreedData?>(null)
    private val _futuresChipFlow = MutableStateFlow<com.example.domain.model.TaiwanFuturesChipSnapshot?>(null)

    init {
        // 進入 App 時背景非同步抓取全市場維持率與恐懼貪婪指數
        viewModelScope.launch {
            try {
                val fng = marketRepo.fetchFearAndGreed(forceRefresh = false)
                _fearAndGreedFlow.value = fng
            } catch (_: Exception) {
            }
            try {
                val rules = marketRepo.getEnabledRulesSnapshot()
                val needsMargin = rules.any {
                    it.condition is com.example.domain.model.MarginMaintenanceCondition ||
                    it.currency == MarketCurrency.TWD
                }
                if (needsMargin) {
                    val snap = marketRepo.fetchTwseMarginSnapshot()
                    _marginSnapshotFlow.value = snap
                    val futures = marketRepo.fetchTaifexFuturesSnapshot()
                    _futuresChipFlow.value = futures
                }
            } catch (_: Exception) {
            }
        }
    }

    val watchlistUiState: StateFlow<WatchlistUiState> = combine(
        marketRepo.rulesFlow,
        marketRepo.cachedQuotesFlow,
        prefsRepo.preferencesFlow,
        _isRefreshing,
        _hasRateLimitWarning,
    ) { rules, cachedQuotes, prefs, refreshing, rateWarning ->
        WatchlistCombineState(rules, cachedQuotes, prefs, refreshing, rateWarning)
    }.combine(marketRepo.alertLogsFlow) { state, alertLogs ->
        state to alertLogs
    }.combine(_marginSnapshotFlow) { (state, alertLogs), marginSnap ->
        Triple(state, alertLogs, marginSnap)
    }.combine(_fearAndGreedFlow) { (state, alertLogs, marginSnap), fngData ->
        Pair(Triple(state, alertLogs, marginSnap), fngData)
    }.combine(_futuresChipFlow) { (triple, fngData), futuresChip ->
        val (state, alertLogs, marginSnap) = triple
        val (rules, cachedQuotes, prefs, refreshing, rateWarning) = state
        val quoteMap = cachedQuotes.associate { it.symbol to marketRepo.attachMarginInfo(it.toStockQuote()) }
        val items = rules.map { rule ->
            val effectiveQuote = if (prefs.dataSourceMode == DataSourceMode.SIMULATED) {
                val cached = quoteMap[rule.symbol]
                val overridePrice = prefs.simulatedPriceFor(rule.symbol)
                when {
                    cached != null -> cached.copy(
                        currentPrice = overridePrice ?: cached.currentPrice,
                        isSimulated = true,
                        marginInfo = cached.marginInfo?.copy(
                            estimatedKeepRate = com.example.domain.model.MovingAverageCalculator
                                .calculateEstimatedMarginKeepRate(overridePrice ?: cached.currentPrice, cached.closeHistory)
                        )
                    )
                    else -> SimulatedMarketProvider.quoteFor(rule.symbol, prefs)
                }
            } else {
                quoteMap[rule.symbol]
            }
            val evaluation = marketRepo.evaluateRule(rule, effectiveQuote)
            val estShares = effectiveQuote?.currentPrice?.takeIf { it > 0.0 }?.let { price ->
                rule.plannedBuyAmount / price
            }
            WatchlistItemUi(
                rule = rule,
                quote = effectiveQuote,
                evaluation = evaluation,
                estimatedShares = estShares,
            )
        }

        val watchlistSimStocks = rules
            .groupBy { it.symbol }
            .map { (symbol, symbolRules) ->
                val leadRule = symbolRules.first()
                val cached = quoteMap[symbol]
                val demoProfile = SimulatedMarketProvider.findProfile(symbol)
                val closes = cached?.closeHistory?.takeIf { it.isNotEmpty() }
                    ?: demoProfile?.closeHistory.orEmpty()
                val currentPrice = prefs.simulatedPriceFor(symbol)
                    ?: cached?.currentPrice
                    ?: demoProfile?.defaultPrice
                    ?: 100.0

                val rulePeriods = symbolRules.mapNotNull {
                    (it.condition as? MovingAverageCondition)?.periodDays
                }
                val candidatePeriods = (rulePeriods + listOf(20, 60, 240)).distinct().sorted()
                val levels = candidatePeriods.mapNotNull { period ->
                    MovingAverageCalculator.calculateSma(closes, period)?.let { sma ->
                        SimulatedTargetLevel(
                            periodDays = period,
                            label = MovingAverageCondition.maPeriodLabel(period),
                            referencePrice = sma,
                        )
                    }
                }

                val anchors = listOf(currentPrice) + levels.map { it.referencePrice }
                val minAnchor = anchors.minOrNull() ?: currentPrice
                val maxAnchor = anchors.maxOrNull() ?: currentPrice
                val minSlider = (minAnchor * 0.78).coerceAtLeast(0.5)
                val maxSlider = (maxAnchor * 1.22).coerceAtLeast(minSlider + 1.0)

                SimulatedStockUi(
                    symbol = symbol,
                    displaySymbol = leadRule.displaySymbol,
                    name = leadRule.name,
                    currency = leadRule.currency,
                    currentPrice = currentPrice,
                    minSliderPrice = minSlider,
                    maxSliderPrice = maxSlider,
                    targetLevels = levels,
                    hasWatchlistRule = true,
                    isFromWatchlist = true,
                )
            }

        val builtInSimStocks = SimulatedMarketProvider.profiles.map { profile ->
            val simPrice = prefs.simulatedPriceFor(profile.symbol) ?: profile.defaultPrice
            val levels = listOf(20, 60, 240).mapNotNull { period ->
                MovingAverageCalculator.calculateSma(profile.closeHistory, period)?.let { sma ->
                    SimulatedTargetLevel(
                        periodDays = period,
                        label = MovingAverageCondition.maPeriodLabel(period),
                        referencePrice = sma,
                    )
                }
            }
            SimulatedStockUi(
                symbol = profile.symbol,
                displaySymbol = profile.displaySymbol,
                name = profile.name,
                currency = profile.currency,
                currentPrice = simPrice,
                minSliderPrice = profile.minSliderPrice,
                maxSliderPrice = profile.maxSliderPrice,
                targetLevels = levels,
                hasWatchlistRule = rules.any { it.symbol == profile.symbol },
                isFromWatchlist = false,
            )
        }

        WatchlistUiState(
            dataSourceMode = prefs.dataSourceMode,
            backgroundCheckEnabled = prefs.backgroundCheckEnabled,
            highFrequencyCheckEnabled = prefs.highFrequencyCheckEnabled,
            highFrequencyIntervalSeconds = prefs.highFrequencyIntervalSeconds,
            lastSyncTimestamp = prefs.lastSyncTimestamp,
            lastSyncMessage = prefs.lastSyncMessage,
            isRefreshing = refreshing,
            hasRateLimitWarning = rateWarning,
            items = items,
            watchlistSimulatedStocks = watchlistSimStocks,
            builtInSimulatedStocks = builtInSimStocks,
            marketKeepRate = marginSnap?.keepRate,
            marketCallAccNum = marginSnap?.callAccNum,
            marketExeAccNum = marginSnap?.exeAccNum,
            fearAndGreed = fngData,
            futuresChip = futuresChip,
            alertLogs = alertLogs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WatchlistUiState(),
    )

    val portfolioUiState: StateFlow<PortfolioUiState> = combine(
        portfolioRepo.recordsFlow,
        portfolioRepo.cachedQuotesFlow,
        _selectedTriggerFilter,
    ) { records, cachedQuotes, selectedFilter ->
        val quoteMap = cachedQuotes.associateBy { it.symbol }
        val tagCounts = PortfolioRepository.buildTriggerTagCounts(records)
        val effectiveFilter = selectedFilter?.takeIf { filter ->
            tagCounts.any { it.tag == filter }
        }
        val filteredRecords = if (effectiveFilter == null) {
            records
        } else {
            records.filter { it.triggerTag.trim() == effectiveFilter }
        }
        val summary = PortfolioRepository.buildPortfolioSummary(
            records = records,
            quoteMap = quoteMap,
            triggerFilter = effectiveFilter,
        )
        PortfolioUiState(
            selectedTriggerFilter = effectiveFilter,
            totalRecordCount = records.size,
            triggerTagCounts = tagCounts,
            summary = summary,
            filteredRecords = filteredRecords,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioUiState(),
    )

    fun setPortfolioTriggerFilter(tag: String?) {
        _selectedTriggerFilter.value = tag
    }

    fun refreshAll(sendNotifications: Boolean = true) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 手動整理時同時更新恐懼與貪婪指數
                try {
                    val fng = marketRepo.fetchFearAndGreed(forceRefresh = true)
                    if (fng != null) {
                        _fearAndGreedFlow.value = fng
                    }
                } catch (_: Exception) {
                }
                // 僅在有設定維持率條件或台股標的時同步更新
                val rules = marketRepo.getEnabledRulesSnapshot()
                val needsMargin = rules.any {
                    it.condition is com.example.domain.model.MarginMaintenanceCondition ||
                    it.currency == MarketCurrency.TWD
                }
                if (needsMargin) {
                    val snap = marketRepo.fetchTwseMarginSnapshot()
                    if (snap != null) {
                        _marginSnapshotFlow.value = snap
                    }
                    val futures = marketRepo.fetchTaifexFuturesSnapshot()
                    if (futures != null) {
                        _futuresChipFlow.value = futures
                    }
                }
                val summary = marketRepo.refreshAllEnabledRules(sendNotifications = sendNotifications)
                _hasRateLimitWarning.value = summary.hasRateLimitOrNetworkIssue
                _snackbarMessage.value = summary.statusMessage
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun lookupTicker(rawInput: String) {
        viewModelScope.launch {
            _lookupState.value = LookupUiState.Loading
            when (val result = marketRepo.lookupTicker(rawInput)) {
                is TickerLookupResult.Success -> {
                    _lookupState.value = LookupUiState.Verified(result.quote)
                }
                is TickerLookupResult.RateLimited -> {
                    _lookupState.value = LookupUiState.Failed(result.message)
                }
                is TickerLookupResult.NotFound -> {
                    _lookupState.value = LookupUiState.Failed(result.message)
                }
                is TickerLookupResult.Error -> {
                    _lookupState.value = LookupUiState.Failed(result.message)
                }
            }
        }
    }

    fun resetLookupState(prefillQuote: StockQuote? = null) {
        _lookupState.value = if (prefillQuote != null) {
            LookupUiState.Verified(prefillQuote)
        } else {
            LookupUiState.Idle
        }
    }

    fun clearAlertLogs() {
        viewModelScope.launch {
            marketRepo.clearAlertLogs()
            _snackbarMessage.value = "已清空所有警報歷史紀錄"
        }
    }

    fun deleteAlertLog(id: Long) {
        viewModelScope.launch {
            marketRepo.deleteAlertLog(id)
            _snackbarMessage.value = "已刪除該筆歷史紀錄"
        }
    }

    fun saveWatchlistRule(
        existingId: Long? = null,
        symbol: String,
        displaySymbol: String,
        name: String,
        currency: MarketCurrency,
        condition: AlertCondition,
        plannedBuyAmount: Double,
        initialQuote: StockQuote?,
    ) {
        viewModelScope.launch {
            marketRepo.saveRule(
                existingId = existingId,
                symbol = symbol,
                displaySymbol = displaySymbol,
                name = name,
                currency = currency,
                condition = condition,
                plannedBuyAmount = plannedBuyAmount,
                initialQuote = initialQuote,
            )
            _snackbarMessage.value = if (existingId == null) {
                "已新增 $displaySymbol 監控規則"
            } else {
                "已更新 $displaySymbol 監控規則"
            }
        }
    }

    fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            marketRepo.toggleRuleEnabled(ruleId, enabled)
        }
    }

    fun resetRuleTrigger(ruleId: Long) {
        viewModelScope.launch {
            marketRepo.resetRuleTrigger(ruleId)
            _snackbarMessage.value = "已重置提醒狀態，下次觸及將重新通知"
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            marketRepo.deleteRule(ruleId)
            _snackbarMessage.value = "已刪除監控規則"
        }
    }

    fun setDataSourceMode(mode: DataSourceMode) {
        viewModelScope.launch {
            prefsRepo.setDataSourceMode(mode)
            _hasRateLimitWarning.value = false
            if (mode == DataSourceMode.SIMULATED) {
                _snackbarMessage.value = "已切換為模擬測試模式，可使用價格滑桿測試通知"
            } else {
                _snackbarMessage.value = "已切換為真實股市報價 (Yahoo Finance)"
                refreshAll(sendNotifications = false)
            }
        }
    }

    fun setBackgroundCheckEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setBackgroundCheckEnabled(enabled)
            WorkScheduler.syncPeriodicCheck(app, enabled)
            _snackbarMessage.value = if (enabled) {
                "已開啟每 15 分鐘背景定期檢查"
            } else {
                "已關閉背景定期檢查"
            }
        }
    }

    fun setHighFrequencyCheckEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setHighFrequencyCheckEnabled(enabled)
            if (enabled) {
                StockMonitorForegroundService.start(app)
                _snackbarMessage.value = "已啟動盤中高頻常駐監控"
            } else {
                StockMonitorForegroundService.stop(app)
                _snackbarMessage.value = "已停止盤中高頻常駐監控"
            }
        }
    }

    fun setHighFrequencyInterval(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.setHighFrequencyIntervalSeconds(seconds)
            val label = when (seconds) {
                15 -> "15 秒 (極速)"
                30 -> "30 秒 (快速)"
                60 -> "1 分鐘 (標準)"
                300 -> "5 分鐘 (節能)"
                else -> "${seconds}秒"
            }
            _snackbarMessage.value = "已將盤中巡檢頻率設定為 $label"
        }
    }

    fun updateSimulatedPrice(symbol: String, price: Double) {
        if (price <= 0.0) return
        viewModelScope.launch {
            val summary = marketRepo.updateSimulatedPriceAndEvaluate(
                symbol = symbol,
                newPrice = price,
                sendNotifications = true,
                ensurePeriodRuleIfMissing = null,
            )
            if (summary.triggeredCount > 0) {
                _snackbarMessage.value = "已觸發監控條件並發送系統通知！"
            }
        }
    }

    fun simulateDropBelowLevel(
        stock: SimulatedStockUi,
        targetLevel: SimulatedTargetLevel?,
    ) {
        viewModelScope.launch {
            val referencePrice = targetLevel?.referencePrice ?: stock.currentPrice
            val periodDays = targetLevel?.periodDays ?: 20
            val periodLabel = targetLevel?.label ?: MovingAverageCondition.maPeriodLabel(periodDays)
            val rawDrop = referencePrice * 0.98
            val targetDrop = if (stock.currency == MarketCurrency.TWD) {
                ((rawDrop * 10.0).toInt() / 10.0).coerceAtLeast(1.0)
            } else {
                ((rawDrop * 100.0).toInt() / 100.0).coerceAtLeast(0.5)
            }
            val summary = marketRepo.updateSimulatedPriceAndEvaluate(
                symbol = stock.symbol,
                newPrice = targetDrop,
                sendNotifications = true,
                ensurePeriodRuleIfMissing = periodDays,
                fallbackName = stock.name,
                fallbackCurrency = stock.currency,
            )
            _snackbarMessage.value = if (summary.triggeredCount > 0) {
                "${stock.displaySymbol} 已模擬跌破 $periodLabel（${Formatters.formatPrice(targetDrop, stock.currency)}），已發送提醒通知！"
            } else {
                "${stock.displaySymbol} 模擬價已調整為 ${Formatters.formatPrice(targetDrop, stock.currency)}"
            }
        }
    }

    fun sendTestNotification() {
        StockNotificationHelper.showTestNotification(app)
        _snackbarMessage.value = "已發送測試通知，點擊通知可直接開啟一鍵記帳"
    }

    fun openQuickBuyFromRule(item: WatchlistItemUi) {
        val currentPrice = item.quote?.currentPrice ?: 0.0
        val tag = item.evaluation?.triggerTag ?: item.rule.condition.conditionDisplayName
        _quickBuyDialogPrefill.value = QuickBuyPrefill(
            ruleId = item.rule.id,
            symbol = item.rule.symbol,
            displaySymbol = item.rule.displaySymbol,
            name = item.rule.name,
            currency = item.rule.currency,
            unitPrice = currentPrice,
            plannedAmount = item.rule.plannedBuyAmount,
            triggerTag = tag,
            buyDate = Formatters.formatDateYmd(),
        )
    }

    fun openQuickBuyFromNotification(prefill: QuickBuyPrefill) {
        _quickBuyDialogPrefill.value = prefill
    }

    fun focusRuleFromWidget(ruleId: Long) {
        if (ruleId > 0L) {
            _highlightedRuleId.value = ruleId
        }
    }

    fun consumeHighlightedRule() {
        _highlightedRuleId.value = null
    }

    suspend fun lookupQuoteForQuickBuy(rawSymbol: String): TickerLookupResult {
        val cleaned = rawSymbol.trim().uppercase()
        if (cleaned.isEmpty()) return TickerLookupResult.NotFound("請輸入股票代號")
        val cachedQuote = watchlistUiState.value.items.firstOrNull {
            !it.quote?.isSimulated.let { sim -> sim == true } &&
                (it.rule.symbol.equals(cleaned, ignoreCase = true) ||
                    it.rule.displaySymbol.equals(cleaned, ignoreCase = true))
        }?.quote
        if (cachedQuote != null && cachedQuote.currentPrice > 0.0) {
            return TickerLookupResult.Success(cachedQuote)
        }
        return marketRepo.lookupTicker(cleaned)
    }

    suspend fun searchTickerCandidates(query: String): List<com.example.domain.model.TickerSearchCandidate> {
        return marketRepo.searchTickers(query)
    }

    fun dismissQuickBuyDialog() {
        _quickBuyDialogPrefill.value = null
    }

    fun saveBuyRecord(
        existingId: Long? = null,
        symbol: String,
        displaySymbol: String,
        name: String,
        currency: MarketCurrency,
        buyDate: String,
        shares: Double,
        unitPrice: Double,
        triggerTag: String,
        resetRuleId: Long? = null,
    ) {
        viewModelScope.launch {
            val totalCost = shares * unitPrice
            val record = BuyRecordEntity(
                id = existingId ?: 0L,
                symbol = symbol.trim().uppercase(),
                displaySymbol = displaySymbol.trim().uppercase(),
                name = name.trim().ifEmpty { displaySymbol.trim().uppercase() },
                currency = currency,
                buyDate = buyDate.trim(),
                buyTimestamp = System.currentTimeMillis(),
                shares = shares,
                unitPrice = unitPrice,
                totalCost = totalCost,
                triggerTag = triggerTag.trim().ifEmpty { "手動買入" },
            )
            if (existingId != null && existingId > 0L) {
                portfolioRepo.updateRecord(record)
                _snackbarMessage.value = "已更新 $displaySymbol 買入紀錄"
            } else {
                portfolioRepo.insertRecord(record)
                _snackbarMessage.value = "已寫入 $displaySymbol 成交紀錄（${Formatters.formatAmount(totalCost, currency)}）"
            }
            if (resetRuleId != null) {
                marketRepo.resetRuleTrigger(resetRuleId)
            }
            _quickBuyDialogPrefill.value = null
        }
    }

    fun deleteBuyRecord(recordId: Long) {
        viewModelScope.launch {
            portfolioRepo.deleteRecord(recordId)
            _snackbarMessage.value = "已刪除該筆成交紀錄"
        }
    }

    fun exportBackup(targetUri: android.net.Uri) {
        viewModelScope.launch {
            val result = backupRepo.exportToUri(targetUri)
            if (result.isSuccess) {
                _snackbarMessage.value = "備份檔案已成功匯出"
            } else {
                _snackbarMessage.value = "匯出失敗：${result.exceptionOrNull()?.localizedMessage ?: "未知錯誤"}"
            }
        }
    }

    suspend fun getShareableBackupUri(filename: String): android.net.Uri? {
        val result = backupRepo.createShareableBackupUri(filename)
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            _snackbarMessage.value = "產生分享檔案失敗：${result.exceptionOrNull()?.localizedMessage ?: "未知錯誤"}"
            null
        }
    }

    fun importBackup(sourceUri: android.net.Uri) {
        viewModelScope.launch {
            when (val result = backupRepo.importFromUri(sourceUri)) {
                is com.example.data.backup.BackupRestoreResult.Success -> {
                    _snackbarMessage.value = "還原成功！已還原 ${result.rulesCount} 檔自選股與 ${result.recordsCount} 筆記帳"
                    refreshAll(sendNotifications = false)
                }
                is com.example.data.backup.BackupRestoreResult.Error -> {
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StockMonitorViewModel(application) as T
        }
    }
}
