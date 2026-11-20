package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class TwseMarginService(
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchMarketMarginSnapshot(): MarketMarginSnapshot? = withContext(Dispatchers.IO) {
        val url = "https://www.twse.com.tw/rwd/zh/marginTrading/BFIJ3U_TREND?response=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("TwseMarginService", "TWSE BFIJ3U_TREND failed with HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val parsed = json.decodeFromString(TwseCreditTrendResponse.serializer(), body)
                if (parsed.stat != "OK" || parsed.data.isEmpty()) {
                    return@withContext null
                }
                val latest = parsed.data.last()
                val rates = parsed.data.map { it.keepRate }.filter { it > 0.0 }
                MarketMarginSnapshot(
                    date = latest.date,
                    keepRate = latest.keepRate,
                    callAccNum = latest.callAccNum,
                    exeAccNum = latest.exeAccNum,
                    recentKeepRates = rates,
                )
            }
        } catch (e: Exception) {
            Log.e("TwseMarginService", "Error fetching TWSE margin data: ${e.message}", e)
            null
        }
    }

    /**
     * 抓取證交所全市場個股融資融券彙總資料 (MI_MARGN)
     * 回傳以股票代號（例如 "2330"）為 Key 的 StockMarginInfo 字典。
     */
    suspend fun fetchAllStockMargins(): Map<String, StockMarginInfo> = withContext(Dispatchers.IO) {
        val url = "https://www.twse.com.tw/rwd/zh/marginTrading/MI_MARGN?response=json&selectType=ALL"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("TwseMarginService", "TWSE MI_MARGN failed with HTTP ${response.code}")
                    return@withContext emptyMap()
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyMap()

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
            }
        } catch (e: Exception) {
            Log.e("TwseMarginService", "Error fetching TWSE MI_MARGN data: ${e.message}", e)
            emptyMap()
        }
    }
}
