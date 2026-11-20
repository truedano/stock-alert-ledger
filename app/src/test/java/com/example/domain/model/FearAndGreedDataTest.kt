package com.example.domain.model

import org.junit.Assert.assertEquals
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
}
