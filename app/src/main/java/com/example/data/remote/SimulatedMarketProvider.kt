package com.example.data.remote

import com.example.data.local.AppPreferencesState
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCalculator
import com.example.domain.model.StockQuote
import kotlin.math.sin

data class SimulatedStockProfile(
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val minSliderPrice: Double,
    val maxSliderPrice: Double,
    val defaultPrice: Double,
    val closeHistory: List<Double>,
) {
    val ma20: Double
        get() = MovingAverageCalculator.calculateSma(closeHistory, 20) ?: defaultPrice

    val ma60: Double
        get() = MovingAverageCalculator.calculateSma(closeHistory, 60) ?: defaultPrice
}

object SimulatedMarketProvider {
    private val tsmcHistory: List<Double> = List(250) { idx ->
        // Gradual uptrend from ~955 to ~1050 with gentle oscillation so 5/10/20/60/120/240MA are all realistic
        val base = 955.0 + (idx / 249.0) * 90.0
        val wave = sin(idx * 0.18) * 14.0
        ((base + wave) * 10.0).toInt() / 10.0
    }

    private val aaplHistory: List<Double> = List(250) { idx ->
        // Gradual uptrend from ~198 to ~229
        val base = 198.0 + (idx / 249.0) * 30.0
        val wave = sin(idx * 0.21) * 3.8
        ((base + wave) * 100.0).toInt() / 100.0
    }

    val profiles: List<SimulatedStockProfile> = listOf(
        SimulatedStockProfile(
            symbol = "2330.TW",
            displaySymbol = "2330.TW",
            name = "台積電 (模擬)",
            currency = MarketCurrency.TWD,
            minSliderPrice = 880.0,
            maxSliderPrice = 1140.0,
            defaultPrice = 1045.0,
            closeHistory = tsmcHistory,
        ),
        SimulatedStockProfile(
            symbol = "AAPL",
            displaySymbol = "AAPL",
            name = "Apple Inc. (模擬)",
            currency = MarketCurrency.USD,
            minSliderPrice = 185.0,
            maxSliderPrice = 255.0,
            defaultPrice = 229.0,
            closeHistory = aaplHistory,
        ),
    )

    fun findProfile(rawInput: String): SimulatedStockProfile? {
        val cleaned = rawInput.trim().uppercase()
        return profiles.firstOrNull { profile ->
            profile.symbol == cleaned ||
                profile.symbol.removeSuffix(".TW") == cleaned ||
                profile.symbol.removeSuffix(".TWO") == cleaned
        }
    }

    fun quoteFor(symbol: String, prefs: AppPreferencesState): StockQuote? {
        val profile = findProfile(symbol) ?: return null
        val price = prefs.simulatedPriceFor(profile.symbol) ?: profile.defaultPrice
        val marginInfo = if (profile.currency == MarketCurrency.TWD) {
            val estimatedRate = com.example.domain.model.MovingAverageCalculator
                .calculateEstimatedMarginKeepRate(price, profile.closeHistory)
            StockMarginInfo(
                symbol = profile.symbol,
                name = profile.name,
                marginBalance = 29764L,
                marginChange = -3020L,
                shortBalance = 59L,
                shortChange = -52L,
                estimatedKeepRate = estimatedRate,
            )
        } else null

        val candles = profile.closeHistory.mapIndexed { idx, c ->
            val prev = if (idx > 0) profile.closeHistory[idx - 1] else c
            com.example.domain.model.CandleStickData(
                timestamp = System.currentTimeMillis() - (profile.closeHistory.size - idx) * 86400000L,
                open = prev,
                high = maxOf(prev, c) * 1.008,
                low = minOf(prev, c) * 0.992,
                close = c,
                volume = 35000000L,
            )
        }

        return StockQuote(
            symbol = profile.symbol,
            displaySymbol = profile.displaySymbol,
            name = profile.name,
            currency = profile.currency,
            currentPrice = price,
            previousClose = profile.closeHistory.lastOrNull(),
            closeHistory = profile.closeHistory,
            lastUpdatedAt = System.currentTimeMillis(),
            isSimulated = true,
            marginInfo = marginInfo,
            candleHistory = candles,
        )
    }
}
