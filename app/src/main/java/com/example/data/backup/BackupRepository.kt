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
                    highFrequencyCheckEnabled = currentPrefs.highFrequencyCheckEnabled,
                    highFrequencyIntervalSeconds = currentPrefs.highFrequencyIntervalSeconds,
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
                    val sb = StringBuilder()
                    val buffer = CharArray(8 * 1024)
                    var read: Int
                    while (reader.read(buffer).also { read = it } != -1) {
                        if (sb.length + read > MAX_BACKUP_CHARS) {
                            return@withContext BackupRestoreResult.Error("備份檔案過大，非有效的備份格式")
                        }
                        sb.append(buffer, 0, read)
                    }
                    sb.toString()
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

            val newRules = backupData.watchlistRules.mapNotNull { dto ->
                val cleanSymbol = dto.symbol.trim().uppercase()
                if (cleanSymbol.isEmpty()) return@mapNotNull null
                val cleanDisplay = dto.displaySymbol.trim().ifEmpty { cleanSymbol }
                val safeAmount = if (dto.plannedBuyAmount.isFinite() && dto.plannedBuyAmount >= 0.0) {
                    dto.plannedBuyAmount
                } else {
                    0.0
                }
                WatchlistRuleEntity(
                    id = 0L, // 產生新主鍵，避免衝突
                    symbol = cleanSymbol,
                    displaySymbol = cleanDisplay,
                    name = dto.name.trim().ifEmpty { cleanDisplay },
                    currency = dto.currency,
                    condition = dto.condition,
                    plannedBuyAmount = safeAmount,
                    isEnabled = dto.isEnabled,
                    isTriggered = dto.isTriggered,
                    triggeredDateKey = dto.triggeredDateKey,
                    lastTriggeredAt = dto.lastTriggeredAt,
                    createdAt = dto.createdAt,
                )
            }

            val newRecords = backupData.buyRecords.mapNotNull { dto ->
                val cleanSymbol = dto.symbol.trim().uppercase()
                if (cleanSymbol.isEmpty()) return@mapNotNull null
                if (!dto.shares.isFinite() || dto.shares <= 0.0) return@mapNotNull null
                if (!dto.unitPrice.isFinite() || dto.unitPrice < 0.0) return@mapNotNull null
                val cleanDisplay = dto.displaySymbol.trim().ifEmpty { cleanSymbol }
                val safeTotalCost = if (dto.totalCost.isFinite() && dto.totalCost >= 0.0) {
                    dto.totalCost
                } else {
                    dto.shares * dto.unitPrice
                }
                BuyRecordEntity(
                    id = 0L, // 產生新主鍵
                    symbol = cleanSymbol,
                    displaySymbol = cleanDisplay,
                    name = dto.name.trim().ifEmpty { cleanDisplay },
                    currency = dto.currency,
                    buyDate = dto.buyDate.trim(),
                    buyTimestamp = dto.buyTimestamp,
                    shares = dto.shares,
                    unitPrice = dto.unitPrice,
                    totalCost = safeTotalCost,
                    triggerTag = dto.triggerTag.trim().ifEmpty { "手動買入" },
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
                preferencesRepository.setHighFrequencyCheckEnabled(prefs.highFrequencyCheckEnabled)
                preferencesRepository.setHighFrequencyIntervalSeconds(prefs.highFrequencyIntervalSeconds)
            }

            BackupRestoreResult.Success(
                rulesCount = newRules.size,
                recordsCount = newRecords.size,
            )
        } catch (e: Exception) {
            BackupRestoreResult.Error("檔案格式無效或已損毀: ${e.localizedMessage ?: e.message}")
        }
    }

    companion object {
        private const val MAX_BACKUP_CHARS = 10 * 1024 * 1024
    }
}
