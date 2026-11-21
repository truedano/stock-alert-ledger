package com.example

import com.example.data.local.BuyRecordEntity
import com.example.data.local.CachedQuoteEntity
import com.example.data.local.Converters
import com.example.data.remote.YahooFinanceService
import com.example.data.repository.PortfolioRepository
import com.example.domain.model.ConditionEngine
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.MovingAverageCalculator
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.MovingAverageTriggerMode
import com.example.domain.model.TickerLookupResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `sma calculation computes last N days and safely degrades when insufficient`() {
        val closes = listOf(100.0, 102.0, 104.0, 106.0, 108.0)
        assertEquals(104.0, MovingAverageCalculator.calculateSma(closes, 5)!!, 0.001)
        assertEquals(106.0, MovingAverageCalculator.calculateSma(closes, 3)!!, 0.001)
        assertNull(MovingAverageCalculator.calculateSma(closes, 20))
        assertNull(MovingAverageCalculator.calculateSma(emptyList(), 5))
    }

    @Test
    fun `condition engine evaluates BELOW_OR_EQUAL and WITHIN_PERCENT accurately`() {
        val closes = List(20) { 100.0 }
        val condBelow = MovingAverageCondition(20, MovingAverageTriggerMode.BELOW_OR_EQUAL)
        val condWithin = MovingAverageCondition(20, MovingAverageTriggerMode.WITHIN_PERCENT, 2.0)

        val aboveFar = ConditionEngine.evaluate(condBelow, MarketSnapshot("2330.TW", 105.0, closes))
        assertFalse(aboveFar.isTriggered)
        assertEquals(5.0, aboveFar.distancePercent!!, 0.001)

        val atMa = ConditionEngine.evaluate(condBelow, MarketSnapshot("2330.TW", 100.0, closes))
        assertTrue(atMa.isTriggered)

        val withinTwoPct = ConditionEngine.evaluate(condWithin, MarketSnapshot("2330.TW", 101.5, closes))
        assertTrue(withinTwoPct.isTriggered)

        val outsideTwoPct = ConditionEngine.evaluate(condWithin, MarketSnapshot("2330.TW", 103.0, closes))
        assertFalse(outsideTwoPct.isTriggered)
    }

    @Test
    fun `room type converter round-trips polymorphic AlertCondition`() {
        val converters = Converters()
        val original = MovingAverageCondition(
            periodDays = 60,
            triggerMode = MovingAverageTriggerMode.WITHIN_PERCENT,
            tolerancePercent = 1.5,
        )
        val json = converters.fromAlertCondition(original)
        val decoded = converters.toAlertCondition(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `yahoo smart ticker resolution builds TW then TWO for digits and direct for US`() {
        val service = YahooFinanceService()
        assertEquals(listOf("2330.TW", "2330.TWO"), service.buildCandidateSymbols("2330"))
        assertEquals(listOf("00878.TW", "00878.TWO"), service.buildCandidateSymbols("00878"))
        assertEquals(listOf("6488.TWO"), service.buildCandidateSymbols("6488.TWO"))
        assertEquals(listOf("AAPL"), service.buildCandidateSymbols("AAPL"))
        assertEquals(listOf("^TWII"), service.buildCandidateSymbols("^TWII"))
        assertEquals(listOf("^GSPC"), service.buildCandidateSymbols("^GSPC"))
    }

    @Test
    fun `yahoo chart parser filters null close prices and extracts metadata`() {
        val sampleJson = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "symbol": "2330.TW",
                      "currency": "TWD",
                      "shortName": "TSMC",
                      "regularMarketPrice": 1025.0,
                      "chartPreviousClose": 1030.0
                    },
                    "indicators": {
                      "quote": [
                        {
                          "close": [1010.0, null, 1020.0, 1030.0, null, 1025.0]
                        }
                      ]
                    }
                  }
                ],
                "error": null
              }
            }
        """.trimIndent()

        val result = YahooFinanceService().parseChartJson("2330.TW", sampleJson)
        assertTrue(result is TickerLookupResult.Success)
        val quote = (result as TickerLookupResult.Success).quote
        assertEquals("2330.TW", quote.symbol)
        assertEquals("TSMC", quote.name)
        assertEquals(MarketCurrency.TWD, quote.currency)
        assertEquals(1025.0, quote.currentPrice, 0.001)
        assertEquals(listOf(1010.0, 1020.0, 1030.0, 1025.0), quote.closeHistory)
    }

    @Test
    fun `portfolio summary separates TWD and USD totals and computes average cost`() {
        val records = listOf(
            BuyRecordEntity(
                id = 1,
                symbol = "2330.TW",
                displaySymbol = "2330.TW",
                name = "台積電",
                currency = MarketCurrency.TWD,
                buyDate = "2026/11/10",
                buyTimestamp = 1000L,
                shares = 100.0,
                unitPrice = 1000.0,
                totalCost = 100000.0,
                triggerTag = "觸及 20MA",
            ),
            BuyRecordEntity(
                id = 2,
                symbol = "2330.TW",
                displaySymbol = "2330.TW",
                name = "台積電",
                currency = MarketCurrency.TWD,
                buyDate = "2026/11/15",
                buyTimestamp = 2000L,
                shares = 100.0,
                unitPrice = 900.0,
                totalCost = 90000.0,
                triggerTag = "觸及 60MA",
            ),
            BuyRecordEntity(
                id = 3,
                symbol = "AAPL",
                displaySymbol = "AAPL",
                name = "Apple Inc.",
                currency = MarketCurrency.USD,
                buyDate = "2026/11/15",
                buyTimestamp = 3000L,
                shares = 2.5,
                unitPrice = 200.0,
                totalCost = 500.0,
                triggerTag = "手動買入",
            ),
        )
        val cached = mapOf(
            "AAPL" to CachedQuoteEntity(
                symbol = "AAPL",
                displaySymbol = "AAPL",
                name = "Apple Inc.",
                currency = MarketCurrency.USD,
                currentPrice = 220.0,
                previousClose = 215.0,
                closeHistory = listOf(215.0, 220.0),
                lastUpdatedAt = 3000L,
            )
        )

        val summary = PortfolioRepository.buildPortfolioSummary(records, cached)
        assertEquals(190000.0, summary.twdTotalInvested, 0.001)
        assertEquals(500.0, summary.usdTotalInvested, 0.001)
        val tsmc = summary.holdings.first { it.symbol == "2330.TW" }
        assertEquals(200.0, tsmc.totalShares, 0.001)
        assertEquals(950.0, tsmc.averageCost, 0.001)
        assertNotNull(summary.holdings.first { it.symbol == "AAPL" }.unrealizedPnlPercent)
        assertEquals("NT$ 10,000", Formatters.formatAmount(10000.0, MarketCurrency.TWD))
        assertEquals("US$ 500.00", Formatters.formatAmount(500.0, MarketCurrency.USD))
    }

    @Test
    fun `trigger condition labels and portfolio trigger filter work for 240MA annual line`() {
        val cond240 = MovingAverageCondition(periodDays = 240)
        assertEquals("年線 240MA", cond240.shortTriggerTag)

        val records = listOf(
            BuyRecordEntity(
                id = 10,
                symbol = "0050.TW",
                displaySymbol = "0050.TW",
                name = "元大台灣50",
                currency = MarketCurrency.TWD,
                buyDate = "2026/11/15",
                buyTimestamp = 1000L,
                shares = 200.0,
                unitPrice = 185.0,
                totalCost = 37000.0,
                triggerTag = "年線 240MA",
            ),
            BuyRecordEntity(
                id = 11,
                symbol = "0050.TW",
                displaySymbol = "0050.TW",
                name = "元大台灣50",
                currency = MarketCurrency.TWD,
                buyDate = "2026/11/10",
                buyTimestamp = 500L,
                shares = 100.0,
                unitPrice = 195.0,
                totalCost = 19500.0,
                triggerTag = "月線 20MA",
            ),
        )

        val filtered240 = PortfolioRepository.buildPortfolioSummary(
            records = records,
            quoteMap = emptyMap(),
            triggerFilter = "年線 240MA",
        )
        assertEquals(37000.0, filtered240.twdTotalInvested, 0.001)
        assertEquals(1, filtered240.holdings.size)
        assertEquals(200.0, filtered240.holdings.first().totalShares, 0.001)
        assertEquals(185.0, filtered240.holdings.first().averageCost, 0.001)

        val tagCounts = PortfolioRepository.buildTriggerTagCounts(records)
        assertEquals(2, tagCounts.size)
    }

    @Test
    fun `alert log entity correctly records trigger details and formatting`() {
        val log = com.example.data.local.AlertLogEntity(
            id = 1L,
            ruleId = 10L,
            symbol = "2330.TW",
            displaySymbol = "2330",
            name = "台積電",
            currency = MarketCurrency.TWD,
            triggerTag = "月線 20MA",
            triggeredPrice = 980.0,
            triggeredTimestamp = 1732156800000L,
            triggeredDateKey = "2026-11-21",
            note = "跌破 20MA (990.0)",
        )

        assertEquals("2330", log.displaySymbol)
        assertEquals(980.0, log.triggeredPrice, 0.001)
        assertEquals("月線 20MA", log.triggerTag)
        assertTrue(log.note.contains("跌破"))
    }
}
