package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class TwseMarginService(
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private suspend fun fetchTwseJson(url: String, endpointName: String): String? {
        return try {
            val outcome = executeWithExponentialBackoff(
                maxAttempts = 3,
                delayFn = delayFn,
                shouldRetry = { it.isTransientServerError },
            ) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.code in 500..599 -> {
                            Log.w("TwseMarginService", "TWSE $endpointName transient HTTP ${response.code}")
                            TwseFetchResult(body = null, isTransientServerError = true)
                        }
                        !response.isSuccessful -> {
                            Log.w("TwseMarginService", "TWSE $endpointName failed with HTTP ${response.code}")
                            TwseFetchResult(body = null, isTransientServerError = false)
                        }
                        else -> {
                            val body = response.body?.string()?.takeIf { it.isNotBlank() }
                            TwseFetchResult(body = body, isTransientServerError = false)
                        }
                    }
                }
            }
            outcome.body
        } catch (e: Exception) {
            Log.e("TwseMarginService", "Error fetching TWSE $endpointName data: ${e.message}", e)
            null
        }
    }

    private data class TwseFetchResult(
        val body: String?,
        val isTransientServerError: Boolean,
    )

    suspend fun fetchMarketMarginSnapshot(): MarketMarginSnapshot? = withContext(Dispatchers.IO) {
        val body = fetchTwseJson(
            url = "https://www.twse.com.tw/rwd/zh/marginTrading/BFIJ3U_TREND?response=json",
            endpointName = "BFIJ3U_TREND",
        ) ?: return@withContext null

        try {
            val parsed = json.decodeFromString(TwseCreditTrendResponse.serializer(), body)
            if (parsed.stat != "OK" || parsed.data.isEmpty()) {
                return@withContext null
            }
            val validRows = parsed.data.filter { it.keepRate > 0.0 }
            val latest = validRows.lastOrNull() ?: parsed.data.last()
            val prev = if (validRows.size >= 2) validRows[validRows.lastIndex - 1] else null
            val rates = validRows.map { it.keepRate }
            val historyPoints = validRows.takeLast(15).map { row ->
                MarketMarginDayPoint(
                    date = row.date,
                    keepRate = row.keepRate,
                    crdAmt = row.crdAmt,
                    belowAccNum = row.belowAccNum,
                    callAccNum = row.callAccNum,
                    callAmt = row.callAmt,
                    exeAccNum = row.exeAccNum,
                    exeAmt = row.exeAmt,
                )
            }
            val crdChange = if (prev != null && latest.crdAmt > 0L && prev.crdAmt > 0L) {
                latest.crdAmt - prev.crdAmt
            } else {
                null
            }
            MarketMarginSnapshot(
                date = latest.date,
                keepRate = latest.keepRate,
                callAccNum = latest.callAccNum,
                exeAccNum = latest.exeAccNum,
                recentKeepRates = rates,
                crdAmt = latest.crdAmt,
                crdAmtChange = crdChange,
                belowAccNum = latest.belowAccNum,
                callAmt = latest.callAmt,
                exeAmt = latest.exeAmt,
                marginAccNum = latest.marginAccNum,
                marginAccRate = latest.marginAccRate,
                historyPoints = historyPoints,
            )
        } catch (e: Exception) {
            Log.e("TwseMarginService", "Error parsing TWSE margin data: ${e.message}", e)
            null
        }
    }

    /**
     * 抓取證交所全市場個股融資融券彙總資料 (MI_MARGN)
     * 回傳以股票代號（例如 "2330"）為 Key 的 StockMarginInfo 字典。
     */
    suspend fun fetchAllStockMargins(): Map<String, StockMarginInfo> = withContext(Dispatchers.IO) {
        val body = fetchTwseJson(
            url = "https://www.twse.com.tw/rwd/zh/marginTrading/MI_MARGN?response=json&selectType=ALL",
            endpointName = "MI_MARGN",
        ) ?: return@withContext emptyMap()

        try {
            val root = json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject
                ?: return@withContext emptyMap()
            val stat = (root["stat"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
            if (stat != "OK") return@withContext emptyMap()

            val tables = root["tables"] as? kotlinx.serialization.json.JsonArray
                ?: return@withContext emptyMap()
            // 第二張表為個股融資融券彙總 (全部)
            val targetTable = tables.firstOrNull { elem ->
                val title = (elem as? kotlinx.serialization.json.JsonObject)?.get("title")
                (title as? kotlinx.serialization.json.JsonPrimitive)?.content?.contains("融資融券彙總") == true
            } ?: tables.getOrNull(1) as? kotlinx.serialization.json.JsonObject
            ?: return@withContext emptyMap()

            val rows = (targetTable as? kotlinx.serialization.json.JsonObject)?.get("data") as? kotlinx.serialization.json.JsonArray
                ?: return@withContext emptyMap()

            val resultMap = mutableMapOf<String, StockMarginInfo>()
            for (rowElem in rows) {
                val row = (rowElem as? kotlinx.serialization.json.JsonArray)?.map {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                } ?: continue

                // 欄位規格：
                // [0]: 代號, [1]: 名稱, [2]: 資買進, [3]: 資賣出, [4]: 資現償, [5]: 資前日餘額, [6]: 資今日餘額,
                // [7]: 限額, [8]: 券買進, [9]: 券賣出, [10]: 券現券償還, [11]: 券前日餘額, [12]: 券今日餘額
                if (row.size < 13) continue
                val symbol = row[0].trim()
                val name = row[1].trim()
                val prevMargin = row[5].replace(",", "").trim().toLongOrNull() ?: 0L
                val currMargin = row[6].replace(",", "").trim().toLongOrNull() ?: 0L
                val marginChange = currMargin - prevMargin

                val prevShort = row[11].replace(",", "").trim().toLongOrNull() ?: 0L
                val currShort = row[12].replace(",", "").trim().toLongOrNull() ?: 0L
                val shortChange = currShort - prevShort

                resultMap[symbol] = StockMarginInfo(
                    symbol = symbol,
                    name = name,
                    marginBalance = currMargin,
                    marginChange = marginChange,
                    shortBalance = currShort,
                    shortChange = shortChange,
                    estimatedKeepRate = null,
                )
            }
            resultMap
        } catch (e: Exception) {
            Log.e("TwseMarginService", "Error parsing TWSE MI_MARGN data: ${e.message}", e)
            emptyMap()
        }
    }

    private companion object {
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
