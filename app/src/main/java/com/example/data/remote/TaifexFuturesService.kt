package com.example.data.remote

import android.util.Log
import com.example.domain.model.TaiwanFuturesChipSnapshot
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class TaifexFuturesService(
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var memoryCache: TaiwanFuturesChipSnapshot? = null
    private var lastFetchTimestamp: Long = 0L

    suspend fun fetchFuturesChipSnapshot(forceRefresh: Boolean = false): TaiwanFuturesChipSnapshot? =
        withContext(Dispatchers.IO) {
            val now = clockMs()
            if (!forceRefresh && memoryCache != null && (now - lastFetchTimestamp) in 0 until CACHE_EXPIRATION_MS) {
                return@withContext memoryCache
            }

            val snapshot = fetchFromOfficialCsv() ?: fetchFromOpenApi()
            if (snapshot != null) {
                memoryCache = snapshot
                lastFetchTimestamp = clockMs()
            }
            snapshot ?: memoryCache
        }

    private suspend fun fetchFromOfficialCsv(): TaiwanFuturesChipSnapshot? {
        val taipeiTz = TimeZone.getTimeZone("Asia/Taipei")
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US).apply {
            timeZone = taipeiTz
        }
        val cal = Calendar.getInstance(taipeiTz).apply {
            timeInMillis = clockMs()
        }
        val endDateStr = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -21)
        val startDateStr = dateFormat.format(cal.time)

        val (mtxCsv, tmfCsv) = withContext(Dispatchers.IO) {
            val mtxDeferred = async {
                postCsvWithRetry(
                    url = OFFICIAL_DAILY_FUT_CSV_URL,
                    formBody = FormBody.Builder()
                        .add("down_type", "1")
                        .add("commodity_id", "MTX")
                        .add("queryStartDate", startDateStr)
                        .add("queryEndDate", endDateStr)
                        .build(),
                    tag = "TAIFEX_CSV_MTX",
                )
            }
            val tmfDeferred = async {
                postCsvWithRetry(
                    url = OFFICIAL_DAILY_FUT_CSV_URL,
                    formBody = FormBody.Builder()
                        .add("down_type", "1")
                        .add("commodity_id", "TMF")
                        .add("queryStartDate", startDateStr)
                        .add("queryEndDate", endDateStr)
                        .build(),
                    tag = "TAIFEX_CSV_TMF",
                )
            }
            mtxDeferred.await() to tmfDeferred.await()
        }

        if (mtxCsv == null || tmfCsv == null) return null

        val mtxTotalOiByDate = parseDailyFutCsvTotalOiByDate(mtxCsv, "MTX")
        val tmfTotalOiByDate = parseDailyFutCsvTotalOiByDate(tmfCsv, "TMF")
        val tradingDates = mtxTotalOiByDate.keys.sorted()
        if (tradingDates.isEmpty()) return null

        // Try the latest trading date first; if institutional data is not yet published
        // right after market close, fall back one trading day so OI denominator stays aligned.
        val minIdx = (tradingDates.lastIndex - 1).coerceAtLeast(0)
        for (idx in tradingDates.lastIndex downTo minIdx) {
            val targetDate = tradingDates[idx]
            val prevDate = tradingDates.getOrNull(idx - 1)
            val queryStartDate = prevDate ?: targetDate

            val instCsv = postCsvWithRetry(
                url = OFFICIAL_INST_FUT_CSV_URL,
                formBody = FormBody.Builder()
                    .add("queryStartDate", queryStartDate)
                    .add("queryEndDate", targetDate)
                    .build(),
                tag = "TAIFEX_CSV_INST",
            ) ?: continue

            if (!instCsv.contains("臺股期貨")) continue

            val parsed = parseOfficialCsvData(
                instCsv = instCsv,
                mtxTotalOiByDate = mtxTotalOiByDate,
                tmfTotalOiByDate = tmfTotalOiByDate,
                targetDate = targetDate,
                prevDate = prevDate,
            )
            if (parsed != null) return parsed
        }
        return null
    }

    private suspend fun fetchFromOpenApi(): TaiwanFuturesChipSnapshot? = withContext(Dispatchers.IO) {
        val instDeferred = async {
            fetchJsonWithRetry(
                url = OPENAPI_INST_FUT_URL,
                tag = "TAIFEX_INST_FUT",
            )
        }
        val dailyDeferred = async {
            fetchJsonWithRetry(
                url = OPENAPI_DAILY_FUT_URL,
                tag = "TAIFEX_DAILY_FUT",
            )
        }

        val instJson = instDeferred.await() ?: return@withContext null
        val dailyJson = dailyDeferred.await() ?: return@withContext null
        parseFuturesChipData(instJson, dailyJson)
    }

    private suspend fun postCsvWithRetry(
        url: String,
        formBody: RequestBody,
        tag: String,
    ): String? {
        return try {
            executeWithExponentialBackoff(
                maxAttempts = 3,
                delayFn = delayFn,
                shouldRetry = { it.isTransient },
            ) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "text/csv, text/plain, */*")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.code in 500..599 -> TaifexFetchAttempt(null, isTransient = true)
                        !response.isSuccessful -> {
                            logWarn("$tag HTTP ${response.code}")
                            TaifexFetchAttempt(null, isTransient = false)
                        }
                        else -> {
                            val bytes = response.body?.bytes()
                            val decoded = bytes?.let { decodeTaifexCsv(it) }
                            val validCsv = decoded?.takeIf { text ->
                                val trimmed = text.trimStart()
                                trimmed.isNotEmpty() && !trimmed.startsWith("<") && trimmed.contains(",")
                            }
                            TaifexFetchAttempt(validCsv, isTransient = false)
                        }
                    }
                }
            }.body
        } catch (e: Exception) {
            logWarn("$tag failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchJsonWithRetry(url: String, tag: String): String? {
        return try {
            executeWithExponentialBackoff(
                maxAttempts = 3,
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
                        response.code in 500..599 -> TaifexFetchAttempt(null, isTransient = true)
                        !response.isSuccessful -> {
                            logWarn("$tag HTTP ${response.code}")
                            TaifexFetchAttempt(null, isTransient = false)
                        }
                        else -> TaifexFetchAttempt(response.body?.string()?.takeIf { it.isNotBlank() }, isTransient = false)
                    }
                }
            }.body
        } catch (e: Exception) {
            logWarn("$tag failed: ${e.message}")
            null
        }
    }

    private data class TaifexFetchAttempt(
        val body: String?,
        val isTransient: Boolean,
    )

    internal fun parseDailyFutCsvTotalOiByDate(
        csv: String,
        expectedContract: String,
    ): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        csv.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val cols = line.split(',')
            if (cols.size <= 17) return@forEach

            val date = cols[0].trim()
            if (!CSV_DATE_REGEX.matches(date)) return@forEach
            if (cols[1].trim() != expectedContract) return@forEach

            val month = cols[2].trim()
            if (month.contains('/')) return@forEach
            if (cols[17].trim() != "一般") return@forEach

            val oi = cols[11].trim().replace(",", "").toLongOrNull() ?: 0L
            if (oi > 0L) {
                result[date] = (result[date] ?: 0L) + oi
            }
        }
        return result.filterValues { it > 0L }
    }

    internal fun parseOfficialCsvData(
        instCsv: String,
        mtxTotalOiByDate: Map<String, Long>,
        tmfTotalOiByDate: Map<String, Long>,
        targetDate: String,
        prevDate: String? = null,
    ): TaiwanFuturesChipSnapshot? {
        return runCatching {
            var foreignNetOi = 0L
            var foreignTradingNet = 0L
            var trustNetOi = 0L
            var dealerNetOi = 0L
            var hasTxForeign = false
            var prevForeignNetOi: Long? = null

            var mtxInstNet = 0L
            var tmfInstNet = 0L

            instCsv.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach
                val cols = line.split(',')
                if (cols.size < 14) return@forEach

                val date = cols[0].trim()
                if (!CSV_DATE_REGEX.matches(date)) return@forEach
                val contractName = cols[1].trim()
                val item = cols[2].trim()
                val tradingNet = cols[7].trim().replace(",", "").toLongOrNull() ?: 0L
                val netOi = cols[13].trim().replace(",", "").toLongOrNull() ?: 0L

                when (date) {
                    targetDate -> when (contractName) {
                        "臺股期貨" -> when {
                            item.contains("外資") -> {
                                foreignNetOi = netOi
                                foreignTradingNet = tradingNet
                                hasTxForeign = true
                            }
                            item.contains("投信") -> trustNetOi = netOi
                            item.contains("自營商") -> dealerNetOi = netOi
                        }
                        "小型臺指期貨" -> mtxInstNet += netOi
                        "微型臺指期貨" -> tmfInstNet += netOi
                    }
                    prevDate -> if (
                        prevDate != targetDate &&
                        contractName == "臺股期貨" &&
                        item.contains("外資")
                    ) {
                        prevForeignNetOi = netOi
                    }
                }
            }

            if (!hasTxForeign) return null

            val mtxTotalOi = mtxTotalOiByDate[targetDate] ?: 0L
            val tmfTotalOi = tmfTotalOiByDate[targetDate] ?: 0L
            val mtxRetailNetOi = -mtxInstNet
            val tmfRetailNetOi = -tmfInstNet

            val mtxRatio = if (mtxTotalOi > 0L) (mtxRetailNetOi.toDouble() / mtxTotalOi.toDouble()) * 100.0 else null
            val tmfRatio = if (tmfTotalOi > 0L) (tmfRetailNetOi.toDouble() / tmfTotalOi.toDouble()) * 100.0 else null

            TaiwanFuturesChipSnapshot(
                date = targetDate,
                foreignTxNetOi = foreignNetOi,
                foreignTxTradingNet = foreignTradingNet,
                trustTxNetOi = trustNetOi,
                dealerTxNetOi = dealerNetOi,
                mtxRetailNetOi = mtxRetailNetOi,
                mtxTotalOi = mtxTotalOi,
                mtxRetailRatio = mtxRatio,
                tmfRetailNetOi = tmfRetailNetOi,
                tmfTotalOi = tmfTotalOi,
                tmfRetailRatio = tmfRatio,
                foreignTxOiChange = prevForeignNetOi?.let { foreignNetOi - it },
            )
        }.onFailure { e ->
            logError("Failed to parse TAIFEX official CSV data: ${e.message}", e)
        }.getOrNull()
    }

    internal fun parseFuturesChipData(
        institutionalJson: String,
        dailyMarketJson: String,
    ): TaiwanFuturesChipSnapshot? {
        return runCatching {
            val instArr = json.parseToJsonElement(institutionalJson) as? JsonArray ?: return null
            val dailyArr = json.parseToJsonElement(dailyMarketJson) as? JsonArray ?: return null

            var dataDate = ""
            var foreignNetOi = 0L
            var foreignTradingNet = 0L
            var trustNetOi = 0L
            var dealerNetOi = 0L
            var hasTxForeign = false

            var mtxInstNet = 0L
            var tmfInstNet = 0L

            for (elem in instArr) {
                val obj = elem as? JsonObject ?: continue
                val contractCode = obj.str("ContractCode")
                val item = obj.str("Item")
                if (dataDate.isEmpty()) {
                    dataDate = obj.str("Date")
                }

                when (contractCode) {
                    "臺股期貨" -> {
                        val netOi = obj.long("OpenInterest(Net)")
                        val tradingNet = obj.long("TradingVolume(Net)")
                        when {
                            item.contains("外資") -> {
                                foreignNetOi = netOi
                                foreignTradingNet = tradingNet
                                hasTxForeign = true
                            }
                            item.contains("投信") -> trustNetOi = netOi
                            item.contains("自營商") -> dealerNetOi = netOi
                        }
                    }
                    "小型臺指期貨" -> mtxInstNet += obj.long("OpenInterest(Net)")
                    "微型臺指期貨" -> tmfInstNet += obj.long("OpenInterest(Net)")
                }
            }

            if (!hasTxForeign) return null

            var mtxTotalOi = 0L
            var tmfTotalOi = 0L

            for (elem in dailyArr) {
                val obj = elem as? JsonObject ?: continue
                if (obj.str("TradingSession") != "一般") continue
                val month = obj.str("ContractMonth(Week)")
                if (month.contains('/')) continue

                when (obj.str("Contract")) {
                    "MTX" -> mtxTotalOi += obj.long("OpenInterest")
                    "TMF" -> tmfTotalOi += obj.long("OpenInterest")
                }
            }

            val mtxRetailNetOi = -mtxInstNet
            val tmfRetailNetOi = -tmfInstNet

            val mtxRatio = if (mtxTotalOi > 0L) (mtxRetailNetOi.toDouble() / mtxTotalOi.toDouble()) * 100.0 else null
            val tmfRatio = if (tmfTotalOi > 0L) (tmfRetailNetOi.toDouble() / tmfTotalOi.toDouble()) * 100.0 else null

            TaiwanFuturesChipSnapshot(
                date = dataDate,
                foreignTxNetOi = foreignNetOi,
                foreignTxTradingNet = foreignTradingNet,
                trustTxNetOi = trustNetOi,
                dealerTxNetOi = dealerNetOi,
                mtxRetailNetOi = mtxRetailNetOi,
                mtxTotalOi = mtxTotalOi,
                mtxRetailRatio = mtxRatio,
                tmfRetailNetOi = tmfRetailNetOi,
                tmfTotalOi = tmfTotalOi,
                tmfRetailRatio = tmfRatio,
                foreignTxOiChange = null,
            )
        }.onFailure { e ->
            logError("Failed to parse TAIFEX futures chip data: ${e.message}", e)
        }.getOrNull()
    }

    private fun decodeTaifexCsv(bytes: ByteArray): String {
        for (charsetName in listOf("MS950", "Big5")) {
            val decoded = runCatching {
                String(bytes, Charset.forName(charsetName))
            }.getOrNull()
            if (decoded != null) return decoded
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun JsonObject.str(key: String): String {
        return (this[key] as? JsonPrimitive)?.content?.trim().orEmpty()
    }

    private fun JsonObject.long(key: String): Long {
        return str(key).replace(",", "").toLongOrNull() ?: 0L
    }

    private fun logWarn(msg: String) {
        runCatching { Log.w("TaifexFuturesService", msg) }
    }

    private fun logError(msg: String, tr: Throwable? = null) {
        runCatching { Log.e("TaifexFuturesService", msg, tr) }
    }

    private companion object {
        const val OFFICIAL_INST_FUT_CSV_URL = "https://www.taifex.com.tw/cht/3/futContractsDateDown"
        const val OFFICIAL_DAILY_FUT_CSV_URL = "https://www.taifex.com.tw/cht/3/futDataDown"
        const val OPENAPI_INST_FUT_URL =
            "https://openapi.taifex.com.tw/v1/MarketDataOfMajorInstitutionalTradersDetailsOfFuturesContractsBytheDate"
        const val OPENAPI_DAILY_FUT_URL = "https://openapi.taifex.com.tw/v1/DailyMarketReportFut"
        const val CACHE_EXPIRATION_MS = 30 * 60 * 1000L
        val CSV_DATE_REGEX = Regex("""^\d{4}/\d{2}/\d{2}$""")
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
