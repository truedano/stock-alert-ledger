package com.example.domain.model

import kotlin.math.abs
import kotlin.reflect.KClass

data class MarketSnapshot(
    val symbol: String,
    val currentPrice: Double,
    val historicalCloses: List<Double>,
    val movingAverages: Map<Int, Double> = emptyMap(),
    val marketKeepRate: Double? = null,
    val recentMarketKeepRates: List<Double> = emptyList(),
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

    /**
     * 計算指定天數內最高收盤價（波段高點）
     */
    fun calculatePeriodHigh(closes: List<Double>, lookbackDays: Int): Double? {
        if (lookbackDays <= 0 || closes.isEmpty()) return null
        val slice = closes.takeLast(lookbackDays)
        if (slice.isEmpty()) return null
        val maxVal = slice.maxOrNull() ?: return null
        return if (maxVal.isNaN() || maxVal.isInfinite() || maxVal <= 0.0) null else maxVal
    }

    /**
     * 計算標準 RSI（相對強弱指標，Wilder 平滑法）
     */
    fun calculateRsi(closes: List<Double>, periodDays: Int = 14): Double? {
        if (periodDays <= 0 || closes.size <= periodDays) return null

        val deltas = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            deltas.add(closes[i] - closes[i - 1])
        }

        if (deltas.size < periodDays) return null

        // 初始第一組平均收益與損失
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 0 until periodDays) {
            val d = deltas[i]
            if (d > 0) avgGain += d else avgLoss += -d
        }
        avgGain /= periodDays
        avgLoss /= periodDays

        // Wilder 平滑遞推
        for (i in periodDays until deltas.size) {
            val d = deltas[i]
            val gain = if (d > 0) d else 0.0
            val loss = if (d < 0) -d else 0.0

            avgGain = (avgGain * (periodDays - 1) + gain) / periodDays
            avgLoss = (avgLoss * (periodDays - 1) + loss) / periodDays
        }

        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }
        val rs = avgGain / avgLoss
        val rsi = 100.0 - (100.0 / (1.0 + rs))
        return if (rsi.isNaN() || rsi.isInfinite()) null else rsi
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
 * 波段高點回撤抄底評估器
 */
class PeakDrawdownEvaluator : ConditionEvaluator<PeakDrawdownCondition> {
    override val conditionClass: KClass<PeakDrawdownCondition> = PeakDrawdownCondition::class

    override fun evaluate(
        condition: PeakDrawdownCondition,
        snapshot: MarketSnapshot,
    ): ConditionEvaluationResult {
        val peakPrice = MovingAverageCalculator.calculatePeriodHigh(snapshot.historicalCloses, condition.lookbackDays)
        val label = "${condition.lookbackDays}日高點"

        if (peakPrice == null || peakPrice <= 0.0) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = null,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "歷史資料不足計算 ${condition.lookbackDays} 日高點",
                isInsufficientData = true,
            )
        }

        val currentPrice = snapshot.currentPrice
        if (currentPrice <= 0.0) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = peakPrice,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "尚無有效現價",
                isInsufficientData = true,
            )
        }

        // 回撤百分比：例如高點 100 現價 80，drawdown = ((80 - 100) / 100) * 100 = -20%
        val drawdownFromPeak = ((currentPrice - peakPrice) / peakPrice) * 100.0
        // 目標價格（例如跌幅 20% 對應價格 = peak * 0.8）
        val targetTriggerPrice = peakPrice * (1.0 - (condition.drawdownPercent / 100.0))
        val triggered = drawdownFromPeak <= -condition.drawdownPercent

        val statusMessage = if (triggered) {
            "自${label} (${Formatters.formatPrice(peakPrice, MarketCurrency.TWD)}) 回撤達 ${String.format(java.util.Locale.US, "%.1f", drawdownFromPeak)}% (觸發抄底)"
        } else {
            "距${label}回撤 ${String.format(java.util.Locale.US, "%.1f", drawdownFromPeak)}%（目標 -${condition.drawdownPercent.toInt()}%）"
        }

        return ConditionEvaluationResult(
            isTriggered = triggered,
            targetReferenceLabel = "${label} -${condition.drawdownPercent.toInt()}%",
            targetReferencePrice = targetTriggerPrice,
            distancePercent = drawdownFromPeak,
            triggerTag = condition.shortTriggerTag,
            statusMessage = statusMessage,
            isInsufficientData = false,
        )
    }
}

/**
 * RSI 相對強弱指標評估器
 */
class RsiEvaluator : ConditionEvaluator<RsiCondition> {
    override val conditionClass: KClass<RsiCondition> = RsiCondition::class

    override fun evaluate(
        condition: RsiCondition,
        snapshot: MarketSnapshot,
    ): ConditionEvaluationResult {
        val rsi = MovingAverageCalculator.calculateRsi(snapshot.historicalCloses, condition.periodDays)
        val label = "RSI(${condition.periodDays})"

        if (rsi == null) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = null,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "歷史收盤價不足計算 RSI(${condition.periodDays})",
                isInsufficientData = true,
            )
        }

        val triggered = if (condition.isAbove) rsi >= condition.threshold else rsi <= condition.threshold
        val diff = rsi - condition.threshold
        val statusMessage = if (triggered) {
            if (condition.isAbove) "目前 $label 為 ${String.format(java.util.Locale.US, "%.1f", rsi)}，已突破 ${condition.threshold.toInt()}（超買反彈調節區）"
            else "目前 $label 為 ${String.format(java.util.Locale.US, "%.1f", rsi)}，已跌破 ${condition.threshold.toInt()}（超賣恐慌區）"
        } else {
            "目前 $label 為 ${String.format(java.util.Locale.US, "%.1f", rsi)}（閾值 ${condition.threshold.toInt()}）"
        }

        val targetPrice = if (condition.isAbove) null else null // RSI 本身非價格指標
        val rsiFormatted = String.format(java.util.Locale.US, "%.1f", rsi)

        return ConditionEvaluationResult(
            isTriggered = triggered,
            targetReferenceLabel = "$label 現值",
            targetReferencePrice = rsi, // 將目前計算出的 RSI 數值傳給 UI 顯示
            distancePercent = diff,
            triggerTag = condition.shortTriggerTag,
            statusMessage = statusMessage,
            isInsufficientData = false,
        )
    }
}

/**
 * 融資維持率斷頭與反彈抄底評估器
 */
class MarginMaintenanceEvaluator : ConditionEvaluator<MarginMaintenanceCondition> {
    override val conditionClass: KClass<MarginMaintenanceCondition> = MarginMaintenanceCondition::class

    override fun evaluate(
        condition: MarginMaintenanceCondition,
        snapshot: MarketSnapshot,
    ): ConditionEvaluationResult {
        val keepRate = snapshot.marketKeepRate
        val label = "全市場融資維持率"

        if (keepRate == null || keepRate <= 0.0) {
            return ConditionEvaluationResult(
                isTriggered = false,
                targetReferenceLabel = label,
                targetReferencePrice = null,
                distancePercent = null,
                triggerTag = condition.shortTriggerTag,
                statusMessage = "尚未取得證交所最新全市場維持率資料",
                isInsufficientData = true,
            )
        }

        if (condition.isExitMode) {
            val triggered = keepRate >= condition.exitThreshold
            val diff = keepRate - condition.exitThreshold
            val statusMessage = if (triggered) {
                "目前維持率 ${String.format(java.util.Locale.US, "%.1f", keepRate)}%，已回升至 ${condition.exitThreshold.toInt()}% 安全水位（獲利退回安全停泊）"
            } else {
                "目前維持率 ${String.format(java.util.Locale.US, "%.1f", keepRate)}%（目標回升至 ${condition.exitThreshold.toInt()}%）"
            }
            return ConditionEvaluationResult(
                isTriggered = triggered,
                targetReferenceLabel = "$label (目標>${condition.exitThreshold.toInt()}%)",
                targetReferencePrice = keepRate,
                distancePercent = diff,
                triggerTag = condition.shortTriggerTag,
                statusMessage = statusMessage,
                isInsufficientData = false,
            )
        } else {
            // 抄底條件：波段曾跌破 panicThreshold，且從低點反彈 >= reboundPoints
            val rates = snapshot.recentMarketKeepRates.ifEmpty { listOf(keepRate) }
            val minRate = rates.minOrNull() ?: keepRate
            val reboundFromMin = keepRate - minRate
            val hasDroppedBelow = minRate <= condition.panicThreshold
            val triggered = hasDroppedBelow && reboundFromMin >= condition.reboundPoints

            val statusMessage = when {
                triggered -> "融資維持率曾跌至 ${String.format(java.util.Locale.US, "%.1f", minRate)}%（破${condition.panicThreshold.toInt()}%），現已反彈 +${String.format(java.util.Locale.US, "%.1f", reboundFromMin)} 點至 ${String.format(java.util.Locale.US, "%.1f", keepRate)}%（觸發黃金抄底）"
                hasDroppedBelow -> "維持率低點 ${String.format(java.util.Locale.US, "%.1f", minRate)}%，目前 ${String.format(java.util.Locale.US, "%.1f", keepRate)}%（反彈 +${String.format(java.util.Locale.US, "%.1f", reboundFromMin)} 點，等待 +${condition.reboundPoints.toInt()} 點確認）"
                else -> "目前維持率 ${String.format(java.util.Locale.US, "%.1f", keepRate)}%（警戒門檻 ${condition.panicThreshold.toInt()}%）"
            }

            return ConditionEvaluationResult(
                isTriggered = triggered,
                targetReferenceLabel = "$label (破${condition.panicThreshold.toInt()}%彈+${condition.reboundPoints.toInt()})",
                targetReferencePrice = keepRate,
                distancePercent = reboundFromMin,
                triggerTag = condition.shortTriggerTag,
                statusMessage = statusMessage,
                isInsufficientData = false,
            )
        }
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
        register(PeakDrawdownEvaluator())
        register(RsiEvaluator())
        register(MarginMaintenanceEvaluator())
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
