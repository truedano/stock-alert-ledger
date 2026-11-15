package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.StockApplication
import com.example.data.local.AppPreferencesState
import com.example.data.local.BuyRecordEntity
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
import com.example.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "尚未同步報價",
    val isRefreshing: Boolean = false,
    val hasRateLimitWarning: Boolean = false,
    val items: List<WatchlistItemUi> = emptyList(),
    val watchlistSimulatedStocks: List<SimulatedStockUi> = emptyList(),
    val builtInSimulatedStocks: List<SimulatedStockUi> = emptyList(),
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

    private val _isRefreshing = MutableStateFlow(false)
    private val _hasRateLimitWarning = MutableStateFlow(false)
    private val _selectedTriggerFilter = MutableStateFlow<String?>(null)
    private val _lookupState = MutableStateFlow<LookupUiState>(LookupUiState.Idle)
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()

    private val _quickBuyDialogPrefill = MutableStateFlow<QuickBuyPrefill?>(null)
    val quickBuyDialogPrefill: StateFlow<QuickBuyPrefill?> = _quickBuyDialogPrefill.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val preferencesState: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesState(),
        )

    val watchlistUiState: StateFlow<WatchlistUiState> = combine(
        marketRepo.rulesFlow,
        marketRepo.cachedQuotesFlow,
        prefsRepo.preferencesFlow,
        _isRefreshing,
        _hasRateLimitWarning,
    ) { rules, cachedQuotes, prefs, refreshing, rateWarning ->
        val quoteMap = cachedQuotes.associate { it.symbol to it.toStockQuote() }
        val items = rules.map { rule ->
            val effectiveQuote = if (prefs.dataSourceMode == DataSourceMode.SIMULATED) {
                val cached = quoteMap[rule.symbol]
                val overridePrice = prefs.simulatedPriceFor(rule.symbol)
                when {
                    cached != null -> cached.copy(
                        currentPrice = overridePrice ?: cached.currentPrice,
                        isSimulated = true,
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
            lastSyncTimestamp = prefs.lastSyncTimestamp,
            lastSyncMessage = prefs.lastSyncMessage,
            isRefreshing = refreshing,
            hasRateLimitWarning = rateWarning,
            items = items,
            watchlistSimulatedStocks = watchlistSimStocks,
            builtInSimulatedStocks = builtInSimStocks,
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
