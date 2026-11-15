package com.example.domain.model

import kotlin.math.abs
import kotlin.reflect.KClass

data class MarketSnapshot(
    val symbol: String,
    val currentPrice: Double,
    val historicalCloses: List<Double>,
    val movingAverages: Map<Int, Double> = emptyMap(),
) {
    val availableDays: Int get() = historicalCloses.size

    fun smaFor(periodDays: Int): Double? {
        return movingAverages[periodDays] ?: MovingAverageCalculator.calculateSma(historicalCloses, periodDays)
    }
}

data class ConditionEvaluationResult(
    val isTriggered: Boolean,
    val targetReferenceLabel: String,
    val targetReferencePrice: Double?,
    val distancePercent: Double?,
    val triggerTag: String,
    val statusMessage: String,
    val isInsufficientData: Boolean = false,
)

/**
 * Strategy Pattern contract for evaluating any AlertCondition against market data.
 * Adding new condition types (e.g., PriceTargetCondition, RsiCondition) only requires
 * implementing this interface and registering the strategy in ConditionEngine.
 */
interface ConditionEvaluator<T : AlertCondition> {
    val conditionClass: KClass<T>
    fun evaluate(condition: T, snapshot: MarketSnapshot): ConditionEvaluationResult
    fun requiredPeriods(condition: T): Set<Int> = emptySet()
}

object MovingAverageCalculator {
    fun calculateSma(closes: List<Double>, periodDays: Int): Double? {
        if (periodDays <= 0 || closes.size < periodDays) return null
        val slice = closes.takeLast(periodDays)
        if (slice.isEmpty()) return null
        val avg = slice.average()
        return if (avg.isNaN() || avg.isInfinite() || avg <= 0.0) null else avg
    }

    fun calculateMultipleSmas(closes: List<Double>, periods: Collection<Int>): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        for (period in periods) {
            calculateSma(closes, period)?.let { result[period] = it }
        }
        return result
    }
}

class MovingAverageEvaluator : ConditionEvaluator<MovingAverageCondition> {
    override val conditionClass: KClass<MovingAverageCondition> = MovingAverageCondition::class

    override fun requiredPeriods(condition: MovingAverageCondition): Set<Int> =
        setOf(condition.periodDays)

    override fun evaluate(
        condition: MovingAverageCondition,
        snapshot: MarketSnapshot,
    ): ConditionEvaluationResult {
        val maPrice = snapshot.smaFor(condition.periodDays)
        val label = "${condition.periodDays}MA"

        if (maPrice == null || maPrice <= 0.0) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = null,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "歷史交易日不足 ${condition.periodDays} 日（目前 ${snapshot.availableDays} 日）",
                isInsufficientData = true,
            )
        }

        val currentPrice = snapshot.currentPrice
        if (currentPrice <= 0.0) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = maPrice,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "尚無有效現價",
                isInsufficientData = true,
            )
        }

        val distancePercent = ((currentPrice - maPrice) / maPrice) * 100.0
        val triggered = when (condition.triggerMode) {
            MovingAverageTriggerMode.BELOW_OR_EQUAL -> currentPrice <= maPrice
            MovingAverageTriggerMode.WITHIN_PERCENT -> {
                val tolerance = condition.tolerancePercent.coerceAtLeast(0.0)
                currentPrice <= maPrice || abs(distancePercent) <= tolerance
            }
        }

        val statusMessage = when {
            triggered && condition.triggerMode == MovingAverageTriggerMode.BELOW_OR_EQUAL ->
                "已跌破或觸及 $label"
            triggered ->
                "已進入 $label ±${condition.tolerancePercent}% 買入區間"
            distancePercent > 0 ->
                "高於 $label ${Formatters.formatSignedPercent(distancePercent)}"
            else ->
                "低於 $label ${Formatters.formatSignedPercent(distancePercent)}"
        }

        return ConditionEvaluationResult(
            isTriggered = triggered,
            targetReferenceLabel = label,
            targetReferencePrice = maPrice,
            distancePercent = distancePercent,
            triggerTag = condition.shortTriggerTag,
            statusMessage = statusMessage,
            isInsufficientData = false,
        )
    }
}

/**
 * Extensible Strategy Registry & Dispatcher for all AlertCondition implementations.
 * New evaluators can be registered via register(...) without modifying callers.
 */
object ConditionEngine {
    private val evaluators = mutableMapOf<KClass<out AlertCondition>, ConditionEvaluator<out AlertCondition>>()

    init {
        register(MovingAverageEvaluator())
    }

    fun <T : AlertCondition> register(evaluator: ConditionEvaluator<T>) {
        evaluators[evaluator.conditionClass] = evaluator
    }

    @Suppress("UNCHECKED_CAST")
    fun evaluate(condition: AlertCondition, snapshot: MarketSnapshot): ConditionEvaluationResult {
        val evaluator = evaluators[condition::class] as? ConditionEvaluator<AlertCondition>
            ?: return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = condition.conditionDisplayName,
                targetReferencePrice = null,
                distancePercent = null,
                triggerTag = condition.conditionDisplayName,
                statusMessage = "尚未註冊此條件類型的評估器",
                isInsufficientData = true,
            )
        return evaluator.evaluate(condition, snapshot)
    }

    @Suppress("UNCHECKED_CAST")
    fun requiredPeriods(condition: AlertCondition): Set<Int> {
        val evaluator = evaluators[condition::class] as? ConditionEvaluator<AlertCondition>
            ?: return emptySet()
        return evaluator.requiredPeriods(condition)
    }
}
