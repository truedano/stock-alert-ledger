package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyConditionEvaluationTest {

    @Test
    fun `test peak drawdown evaluator triggers when drop exceeds threshold`() {
        val historicalCloses = listOf(100.0, 110.0, 120.0, 105.0, 95.0) // 高點為 120.0
        // 現價 96.0，自 120.0 回撤：(96 - 120) / 120 = -20.0%
        val snapshot = MarketSnapshot(
            symbol = "0050.TW",
            currentPrice = 96.0,
            historicalCloses = historicalCloses,
        )

        val condition20 = PeakDrawdownCondition(lookbackDays = 10, drawdownPercent = 20.0)
        val result20 = ConditionEngine.evaluate(condition20, snapshot)

        assertTrue(result20.isTriggered)
        assertEquals(-20.0, result20.distancePercent ?: 0.0, 0.001)
        assertEquals(96.0, result20.targetReferencePrice ?: 0.0, 0.001)
        assertEquals("波段高點回撤 -20%", result20.triggerTag)

        // 尚未達到 -25% 回撤
        val condition25 = PeakDrawdownCondition(lookbackDays = 10, drawdownPercent = 25.0)
        val result25 = ConditionEngine.evaluate(condition25, snapshot)
        assertFalse(result25.isTriggered)
        assertEquals(90.0, result25.targetReferencePrice ?: 0.0, 0.001)
    }

    @Test
    fun `test rsi evaluator triggers on overbought and oversold`() {
        // 連續上漲以產生高 RSI
        val upCloses = listOf(
            10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0,
            20.0, 21.0, 22.0, 23.0, 24.0, 25.0
        )
        val snapshotUp = MarketSnapshot(
            symbol = "00631L.TW",
            currentPrice = 25.0,
            historicalCloses = upCloses,
        )

        val rsiAbove70 = RsiCondition(periodDays = 14, threshold = 70.0, isAbove = true)
        val resultRsi = ConditionEngine.evaluate(rsiAbove70, snapshotUp)

        assertTrue(resultRsi.isTriggered)
        assertTrue(resultRsi.statusMessage.contains("超買反彈調節區"))
        assertEquals("RSI>70", resultRsi.triggerTag)

        // 測試超賣未觸發
        val rsiBelow30 = RsiCondition(periodDays = 14, threshold = 30.0, isAbove = false)
        val resultOversold = ConditionEngine.evaluate(rsiBelow30, snapshotUp)
        assertFalse(resultOversold.isTriggered)
    }

    @Test
    fun `test margin maintenance evaluator triggers on panic drop and rebound`() {
        // 曾跌到 138.0 (破 150%)，現價反彈至 158.5 (反彈 20.5 點 >= 20 點)
        val snapshotRebound = MarketSnapshot(
            symbol = "0050.TW",
            currentPrice = 85.0,
            historicalCloses = listOf(80.0, 85.0),
            marketKeepRate = 158.5,
            recentMarketKeepRates = listOf(160.0, 148.0, 138.0, 145.0, 158.5),
        )

        val marginCond = MarginMaintenanceCondition(
            panicThreshold = 150.0,
            reboundPoints = 20.0,
            isExitMode = false,
        )
        val result = ConditionEngine.evaluate(marginCond, snapshotRebound)

        assertTrue(result.isTriggered)
        assertEquals(20.5, result.distancePercent ?: 0.0, 0.001)
        assertTrue(result.statusMessage.contains("觸發黃金抄底"))

        // 測試出場模式：回升至 170% 以上
        val exitCond = MarginMaintenanceCondition(isExitMode = true, exitThreshold = 170.0)
        val resultNotExit = ConditionEngine.evaluate(exitCond, snapshotRebound)
        assertFalse(resultNotExit.isTriggered) // 158.5 < 170.0

        val snapshotHigh = snapshotRebound.copy(marketKeepRate = 172.0)
        val resultExit = ConditionEngine.evaluate(exitCond, snapshotHigh)
        assertTrue(resultExit.isTriggered)
        assertTrue(resultExit.statusMessage.contains("獲利退回安全停泊"))
    }

    @Test
    fun `test insufficient data handling`() {
        val emptySnapshot = MarketSnapshot(
            symbol = "0050.TW",
            currentPrice = 100.0,
            historicalCloses = emptyList(),
        )

        val drawdownResult = ConditionEngine.evaluate(PeakDrawdownCondition(), emptySnapshot)
        assertFalse(drawdownResult.isTriggered)
        assertTrue(drawdownResult.isInsufficientData)

        val rsiResult = ConditionEngine.evaluate(RsiCondition(), emptySnapshot)
        assertFalse(rsiResult.isTriggered)
        assertTrue(rsiResult.isInsufficientData)
    }

    @Test
    fun `test PriceTargetCondition below or equal`() {
        val targetCond = PriceTargetCondition(
            targetPrice = 1000.0,
            direction = PriceTargetDirection.BELOW_OR_EQUAL,
            tolerancePercent = 0.0,
            autoDisableOnTrigger = true,
        )

        val snapshotAbove = MarketSnapshot(
            symbol = "2330.TW",
            currentPrice = 1050.0,
            historicalCloses = listOf(1050.0),
        )
        val evalAbove = ConditionEngine.evaluate(targetCond, snapshotAbove)
        assertFalse(evalAbove.isTriggered)
        assertTrue(evalAbove.statusMessage.contains("高於目標價"))

        val snapshotBelow = MarketSnapshot(
            symbol = "2330.TW",
            currentPrice = 995.0,
            historicalCloses = listOf(995.0),
        )
        val evalBelow = ConditionEngine.evaluate(targetCond, snapshotBelow)
        assertTrue(evalBelow.isTriggered)
        assertTrue(evalBelow.statusMessage.contains("已跌破或觸及目標價"))
    }

    @Test
    fun `test PriceTargetCondition above or equal and tolerance`() {
        val targetCond = PriceTargetCondition(
            targetPrice = 200.0,
            direction = PriceTargetDirection.ABOVE_OR_EQUAL,
            tolerancePercent = 1.0, // ±1% = 198.0 ~ 202.0
            autoDisableOnTrigger = true,
        )

        // 195.0 -> 未進入 ±1%
        val snapshotFar = MarketSnapshot(
            symbol = "AAPL",
            currentPrice = 195.0,
            historicalCloses = listOf(195.0),
        )
        val evalFar = ConditionEngine.evaluate(targetCond, snapshotFar)
        assertFalse(evalFar.isTriggered)

        // 198.5 -> 在 -0.75% 內，接近目標價觸發預警
        val snapshotNear = MarketSnapshot(
            symbol = "AAPL",
            currentPrice = 198.5,
            historicalCloses = listOf(198.5),
        )
        val evalNear = ConditionEngine.evaluate(targetCond, snapshotNear)
        assertTrue(evalNear.isTriggered)
        assertTrue(evalNear.statusMessage.contains("已接近目標價"))

        // 205.0 -> 突破目標價觸發
        val snapshotBreak = MarketSnapshot(
            symbol = "AAPL",
            currentPrice = 205.0,
            historicalCloses = listOf(205.0),
        )
        val evalBreak = ConditionEngine.evaluate(targetCond, snapshotBreak)
        assertTrue(evalBreak.isTriggered)
        assertTrue(evalBreak.statusMessage.contains("已突破或觸及目標價"))
    }
}
