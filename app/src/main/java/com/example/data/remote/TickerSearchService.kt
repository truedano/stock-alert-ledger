package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.model.MarketCurrency
import com.example.domain.model.TickerSearchCandidate
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class TickerSearchService(
    private val context: Context? = null,
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val directoryMutex = Mutex()
    private var cachedTwDirectory: List<TickerSearchCandidate> = emptyList()
    private var lastDirectoryFetchMs: Long = 0L

    suspend fun search(rawQuery: String, limit: Int = 10): List<TickerSearchCandidate> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return@withContext emptyList()

        val directory = getOrLoadTwDirectory()
        val localPool = BUILTIN_INDICES + directory
        val localMatches = filterAndRankLocalCandidates(query, localPool, limit)

        val shouldQueryYahoo = query.any { it in 'A'..'Z' || it in 'a'..'z' } ||
            (localMatches.size < 5 && query.length >= 2)

        if (!shouldQueryYahoo) {
            return@withContext localMatches
        }

        val yahooMatches = fetchYahooSearch(query)
        val combined = LinkedHashMap<String, TickerSearchCandidate>()
        for (item in localMatches) {
            combined.putIfAbsent(item.symbol.uppercase(Locale.US), item)
        }
        for (item in yahooMatches) {
            combined.putIfAbsent(item.symbol.uppercase(Locale.US), item)
        }
        combined.values.take(limit)
    }

    private suspend fun getOrLoadTwDirectory(): List<TickerSearchCandidate> {
        val now = System.currentTimeMillis()
        if (cachedTwDirectory.isNotEmpty() && now - lastDirectoryFetchMs < MEMORY_TTL_MS) {
            return cachedTwDirectory
        }

        return directoryMutex.withLock {
            val currentNow = System.currentTimeMillis()
            if (cachedTwDirectory.isNotEmpty() && currentNow - lastDirectoryFetchMs < MEMORY_TTL_MS) {
                return@withLock cachedTwDirectory
            }

            // Try reading disk cache first
            val cacheFile = context?.cacheDir?.let { File(it, CACHE_FILE_NAME) }
            if (cacheFile != null && cacheFile.exists()) {
                val diskAgeMs = currentNow - cacheFile.lastModified()
                if (diskAgeMs < DISK_TTL_MS) {
                    val diskItems = runCatching {
                        json.decodeFromString(ListSerializer(TickerSearchCandidate.serializer()), cacheFile.readText())
                    }.getOrNull()
                    if (!diskItems.isNullOrEmpty()) {
                        cachedTwDirectory = diskItems
                        lastDirectoryFetchMs = cacheFile.lastModified()
                        return@withLock diskItems
                    }
                }
            }

            // Fetch TWSE + TPEx directories in sequence (fast & resilient)
            val twseBody = fetchJsonWithRetry("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL", "TWSE_STOCK_DAY_ALL")
            val tpexBody = fetchJsonWithRetry("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes", "TPEX_QUOTES")

            val merged = buildList {
                if (!twseBody.isNullOrBlank()) addAll(parseTwseStockListJson(twseBody))
                if (!tpexBody.isNullOrBlank()) addAll(parseTpexStockListJson(tpexBody))
            }.distinctBy { it.symbol }

            if (merged.isNotEmpty()) {
                cachedTwDirectory = merged
                lastDirectoryFetchMs = currentNow
                if (cacheFile != null) {
                    runCatching {
                        cacheFile.writeText(
                            json.encodeToString(ListSerializer(TickerSearchCandidate.serializer()), merged)
                        )
                    }
                }
            }
            cachedTwDirectory
        }
    }

    private suspend fun fetchYahooSearch(query: String): List<TickerSearchCandidate> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v1/finance/search?q=$encoded&quotesCount=8&newsCount=0"
        val body = fetchJsonWithRetry(url, "YAHOO_SEARCH") ?: return emptyList()
        return parseYahooSearchJson(body)
    }

    private suspend fun fetchJsonWithRetry(url: String, tag: String): String? {
        return try {
            executeWithExponentialBackoff(
                maxAttempts = 2,
                initialDelayMs = 300L,
                delayFn = delayFn,
                shouldRetry = { it.isTransient },
            ) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.code in 500..599 -> HttpBodyAttempt(null, isTransient = true)
                        !response.isSuccessful -> {
                            Log.w("TickerSearchService", "$tag HTTP ${response.code}")
                            HttpBodyAttempt(null, isTransient = false)
                        }
                        else -> HttpBodyAttempt(response.body?.string()?.takeIf { it.isNotBlank() }, isTransient = false)
                    }
                }
            }.body
        } catch (e: Exception) {
            Log.w("TickerSearchService", "$tag failed: ${e.message}")
            null
        }
    }

    private data class HttpBodyAttempt(
        val body: String?,
        val isTransient: Boolean,
    )

    internal fun parseTwseStockListJson(rawJson: String): List<TickerSearchCandidate> {
        return runCatching {
            val arr = json.parseToJsonElement(rawJson) as? JsonArray ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val code = (obj["Code"] as? JsonPrimitive)?.content?.trim().orEmpty()
                val name = (obj["Name"] as? JsonPrimitive)?.content?.trim().orEmpty()
                if (code.isEmpty() || name.isEmpty()) return@mapNotNull null
                TickerSearchCandidate(
                    symbol = "${code.uppercase(Locale.US)}.TW",
                    name = name,
                    currency = MarketCurrency.TWD,
                    marketLabel = "台股上市",
                )
            }
        }.getOrDefault(emptyList())
    }

    internal fun parseTpexStockListJson(rawJson: String): List<TickerSearchCandidate> {
        return runCatching {
            val arr = json.parseToJsonElement(rawJson) as? JsonArray ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val code = (obj["SecuritiesCompanyCode"] as? JsonPrimitive)?.content?.trim().orEmpty()
                val name = (obj["CompanyName"] as? JsonPrimitive)?.content?.trim().orEmpty()
                if (code.isEmpty() || name.isEmpty()) return@mapNotNull null
                TickerSearchCandidate(
                    symbol = "${code.uppercase(Locale.US)}.TWO",
                    name = name,
                    currency = MarketCurrency.TWD,
                    marketLabel = "台股上櫃",
                )
            }
        }.getOrDefault(emptyList())
    }

    internal fun parseYahooSearchJson(rawJson: String): List<TickerSearchCandidate> {
        return runCatching {
            val parsed = json.decodeFromString(YahooSearchResponse.serializer(), rawJson)
            parsed.quotes.mapNotNull { item ->
                val symbol = item.symbol.trim().uppercase(Locale.US)
                if (symbol.isEmpty()) return@mapNotNull null
                val quoteType = item.quoteType.uppercase(Locale.US)
                if (quoteType !in ALLOWED_QUOTE_TYPES) return@mapNotNull null

                val name = item.shortname.ifBlank { item.longname }.ifBlank { symbol }
                val isTaiwan = symbol.endsWith(".TW") || symbol.endsWith(".TWO") ||
                    symbol == "^TWII" || symbol == "^TWOII"
                val currency = if (isTaiwan) MarketCurrency.TWD else MarketCurrency.USD
                val marketLabel = when {
                    symbol.endsWith(".TW") -> "台股上市"
                    symbol.endsWith(".TWO") -> "台股上櫃"
                    quoteType == "INDEX" -> "大盤指數"
                    quoteType == "ETF" -> "ETF"
                    else -> item.exchDisp.ifBlank { "美股" }
                }
                TickerSearchCandidate(
                    symbol = symbol,
                    name = name,
                    currency = currency,
                    marketLabel = marketLabel,
                )
            }
        }.getOrDefault(emptyList())
    }

    internal fun filterAndRankLocalCandidates(
        query: String,
        pool: List<TickerSearchCandidate>,
        limit: Int = 10,
    ): List<TickerSearchCandidate> {
        val qLower = query.trim().lowercase(Locale.ROOT)
        val qUpper = query.trim().uppercase(Locale.US)
        if (qLower.isEmpty()) return emptyList()

        return pool.mapNotNull { item ->
            val symbolUpper = item.symbol.uppercase(Locale.US)
            val baseCode = symbolUpper.substringBefore('.')
            val nameLower = item.name.lowercase(Locale.ROOT)

            val score = when {
                baseCode == qUpper || symbolUpper == qUpper -> 0
                baseCode.startsWith(qUpper) -> 1
                nameLower.startsWith(qLower) -> 2
                symbolUpper.contains(qUpper) -> 3
                nameLower.contains(qLower) -> 4
                else -> return@mapNotNull null
            }
            score to item
        }
            .sortedWith(compareBy<Pair<Int, TickerSearchCandidate>> { it.first }.thenBy { it.second.symbol.length })
            .map { it.second }
            .distinctBy { it.symbol }
            .take(limit)
    }

    @Serializable
    private data class YahooSearchResponse(
        val quotes: List<YahooQuoteItem> = emptyList(),
    )

    @Serializable
    private data class YahooQuoteItem(
        val symbol: String = "",
        val shortname: String = "",
        val longname: String = "",
        val quoteType: String = "",
        val exchDisp: String = "",
    )

    private companion object {
        const val CACHE_FILE_NAME = "tw_ticker_directory.json"
        const val MEMORY_TTL_MS = 12 * 60 * 60 * 1000L
        const val DISK_TTL_MS = 7 * 24 * 60 * 60 * 1000L
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        val ALLOWED_QUOTE_TYPES = setOf("EQUITY", "ETF", "INDEX", "MUTUALFUND")

        val BUILTIN_INDICES = listOf(
            TickerSearchCandidate("^TWII", "台灣加權指數 (大盤)", MarketCurrency.TWD, "大盤指數"),
            TickerSearchCandidate("^TWOII", "台灣櫃買指數 (OTC)", MarketCurrency.TWD, "大盤指數"),
            TickerSearchCandidate("^GSPC", "標普 500 指數 (S&P 500)", MarketCurrency.USD, "美股指數"),
            TickerSearchCandidate("^IXIC", "那斯達克綜合指數 (Nasdaq)", MarketCurrency.USD, "美股指數"),
            TickerSearchCandidate("^DJI", "道瓊工業指數 (Dow Jones)", MarketCurrency.USD, "美股指數"),
            TickerSearchCandidate("^SOX", "費城半導體指數 (SOX)", MarketCurrency.USD, "美股指數"),
            TickerSearchCandidate("^VIX", "VIX 恐慌指數", MarketCurrency.USD, "市場情緒"),
        )
    }
}
