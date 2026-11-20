package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Extensible alert condition contract serialized into Room via JSON.
 * Future condition types (e.g. PriceTargetCondition, RsiCondition) can be added here
 * as new @Serializable subclasses and registered in ConditionEngine without changing
 * the core watchlist storage or worker pipeline.
 */
@Serializable
sealed interface AlertCondition {
    val conditionDisplayName: String
}

@Serializable
enum class MovingAverageTriggerMode {
    @SerialName("below_or_equal")
    BELOW_OR_EQUAL,

    @SerialName("within_percent")
    WITHIN_PERCENT,
}

@Serializable
@SerialName("moving_average")
data class MovingAverageCondition(
    val periodDays: Int = 20,
    val triggerMode: MovingAverageTriggerMode = MovingAverageTriggerMode.BELOW_OR_EQUAL,
    val tolerancePercent: Double = 1.0,
) : AlertCondition {
    override val conditionDisplayName: String
        get() = when (triggerMode) {
            MovingAverageTriggerMode.BELOW_OR_EQUAL -> "跌破或觸及 ${periodDays}MA"
            MovingAverageTriggerMode.WITHIN_PERCENT -> "接近 ${periodDays}MA（±${tolerancePercent}% 內）"
        }

    val shortTriggerTag: String
        get() = maPeriodLabel(periodDays)

    companion object {
        fun maPeriodLabel(days: Int): String = when (days) {
            5 -> "週線 5MA"
            10 -> "雙週線 10MA"
            20 -> "月線 20MA"
            60 -> "季線 60MA"
            120 -> "半年線 120MA"
            240 -> "年線 240MA"
            720 -> "3年線 720MA"
            1200 -> "5年線 1200MA"
            2400 -> "10年線 2400MA"
            else -> "觸及 ${days}MA"
        }
    }
}

/**
 * 極端恐慌抄底條件：自波段高點（預設 250 日 / 52 週最高收盤價）回撤達到指定百分比
 * 例如 drawdownPercent = 20.0 表示從最高點下跌 20%
 */
@Serializable
@SerialName("peak_drawdown")
data class PeakDrawdownCondition(
    val lookbackDays: Int = 250,
    val drawdownPercent: Double = 20.0,
) : AlertCondition {
    override val conditionDisplayName: String
        get() = "${lookbackDays}日高點回撤達 -${drawdownPercent.toInt()}%（恐慌抄底）"

    val shortTriggerTag: String
        get() = "波段高點回撤 -${drawdownPercent.toInt()}%"
}

/**
 * 技術指標出場/超買條件：相對強弱指標 (RSI) 超買或超賣
 * 例如 periodDays = 14, threshold = 70.0, isAbove = true 表示 RSI(14) > 70 獲利了結/過熱警示
 */
@Serializable
@SerialName("rsi")
data class RsiCondition(
    val periodDays: Int = 14,
    val threshold: Double = 70.0,
    val isAbove: Boolean = true,
) : AlertCondition {
    override val conditionDisplayName: String
        get() = if (isAbove) "RSI(${periodDays}) 突破 ${threshold.toInt()}（超買反彈調節）"
                else "RSI(${periodDays}) 跌破 ${threshold.toInt()}（超賣恐慌）"

    val shortTriggerTag: String
        get() = if (isAbove) "RSI>${threshold.toInt()}" else "RSI<${threshold.toInt()}"
}

/**
 * 融資斷頭與反彈抄底條件：
 * 針對大盤全市場信用交易融資維持率（例如跌破 150% 後自低點反彈 20 點）或出場（回升至 170%）
 */
@Serializable
@SerialName("margin_maintenance")
data class MarginMaintenanceCondition(
    val panicThreshold: Double = 150.0,
    val reboundPoints: Double = 20.0,
    val isExitMode: Boolean = false,
    val exitThreshold: Double = 170.0,
) : AlertCondition {
    override val conditionDisplayName: String
        get() = if (isExitMode) {
            "融資維持率回升至 ${exitThreshold.toInt()}% 以上（獲利退回安全停泊）"
        } else {
            "融資維持率跌破 ${panicThreshold.toInt()}% 且低點反彈 ${reboundPoints.toInt()} 點（恐慌斷頭抄底）"
        }

    val shortTriggerTag: String
        get() = if (isExitMode) "維持率回升>${exitThreshold.toInt()}%" else "維持率<${panicThreshold.toInt()}%反彈+${reboundPoints.toInt()}"
}

@Serializable
enum class PriceTargetDirection {
    @SerialName("below_or_equal")
    BELOW_OR_EQUAL,

    @SerialName("above_or_equal")
    ABOVE_OR_EQUAL,
}

/**
 * 絕對目標價提醒條件：
 * 例如 targetPrice = 1000.0, direction = BELOW_OR_EQUAL 表示現價跌破或觸及 1000 元時提醒。
 * autoDisableOnTrigger = true (預設)：觸發一次發出通知後自動停用/完成該規則，避免後續重複騷擾。
 */
@Serializable
@SerialName("price_target")
data class PriceTargetCondition(
    val targetPrice: Double,
    val direction: PriceTargetDirection = PriceTargetDirection.BELOW_OR_EQUAL,
    val tolerancePercent: Double = 0.0,
    val autoDisableOnTrigger: Boolean = true,
) : AlertCondition {
    override val conditionDisplayName: String
        get() {
            val dirText = when (direction) {
                PriceTargetDirection.BELOW_OR_EQUAL -> "跌破或觸及"
                PriceTargetDirection.ABOVE_OR_EQUAL -> "突破或觸及"
            }
            val tolText = if (tolerancePercent > 0.0) "（接近 ±${tolerancePercent}% 內）" else ""
            return "股價 $dirText ${Formatters.formatPrice(targetPrice)}$tolText"
        }

    val shortTriggerTag: String
        get() {
            val symbol = when (direction) {
                PriceTargetDirection.BELOW_OR_EQUAL -> "≤"
                PriceTargetDirection.ABOVE_OR_EQUAL -> "≥"
            }
            return "目標價 $symbol${Formatters.formatPrice(targetPrice)}"
        }
}

