package com.example.data.remote

import android.util.Log
import com.example.domain.model.FearAndGreedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class CnnFearAndGreedResponse(
    @SerialName("fear_and_greed")
    val fearAndGreed: CnnFearAndGreedItem? = null,
)

@Serializable
internal data class CnnFearAndGreedItem(
    val score: Double = 0.0,
    val rating: String = "",
    val timestamp: String = "",
    @SerialName("previous_close")
    val previousClose: Double? = null,
    @SerialName("previous_1_week")
    val previousOneWeek: Double? = null,
    @SerialName("previous_1_month")
    val previousOneMonth: Double? = null,
    @SerialName("previous_1_year")
    val previousOneYear: Double? = null,
)

class FearAndGreedService(
    private val client: OkHttpClient = YahooFinanceService.defaultOkHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var memoryCache: FearAndGreedData? = null
    private var lastFetchTimestamp: Long = 0L

    companion object {
        private const val CNN_FNG_URL = "https://production.dataviz.cnn.io/index/fearandgreed/graphdata"
        private const val CACHE_EXPIRATION_MS = 60 * 60 * 1000L // 1 小時快取
    }

    suspend fun fetchFearAndGreed(forceRefresh: Boolean = false): FearAndGreedData? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && memoryCache != null && (now - lastFetchTimestamp < CACHE_EXPIRATION_MS)) {
            return@withContext memoryCache
        }

        val request = Request.Builder()
            .url(CNN_FNG_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.cnn.com/markets/fear-and-greed")
            .header("Origin", "https://www.cnn.com")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("FearAndGreedService", "CNN Fear & Greed API failed with HTTP ${response.code}")
                    return@withContext memoryCache
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext memoryCache

                val parsed = json.decodeFromString(CnnFearAndGreedResponse.serializer(), body)
                val item = parsed.fearAndGreed ?: return@withContext memoryCache

                val data = FearAndGreedData(
                    score = item.score,
                    rating = item.rating,
                    timestamp = item.timestamp,
                    previousClose = item.previousClose,
                    previousOneWeek = item.previousOneWeek,
                    previousOneMonth = item.previousOneMonth,
                    previousOneYear = item.previousOneYear,
                )
                memoryCache = data
                lastFetchTimestamp = now
                data
            }
        } catch (e: Exception) {
            Log.e("FearAndGreedService", "Error fetching CNN Fear & Greed data: ${e.message}", e)
            memoryCache
        }
    }
}
