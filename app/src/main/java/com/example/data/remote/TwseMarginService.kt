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
}
