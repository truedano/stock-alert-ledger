package com.example.ui.screens

import com.example.data.local.WatchlistRuleEntity
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCondition
import com.example.ui.WatchlistItemUi
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistFilterTest {

    private fun ruleItem(
        id: Long,
        symbol: String,
        currency: MarketCurrency,
        isTriggered: Boolean = false,
        isEnabled: Boolean = true,
    ): WatchlistItemUi {
        return WatchlistItemUi(
            rule = WatchlistRuleEntity(
                id = id,
                symbol = symbol,
                displaySymbol = symbol,
                name = symbol,
                currency = currency,
                condition = MovingAverageCondition(periodDays = 20),
                plannedBuyAmount = 10000.0,
                isEnabled = isEnabled,
                isTriggered = isTriggered,
            ),
            quote = null,
            evaluation = null,
            estimatedShares = 0.0,
        )
    }

    @Test
    fun `filters watchlist items by category accurately`() {
        val items = listOf(
            ruleItem(1L, "^TWII", MarketCurrency.TWD, isTriggered = true),
            ruleItem(2L, "2330.TW", MarketCurrency.TWD, isTriggered = false),
            ruleItem(3L, "0050.TW", MarketCurrency.TWD, isEnabled = false),
            ruleItem(4L, "NVDA", MarketCurrency.USD, isTriggered = true),
        )

        assertEquals(4, filterWatchlistItems(items, WatchlistFilter.ALL).size)
        assertEquals(listOf(1L, 4L), filterWatchlistItems(items, WatchlistFilter.TRIGGERED).map { it.rule.id })
        assertEquals(listOf(1L), filterWatchlistItems(items, WatchlistFilter.INDEX).map { it.rule.id })
        assertEquals(listOf(2L, 3L), filterWatchlistItems(items, WatchlistFilter.TAIWAN).map { it.rule.id })
        assertEquals(listOf(4L), filterWatchlistItems(items, WatchlistFilter.US).map { it.rule.id })
        assertEquals(listOf(3L), filterWatchlistItems(items, WatchlistFilter.PAUSED).map { it.rule.id })
    }
}
