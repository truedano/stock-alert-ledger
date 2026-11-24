package com.example.data.backup

import com.example.domain.model.AlertCondition
import com.example.domain.model.DataSourceMode
import com.example.domain.model.MarketCurrency
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistRuleBackupDto(
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

@Serializable
data class BuyRecordBackupDto(
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val buyDate: String,
    val buyTimestamp: Long,
    val shares: Double,
    val unitPrice: Double,
    val totalCost: Double,
    val triggerTag: String,
)

@Serializable
data class PreferencesBackupDto(
    val dataSourceMode: DataSourceMode = DataSourceMode.REAL,
    val backgroundCheckEnabled: Boolean = true,
    val highFrequencyCheckEnabled: Boolean = false,
    val highFrequencyIntervalSeconds: Int = 60,
)

@Serializable
data class AppBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersionName: String = "",
    val watchlistRules: List<WatchlistRuleBackupDto> = emptyList(),
    val buyRecords: List<BuyRecordBackupDto> = emptyList(),
    val preferences: PreferencesBackupDto? = null,
)
