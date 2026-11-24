package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CandleStickData
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCondition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class AppDatabaseMigrationTest {

    private fun createV1Database(context: Context, dbName: String) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `watchlist_rules` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `symbol` TEXT NOT NULL,
                            `displaySymbol` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `currency` TEXT NOT NULL,
                            `condition` TEXT NOT NULL,
                            `plannedBuyAmount` REAL NOT NULL,
                            `isEnabled` INTEGER NOT NULL,
                            `isTriggered` INTEGER NOT NULL,
                            `triggeredDateKey` TEXT,
                            `lastTriggeredAt` INTEGER,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `cached_quotes` (
                            `symbol` TEXT NOT NULL,
                            `displaySymbol` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `currency` TEXT NOT NULL,
                            `currentPrice` REAL NOT NULL,
                            `previousClose` REAL,
                            `closeHistory` TEXT NOT NULL,
                            `lastUpdatedAt` INTEGER NOT NULL,
                            `isSimulated` INTEGER NOT NULL,
                            PRIMARY KEY(`symbol`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `buy_records` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `symbol` TEXT NOT NULL,
                            `displaySymbol` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `currency` TEXT NOT NULL,
                            `buyDate` TEXT NOT NULL,
                            `buyTimestamp` INTEGER NOT NULL,
                            `shares` Double NOT NULL,
                            `unitPrice` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `triggerTag` TEXT NOT NULL
                        )
                        """.trimIndent().replace("`shares` Double", "`shares` REAL")
                    )

                    // Insert sample v1 records using Room's Converters for valid condition JSON
                    val sampleConditionJson = Converters().fromAlertCondition(
                        MovingAverageCondition(periodDays = 20)
                    )
                    db.execSQL(
                        """
                        INSERT INTO `watchlist_rules`
                        (`id`, `symbol`, `displaySymbol`, `name`, `currency`, `condition`, `plannedBuyAmount`, `isEnabled`, `isTriggered`, `triggeredDateKey`, `lastTriggeredAt`, `createdAt`)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            1,
                            "2330.TW",
                            "2330.TW",
                            "台積電",
                            "TWD",
                            sampleConditionJson,
                            30000.0,
                            1,
                            0,
                            1700000000000L,
                        ),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `buy_records`
                        (`id`, `symbol`, `displaySymbol`, `name`, `currency`, `buyDate`, `buyTimestamp`, `shares`, `unitPrice`, `totalCost`, `triggerTag`)
                        VALUES (10, '2330.TW', '2330.TW', '台積電', 'TWD', '2026-11-20', 1700000000000, 30.0, 1000.0, 30000.0, '月線 20MA')
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
    }

    @Test
    fun `migrate 1 to 2 preserves existing rules and buy records and creates alert_logs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_1_2.db"
        context.deleteDatabase(dbName)

        createV1Database(context, dbName)

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val rules = migratedDb.watchlistDao().getAllRules()
            assertEquals(1, rules.size)
            assertEquals("2330.TW", rules[0].symbol)
            assertEquals(30000.0, rules[0].plannedBuyAmount, 0.001)

            val records = migratedDb.buyRecordDao().getAllRecords()
            assertEquals(1, records.size)
            assertEquals(10L, records[0].id)
            assertEquals(30.0, records[0].shares, 0.001)
            assertEquals("月線 20MA", records[0].triggerTag)

            // Verify new alert_logs table works post-migration
            val insertedLogId = migratedDb.alertLogDao().insertLog(
                AlertLogEntity(
                    ruleId = 1L,
                    symbol = "2330.TW",
                    displaySymbol = "2330.TW",
                    name = "台積電",
                    currency = MarketCurrency.TWD,
                    triggerTag = "月線 20MA",
                    triggeredPrice = 998.0,
                    triggeredTimestamp = 1700000500000L,
                    triggeredDateKey = "2026-11-22",
                    note = "測試紀錄",
                )
            )
            val logs = migratedDb.alertLogDao().getRecentLogs(10)
            assertEquals(1, logs.size)
            assertNotNull(insertedLogId)
            assertEquals(998.0, logs[0].triggeredPrice, 0.001)

            // Verify cached_quotes candleHistory column works post-migration 2->3
            val sampleCandles = listOf(
                CandleStickData(
                    timestamp = 1700000000L,
                    open = 990.0,
                    high = 1005.0,
                    low = 985.0,
                    close = 1000.0,
                    volume = 25000L,
                )
            )
            migratedDb.cachedQuoteDao().upsertQuote(
                CachedQuoteEntity(
                    symbol = "2330.TW",
                    displaySymbol = "2330.TW",
                    name = "台積電",
                    currency = MarketCurrency.TWD,
                    currentPrice = 1000.0,
                    previousClose = 990.0,
                    closeHistory = listOf(990.0, 1000.0),
                    lastUpdatedAt = 1700000500000L,
                    isSimulated = false,
                    candleHistory = sampleCandles,
                )
            )
            val cachedQuote = migratedDb.cachedQuoteDao().getQuoteBySymbol("2330.TW")
            assertNotNull(cachedQuote)
            assertEquals(1, cachedQuote!!.candleHistory.size)
            assertEquals(1005.0, cachedQuote.candleHistory[0].high, 0.001)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
