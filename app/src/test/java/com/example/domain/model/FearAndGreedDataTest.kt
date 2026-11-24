package com.example.domain.model

import com.example.data.remote.FearAndGreedService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FearAndGreedDataTest {

    @Test
    fun testClassification_extremeFear() {
        val data = FearAndGreedData(score = 9.6, rating = "extreme fear")
        assertEquals(SentimentClassification.EXTREME_FEAR, data.ratingClassification)
        assertEquals("極度恐慌", data.localizedRating)
        assertEquals(9, data.roundedScore)
    }

    @Test
    fun testClassification_fear() {
        val data = FearAndGreedData(score = 35.0, rating = "fear")
        assertEquals(SentimentClassification.FEAR, data.ratingClassification)
        assertEquals("恐慌", data.localizedRating)
    }

    @Test
    fun testClassification_neutral() {
        val data = FearAndGreedData(score = 50.0, rating = "neutral")
        assertEquals(SentimentClassification.NEUTRAL, data.ratingClassification)
        assertEquals("中立", data.localizedRating)
    }

    @Test
    fun testClassification_greed() {
        val data = FearAndGreedData(score = 65.0, rating = "greed")
        assertEquals(SentimentClassification.GREED, data.ratingClassification)
        assertEquals("貪婪", data.localizedRating)
    }

    @Test
    fun testClassification_extremeGreed() {
        val data = FearAndGreedData(score = 88.0, rating = "extreme greed")
        assertEquals(SentimentClassification.EXTREME_GREED, data.ratingClassification)
        assertEquals("極度貪婪", data.localizedRating)
    }

    @Test
    fun `parses CNN graphdata JSON with historical spans and 7 sub-indicators`() {
        val sampleJson = """
            {
              "fear_and_greed": {
                "score": 41.8,
                "rating": "fear",
                "timestamp": "2026-11-23T21:00:00+00:00",
                "previous_close": 44.2,
                "previous_1_week": 52.0,
                "previous_1_month": 66.5,
                "previous_1_year": 77.0
              },
              "market_momentum_sp500": {"score": 48.5, "rating": "neutral"},
              "stock_price_strength": {"score": 32.0, "rating": "fear"},
              "stock_price_breadth": {"score": 21.4, "rating": "extreme fear"},
              "put_call_options": {"score": 38.0, "rating": "fear"},
              "market_volatility_vix": {"score": 50.0, "rating": "neutral"},
              "safe_haven_demand": {"score": 29.0, "rating": "fear"},
              "junk_bond_demand": {"score": 76.0, "rating": "extreme greed"}
            }
        """.trimIndent()

        val parsed = FearAndGreedService().parseFearAndGreedJson(sampleJson)
        assertNotNull(parsed)
        parsed!!
        assertEquals(41, parsed.roundedScore)
        assertEquals(SentimentClassification.FEAR, parsed.ratingClassification)
        assertEquals(44.2, parsed.previousClose!!, 0.01)
        assertEquals(52.0, parsed.previousOneWeek!!, 0.01)
        assertEquals(66.5, parsed.previousOneMonth!!, 0.01)
        assertEquals(77.0, parsed.previousOneYear!!, 0.01)

        assertEquals(7, parsed.subIndicators.size)
        assertEquals("市場動能", parsed.subIndicators[0].nameZh)
        assertEquals(48, parsed.subIndicators[0].roundedScore)
        assertEquals(SentimentClassification.EXTREME_FEAR, parsed.subIndicators[2].classification)
        assertEquals("垃圾債需求", parsed.subIndicators[6].nameZh)
        assertEquals(SentimentClassification.EXTREME_GREED, parsed.subIndicators[6].classification)
    }
}
