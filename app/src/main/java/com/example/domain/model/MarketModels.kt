package com.example.domain.model

enum class DataSourceMode {
    REAL,
    SIMULATED;

    val displayLabel: String
        get() = when (this) {
            REAL -> "真實報價"
            SIMULATED -> "模擬測試模式"
        }
}

data class StockQuote(
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val currentPrice: Double,
    val previousClose: Double?,
    val closeHistory: List<Double>,
    val lastUpdatedAt: Long,
    val isSimulated: Boolean,
    val marginInfo: com.example.data.remote.StockMarginInfo? = null,
    val candleHistory: List<CandleStickData> = emptyList(),
)

sealed interface TickerLookupResult {
    data class Success(val quote: StockQuote) : TickerLookupResult
    data class RateLimited(val message: String) : TickerLookupResult
    data class NotFound(val message: String) : TickerLookupResult
    data class Error(val message: String) : TickerLookupResult
}

data class RefreshSummary(
    val updatedCount: Int,
    val cachedFallbackCount: Int,
    val triggeredCount: Int,
    val statusMessage: String,
    val hasRateLimitOrNetworkIssue: Boolean,
)
