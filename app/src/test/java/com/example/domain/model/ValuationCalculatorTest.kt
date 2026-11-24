package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationCalculatorTest {

    @Test
    fun `deduplicateAndFilterTop10 removes GOOG, BRK-A, cross-listed TSM, and invalid fundamentals`() {
        val candidates = listOf(
            RawScreenerQuote("AAPL", marketCap = 5_000_000L, regularMarketPrice = 349.0, trailingPE = 38.8, forwardPE = 33.0),
            RawScreenerQuote("NVDA", marketCap = 4_000_000L, regularMarketPrice = 165.0, trailingPE = 17.7, forwardPE = 9.9),
            RawScreenerQuote("MSFT", marketCap = 3_600_000L, regularMarketPrice = 492.0, trailingPE = 25.7, forwardPE = 20.3),
            RawScreenerQuote("GOOGL", marketCap = 3_468_000L, regularMarketPrice = 282.0, trailingPE = 14.7, forwardPE = 18.4),
            RawScreenerQuote("GOOG", marketCap = 3_436_000L, regularMarketPrice = 279.0, trailingPE = 14.6, forwardPE = 18.2),
            RawScreenerQuote("AMZN", marketCap = 2_761_000L, regularMarketPrice = 255.0, trailingPE = 20.6, forwardPE = 23.3),
            RawScreenerQuote("TSM", marketCap = 1_885_000L, regularMarketPrice = 363.0, trailingPE = 23.5, forwardPE = 15.5),
            RawScreenerQuote("META", marketCap = 1_527_000L, regularMarketPrice = 597.0, trailingPE = 20.3, forwardPE = 16.8),
            RawScreenerQuote("TSLA", marketCap = 1_523_000L, regularMarketPrice = 385.0, trailingPE = 423.5, forwardPE = 178.5),
            RawScreenerQuote("AVGO", marketCap = 1_496_000L, regularMarketPrice = 313.0, trailingPE = 40.0, forwardPE = 16.2),
            RawScreenerQuote("BRK-A", marketCap = 1_216_900L, regularMarketPrice = 859191.0, trailingPE = 16.0, bookValue = 536413.0),
            RawScreenerQuote("BRK-B", marketCap = 1_215_900L, regularMarketPrice = 572.36, trailingPE = 16.0, forwardPE = 25.0, bookValue = 536413.0),
            RawScreenerQuote("LLY", marketCap = 1_215_500L, regularMarketPrice = 1364.6, trailingPE = 44.2, forwardPE = 26.8),
            RawScreenerQuote("NOFUND", marketCap = 1_200_000L, regularMarketPrice = 100.0, trailingPE = -5.0, forwardPE = null, bookValue = -2.0),
            RawScreenerQuote("WMT", marketCap = 1_100_000L, regularMarketPrice = 120.0, trailingPE = 30.0, forwardPE = 26.0),
        )

        val top10 = ValuationCalculator.deduplicateAndFilterTop10(candidates, ValuationMarket.US)
        assertEquals(10, top10.size)
        val symbols = top10.map { it.symbol }
        assertTrue(symbols.contains("GOOGL"))
        assertFalse(symbols.contains("GOOG"))
        assertTrue(symbols.contains("BRK-B"))
        assertFalse(symbols.contains("BRK-A"))
        assertFalse(symbols.contains("TSM"))
        assertFalse(symbols.contains("NOFUND"))
        assertEquals(listOf("AAPL", "NVDA", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "AVGO", "BRK-B", "LLY"), symbols)
    }

    @Test
    fun `sanitizeBrkB fixes Yahoo 1500x book value anomaly`() {
        val (fixedBook, fixedPb) = ValuationCalculator.sanitizeBrkB(
            symbol = "BRK-B",
            currentPrice = 572.36,
            rawBookValue = 536413.0,
            rawPb = 0.001067,
        )
        assertNotNull(fixedBook)
        assertNotNull(fixedPb)
        assertEquals(357.608, fixedBook!!, 0.01)
        assertEquals(1.6005, fixedPb!!, 0.01)
    }

    @Test
    fun `computeHistoricalPeBand filters outliers and computes P20 P50 P80 percentiles`() {
        val rawHistory = listOf(0.5, 1.2, 15.0, 18.0, 20.0, 22.0, 25.0, 28.0, 30.0, 250.0)
        val band = ValuationCalculator.computeHistoricalPeBand(rawHistory, ValuationCalculator.DEFAULT_TW_PE_BAND)

        assertTrue(band.isAutoCalibrated)
        assertEquals(7, band.sampleCount)
        assertTrue(band.lowMultiple < band.fairMultiple)
        assertTrue(band.fairMultiple < band.highMultiple)
        assertEquals(22.0, band.fairMultiple, 0.01)
    }

    @Test
    fun `computeHistoricalPeBand falls back when samples are fewer than 4`() {
        val band = ValuationCalculator.computeHistoricalPeBand(
            rawHistory = listOf(18.0, 22.0),
            fallback = ValuationCalculator.DEFAULT_TW_PE_BAND,
        )
        assertFalse(band.isAutoCalibrated)
        assertEquals(ValuationCalculator.DEFAULT_TW_PE_BAND, band)
    }

    @Test
    fun `financial stocks and BRK-B prioritize PB valuation while tech stocks prioritize Forward PE`() {
        val tsmc = ValuationCalculator.evaluateStockValuation(
            rank = 1,
            raw = RawScreenerQuote(
                symbol = "2330.TW",
                regularMarketPrice = 1000.0,
                marketCap = 25_000_000_000_000L,
                trailingPE = 22.0,
                forwardPE = 16.0,
                epsTrailingTwelveMonths = 45.45,
                epsForward = 62.5,
                priceToBook = 6.0,
                bookValue = 166.6,
            ),
            market = ValuationMarket.TW,
            peBand = HistoricalMultipleBand(16.0, 20.0, 25.0, 8, true),
            pbBand = HistoricalMultipleBand(4.0, 5.5, 7.0, 8, true),
        )
        assertEquals(ValuationMethod.PE_PRIORITY, tsmc.primaryMethod)
        assertEquals(1250.0, tsmc.activePriceBand.fairPrice, 0.1)
        assertEquals(ValuationTier.CHEAP, tsmc.tier) // 1000 == cheapPrice (62.5 * 16 = 1000)
        assertTrue(tsmc.impliedEpsGrowthPercent!! > 30.0)

        val fubon = ValuationCalculator.evaluateStockValuation(
            rank = 7,
            raw = RawScreenerQuote(
                symbol = "2881.TW",
                regularMarketPrice = 143.5,
                marketCap = 1_960_000_000_000L,
                trailingPE = 17.9,
                forwardPE = 13.8,
                epsTrailingTwelveMonths = 8.01,
                epsForward = 10.36,
                priceToBook = 1.60,
                bookValue = 89.357,
            ),
            market = ValuationMarket.TW,
            peBand = HistoricalMultipleBand(9.0, 12.0, 15.0, 6, true),
            pbBand = HistoricalMultipleBand(1.15, 1.40, 1.75, 6, true),
        )
        assertEquals(ValuationMethod.PB_PRIORITY, fubon.primaryMethod)
        // Fair price uses PB: 89.357 * 1.40 = 125.10
        assertEquals(89.357 * 1.40, fubon.activePriceBand.fairPrice, 0.1)
        assertEquals(ValuationTier.FAIR_HIGH, fubon.tier)
    }
}
