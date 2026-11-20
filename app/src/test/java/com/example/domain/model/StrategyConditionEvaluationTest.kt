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
}
