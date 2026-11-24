package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.CandleStickData
import com.example.domain.model.MarketCurrency
import com.example.domain.model.StockQuote

@Entity(tableName = "cached_quotes")
data class CachedQuoteEntity(
    @PrimaryKey val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val currentPrice: Double,
    val previousClose: Double?,
    val closeHistory: List<Double>,
    val lastUpdatedAt: Long,
    val isSimulated: Boolean = false,
    val candleHistory: List<CandleStickData> = emptyList(),
) {
    fun toStockQuote(): StockQuote = StockQuote(
        symbol = symbol,
        displaySymbol = displaySymbol,
        name = name,
        currency = currency,
        currentPrice = currentPrice,
        previousClose = previousClose,
        closeHistory = closeHistory,
        lastUpdatedAt = lastUpdatedAt,
        isSimulated = isSimulated,
        candleHistory = candleHistory,
    )
}
