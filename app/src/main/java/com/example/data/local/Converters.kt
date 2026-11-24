package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.AlertCondition
import com.example.domain.model.CandleStickData
import com.example.domain.model.MarketCurrency
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object RoomJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
}

class Converters {
    @TypeConverter
    fun fromAlertCondition(condition: AlertCondition): String {
        return RoomJson.json.encodeToString(AlertCondition.serializer(), condition)
    }

    @TypeConverter
    fun toAlertCondition(raw: String): AlertCondition {
        return RoomJson.json.decodeFromString(AlertCondition.serializer(), raw)
    }

    @TypeConverter
    fun fromDoubleList(values: List<Double>): String {
        return RoomJson.json.encodeToString(ListSerializer(Double.serializer()), values)
    }

    @TypeConverter
    fun toDoubleList(raw: String): List<Double> {
        if (raw.isBlank()) return emptyList()
        return RoomJson.json.decodeFromString(ListSerializer(Double.serializer()), raw)
    }

    @TypeConverter
    fun fromCandleList(values: List<CandleStickData>): String {
        return RoomJson.json.encodeToString(ListSerializer(CandleStickData.serializer()), values)
    }

    @TypeConverter
    fun toCandleList(raw: String): List<CandleStickData> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            RoomJson.json.decodeFromString(ListSerializer(CandleStickData.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromMarketCurrency(currency: MarketCurrency): String = currency.name

    @TypeConverter
    fun toMarketCurrency(raw: String): MarketCurrency {
        return MarketCurrency.fromCurrencyCode(raw)
    }
}
