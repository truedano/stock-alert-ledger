package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.model.HistoricalMultipleBand
import com.example.domain.model.RawScreenerQuote
import com.example.domain.model.StockValuationItem
import com.example.domain.model.ValuationCalculator
import com.example.domain.model.ValuationMarket
import com.example.domain.model.ValuationRadarSnapshot
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class TwseValuationRow(
    val code: String,
    val name: String,
    val peRatio: Double?,
    val pbRatio: Double?,
    val dividendYield: Double?,
)

class ValuationRemoteService(
    private val context: Context? = null,
    client: OkHttpClient? = null,
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()
    private val yahooClient: OkHttpClient = client ?: YahooFinanceService.defaultOkHttpClient()
        .newBuilder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val hostKey = url.host.substringAfterLast("yahoo.com", url.host)
                synchronized(cookieStore) {
                    val bucket = cookieStore.getOrPut(hostKey) { mutableListOf() }
                    for (cookie in cookies) {
                        bucket.removeAll { it.name == cookie.name }
                        bucket.add(cookie)
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val hostKey = url.host.substringAfterLast("yahoo.com", url.host)
                return synchronized(cookieStore) {
                    cookieStore[hostKey]?.toList().orEmpty()
                }
            }
        })
        .build()

    private val cacheMutex = Mutex()
    @Volatile
    private var memorySnapshot: ValuationRadarSnapshot? = null
    @Volatile
    private var cachedCrumb: String? = null
    @Volatile
    private var cachedCrumbAtMs: Long = 0L

    suspend fun fetchValuationRadar(forceRefresh: Boolean = false): ValuationRadarSnapshot? = withContext(Dispatchers.IO) {
        val now = clockMs()
        if (!forceRefresh) {
            memorySnapshot?.let { mem ->
                if (now - mem.updatedAtMs < MEMORY_CACHE_TTL_MS &&
                    mem.twTop10.isNotEmpty() &&
                    mem.usTop10.isNotEmpty()
                ) {
                    return@withContext mem.copy(isFromCache = true)
                }
            }
            readDiskCache()?.let { disk ->
                if (now - disk.updatedAtMs < DISK_CACHE_TTL_MS &&
                    disk.twTop10.isNotEmpty() &&
                    disk.usTop10.isNotEmpty()
                ) {
                    memorySnapshot = disk
                    return@withContext disk.copy(isFromCache = true)
                }
            }
        }

        cacheMutex.withLock {
            val lockedNow = clockMs()
            if (!forceRefresh) {
                memorySnapshot?.let { mem ->
                    if (lockedNow - mem.updatedAtMs < MEMORY_CACHE_TTL_MS &&
                        mem.twTop10.isNotEmpty() &&
                        mem.usTop10.isNotEmpty()
                    ) {
                        return@withLock mem.copy(isFromCache = true)
                    }
                }
            }

            val fresh = fetchFreshRadarSnapshot()
            if (fresh != null && (fresh.twTop10.isNotEmpty() || fresh.usTop10.isNotEmpty())) {
                memorySnapshot = fresh
                writeDiskCache(fresh)
                return@withLock fresh
            }

            memorySnapshot?.copy(isFromCache = true) ?: readDiskCache()?.copy(isFromCache = true)
        }
    }

    private suspend fun fetchFreshRadarSnapshot(): ValuationRadarSnapshot? = coroutineScope {
        val crumb = ensureYahooCrumb(forceRefresh = false)
        val twseMapDeferred = async { fetchTwseValuationMap() }

        val twCandidatesDeferred = async {
            val fromScreener = if (crumb != null) fetchScreenerQuotes(ValuationMarket.TW, crumb) else emptyList()
            if (fromScreener.size >= 10) {
                fromScreener
            } else {
                val fallback = fetchFallbackBatchQuotes(ValuationCalculator.FALLBACK_TW_CANDIDATES, crumb)
                (fromScreener + fallback).distinctBy { it.symbol.uppercase(Locale.US) }
            }
        }

        val usCandidatesDeferred = async {
            val fromScreener = if (crumb != null) fetchScreenerQuotes(ValuationMarket.US, crumb) else emptyList()
            if (fromScreener.size >= 10) {
                fromScreener
            } else {
                val fallback = fetchFallbackBatchQuotes(ValuationCalculator.FALLBACK_US_CANDIDATES, crumb)
                (fromScreener + fallback).distinctBy { it.symbol.uppercase(Locale.US) }
            }
        }

        val twseMap = twseMapDeferred.await()
        val rawTwList = ValuationCalculator.deduplicateAndFilterTop10(
            candidates = enrichTwWithTwse(twCandidatesDeferred.await(), twseMap),
            market = ValuationMarket.TW,
        )
        val rawUsList = ValuationCalculator.deduplicateAndFilterTop10(
            candidates = usCandidatesDeferred.await(),
            market = ValuationMarket.US,
        )

        if (rawTwList.isEmpty() && rawUsList.isEmpty()) {
            return@coroutineScope null
        }

        val twItemsDeferred = rawTwList.mapIndexed { index, raw ->
            async {
                val (peBand, pbBand) = fetchHistoricalBands(raw.symbol, ValuationMarket.TW)
                val twCode = raw.symbol.substringBefore(".").uppercase(Locale.US)
                val twName = ValuationCalculator.BUILTIN_TW_CHINESE_NAMES[raw.symbol]
                    ?: twseMap[twCode]?.name
                ValuationCalculator.evaluateStockValuation(
                    rank = index + 1,
                    raw = raw,
                    market = ValuationMarket.TW,
                    peBand = peBand,
                    pbBand = pbBand,
                    resolvedName = twName,
                )
            }
        }

        val usItemsDeferred = rawUsList.mapIndexed { index, raw ->
            async {
                val (peBand, pbBand) = fetchHistoricalBands(raw.symbol, ValuationMarket.US)
                ValuationCalculator.evaluateStockValuation(
                    rank = index + 1,
                    raw = raw,
                    market = ValuationMarket.US,
                    peBand = peBand,
                    pbBand = pbBand,
                    resolvedName = ValuationCalculator.BUILTIN_US_CHINESE_NAMES[raw.symbol],
                )
            }
        }

        val twItems: List<StockValuationItem> = twItemsDeferred.awaitAll()
        val usItems: List<StockValuationItem> = usItemsDeferred.awaitAll()

        ValuationRadarSnapshot(
            twTop10 = twItems,
            usTop10 = usItems,
            updatedAtMs = clockMs(),
            isFromCache = false,
        )
    }

    private suspend fun ensureYahooCrumb(forceRefresh: Boolean): String? {
        val now = clockMs()
        if (!forceRefresh && cachedCrumb != null && now - cachedCrumbAtMs < CRUMB_TTL_MS) {
            return cachedCrumb
        }
        return runCatching {
            val cookieReq = Request.Builder()
                .url("https://fc.yahoo.com")
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()
            runCatching { yahooClient.newCall(cookieReq).execute().close() }

            val crumbReq = Request.Builder()
                .url("https://query1.finance.yahoo.com/v1/test/getcrumb")
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()
            yahooClient.newCall(crumbReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "getcrumb failed HTTP ${resp.code}")
                    return@runCatching null
                }
                val body = resp.body?.string()?.trim().orEmpty()
                if (body.isNotBlank() && !body.startsWith("{") && !body.startsWith("<")) {
                    cachedCrumb = body
                    cachedCrumbAtMs = clockMs()
                    body
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private suspend fun fetchScreenerQuotes(
        market: ValuationMarket,
        crumb: String,
    ): List<RawScreenerQuote> {
        val encodedCrumb = URLEncoder.encode(crumb, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v1/finance/screener?crumb=$encodedCrumb"
        val payload = buildJsonObject {
            put("size", 25)
            put("offset", 0)
            put("sortField", "intradaymarketcap")
            put("sortType", "DESC")
            put("quoteType", "EQUITY")
            putJsonObject("query") {
                put("operator", "AND")
                putJsonArray("operands") {
                    add(
                        buildJsonObject {
                            put("operator", "EQ")
                            putJsonArray("operands") {
                                add(JsonPrimitive("region"))
                                add(JsonPrimitive(market.regionCode))
                            }
                        }
                    )
                }
            }
        }.toString()

        return try {
            val responseBody = executeWithExponentialBackoff(
                maxAttempts = 2,
                initialDelayMs = 300L,
                delayFn = delayFn,
                shouldRetry = { it.isTransient },
            ) {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                yahooClient.newCall(req).execute().use { resp ->
                    when {
                        resp.code in 500..599 -> HttpAttempt(null, isTransient = true)
                        !resp.isSuccessful -> {
                            Log.w(TAG, "Screener ${market.regionCode} HTTP ${resp.code}")
                            HttpAttempt(null, isTransient = false)
                        }
                        else -> HttpAttempt(resp.body?.string(), isTransient = false)
                    }
                }
            }.body ?: return emptyList()

            parseScreenerJson(responseBody)
        } catch (e: Exception) {
            Log.w(TAG, "Screener ${market.regionCode} exception: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchFallbackBatchQuotes(
        symbols: List<String>,
        crumb: String?,
    ): List<RawScreenerQuote> {
        val activeCrumb = crumb ?: ensureYahooCrumb(forceRefresh = true) ?: return emptyList()
        val joined = URLEncoder.encode(symbols.joinToString(","), "UTF-8")
        val encodedCrumb = URLEncoder.encode(activeCrumb, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$joined&crumb=$encodedCrumb"

        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()
            yahooClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string().orEmpty()
                parseQuoteResponseJson(body)
            }
        }.getOrDefault(emptyList())
    }

    internal suspend fun fetchTwseValuationMap(): Map<String, TwseValuationRow> {
        return try {
            val body = executeWithExponentialBackoff(
                maxAttempts = 2,
                initialDelayMs = 300L,
                delayFn = delayFn,
                shouldRetry = { it.isTransient },
            ) {
                val req = Request.Builder()
                    .url("https://openapi.twse.com.tw/v1/exchangeReport/BWIBBU_ALL")
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()
                yahooClient.newCall(req).execute().use { resp ->
                    when {
                        resp.code in 500..599 -> HttpAttempt(null, isTransient = true)
                        !resp.isSuccessful -> HttpAttempt(null, isTransient = false)
                        else -> HttpAttempt(resp.body?.string(), isTransient = false)
                    }
                }
            }.body ?: return emptyMap()

            parseTwseBwibbuAll(body)
        } catch (e: Exception) {
            Log.w(TAG, "TWSE BWIBBU_ALL failed: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun fetchHistoricalBands(
        symbol: String,
        market: ValuationMarket,
    ): Pair<HistoricalMultipleBand, HistoricalMultipleBand> {
        val defaultPe = ValuationCalculator.defaultPeBandFor(symbol, market)
        val defaultPb = ValuationCalculator.defaultPbBandFor(symbol)

        val nowSec = clockMs() / 1000L
        val fiveYearsAgoSec = nowSec - (5L * 365L * 86400L)
        val encodedSymbol = URLEncoder.encode(symbol, "UTF-8")
        val url = "https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/$encodedSymbol" +
            "?type=trailingPeRatio,trailingPbRatio&period1=$fiveYearsAgoSec&period2=$nowSec"

        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()
            yahooClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@runCatching defaultPe to defaultPb
                }
                val body = resp.body?.string().orEmpty()
                parseTimeseriesBands(body, defaultPe, defaultPb)
            }
        }.getOrDefault(defaultPe to defaultPb)
    }

    internal fun enrichTwWithTwse(
        quotes: List<RawScreenerQuote>,
        twseMap: Map<String, TwseValuationRow>,
    ): List<RawScreenerQuote> {
        if (twseMap.isEmpty()) return quotes
        return quotes.map { q ->
            val code = q.symbol.substringBefore(".").trim().uppercase(Locale.US)
            val twse = twseMap[code] ?: return@map q
            val price = q.regularMarketPrice
            val mergedPe = q.trailingPE?.takeIf { it > 0.0 } ?: twse.peRatio
            val mergedPb = q.priceToBook?.takeIf { it > 0.0 } ?: twse.pbRatio
            val mergedEps = q.epsTrailingTwelveMonths?.takeIf { it > 0.0 }
                ?: if (price > 0.0 && mergedPe != null && mergedPe > 0.0) price / mergedPe else null
            val mergedBook = q.bookValue?.takeIf { it > 0.0 }
                ?: if (price > 0.0 && mergedPb != null && mergedPb > 0.0) price / mergedPb else null
            q.copy(
                trailingPE = mergedPe,
                priceToBook = mergedPb,
                epsTrailingTwelveMonths = mergedEps,
                bookValue = mergedBook,
                dividendYieldPercent = q.dividendYieldPercent ?: twse.dividendYield,
            )
        }
    }

    internal fun parseScreenerJson(rawJson: String): List<RawScreenerQuote> {
        return runCatching {
            val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return emptyList()
            val finance = root["finance"] as? JsonObject ?: return emptyList()
            val resultArr = finance["result"] as? JsonArray ?: return emptyList()
            val firstResult = resultArr.firstOrNull() as? JsonObject ?: return emptyList()
            val quotesArr = firstResult["quotes"] as? JsonArray ?: return emptyList()
            parseQuotesArray(quotesArr)
        }.getOrDefault(emptyList())
    }

    internal fun parseQuoteResponseJson(rawJson: String): List<RawScreenerQuote> {
        return runCatching {
            val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return emptyList()
            val qr = root["quoteResponse"] as? JsonObject ?: return emptyList()
            val resultArr = qr["result"] as? JsonArray ?: return emptyList()
            parseQuotesArray(resultArr)
        }.getOrDefault(emptyList())
    }

    private fun parseQuotesArray(quotesArr: JsonArray): List<RawScreenerQuote> {
        return quotesArr.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val symbol = obj.str("symbol")?.trim()?.uppercase(Locale.US).orEmpty()
            if (symbol.isEmpty()) return@mapNotNull null
            RawScreenerQuote(
                symbol = symbol,
                shortName = obj.str("shortName").orEmpty(),
                longName = obj.str("longName").orEmpty(),
                marketCap = obj.long("marketCap") ?: 0L,
                regularMarketPrice = obj.dbl("regularMarketPrice") ?: 0.0,
                regularMarketChangePercent = obj.dbl("regularMarketChangePercent"),
                trailingPE = obj.dbl("trailingPE"),
                forwardPE = obj.dbl("forwardPE"),
                epsTrailingTwelveMonths = obj.dbl("epsTrailingTwelveMonths"),
                epsForward = obj.dbl("epsForward"),
                priceToBook = obj.dbl("priceToBook"),
                bookValue = obj.dbl("bookValue"),
                dividendYieldPercent = obj.dbl("trailingAnnualDividendYield")?.let {
                    if (it > 0.0 && it < 0.3) it * 100.0 else it
                },
            )
        }
    }

    internal fun parseTwseBwibbuAll(rawJson: String): Map<String, TwseValuationRow> {
        return runCatching {
            val arr = json.parseToJsonElement(rawJson) as? JsonArray ?: return emptyMap()
            val map = HashMap<String, TwseValuationRow>(arr.size)
            for (elem in arr) {
                val obj = elem as? JsonObject ?: continue
                val code = obj.str("Code")?.trim()?.uppercase(Locale.US).orEmpty()
                val name = obj.str("Name")?.trim().orEmpty()
                if (code.isEmpty()) continue
                val pe = obj.str("PEratio")?.replace(",", "")?.toDoubleOrNull()?.takeIf { it > 0.0 }
                val pb = obj.str("PBratio")?.replace(",", "")?.toDoubleOrNull()?.takeIf { it > 0.0 }
                val div = obj.str("DividendYield")?.replace(",", "")?.toDoubleOrNull()?.takeIf { it >= 0.0 }
                map[code] = TwseValuationRow(
                    code = code,
                    name = name,
                    peRatio = pe,
                    pbRatio = pb,
                    dividendYield = div,
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    internal fun parseTimeseriesBands(
        rawJson: String,
        defaultPe: HistoricalMultipleBand,
        defaultPb: HistoricalMultipleBand,
    ): Pair<HistoricalMultipleBand, HistoricalMultipleBand> {
        return runCatching {
            val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return defaultPe to defaultPb
            val ts = root["timeseries"] as? JsonObject ?: return defaultPe to defaultPb
            val results = ts["result"] as? JsonArray ?: return defaultPe to defaultPb

            val peHistory = ArrayList<Double>()
            val pbHistory = ArrayList<Double>()

            for (item in results) {
                val obj = item as? JsonObject ?: continue
                extractReportedValues(obj["trailingPeRatio"] as? JsonArray, peHistory)
                extractReportedValues(obj["trailingPbRatio"] as? JsonArray, pbHistory)
            }

            val peBand = ValuationCalculator.computeHistoricalPeBand(peHistory, defaultPe)
            val pbBand = ValuationCalculator.computeHistoricalPbBand(pbHistory, defaultPb)
            peBand to pbBand
        }.getOrDefault(defaultPe to defaultPb)
    }

    private fun extractReportedValues(arr: JsonArray?, target: MutableList<Double>) {
        if (arr == null) return
        for (entry in arr) {
            val obj = entry as? JsonObject ?: continue
            val reported = obj["reportedValue"] as? JsonObject ?: continue
            val rawVal = reported.dbl("raw") ?: continue
            if (!rawVal.isNaN() && !rawVal.isInfinite()) {
                target.add(rawVal)
            }
        }
    }

    private fun readDiskCache(): ValuationRadarSnapshot? {
        val file = context?.cacheDir?.let { File(it, CACHE_FILE_NAME) } ?: return null
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(ValuationRadarSnapshot.serializer(), file.readText())
        }.getOrNull()
    }

    private fun writeDiskCache(snapshot: ValuationRadarSnapshot) {
        val file = context?.cacheDir?.let { File(it, CACHE_FILE_NAME) } ?: return
        runCatching {
            file.writeText(json.encodeToString(ValuationRadarSnapshot.serializer(), snapshot))
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.dbl(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull

    private data class HttpAttempt(
        val body: String?,
        val isTransient: Boolean,
    )

    companion object {
        private const val TAG = "ValuationRemoteService"
        private const val CACHE_FILE_NAME = "valuation_radar_cache.json"
        private const val MEMORY_CACHE_TTL_MS = 30 * 60 * 1000L
        private const val DISK_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val CRUMB_TTL_MS = 30 * 60 * 1000L
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
