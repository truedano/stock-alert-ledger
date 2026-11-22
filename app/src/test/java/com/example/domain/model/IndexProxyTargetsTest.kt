package com.example.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexProxyTargetsTest {

    @Test
    fun `detects market index symbols accurately`() {
        assertTrue(isMarketIndexSymbol("^TWII"))
        assertTrue(isMarketIndexSymbol(" ^GSPC "))
        assertFalse(isMarketIndexSymbol("0050.TW"))
        assertFalse(isMarketIndexSymbol("2330.TW"))
        assertFalse(isMarketIndexSymbol("AAPL"))
    }

    @Test
    fun `recommends tradable ETFs for TWII and US indices`() {
        val twiiTargets = recommendedProxyTargetsForIndex("^TWII").map { it.symbol }
        assertTrue("0050.TW" in twiiTargets)
        assertTrue("00631L.TW" in twiiTargets)
        assertTrue(twiiTargets.none { isMarketIndexSymbol(it) })

        val gspcTargets = recommendedProxyTargetsForIndex("^GSPC").map { it.symbol }
        assertTrue("VOO" in gspcTargets)
        assertTrue(gspcTargets.none { isMarketIndexSymbol(it) })
    }
}
