package com.example.data.repository

import com.example.data.local.BuyRecordDao
import com.example.data.local.BuyRecordEntity
import com.example.data.local.CachedQuoteDao
import com.example.data.local.CachedQuoteEntity
import com.example.domain.model.MarketCurrency
import com.example.domain.model.PortfolioSummary
import com.example.domain.model.StockHoldingSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class TriggerTagCount(
    val tag: String,
    val count: Int,
)

class PortfolioRepository(
    private val buyRecordDao: BuyRecordDao,
    private val cachedQuoteDao: CachedQuoteDao,
) {
    val recordsFlow: Flow<List<BuyRecordEntity>> = buyRecordDao.observeAllRecords()
    val cachedQuotesFlow: Flow<List<CachedQuoteEntity>> = cachedQuoteDao.observeAllQuotes()

    val portfolioSummaryFlow: Flow<PortfolioSummary> = combine(
        buyRecordDao.observeAllRecords(),
        cachedQuoteDao.observeAllQuotes(),
    ) { records, cachedQuotes ->
        val quoteMap = cachedQuotes.associateBy { it.symbol }
        buildPortfolioSummary(records, quoteMap)
    }

    suspend fun insertRecord(record: BuyRecordEntity): Long = withContext(Dispatchers.IO) {
        buyRecordDao.insertRecord(record)
    }

    suspend fun updateRecord(record: BuyRecordEntity) = withContext(Dispatchers.IO) {
        buyRecordDao.updateRecord(record)
    }

    suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        buyRecordDao.deleteRecordById(id)
    }

    companion object {
        fun buildTriggerTagCounts(records: List<BuyRecordEntity>): List<TriggerTagCount> {
            return records
                .groupingBy { it.triggerTag.trim().ifEmpty { "手動買入" } }
                .eachCount()
                .entries
                .map { TriggerTagCount(tag = it.key, count = it.value) }
                .sortedByDescending { it.count }
        }

        fun buildPortfolioSummary(
            records: List<BuyRecordEntity>,
            quoteMap: Map<String, CachedQuoteEntity>,
            triggerFilter: String? = null,
        ): PortfolioSummary {
            val filtered = if (triggerFilter.isNullOrBlank()) {
                records
            } else {
                records.filter { it.triggerTag.trim() == triggerFilter }
            }

            val twdTotal = filtered
                .filter { it.currency == MarketCurrency.TWD }
                .sumOf { it.totalCost }
            val usdTotal = filtered
                .filter { it.currency == MarketCurrency.USD }
                .sumOf { it.totalCost }

            val holdings = filtered
                .groupBy { it.symbol.uppercase() to it.currency }
                .values
                .map { group ->
                    val latest = group.first()
                    val totalShares = group.sumOf { it.shares }
                    val totalCost = group.sumOf { it.totalCost }
                    val avgCost = if (totalShares > 0.0) totalCost / totalShares else 0.0
                    val cachedPrice = quoteMap[latest.symbol]?.currentPrice
                    StockHoldingSummary(
                        symbol = latest.displaySymbol,
                        name = latest.name,
                        currency = latest.currency,
                        totalShares = totalShares,
                        totalCost = totalCost,
                        averageCost = avgCost,
                        recordCount = group.size,
                        latestCachedPrice = cachedPrice,
                    )
                }
                .sortedWith(compareBy<StockHoldingSummary> { it.currency.ordinal }.thenByDescending { it.totalCost })

            return PortfolioSummary(
                twdTotalInvested = twdTotal,
                usdTotalInvested = usdTotal,
                holdings = holdings,
            )
        }
    }
}
