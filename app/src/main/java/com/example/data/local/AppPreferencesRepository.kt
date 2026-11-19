package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.DataSourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stock_assistant_prefs"
)

data class AppPreferencesState(
    val dataSourceMode: DataSourceMode = DataSourceMode.REAL,
    val backgroundCheckEnabled: Boolean = true,
    val highFrequencyCheckEnabled: Boolean = false,
    val highFrequencyIntervalSeconds: Int = 60,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "尚未同步報價",
    val simulatedPrices: Map<String, Double> = emptyMap(),
) {
    fun simulatedPriceFor(symbol: String): Double? {
        return simulatedPrices[symbol.trim().uppercase()]
    }
}

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val DATA_SOURCE_MODE = stringPreferencesKey("data_source_mode")
        val BACKGROUND_CHECK_ENABLED = booleanPreferencesKey("background_check_enabled")
        val HIGH_FREQUENCY_CHECK_ENABLED = booleanPreferencesKey("high_frequency_check_enabled")
        val HIGH_FREQUENCY_INTERVAL_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("high_frequency_interval_seconds")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
        val SIMULATED_PRICES_JSON = stringPreferencesKey("simulated_prices_json")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), Double.serializer())

    val preferencesFlow: Flow<AppPreferencesState> = context.appDataStore.data.map { prefs ->
        val modeRaw = prefs[Keys.DATA_SOURCE_MODE]
        val mode = if (modeRaw == DataSourceMode.SIMULATED.name) {
            DataSourceMode.SIMULATED
        } else {
            DataSourceMode.REAL
        }
        val simPrices = prefs[Keys.SIMULATED_PRICES_JSON]
            ?.takeIf { it.isNotBlank() }
            ?.let { raw ->
                runCatching { json.decodeFromString(mapSerializer, raw) }.getOrDefault(emptyMap())
            }
            ?: emptyMap()

        AppPreferencesState(
            dataSourceMode = mode,
            backgroundCheckEnabled = prefs[Keys.BACKGROUND_CHECK_ENABLED] ?: true,
            highFrequencyCheckEnabled = prefs[Keys.HIGH_FREQUENCY_CHECK_ENABLED] ?: false,
            highFrequencyIntervalSeconds = prefs[Keys.HIGH_FREQUENCY_INTERVAL_SECONDS] ?: 60,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L,
            lastSyncMessage = prefs[Keys.LAST_SYNC_MESSAGE] ?: "尚未同步報價",
            simulatedPrices = simPrices,
        )
    }

    suspend fun getSnapshot(): AppPreferencesState = preferencesFlow.first()

    suspend fun setDataSourceMode(mode: DataSourceMode) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DATA_SOURCE_MODE] = mode.name
        }
    }

    suspend fun setBackgroundCheckEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setHighFrequencyCheckEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.HIGH_FREQUENCY_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setHighFrequencyIntervalSeconds(seconds: Int) {
        val valid = seconds.coerceIn(15, 600)
        context.appDataStore.edit { prefs ->
            prefs[Keys.HIGH_FREQUENCY_INTERVAL_SECONDS] = valid
        }
    }

    suspend fun updateSyncInfo(timestamp: Long, message: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp
            prefs[Keys.LAST_SYNC_MESSAGE] = message
        }
    }

    suspend fun setSimulatedPrice(symbol: String, price: Double) {
        val key = symbol.trim().uppercase()
        if (key.isEmpty() || price <= 0.0) return
        context.appDataStore.edit { prefs ->
            val current = prefs[Keys.SIMULATED_PRICES_JSON]
                ?.takeIf { it.isNotBlank() }
                ?.let { raw ->
                    runCatching { json.decodeFromString(mapSerializer, raw) }.getOrDefault(emptyMap())
                }
                ?: emptyMap()
            val updated = current.toMutableMap().apply { put(key, price) }
            prefs[Keys.SIMULATED_PRICES_JSON] = json.encodeToString(mapSerializer, updated)
        }
    }
}
