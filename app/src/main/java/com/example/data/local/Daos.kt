package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_rules ORDER BY isTriggered DESC, createdAt DESC")
    fun observeAllRules(): Flow<List<WatchlistRuleEntity>>

    @Query("SELECT * FROM watchlist_rules WHERE isEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledRules(): List<WatchlistRuleEntity>

    @Query("SELECT * FROM watchlist_rules")
    suspend fun getAllRules(): List<WatchlistRuleEntity>

    @Query("SELECT * FROM watchlist_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): WatchlistRuleEntity?

    @Query("SELECT * FROM watchlist_rules WHERE symbol = :symbol")
    suspend fun getRulesBySymbol(symbol: String): List<WatchlistRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: WatchlistRuleEntity): Long

    @Update
    suspend fun updateRule(rule: WatchlistRuleEntity)

    @Query("DELETE FROM watchlist_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM watchlist_rules")
    suspend fun clearAllRules()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<WatchlistRuleEntity>)

    @Query("UPDATE watchlist_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query(
        "UPDATE watchlist_rules SET isTriggered = 1, triggeredDateKey = :dateKey, lastTriggeredAt = :timestamp WHERE id = :id"
    )
    suspend fun markTriggered(id: Long, dateKey: String, timestamp: Long)

    @Query(
        "UPDATE watchlist_rules SET isTriggered = 0, triggeredDateKey = NULL, lastTriggeredAt = NULL WHERE id = :id"
    )
    suspend fun resetTrigger(id: Long)
}

@Dao
interface CachedQuoteDao {
    @Query("SELECT * FROM cached_quotes")
    fun observeAllQuotes(): Flow<List<CachedQuoteEntity>>

    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol LIMIT 1")
    suspend fun getQuoteBySymbol(symbol: String): CachedQuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(quote: CachedQuoteEntity)
}

@Dao
interface BuyRecordDao {
    @Query("SELECT * FROM buy_records ORDER BY buyDate DESC, buyTimestamp DESC, id DESC")
    fun observeAllRecords(): Flow<List<BuyRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BuyRecordEntity): Long

    @Update
    suspend fun updateRecord(record: BuyRecordEntity)

    @Query("DELETE FROM buy_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM buy_records")
    suspend fun getAllRecords(): List<BuyRecordEntity>

    @Query("DELETE FROM buy_records")
    suspend fun clearAllRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BuyRecordEntity>)
}

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY triggeredTimestamp DESC, id DESC")
    fun observeAllLogs(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlertLogEntity): Long

    @Query("SELECT * FROM alert_logs ORDER BY triggeredTimestamp DESC, id DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<AlertLogEntity>

    @Query("DELETE FROM alert_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllLogs()
}

