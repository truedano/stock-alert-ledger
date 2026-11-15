package com.example.domain.model

data class StockHoldingSummary(
    val symbol: String,
    val name: String,
    val currency: MarketCurrency,
    val totalShares: Double,
    val totalCost: Double,
    val averageCost: Double,
    val recordCount: Int,
    val latestCachedPrice: Double?,
) {
    val marketValue: Double?
        get() = latestCachedPrice?.let { it * totalShares }

    val unrealizedPnlPercent: Double?
        get() = if (latestCachedPrice != null && averageCost > 0.0) {
            ((latestCachedPrice - averageCost) / averageCost) * 100.0
        } else {
            null
        }
}

data class PortfolioSummary(
    val twdTotalInvested: Double,
    val usdTotalInvested: Double,
    val holdings: List<StockHoldingSummary>,
)
