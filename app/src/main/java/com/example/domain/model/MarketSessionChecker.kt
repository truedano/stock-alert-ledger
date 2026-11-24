package com.example.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Helper to determine market trading sessions and dynamic polling intervals.
 * Supports Taiwan Stock Exchange (TWSE/TPEx) and US Stock Markets (NYSE/NASDAQ).
 */
object MarketSessionChecker {

    private val TAIWAN_TZ = TimeZone.getTimeZone("Asia/Taipei")
    private val US_EASTERN_TZ = TimeZone.getTimeZone("America/New_York")

    const val INTERVAL_MARKET_OPEN_MS = 60_000L // 1 minute during active market
    const val INTERVAL_POST_MARKET_STANDBY_MS = 15 * 60_000L // 15 minutes when markets are closed

    /**
     * Determines whether the market for a given ticker or currency is currently in open trading session.
     *
     * Taiwan Stock Exchange (TWSE/TPEx):
     * - Monday to Friday: 09:00 - 13:35 (including closing auction buffer)
     *
     * US Markets (NYSE/NASDAQ):
     * - Monday to Friday: 09:30 - 16:00 Eastern Time
     */
    fun isTaiwanSymbol(symbol: String): Boolean {
        val trimmed = symbol.trim()
        return trimmed.endsWith(".TW", ignoreCase = true) ||
            trimmed.endsWith(".TWO", ignoreCase = true) ||
            trimmed.equals("^TWII", ignoreCase = true) ||
            trimmed.equals("^TWOII", ignoreCase = true)
    }

    fun isMarketOpen(
        symbol: String? = null,
        currency: MarketCurrency? = null,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val isTwStock = (symbol != null && isTaiwanSymbol(symbol)) ||
            currency == MarketCurrency.TWD

        return if (isTwStock) {
            isTaiwanMarketOpen(currentTimeMillis)
        } else {
            isUsMarketOpen(currentTimeMillis)
        }
    }

    /**
     * Checks if any market represented by the given symbols is currently open.
     * If no symbols are given, checks if either TW or US market is open.
     */
    fun isAnyMarketOpen(
        symbols: Collection<String>,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (symbols.isEmpty()) {
            return isTaiwanMarketOpen(currentTimeMillis) || isUsMarketOpen(currentTimeMillis)
        }
        val hasTw = symbols.any { isTaiwanSymbol(it) }
        val hasUs = symbols.any { !isTaiwanSymbol(it) }

        val twOpen = if (hasTw) isTaiwanMarketOpen(currentTimeMillis) else false
        val usOpen = if (hasUs) isUsMarketOpen(currentTimeMillis) else false

        return twOpen || usOpen
    }

    fun isTaiwanMarketOpen(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance(TAIWAN_TZ).apply {
            timeInMillis = currentTimeMillis
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minuteOfDay = hour * 60 + minute

        // 09:00 - 13:35 (9 * 60 = 540, 13 * 60 + 35 = 815)
        return minuteOfDay in 540..815
    }

    fun isUsMarketOpen(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance(US_EASTERN_TZ).apply {
            timeInMillis = currentTimeMillis
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minuteOfDay = hour * 60 + minute

        // 09:30 - 16:00 (9 * 60 + 30 = 570, 16 * 60 = 960)
        return minuteOfDay in 570..960
    }

    /**
     * Recommends next delay in milliseconds based on market open status.
     * In SIMULATED mode, always returns 1 minute.
     */
    fun getSuggestedPollingDelayMs(
        symbols: Collection<String>,
        isSimulatedMode: Boolean,
        userIntervalSeconds: Int = 60,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Long {
        val userOpenIntervalMs = (userIntervalSeconds.coerceIn(15, 600) * 1000L)
        if (isSimulatedMode) {
            return userOpenIntervalMs
        }
        val anyOpen = isAnyMarketOpen(symbols, currentTimeMillis)
        return if (anyOpen) userOpenIntervalMs else INTERVAL_POST_MARKET_STANDBY_MS
    }
}
