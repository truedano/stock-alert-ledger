package com.example.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwseMarginTrendResponse(
    val stat: String = "",
    val date: String = "",
    val days: Int = 0,
    val data: List<TwseMarginTrendItem> = emptyList(),
)

@Serializable
data class TwseMarginTrendItem(
    val date: String = "",
    val marginShr: Long = 0L,
    val marginAmt: Long = 0L,
    val shortShr: Long = 0L,
    val marketValue: Double = 0.0,
)

@Serializable
data class TwseCreditTrendResponse(
    val stat: String = "",
    val date: String = "",
    val days: Int = 0,
    val data: List<TwseCreditTrendItem> = emptyList(),
)

@Serializable
data class TwseCreditTrendItem(
    val date: String = "",
    val crdAmt: Long = 0L,
    /**
     * 全市場整戶擔保維持率（例如 156.18 代表 156.18%）
     */
    val keepRate: Double = 0.0,
    val belowAccNum: Long = 0L,
    val callAccNum: Long = 0L,
    val callAmt: Long = 0L,
    val exeAccNum: Long = 0L,
    val exeAmt: Long = 0L,
    val marginAccNum: Long = 0L,
    val marginAccRate: Double = 0.0,
)

data class MarketMarginDayPoint(
    val date: String,
    val keepRate: Double,
    val crdAmt: Long = 0L,
    val belowAccNum: Long = 0L,
    val callAccNum: Long = 0L,
    val callAmt: Long = 0L,
    val exeAccNum: Long = 0L,
    val exeAmt: Long = 0L,
)

data class MarketMarginSnapshot(
    val date: String,
    val keepRate: Double,
    val callAccNum: Long,
    val exeAccNum: Long,
    val recentKeepRates: List<Double>,
    val crdAmt: Long = 0L,
    val crdAmtChange: Long? = null,
    val belowAccNum: Long = 0L,
    val callAmt: Long = 0L,
    val exeAmt: Long = 0L,
    val marginAccNum: Long = 0L,
    val marginAccRate: Double = 0.0,
    val historyPoints: List<MarketMarginDayPoint> = emptyList(),
) {
    val keepRateChange: Double?
        get() = if (recentKeepRates.size >= 2) {
            recentKeepRates.last() - recentKeepRates[recentKeepRates.lastIndex - 1]
        } else {
            null
        }
}

data class StockMarginInfo(
    val symbol: String,
    val name: String,
    val marginBalance: Long, // 融資今日餘額 (張)
    val marginChange: Long,  // 融資今日增減 (張，今日 - 前日)
    val shortBalance: Long,  // 融券今日餘額 (張)
    val shortChange: Long,   // 融券今日增減 (張，今日 - 前日)
    val estimatedKeepRate: Double? = null, // 推估融資維持率 (%)
)
