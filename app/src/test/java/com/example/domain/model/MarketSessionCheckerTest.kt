package com.example.domain.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketSessionCheckerTest {

    private val taiwanTz = TimeZone.getTimeZone("Asia/Taipei")
    private val usTz = TimeZone.getTimeZone("America/New_York")

    private fun createTime(tz: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(tz).apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun taiwanMarket_openHours_returnsTrue() {
        // Wednesday at 10:30 Taiwan time
        val openTime = createTime(taiwanTz, 2026, 11, 18, 10, 30)
        assertTrue(MarketSessionChecker.isTaiwanMarketOpen(openTime))
        assertTrue(MarketSessionChecker.isMarketOpen(symbol = "2330.TW", currentTimeMillis = openTime))
    }

    @Test
    fun taiwanMarket_closingBuffer_returnsTrue() {
        // Wednesday at 13:34 Taiwan time
        val closingTime = createTime(taiwanTz, 2026, 11, 18, 13, 34)
        assertTrue(MarketSessionChecker.isTaiwanMarketOpen(closingTime))
    }

    @Test
    fun taiwanMarket_afterMarket_returnsFalse() {
        // Wednesday at 14:00 Taiwan time
        val afterClose = createTime(taiwanTz, 2026, 11, 18, 14, 0)
        assertFalse(MarketSessionChecker.isTaiwanMarketOpen(afterClose))
        assertFalse(MarketSessionChecker.isMarketOpen(symbol = "2330.TW", currentTimeMillis = afterClose))
    }

    @Test
    fun taiwanMarket_weekend_returnsFalse() {
        // Saturday at 10:00 Taiwan time
        val weekendTime = createTime(taiwanTz, 2026, 11, 21, 10, 0)
        assertFalse(MarketSessionChecker.isTaiwanMarketOpen(weekendTime))
    }

    @Test
    fun usMarket_openHours_returnsTrue() {
        // Wednesday at 11:00 US Eastern time
        val openTime = createTime(usTz, 2026, 11, 18, 11, 0)
        assertTrue(MarketSessionChecker.isUsMarketOpen(openTime))
        assertTrue(MarketSessionChecker.isMarketOpen(symbol = "AAPL", currentTimeMillis = openTime))
    }

    @Test
    fun usMarket_afterHours_returnsFalse() {
        // Wednesday at 18:00 US Eastern time
        val afterHours = createTime(usTz, 2026, 11, 18, 18, 0)
        assertFalse(MarketSessionChecker.isUsMarketOpen(afterHours))
        assertFalse(MarketSessionChecker.isMarketOpen(symbol = "AAPL", currentTimeMillis = afterHours))
    }

    @Test
    fun suggestedDelay_duringMarketHours_returnsUserInterval() {
        val openTime = createTime(taiwanTz, 2026, 11, 18, 10, 30)
        val delay15s = MarketSessionChecker.getSuggestedPollingDelayMs(
            symbols = listOf("2330.TW"),
            isSimulatedMode = false,
            userIntervalSeconds = 15,
            currentTimeMillis = openTime,
        )
        assertEquals(15_000L, delay15s)

        val delayDefault = MarketSessionChecker.getSuggestedPollingDelayMs(
            symbols = listOf("2330.TW"),
            isSimulatedMode = false,
            userIntervalSeconds = 60,
            currentTimeMillis = openTime,
        )
        assertEquals(60_000L, delayDefault)
    }

    @Test
    fun suggestedDelay_postMarket_returnsStandbyDelay() {
        val closedTime = createTime(taiwanTz, 2026, 11, 21, 10, 0) // weekend
        val delay = MarketSessionChecker.getSuggestedPollingDelayMs(
            symbols = listOf("2330.TW"),
            isSimulatedMode = false,
            currentTimeMillis = closedTime,
        )
        assertEquals(MarketSessionChecker.INTERVAL_POST_MARKET_STANDBY_MS, delay)
    }

    @Test
    fun taiwanIndices_classifiedAsTaiwanSession() {
        val twOpenTime = createTime(taiwanTz, 2026, 11, 18, 10, 30)
        val twAfterClose = createTime(taiwanTz, 2026, 11, 18, 14, 0)

        assertTrue(MarketSessionChecker.isMarketOpen(symbol = "^TWII", currentTimeMillis = twOpenTime))
        assertTrue(MarketSessionChecker.isMarketOpen(symbol = "^TWOII", currentTimeMillis = twOpenTime))
        assertTrue(MarketSessionChecker.isAnyMarketOpen(listOf("^TWII", "^TWOII"), twOpenTime))

        assertFalse(MarketSessionChecker.isMarketOpen(symbol = "^TWII", currentTimeMillis = twAfterClose))
        assertFalse(MarketSessionChecker.isMarketOpen(symbol = "^TWOII", currentTimeMillis = twAfterClose))
        assertFalse(MarketSessionChecker.isAnyMarketOpen(listOf("^TWII", "^TWOII"), twAfterClose))
    }

    @Test
    fun suggestedDelay_simulatedMode_alwaysOneMinute() {
        val closedTime = createTime(taiwanTz, 2026, 11, 21, 10, 0) // weekend
        val delay = MarketSessionChecker.getSuggestedPollingDelayMs(
            symbols = listOf("2330.TW"),
            isSimulatedMode = true,
            currentTimeMillis = closedTime,
        )
        assertEquals(MarketSessionChecker.INTERVAL_MARKET_OPEN_MS, delay)
    }
}
