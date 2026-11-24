package com.example.data.remote

import android.util.Log
import com.example.domain.model.FearAndGreedData
import com.example.domain.model.FearAndGreedSubIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class CnnSubIndicatorSection(
    val score: Double? = null,
    val rating: String = "",
)

@Serializable
internal data class CnnFearAndGreedResponse(
    @SerialName("fear_and_greed")
    val fearAndGreed: CnnFearAndGreedItem? = null,
    @SerialName("market_momentum_sp500")
    val marketMomentumSp500: CnnSubIndicatorSection? = null,
    @SerialName("stock_price_strength")
    val stockPriceStrength: CnnSubIndicatorSection? = null,
    @SerialName("stock_price_breadth")
    val stockPriceBreadth: CnnSubIndicatorSection? = null,
    @SerialName("put_call_options")
    val putCallOptions: CnnSubIndicatorSection? = null,
    @SerialName("market_volatility_vix")
    val marketVolatilityVix: CnnSubIndicatorSection? = null,
    @SerialName("safe_haven_demand")
    val safeHavenDemand: CnnSubIndicatorSection? = null,
    @SerialName("junk_bond_demand")
    val junkBondDemand: CnnSubIndicatorSection? = null,
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

    internal fun parseFearAndGreedJson(body: String): FearAndGreedData? {
        if (body.isBlank()) return null
        return runCatching {
            val parsed = json.decodeFromString(CnnFearAndGreedResponse.serializer(), body)
            val item = parsed.fearAndGreed ?: return@runCatching null

            val subIndicators = listOfNotNull(
                parsed.marketMomentumSp500?.toSubIndicator(
                    key = "market_momentum_sp500",
                    nameZh = "市場動能",
                    descriptionZh = "S&P 500 指數與 125 日均線乖離",
                ),
                parsed.stockPriceStrength?.toSubIndicator(
                    key = "stock_price_strength",
                    nameZh = "股價強度",
                    descriptionZh = "NYSE 創 52 週新高與新低家數比率",
                ),
                parsed.stockPriceBreadth?.toSubIndicator(
                    key = "stock_price_breadth",
                    nameZh = "股價廣度",
                    descriptionZh = "上漲與下跌成交量比 (McClellan 指標)",
                ),
                parsed.putCallOptions?.toSubIndicator(
                    key = "put_call_options",
                    nameZh = "期權看跌/看漲比",
                    descriptionZh = "5 日平均 Put/Call Ratio 避險情緒",
                ),
                parsed.marketVolatilityVix?.toSubIndicator(
                    key = "market_volatility_vix",
                    nameZh = "市場波動率",
                    descriptionZh = "VIX 恐慌指數與 50 日均線水位",
                ),
                parsed.safeHavenDemand?.toSubIndicator(
                    key = "safe_haven_demand",
                    nameZh = "避險需求",
                    descriptionZh = "近 20 日股票與公債報酬率差距",
                ),
                parsed.junkBondDemand?.toSubIndicator(
                    key = "junk_bond_demand",
                    nameZh = "垃圾債需求",
                    descriptionZh = "非投資等級債與投資級公債利差",
                ),
            )

            FearAndGreedData(
                score = item.score,
                rating = item.rating,
                timestamp = item.timestamp,
                previousClose = item.previousClose,
                previousOneWeek = item.previousOneWeek,
                previousOneMonth = item.previousOneMonth,
                previousOneYear = item.previousOneYear,
                subIndicators = subIndicators,
            )
        }.getOrNull()
    }

    private fun CnnSubIndicatorSection.toSubIndicator(
        key: String,
        nameZh: String,
        descriptionZh: String,
    ): FearAndGreedSubIndicator? {
        val value = score ?: return null
        return FearAndGreedSubIndicator(
            key = key,
            nameZh = nameZh,
            descriptionZh = descriptionZh,
            score = value,
            rating = rating,
        )
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
                val data = parseFearAndGreedJson(body) ?: return@withContext memoryCache
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
