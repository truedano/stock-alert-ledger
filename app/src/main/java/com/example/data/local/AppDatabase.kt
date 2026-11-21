package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `alert_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ruleId` INTEGER,
                `symbol` TEXT NOT NULL,
                `displaySymbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `triggerTag` TEXT NOT NULL,
                `triggeredPrice` REAL NOT NULL,
                `triggeredTimestamp` INTEGER NOT NULL,
                `triggeredDateKey` TEXT NOT NULL,
                `note` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        WatchlistRuleEntity::class,
        CachedQuoteEntity::class,
        BuyRecordEntity::class,
        AlertLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun cachedQuoteDao(): CachedQuoteDao
    abstract fun buyRecordDao(): BuyRecordDao
    abstract fun alertLogDao(): AlertLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_ma_assistant.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
