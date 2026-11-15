package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MarketCurrency

@Entity(tableName = "buy_records")
data class BuyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
