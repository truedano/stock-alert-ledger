package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MarketCurrency

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ruleId: Long?,
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val triggerTag: String,
    val triggeredPrice: Double,
    val triggeredTimestamp: Long = System.currentTimeMillis(),
    val triggeredDateKey: String,
    val note: String = "",
)
