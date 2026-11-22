package com.example.domain.model

/**
 * Represents a tradable stock or ETF target suggested when an index rule triggers.
 */
data class ProxyBuyTarget(
    val symbol: String,
    val name: String,
    val currency: MarketCurrency,
)

fun isMarketIndexSymbol(symbol: String): Boolean {
    return symbol.trim().startsWith("^")
}

fun recommendedProxyTargetsForIndex(indexSymbol: String): List<ProxyBuyTarget> {
    return when (indexSymbol.trim().uppercase()) {
        "^TWII" -> listOf(
            ProxyBuyTarget("0050.TW", "元大台灣50", MarketCurrency.TWD),
            ProxyBuyTarget("00631L.TW", "元大台灣50正2", MarketCurrency.TWD),
            ProxyBuyTarget("006208.TW", "富邦台50", MarketCurrency.TWD),
        )
        "^TWOII" -> listOf(
            ProxyBuyTarget("0051.TW", "元大中型100", MarketCurrency.TWD),
            ProxyBuyTarget("00733.TW", "富邦臺灣中小", MarketCurrency.TWD),
        )
        "^GSPC" -> listOf(
            ProxyBuyTarget("VOO", "Vanguard S&P 500", MarketCurrency.USD),
            ProxyBuyTarget("SPY", "SPDR S&P 500", MarketCurrency.USD),
            ProxyBuyTarget("00646.TW", "元大S&P500", MarketCurrency.TWD),
        )
        "^IXIC" -> listOf(
            ProxyBuyTarget("QQQ", "Invesco QQQ", MarketCurrency.USD),
            ProxyBuyTarget("00662.TW", "富邦NASDAQ", MarketCurrency.TWD),
        )
        "^DJI" -> listOf(
            ProxyBuyTarget("DIA", "SPDR道瓊", MarketCurrency.USD),
            ProxyBuyTarget("00647L.TW", "元大S&P500正2", MarketCurrency.TWD),
        )
        "^SOX" -> listOf(
            ProxyBuyTarget("SOXX", "iShares半導體", MarketCurrency.USD),
            ProxyBuyTarget("00830.TW", "國泰費城半導體", MarketCurrency.TWD),
        )
        else -> listOf(
            ProxyBuyTarget("0050.TW", "元大台灣50", MarketCurrency.TWD),
            ProxyBuyTarget("00631L.TW", "元大台灣50正2", MarketCurrency.TWD),
            ProxyBuyTarget("VOO", "Vanguard S&P 500", MarketCurrency.USD),
            ProxyBuyTarget("QQQ", "Invesco QQQ", MarketCurrency.USD),
        )
    }
}
