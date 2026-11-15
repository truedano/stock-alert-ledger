package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.BuyRecordEntity
import com.example.data.local.WatchlistRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

sealed interface BackupRestoreResult {
    data class Success(val rulesCount: Int, val recordsCount: Int) : BackupRestoreResult
    data class Error(val message: String) : BackupRestoreResult
}

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: AppPreferencesRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "type"
    }

    suspend fun createBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rules = database.watchlistDao().getAllRules().map { rule ->
                WatchlistRuleBackupDto(
                    symbol = rule.symbol,
                    displaySymbol = rule.displaySymbol,
                    name = rule.name,
                    currency = rule.currency,
                    condition = rule.condition,
                    plannedBuyAmount = rule.plannedBuyAmount,
                    isEnabled = rule.isEnabled,
                    isTriggered = rule.isTriggered,
                    triggeredDateKey = rule.triggeredDateKey,
                    lastTriggeredAt = rule.lastTriggeredAt,
                    createdAt = rule.createdAt,
                )
            }

            val records = database.buyRecordDao().getAllRecords().map { rec ->
                BuyRecordBackupDto(
                    symbol = rec.symbol,
                    displaySymbol = rec.displaySymbol,
                    name = rec.name,
                    currency = rec.currency,
                    buyDate = rec.buyDate,
                    buyTimestamp = rec.buyTimestamp,
                    shares = rec.shares,
                    unitPrice = rec.unitPrice,
                    totalCost = rec.totalCost,
                    triggerTag = rec.triggerTag,
                )
            }

            val currentPrefs = preferencesRepository.getSnapshot()
            val backupDto = AppBackupData(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                appVersionName = BuildConfig.VERSION_NAME,
                watchlistRules = rules,
                buyRecords = records,
                preferences = PreferencesBackupDto(
                    dataSourceMode = currentPrefs.dataSourceMode,
                    backgroundCheckEnabled = currentPrefs.backgroundCheckEnabled,
                ),
            )

            json.encodeToString(AppBackupData.serializer(), backupDto)
        }
    }

    suspend fun exportToUri(targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = createBackupJson().getOrThrow()
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            } ?: throw IllegalStateException("無法開啟輸出檔案串流")
        }
    }

    suspend fun createShareableBackupUri(filename: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = createBackupJson().getOrThrow()
            val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val backupFile = File(backupDir, filename)
            FileOutputStream(backupFile).use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
        }
    }

    suspend fun importFromUri(sourceUri: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext BackupRestoreResult.Error("無法讀取所選檔案")

            restoreFromJson(content)
        } catch (e: Exception) {
            BackupRestoreResult.Error("匯入過程發生錯誤: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun restoreFromJson(jsonContent: String): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val backupData = json.decodeFromString(AppBackupData.serializer(), jsonContent)

            val newRules = backupData.watchlistRules.map { dto ->
                WatchlistRuleEntity(
                    id = 0L, // 產生新主鍵，避免衝突
                    symbol = dto.symbol,
                    displaySymbol = dto.displaySymbol,
                    name = dto.name,
                    currency = dto.currency,
                    condition = dto.condition,
                    plannedBuyAmount = dto.plannedBuyAmount,
                    isEnabled = dto.isEnabled,
                    isTriggered = dto.isTriggered,
                    triggeredDateKey = dto.triggeredDateKey,
                    lastTriggeredAt = dto.lastTriggeredAt,
                    createdAt = dto.createdAt,
                )
            }

            val newRecords = backupData.buyRecords.map { dto ->
                BuyRecordEntity(
                    id = 0L, // 產生新主鍵
                    symbol = dto.symbol,
                    displaySymbol = dto.displaySymbol,
                    name = dto.name,
                    currency = dto.currency,
                    buyDate = dto.buyDate,
                    buyTimestamp = dto.buyTimestamp,
                    shares = dto.shares,
                    unitPrice = dto.unitPrice,
                    totalCost = dto.totalCost,
                    triggerTag = dto.triggerTag,
                )
            }

            database.withTransaction {
                database.watchlistDao().clearAllRules()
                database.buyRecordDao().clearAllRecords()

                if (newRules.isNotEmpty()) {
                    database.watchlistDao().insertAll(newRules)
                }
                if (newRecords.isNotEmpty()) {
                    database.buyRecordDao().insertAll(newRecords)
                }
            }

            backupData.preferences?.let { prefs ->
                preferencesRepository.setDataSourceMode(prefs.dataSourceMode)
                preferencesRepository.setBackgroundCheckEnabled(prefs.backgroundCheckEnabled)
            }

            BackupRestoreResult.Success(
                rulesCount = newRules.size,
                recordsCount = newRecords.size,
            )
        } catch (e: Exception) {
            BackupRestoreResult.Error("檔案格式無效或已損毀: ${e.localizedMessage ?: e.message}")
        }
    }
}
