package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlertCondition
import com.example.domain.model.MarketCurrency

@Entity(tableName = "watchlist_rules")
data class WatchlistRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val condition: AlertCondition,
    val plannedBuyAmount: Double,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredDateKey: String? = null,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
