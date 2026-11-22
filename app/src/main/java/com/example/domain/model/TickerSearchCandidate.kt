package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TickerSearchCandidate(
    val symbol: String,
    val name: String,
    val currency: MarketCurrency,
    val marketLabel: String,
)
