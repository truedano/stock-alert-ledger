package com.example.data.remote

import com.example.domain.model.MarketCurrency
import com.example.domain.model.StockQuote
import com.example.domain.model.TickerLookupResult
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request

class YahooFinanceService(
    private val client: OkHttpClient = defaultOkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Smart Ticker Resolution:
     * - Recognizes common index/ETF aliases (e.g. "大盤" -> "^TWII", "標普" -> "^GSPC", etc.)
     * - Contains digits without .TW/.TWO -> try {code}.TW first; on 404/no data, fallback to {code}.TWO.
     * - Pure letters -> query directly as US stock/ETF.
     * - Explicit .TW or .TWO -> query directly.
     */
    suspend fun resolveAndFetch(rawInput: String): TickerLookupResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return@withContext TickerLookupResult.NotFound("請輸入股票代號")
        }

        val mappedSymbol = INDEX_ALIASES[trimmed.lowercase(Locale.ROOT)] ?: trimmed
        val cleaned = mappedSymbol.uppercase(Locale.US)

        if (!SAFE_TICKER_INPUT_REGEX.matches(cleaned)) {
            return@withContext TickerLookupResult.NotFound("股票代號格式不正確，僅支援英數字及 . - ^ =")
        }

        val candidates = buildCandidateSymbols(cleaned)
        var lastNonNotFound: TickerLookupResult? = null

        for (candidate in candidates) {
            when (val outcome = fetchSymbol(candidate)) {
                is TickerLookupResult.Success -> return@withContext outcome
                is TickerLookupResult.RateLimited -> return@withContext outcome
                is TickerLookupResult.Error -> lastNonNotFound = outcome
                is TickerLookupResult.NotFound -> {
                    // Continue trying next candidate (e.g., .TW -> .TWO)
                }
            }
        }

        lastNonNotFound ?: TickerLookupResult.NotFound("查無「$cleaned」的報價資料，請確認代號是否正確")
    }

    suspend fun fetchExactSymbol(symbol: String): TickerLookupResult = withContext(Dispatchers.IO) {
        fetchSymbol(symbol.trim().uppercase(Locale.US))
    }

    internal fun buildCandidateSymbols(cleaned: String): List<String> {
        if (cleaned.startsWith("^") || cleaned.endsWith(".TW") || cleaned.endsWith(".TWO")) {
            return listOf(cleaned)
        }
        val containsDigit = cleaned.any { it.isDigit() }
        return if (containsDigit) {
            listOf("$cleaned.TW", "$cleaned.TWO")
        } else {
            listOf(cleaned.replace('.', '-'))
        }
    }

    private fun fetchSymbol(symbol: String): TickerLookupResult {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=10y&interval=1d"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 429 -> {
                        TickerLookupResult.RateLimited("Yahoo Finance 請求過於頻繁 (HTTP 429)，已保留快取報價")
                    }
                    response.code == 404 -> {
                        TickerLookupResult.NotFound("查無 $symbol")
                    }
                    !response.isSuccessful -> {
                        TickerLookupResult.Error("伺服器回應錯誤 (HTTP ${response.code})")
                    }
                    else -> {
                        val body = response.body?.string().orEmpty()
                        if (body.isBlank()) {
                            return TickerLookupResult.NotFound("回傳內容為空")
                        }
                        parseChartJson(symbol, body)
                    }
                }
            }
        } catch (e: IOException) {
            TickerLookupResult.Error("網路連線失敗：${e.localizedMessage ?: "請檢查網路狀態"}")
        } catch (e: Exception) {
            TickerLookupResult.Error("解析報價失敗：${e.localizedMessage ?: "未知錯誤"}")
        }
    }

    internal fun parseChartJson(requestedSymbol: String, rawJson: String): TickerLookupResult {
        val parsed = json.decodeFromString(YahooChartResponse.serializer(), rawJson)
        val container = parsed.chart
            ?: return TickerLookupResult.NotFound("無效的報價結構")

        if (container.error != null) {
            return TickerLookupResult.NotFound(
                container.error.description ?: "查無 $requestedSymbol"
            )
        }

        val result = container.result?.firstOrNull()
            ?: return TickerLookupResult.NotFound("查無 $requestedSymbol 的交易資料")

        val meta = result.meta
            ?: return TickerLookupResult.NotFound("缺少標的資訊")

        val quoteSeries = result.indicators?.quote?.firstOrNull()
        val closes = quoteSeries
            ?.close
            ?.filterNotNull()
            ?.filter { !it.isNaN() && !it.isInfinite() && it > 0.0 }
            .orEmpty()

        val timestamps = result.timestamp.orEmpty()
        val rawOpens = quoteSeries?.open.orEmpty()
        val rawHighs = quoteSeries?.high.orEmpty()
        val rawLows = quoteSeries?.low.orEmpty()
        val rawCloses = quoteSeries?.close.orEmpty()
        val rawVolumes = quoteSeries?.volume.orEmpty()

        val candles = ArrayList<com.example.domain.model.CandleStickData>()
        val minLen = minOf(timestamps.size, rawCloses.size)
        for (i in 0 until minLen) {
            val c = rawCloses.getOrNull(i)
            if (c != null && !c.isNaN() && !c.isInfinite() && c > 0.0) {
                val o = rawOpens.getOrNull(i)?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 } ?: c
                val h = rawHighs.getOrNull(i)?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 } ?: maxOf(o, c)
                val l = rawLows.getOrNull(i)?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 } ?: minOf(o, c)
                val v = rawVolumes.getOrNull(i)?.takeIf { it >= 0 } ?: 0L
                val t = timestamps.getOrNull(i) ?: 0L
                candles.add(
                    com.example.domain.model.CandleStickData(
                        timestamp = t,
                        open = o,
                        high = maxOf(h, maxOf(o, c)),
                        low = minOf(l, minOf(o, c)),
                        close = c,
                        volume = v,
                    )
                )
            }
        }

        val resolvedSymbol = meta.symbol?.takeIf { it.isNotBlank() } ?: requestedSymbol
        val currentPrice = meta.regularMarketPrice?.takeIf { it > 0.0 }
            ?: closes.lastOrNull()
            ?: return TickerLookupResult.NotFound("目前無有效成交價格")

        val name = meta.shortName?.takeIf { it.isNotBlank() }
            ?: meta.longName?.takeIf { it.isNotBlank() }
            ?: resolvedSymbol

        val currency = if (
            meta.currency.equals("TWD", ignoreCase = true) ||
            resolvedSymbol.endsWith(".TW", ignoreCase = true) ||
            resolvedSymbol.endsWith(".TWO", ignoreCase = true)
        ) {
            MarketCurrency.TWD
        } else {
            MarketCurrency.fromCurrencyCode(meta.currency)
        }

        val previousClose = meta.chartPreviousClose ?: meta.previousClose

        return TickerLookupResult.Success(
            StockQuote(
                symbol = resolvedSymbol,
                displaySymbol = resolvedSymbol,
                name = name,
                currency = currency,
                currentPrice = currentPrice,
                previousClose = previousClose,
                closeHistory = closes,
                lastUpdatedAt = System.currentTimeMillis(),
                isSimulated = false,
                candleHistory = candles,
            )
        )
    }

    companion object {
        private val SAFE_TICKER_INPUT_REGEX = Regex("^[A-Z0-9.\\-^=]{1,20}$")
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private val INDEX_ALIASES = mapOf(
            "大盤" to "^TWII",
            "加權" to "^TWII",
            "加權指數" to "^TWII",
            "台股大盤" to "^TWII",
            "櫃買" to "^TWOII",
            "櫃買指數" to "^TWOII",
            "otc" to "^TWOII",
            "標普" to "^GSPC",
            "標普500" to "^GSPC",
            "s&p" to "^GSPC",
            "s&p500" to "^GSPC",
            "sp500" to "^GSPC",
            "那指" to "^IXIC",
            "那斯達克" to "^IXIC",
            "nasdaq" to "^IXIC",
            "道瓊" to "^DJI",
            "道瓊指數" to "^DJI",
            "費半" to "^SOX",
            "費城半導體" to "^SOX",
            "vix" to "^VIX",
            "恐慌指數" to "^VIX",
        )

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.RESTRICTED_TLS))
            .followRedirects(false)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
