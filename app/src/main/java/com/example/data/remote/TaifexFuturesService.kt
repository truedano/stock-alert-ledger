package com.example.data.remote

import android.util.Log
import com.example.domain.model.TaiwanFuturesChipSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class TaifexFuturesService(
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchFuturesChipSnapshot(): TaiwanFuturesChipSnapshot? = withContext(Dispatchers.IO) {
        val instDeferred = async {
            fetchJsonWithRetry(
                url = "https://openapi.taifex.com.tw/v1/MarketDataOfMajorInstitutionalTradersDetailsOfFuturesContractsBytheDate",
                tag = "TAIFEX_INST_FUT",
            )
        }
        val dailyDeferred = async {
            fetchJsonWithRetry(
                url = "https://openapi.taifex.com.tw/v1/DailyMarketReportFut",
                tag = "TAIFEX_DAILY_FUT",
            )
        }

        val instJson = instDeferred.await() ?: return@withContext null
        val dailyJson = dailyDeferred.await() ?: return@withContext null
        parseFuturesChipData(instJson, dailyJson)
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
                            Log.w("TaifexFuturesService", "$tag HTTP ${response.code}")
                            TaifexFetchAttempt(null, isTransient = false)
                        }
                        else -> TaifexFetchAttempt(response.body?.string()?.takeIf { it.isNotBlank() }, isTransient = false)
                    }
                }
            }.body
        } catch (e: Exception) {
            Log.w("TaifexFuturesService", "$tag failed: ${e.message}")
            null
        }
    }

    private data class TaifexFetchAttempt(
        val body: String?,
        val isTransient: Boolean,
    )

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
            )
        }.onFailure { e ->
            Log.e("TaifexFuturesService", "Failed to parse TAIFEX futures chip data: ${e.message}", e)
        }.getOrNull()
    }

    private fun JsonObject.str(key: String): String {
        return (this[key] as? JsonPrimitive)?.content?.trim().orEmpty()
    }

    private fun JsonObject.long(key: String): Long {
        return str(key).replace(",", "").toLongOrNull() ?: 0L
    }

    private companion object {
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
