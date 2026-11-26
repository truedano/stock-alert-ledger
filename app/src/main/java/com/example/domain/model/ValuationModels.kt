package com.example.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
enum class ValuationMarket(val label: String, val regionCode: String) {
    TW("台股前十大", "tw"),
    US("美股前十大", "us"),
    CUSTOM("自選與搜尋", "custom"),
}

@Serializable
enum class ValuationMethod(val label: String) {
    PE_PRIORITY("預估本益比為主"),
    PB_PRIORITY("股價淨值比為主"),
}

@Serializable
enum class ValuationTier(val label: String, val shortLabel: String) {
    CHEAP("便宜低估區", "便宜區"),
    FAIR_LOW("合理偏低區", "合理偏低"),
    FAIR_HIGH("合理偏高區", "合理偏高"),
    EXPENSIVE("昂貴高估區", "昂貴區"),
}

@Serializable
data class HistoricalMultipleBand(
    val lowMultiple: Double,
    val fairMultiple: Double,
    val highMultiple: Double,
    val sampleCount: Int = 0,
    val isAutoCalibrated: Boolean = false,
)

@Serializable
data class PriceBand(
    val cheapPrice: Double,
    val fairPrice: Double,
    val expensivePrice: Double,
)

@Serializable
data class RawScreenerQuote(
    val symbol: String,
    val shortName: String = "",
    val longName: String = "",
    val marketCap: Long = 0L,
    val regularMarketPrice: Double = 0.0,
    val regularMarketChangePercent: Double? = null,
    val trailingPE: Double? = null,
    val forwardPE: Double? = null,
    val epsTrailingTwelveMonths: Double? = null,
    val epsForward: Double? = null,
    val priceToBook: Double? = null,
    val bookValue: Double? = null,
    val dividendYieldPercent: Double? = null,
)

@Serializable
data class StockValuationItem(
    val rank: Int,
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val market: ValuationMarket,
    val currency: MarketCurrency,
    val marketCap: Long,
    val currentPrice: Double,
    val changePercent: Double? = null,
    val trailingPe: Double? = null,
    val forwardPe: Double? = null,
    val trailingEps: Double? = null,
    val forwardEps: Double? = null,
    val priceToBook: Double? = null,
    val bookValuePerShare: Double? = null,
    val dividendYieldPercent: Double? = null,
    val primaryMethod: ValuationMethod,
    val peBand: HistoricalMultipleBand,
    val pbBand: HistoricalMultipleBand,
    val forwardPePriceBand: PriceBand? = null,
    val trailingPePriceBand: PriceBand? = null,
    val pbPriceBand: PriceBand? = null,
    val activePriceBand: PriceBand,
    val tier: ValuationTier,
    val marginOfSafetyPercent: Double,
    val impliedEpsGrowthPercent: Double? = null,
    val isFallbackBand: Boolean = false,
)

@Serializable
data class ValuationRadarSnapshot(
    val twTop10: List<StockValuationItem>,
    val usTop10: List<StockValuationItem>,
    val updatedAtMs: Long,
    val isFromCache: Boolean = false,
)

object ValuationCalculator {

    private val US_CROSS_LISTED_TW_ADRS = setOf("TSM", "UMC", "ASX", "CHT", "AUOTY")

    private val FINANCIAL_TW_PREFIXES = setOf(
        "2801", "2809", "2812", "2816", "2820", "2832", "2834", "2836", "2838",
        "2845", "2849", "2850", "2851", "2852", "2855", "2867",
        "2880", "2881", "2882", "2883", "2884", "2885", "2886", "2887",
        "2888", "2889", "2890", "2891", "2892", "2897",
        "5871", "5876", "5880", "6005", "6024",
    )

    private val US_FINANCIAL_SYMBOLS = setOf("BRK-B", "BRK-A", "JPM", "BAC", "WFC", "C", "GS", "MS")

    val DEFAULT_GROWTH_PE_BAND = HistoricalMultipleBand(
        lowMultiple = 18.0,
        fairMultiple = 24.0,
        highMultiple = 32.0,
        sampleCount = 0,
        isAutoCalibrated = false,
    )

    val DEFAULT_TW_PE_BAND = HistoricalMultipleBand(
        lowMultiple = 14.0,
        fairMultiple = 18.0,
        highMultiple = 24.0,
        sampleCount = 0,
        isAutoCalibrated = false,
    )

    val DEFAULT_FINANCIAL_PE_BAND = HistoricalMultipleBand(
        lowMultiple = 10.0,
        fairMultiple = 13.0,
        highMultiple = 16.5,
        sampleCount = 0,
        isAutoCalibrated = false,
    )

    val DEFAULT_FINANCIAL_PB_BAND = HistoricalMultipleBand(
        lowMultiple = 1.15,
        fairMultiple = 1.45,
        highMultiple = 1.85,
        sampleCount = 0,
        isAutoCalibrated = false,
    )

    val DEFAULT_TECH_PB_BAND = HistoricalMultipleBand(
        lowMultiple = 4.0,
        fairMultiple = 6.5,
        highMultiple = 10.0,
        sampleCount = 0,
        isAutoCalibrated = false,
    )

    val BUILTIN_TW_CHINESE_NAMES = mapOf(
        "2330.TW" to "台積電",
        "2454.TW" to "聯發科",
        "2317.TW" to "鴻海",
        "2308.TW" to "台達電",
        "3711.TW" to "日月光投控",
        "1303.TW" to "南亞",
        "2881.TW" to "富邦金",
        "2882.TW" to "國泰金",
        "2891.TW" to "中信金",
        "2412.TW" to "中華電",
        "2303.TW" to "聯電",
        "2383.TW" to "台光電",
        "2382.TW" to "廣達",
        "2408.TW" to "南亞科",
        "3017.TW" to "奇鋐",
        "2886.TW" to "兆豐金",
        "2884.TW" to "玉山金",
        "2885.TW" to "元大金",
        "1216.TW" to "統一",
        "6669.TW" to "緯穎",
        "3231.TW" to "緯創",
        "3008.TW" to "大立光",
        "2357.TW" to "華碩",
        "2603.TW" to "長榮",
    )

    val BUILTIN_US_CHINESE_NAMES = mapOf(
        "AAPL" to "蘋果 Apple",
        "NVDA" to "輝達 NVIDIA",
        "MSFT" to "微軟 Microsoft",
        "GOOGL" to "Alphabet (Google)",
        "AMZN" to "亞馬遜 Amazon",
        "META" to "Meta Platforms",
        "TSLA" to "特斯拉 Tesla",
        "AVGO" to "博通 Broadcom",
        "BRK-B" to "波克夏 Berkshire",
        "LLY" to "禮來 Eli Lilly",
        "WMT" to "沃爾瑪 Walmart",
        "JPM" to "摩根大通 JPMorgan",
        "V" to "Visa",
        "ORCL" to "甲骨文 Oracle",
        "NFLX" to "網飛 Netflix",
        "COST" to "好市多 Costco",
        "XOM" to "埃克森美孚 Exxon",
        "MA" to "萬事達卡 Mastercard",
    )

    val FALLBACK_TW_CANDIDATES = listOf(
        "2330.TW", "2454.TW", "2317.TW", "2308.TW", "3711.TW",
        "1303.TW", "2881.TW", "2882.TW", "2891.TW", "2412.TW",
        "2303.TW", "2383.TW", "2382.TW", "2886.TW", "2884.TW",
    )

    val FALLBACK_US_CANDIDATES = listOf(
        "AAPL", "NVDA", "MSFT", "GOOGL", "AMZN",
        "META", "TSLA", "AVGO", "BRK-B", "LLY",
        "WMT", "JPM", "V", "ORCL", "COST",
    )

    fun determinePrimaryMethod(symbol: String): ValuationMethod {
        val upper = symbol.trim().uppercase(Locale.US)
        val baseTw = upper.substringBefore(".")
        if (upper in US_FINANCIAL_SYMBOLS || baseTw in FINANCIAL_TW_PREFIXES) {
            return ValuationMethod.PB_PRIORITY
        }
        return ValuationMethod.PE_PRIORITY
    }

    fun defaultPeBandFor(symbol: String, market: ValuationMarket): HistoricalMultipleBand {
        return when {
            determinePrimaryMethod(symbol) == ValuationMethod.PB_PRIORITY -> DEFAULT_FINANCIAL_PE_BAND
            market == ValuationMarket.US -> DEFAULT_GROWTH_PE_BAND
            else -> DEFAULT_TW_PE_BAND
        }
    }

    fun defaultPbBandFor(symbol: String): HistoricalMultipleBand {
        return if (determinePrimaryMethod(symbol) == ValuationMethod.PB_PRIORITY) {
            DEFAULT_FINANCIAL_PB_BAND
        } else {
            DEFAULT_TECH_PB_BAND
        }
    }

    /**
     * Fixes Yahoo Finance API anomaly where BRK-B returns BRK-A's book value (~536,413 instead of ~357),
     * which causes priceToBook to report ~0.001x instead of ~1.60x.
     */
    fun sanitizeBrkB(
        symbol: String,
        currentPrice: Double,
        rawBookValue: Double?,
        rawPb: Double?,
    ): Pair<Double?, Double?> {
        val upper = symbol.trim().uppercase(Locale.US)
        if (upper == "BRK-B" && currentPrice > 0.0 && rawBookValue != null && rawBookValue > currentPrice * 100.0) {
            val fixedBookValue = rawBookValue / 1500.0
            val fixedPb = if (fixedBookValue > 0.0) currentPrice / fixedBookValue else rawPb
            return fixedBookValue to fixedPb
        }
        val cleanBook = rawBookValue?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 }
        val cleanPb = when {
            rawPb != null && !rawPb.isNaN() && !rawPb.isInfinite() && rawPb > 0.0 -> rawPb
            cleanBook != null && currentPrice > 0.0 -> currentPrice / cleanBook
            else -> null
        }
        return cleanBook to cleanPb
    }

    /**
     * Computes P20 / P50 / P80 historical PE band after filtering extreme outliers.
     */
    fun computeHistoricalPeBand(
        rawHistory: List<Double>,
        fallback: HistoricalMultipleBand,
    ): HistoricalMultipleBand {
        val valid = rawHistory
            .filter { !it.isNaN() && !it.isInfinite() && it > 2.0 && it < 120.0 }
            .sorted()
        if (valid.size < 4) return fallback

        val p20 = percentile(valid, 0.20)
        val p50 = percentile(valid, 0.50)
        val p80 = percentile(valid, 0.80)
        return ensureMonotonicBand(
            low = p20,
            fair = p50,
            high = p80,
            minSpread = 1.0,
            sampleCount = valid.size,
        )
    }

    /**
     * Computes P20 / P50 / P80 historical PB band after filtering extreme outliers.
     */
    fun computeHistoricalPbBand(
        rawHistory: List<Double>,
        fallback: HistoricalMultipleBand,
    ): HistoricalMultipleBand {
        val valid = rawHistory
            .filter { !it.isNaN() && !it.isInfinite() && it > 0.2 && it < 80.0 }
            .sorted()
        if (valid.size < 4) return fallback

        val p20 = percentile(valid, 0.20)
        val p50 = percentile(valid, 0.50)
        val p80 = percentile(valid, 0.80)
        return ensureMonotonicBand(
            low = p20,
            fair = p50,
            high = p80,
            minSpread = 0.15,
            sampleCount = valid.size,
        )
    }

    private fun ensureMonotonicBand(
        low: Double,
        fair: Double,
        high: Double,
        minSpread: Double,
        sampleCount: Int,
    ): HistoricalMultipleBand {
        val safeLow = low.coerceAtLeast(0.1)
        val safeFair = fair.coerceAtLeast(safeLow + minSpread)
        val safeHigh = high.coerceAtLeast(safeFair + minSpread)
        return HistoricalMultipleBand(
            lowMultiple = safeLow,
            fairMultiple = safeFair,
            highMultiple = safeHigh,
            sampleCount = sampleCount,
            isAutoCalibrated = true,
        )
    }

    internal fun percentile(sorted: List<Double>, q: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted.first()
        val index = q.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lower = index.toInt()
        val upper = (lower + 1).coerceAtMost(sorted.lastIndex)
        val fraction = index - lower
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction
    }

    /**
     * Cleans screener candidates:
     * - Deduplicates dual share classes (keeps GOOGL over GOOG, keeps BRK-B over BRK-A)
     * - Excludes cross-listed TW ADRs (TSM, UMC, ASX, CHT) from the US Top 10 list
     * - Excludes entries without valid price, marketCap, or usable positive PE/PB fundamentals
     * - Sorts descending by marketCap and returns top 10.
     */
    fun deduplicateAndFilterTop10(
        candidates: List<RawScreenerQuote>,
        market: ValuationMarket,
    ): List<RawScreenerQuote> {
        val sorted = candidates
            .filter { it.regularMarketPrice > 0.0 && it.marketCap > 0L }
            .sortedByDescending { it.marketCap }

        val hasGoogl = sorted.any { it.symbol.equals("GOOGL", ignoreCase = true) }
        val hasBrkB = sorted.any { it.symbol.equals("BRK-B", ignoreCase = true) }

        val seenCompanies = HashSet<String>()
        val result = ArrayList<RawScreenerQuote>(10)

        for (item in sorted) {
            val upper = item.symbol.trim().uppercase(Locale.US)
            if (upper.isBlank()) continue

            if (market == ValuationMarket.US) {
                if (upper.endsWith(".TW") || upper.endsWith(".TWO")) continue
                if (upper in US_CROSS_LISTED_TW_ADRS) continue
                if (upper == "GOOG" && hasGoogl) continue
                if (upper == "BRK-A" && hasBrkB) continue
                // Skip pure BRK-A when BRK-B isn't in candidate slice to avoid $800k share price distortion
                if (upper == "BRK-A") continue
            }

            if (market == ValuationMarket.TW && !upper.endsWith(".TW") && !upper.endsWith(".TWO")) {
                continue
            }

            val hasUsableFundamental = hasPositive(item.forwardPE) ||
                hasPositive(item.trailingPE) ||
                hasPositive(item.epsForward) ||
                hasPositive(item.epsTrailingTwelveMonths) ||
                hasPositive(item.bookValue) ||
                hasPositive(item.priceToBook)

            if (!hasUsableFundamental) continue

            val canonicalEntity = when (upper) {
                "GOOG", "GOOGL" -> "ALPHABET"
                "BRK-A", "BRK-B" -> "BERKSHIRE"
                else -> upper
            }

            if (seenCompanies.add(canonicalEntity)) {
                result.add(item.copy(symbol = upper))
                if (result.size >= 10) break
            }
        }
        return result
    }

    private fun hasPositive(v: Double?): Boolean =
        v != null && !v.isNaN() && !v.isInfinite() && v > 0.0

    fun isTaiwanSymbol(symbol: String): Boolean {
        val upper = symbol.trim().uppercase(Locale.US)
        return upper.endsWith(".TW") || upper.endsWith(".TWO")
    }

    fun inferMarketForSymbol(symbol: String): ValuationMarket =
        if (isTaiwanSymbol(symbol)) ValuationMarket.TW else ValuationMarket.US

    fun evaluateStockValuation(
        rank: Int,
        raw: RawScreenerQuote,
        market: ValuationMarket,
        peBand: HistoricalMultipleBand,
        pbBand: HistoricalMultipleBand,
        resolvedName: String? = null,
    ): StockValuationItem {
        val symbol = raw.symbol.trim().uppercase(Locale.US)
        val price = raw.regularMarketPrice
        val isTwSymbol = isTaiwanSymbol(symbol)
        val currency = if (market == ValuationMarket.TW || (market == ValuationMarket.CUSTOM && isTwSymbol)) {
            MarketCurrency.TWD
        } else {
            MarketCurrency.USD
        }

        val (sanitizedBook, sanitizedPb) = sanitizeBrkB(
            symbol = symbol,
            currentPrice = price,
            rawBookValue = raw.bookValue,
            rawPb = raw.priceToBook,
        )

        val trailingEps = raw.epsTrailingTwelveMonths?.takeIf { hasPositive(it) }
            ?: raw.trailingPE?.takeIf { hasPositive(it) }?.let { pe -> price / pe }

        val trailingPe = raw.trailingPE?.takeIf { hasPositive(it) }
            ?: trailingEps?.let { eps -> price / eps }

        val forwardEps = raw.epsForward?.takeIf { hasPositive(it) }
            ?: raw.forwardPE?.takeIf { hasPositive(it) }?.let { pe -> price / pe }

        val forwardPe = raw.forwardPE?.takeIf { hasPositive(it) }
            ?: forwardEps?.let { eps -> price / eps }

        val forwardPriceBand = forwardEps?.let { eps ->
            PriceBand(
                cheapPrice = eps * peBand.lowMultiple,
                fairPrice = eps * peBand.fairMultiple,
                expensivePrice = eps * peBand.highMultiple,
            )
        }

        val trailingPriceBand = trailingEps?.let { eps ->
            PriceBand(
                cheapPrice = eps * peBand.lowMultiple,
                fairPrice = eps * peBand.fairMultiple,
                expensivePrice = eps * peBand.highMultiple,
            )
        }

        val pbPriceBand = sanitizedBook?.let { bv ->
            PriceBand(
                cheapPrice = bv * pbBand.lowMultiple,
                fairPrice = bv * pbBand.fairMultiple,
                expensivePrice = bv * pbBand.highMultiple,
            )
        }

        val primaryMethod = determinePrimaryMethod(symbol)
        val computedBand = when (primaryMethod) {
            ValuationMethod.PB_PRIORITY -> pbPriceBand ?: forwardPriceBand ?: trailingPriceBand
            ValuationMethod.PE_PRIORITY -> forwardPriceBand ?: trailingPriceBand ?: pbPriceBand
        }
        val isFallbackBand = computedBand == null
        val activePriceBand = computedBand ?: PriceBand(
            cheapPrice = price * 0.85,
            fairPrice = price,
            expensivePrice = price * 1.15,
        )

        val tier = computeTier(price, activePriceBand)
        val marginOfSafetyPercent = computeMarginOfSafetyPercent(price, activePriceBand)

        val impliedGrowth = if (trailingEps != null && trailingEps > 0.0 && forwardEps != null && forwardEps > 0.0) {
            ((forwardEps / trailingEps) - 1.0) * 100.0
        } else null

        val displayName = resolvedName?.takeIf { it.isNotBlank() }
            ?: (if (currency == MarketCurrency.TWD) BUILTIN_TW_CHINESE_NAMES[symbol] else BUILTIN_US_CHINESE_NAMES[symbol])
            ?: raw.shortName.ifBlank { raw.longName }.ifBlank { symbol }

        return StockValuationItem(
            rank = rank,
            symbol = symbol,
            displaySymbol = symbol,
            name = displayName,
            market = market,
            currency = currency,
            marketCap = raw.marketCap,
            currentPrice = price,
            changePercent = raw.regularMarketChangePercent,
            trailingPe = trailingPe,
            forwardPe = forwardPe,
            trailingEps = trailingEps,
            forwardEps = forwardEps,
            priceToBook = sanitizedPb,
            bookValuePerShare = sanitizedBook,
            dividendYieldPercent = raw.dividendYieldPercent,
            primaryMethod = primaryMethod,
            peBand = peBand,
            pbBand = pbBand,
            forwardPePriceBand = forwardPriceBand,
            trailingPePriceBand = trailingPriceBand,
            pbPriceBand = pbPriceBand,
            activePriceBand = activePriceBand,
            tier = tier,
            marginOfSafetyPercent = marginOfSafetyPercent,
            impliedEpsGrowthPercent = impliedGrowth,
            isFallbackBand = isFallbackBand,
        )
    }

    fun formatMarketCap(marketCap: Long, currency: MarketCurrency): String {
        if (marketCap <= 0L) return "-"
        return when (currency) {
            MarketCurrency.TWD -> {
                val trillion = marketCap / 1_000_000_000_000.0
                if (trillion >= 1.0) {
                    String.format(Locale.US, "%.2f 兆", trillion)
                } else {
                    val hundredMillion = marketCap / 100_000_000.0
                    String.format(Locale.US, "%.0f 億", hundredMillion)
                }
            }
            MarketCurrency.USD -> {
                val trillion = marketCap / 1_000_000_000_000.0
                if (trillion >= 1.0) {
                    String.format(Locale.US, "$%.2fT", trillion)
                } else {
                    val billion = marketCap / 1_000_000_000.0
                    String.format(Locale.US, "$%.0fB", billion)
                }
            }
        }
    }

    fun computeTier(price: Double, band: PriceBand): ValuationTier = when {
        price <= band.cheapPrice -> ValuationTier.CHEAP
        price <= band.fairPrice -> ValuationTier.FAIR_LOW
        price <= band.expensivePrice -> ValuationTier.FAIR_HIGH
        else -> ValuationTier.EXPENSIVE
    }

    fun computeMarginOfSafetyPercent(price: Double, band: PriceBand): Double =
        if (band.fairPrice > 0.0) ((price - band.fairPrice) / band.fairPrice) * 100.0 else 0.0

    fun roundTargetPrice(price: Double, currency: MarketCurrency): Double {
        if (price <= 0.0 || price.isNaN() || price.isInfinite()) return 0.0
        return when (currency) {
            MarketCurrency.TWD -> {
                when {
                    price >= 1000.0 -> (kotlin.math.round(price / 5.0) * 5.0)
                    price >= 100.0 -> kotlin.math.round(price)
                    price >= 50.0 -> (kotlin.math.round(price * 2.0) / 2.0)
                    else -> (kotlin.math.round(price * 10.0) / 10.0)
                }
            }
            MarketCurrency.USD -> kotlin.math.round(price * 100.0) / 100.0
        }
    }

    fun formatMultiple(value: Double?): String {
        if (value == null || value.isNaN() || value.isInfinite() || value <= 0.0) return "—"
        return String.format(Locale.US, "%.1fx", value)
    }

    fun formatEps(value: Double?, currency: MarketCurrency): String {
        if (value == null || value.isNaN() || value.isInfinite() || abs(value) < 1e-6) return "—"
        val prefix = if (currency == MarketCurrency.TWD) "NT$" else "$"
        return String.format(Locale.US, "%s%.2f", prefix, value)
    }
}
