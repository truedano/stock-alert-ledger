package com.example.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(
    val chart: YahooChartContainer? = null,
)

@Serializable
data class YahooChartContainer(
    val result: List<YahooChartResult>? = null,
    val error: YahooChartError? = null,
)

@Serializable
data class YahooChartError(
    val code: String? = null,
    val description: String? = null,
)

@Serializable
data class YahooChartResult(
    val meta: YahooChartMeta? = null,
    val indicators: YahooIndicators? = null,
)

@Serializable
data class YahooChartMeta(
    val symbol: String? = null,
    val currency: String? = null,
    val shortName: String? = null,
    val longName: String? = null,
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
    val previousClose: Double? = null,
)

@Serializable
data class YahooIndicators(
    val quote: List<YahooQuoteSeries>? = null,
)

@Serializable
data class YahooQuoteSeries(
    val close: List<Double?>? = null,
)
