package com.example.data.backup

import com.example.domain.model.AlertCondition
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.MovingAverageTriggerMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "type"
    }

    @Test
    fun `test serialization and deserialization of AppBackupData`() {
        val sampleRule = WatchlistRuleBackupDto(
            symbol = "2330.TW",
            displaySymbol = "2330",
            name = "台積電",
            currency = MarketCurrency.TWD,
            condition = MovingAverageCondition(
                periodDays = 20,
                triggerMode = MovingAverageTriggerMode.BELOW_OR_EQUAL
            ),
            plannedBuyAmount = 50000.0,
            isEnabled = true,
            isTriggered = false
        )

        val sampleRecord = BuyRecordBackupDto(
            symbol = "2330.TW",
            displaySymbol = "2330",
            name = "台積電",
            currency = MarketCurrency.TWD,
            buyDate = "2026/11/15",
            buyTimestamp = 1731672000000L,
            shares = 100.0,
            unitPrice = 1000.0,
            totalCost = 100000.0,
            triggerTag = "月線 20MA"
        )

        val backupData = AppBackupData(
            version = 1,
            exportedAt = 1731672000000L,
            appVersionName = "1.0.0",
            watchlistRules = listOf(sampleRule),
            buyRecords = listOf(sampleRecord),
            preferences = PreferencesBackupDto(
                backgroundCheckEnabled = true
            )
        )

        val encoded = json.encodeToString(AppBackupData.serializer(), backupData)
        assertTrue(encoded.contains("2330.TW"))
        assertTrue(encoded.contains("moving_average"))

        val decoded = json.decodeFromString(AppBackupData.serializer(), encoded)
        assertEquals(1, decoded.version)
        assertEquals(1, decoded.watchlistRules.size)
        assertEquals(1, decoded.buyRecords.size)

        val decodedRule = decoded.watchlistRules[0]
        assertEquals("2330.TW", decodedRule.symbol)
        assertTrue(decodedRule.condition is MovingAverageCondition)
        assertEquals(20, (decodedRule.condition as MovingAverageCondition).periodDays)

        val decodedRecord = decoded.buyRecords[0]
        assertEquals(100.0, decodedRecord.shares, 0.001)
        assertEquals(100000.0, decodedRecord.totalCost, 0.001)
    }

    @Test
    fun `test forward compatibility when old backup lacks optional fields or has new fields`() {
        // 模擬最精簡的舊版備份檔 JSON（甚至可能沒有 preferences 或未來的欄位）
        val legacyJson = """
            {
                "version": 1,
                "exportedAt": 1731672000000,
                "appVersionName": "1.0.0",
                "watchlistRules": [
                    {
                        "symbol": "AAPL",
                        "displaySymbol": "AAPL",
                        "name": "Apple Inc.",
                        "currency": "USD",
                        "condition": {
                            "type": "moving_average",
                            "periodDays": 60,
                            "triggerMode": "below_or_equal",
                            "tolerancePercent": 1.0
                        },
                        "plannedBuyAmount": 1000.0
                    }
                ],
                "buyRecords": [
                    {
                        "symbol": "AAPL",
                        "displaySymbol": "AAPL",
                        "name": "Apple Inc.",
                        "currency": "USD",
                        "buyDate": "2026/11/15",
                        "buyTimestamp": 1731672000000,
                        "shares": 5.0,
                        "unitPrice": 200.0,
                        "totalCost": 1000.0,
                        "triggerTag": "季線 60MA"
                    }
                ],
                "extraFutureField": "should be safely ignored"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(AppBackupData.serializer(), legacyJson)
        assertEquals(1, decoded.version)
        assertEquals(1, decoded.watchlistRules.size)
        val rule = decoded.watchlistRules[0]
        assertEquals("AAPL", rule.symbol)
        assertTrue(rule.isEnabled) // 驗證預設值作用正常
        assertEquals(null, decoded.preferences) // 驗證可為空
    }
}
