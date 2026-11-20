package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import kotlinx.serialization.Serializable

@Serializable
data class FearAndGreedData(
    val score: Double,
    val rating: String,
    val timestamp: String = "",
    val previousClose: Double? = null,
    val previousOneWeek: Double? = null,
    val previousOneMonth: Double? = null,
    val previousOneYear: Double? = null,
) {
    val roundedScore: Int
        get() = score.toInt().coerceIn(0, 100)

    val ratingClassification: SentimentClassification
        get() = when {
            rating.contains("extreme fear", ignoreCase = true) || score < 25.0 -> SentimentClassification.EXTREME_FEAR
            rating.contains("extreme greed", ignoreCase = true) || score >= 75.0 -> SentimentClassification.EXTREME_GREED
            rating.contains("fear", ignoreCase = true) || score < 45.0 -> SentimentClassification.FEAR
            rating.contains("greed", ignoreCase = true) || score >= 55.0 -> SentimentClassification.GREED
            else -> SentimentClassification.NEUTRAL
        }

    val localizedRating: String
        get() = ratingClassification.displayNameZh
}

enum class SentimentClassification(
    val displayNameZh: String,
    val displayNameEn: String,
) {
    EXTREME_FEAR("極度恐慌", "Extreme Fear"),
    FEAR("恐慌", "Fear"),
    NEUTRAL("中立", "Neutral"),
    GREED("貪婪", "Greed"),
    EXTREME_GREED("極度貪婪", "Extreme Greed");

    val primaryColor: Color
        get() = when (this) {
            EXTREME_FEAR -> TaiwanRed
            FEAR -> AlertAmber
            NEUTRAL -> Color(0xFF94A3B8) // Slate Gray
            GREED -> Color(0xFF34D399) // Soft Green
            EXTREME_GREED -> TaiwanGreen
        }

    val containerColor: Color
        get() = when (this) {
            EXTREME_FEAR -> TaiwanRedContainer
            FEAR -> AlertAmberContainer
            NEUTRAL -> Color(0xFF1E293B)
            GREED -> Color(0xFF0F382E)
            EXTREME_GREED -> TaiwanGreenContainer
        }
}
