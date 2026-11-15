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
            else -> "觸及 ${days}MA"
        }
    }
}
