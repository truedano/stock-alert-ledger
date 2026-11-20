package com.example.di

import android.content.Context
import com.example.data.backup.BackupRepository
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferencesRepository
import com.example.data.remote.YahooFinanceService
import com.example.data.repository.MarketDataRepository
import com.example.data.repository.PortfolioRepository
import com.example.updater.AppUpdateManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(appContext)
    }
    val yahooFinanceService: YahooFinanceService by lazy { YahooFinanceService() }
    val twseMarginService: com.example.data.remote.TwseMarginService by lazy {
        com.example.data.remote.TwseMarginService()
    }
    val updateManager: AppUpdateManager by lazy { AppUpdateManager(appContext) }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            context = appContext,
            database = database,
            preferencesRepository = preferencesRepository,
        )
    }

    val marketDataRepository: MarketDataRepository by lazy {
        MarketDataRepository(
            appContext = appContext,
            yahooFinanceService = yahooFinanceService,
            twseMarginService = twseMarginService,
            watchlistDao = database.watchlistDao(),
            cachedQuoteDao = database.cachedQuoteDao(),
            preferencesRepository = preferencesRepository,
        )
    }

    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepository(
            buyRecordDao = database.buyRecordDao(),
            cachedQuoteDao = database.cachedQuoteDao(),
        )
    }
}
