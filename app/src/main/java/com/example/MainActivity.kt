package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.example.data.local.BuyRecordEntity
import com.example.data.local.WatchlistRuleEntity
import com.example.notification.StockNotificationHelper
import com.example.ui.StockMonitorViewModel
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.AddEditRuleDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BuyRecordDialog
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
import com.example.ui.theme.CanvasObsidian
import com.example.ui.theme.StockAssistantTheme
import com.example.ui.theme.SurfaceElevated
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object WatchlistRoute

@Serializable
data object PortfolioRoute

@Serializable
data object SimulatorRoute

private data class TopLevelTab<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    private val viewModel: StockMonitorViewModel by viewModels {
        StockMonitorViewModel.Factory(application)
    }

    private val app by lazy { application as StockApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CanvasObsidian.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(SurfaceElevated.toArgb()),
        )
        handleNotificationIntent(intent)

        // 啟動時非阻塞式檢查 GitHub Releases 是否有新版本
        lifecycleScope.launch {
            app.container.updateManager.checkForUpdates()
        }

        setContent {
            StockAssistantTheme {
                val updateState by app.container.updateManager.uiState.collectAsStateWithLifecycle()

                StockAssistantRoot(
                    viewModel = viewModel,
                    onCheckForUpdate = {
                        lifecycleScope.launch {
                            app.container.updateManager.checkForUpdates(isManual = true)
                        }
                    }
                )

                AppUpdateDialog(
                    state = updateState,
                    onStartDownload = { apkUrl ->
                        lifecycleScope.launch {
                            app.container.updateManager.downloadApk(apkUrl)
                        }
                    },
                    onInstall = { apkFile ->
                        app.container.updateManager.installApk(apkFile)
                    },
                    onDismiss = {
                        app.container.updateManager.dismissUpdate()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val prefill = StockNotificationHelper.parseQuickBuyPrefill(this, intent) ?: return
        viewModel.openQuickBuyFromNotification(prefill)
        // Clear flag and token so configuration changes do not re-open unexpectedly
        intent?.removeExtra(StockNotificationHelper.EXTRA_OPEN_QUICK_BUY)
        intent?.removeExtra(StockNotificationHelper.EXTRA_INTERNAL_TOKEN)
        intent?.action = null
    }
}

@Composable
fun StockAssistantRoot(
    viewModel: StockMonitorViewModel,
    onCheckForUpdate: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val watchlistState by viewModel.watchlistUiState.collectAsStateWithLifecycle()
    val portfolioUiState by viewModel.portfolioUiState.collectAsStateWithLifecycle()
    val lookupState by viewModel.lookupState.collectAsStateWithLifecycle()
    val quickBuyPrefill by viewModel.quickBuyDialogPrefill.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(StockNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || StockNotificationHelper.hasNotificationPermission(context)
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    // Dialog states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddEditRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<WatchlistRuleEntity?>(null) }

    var showPortfolioRecordDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<BuyRecordEntity?>(null) }

    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { pendingRestoreUri = it }
    }

    val watchlistLabel = stringResource(R.string.tab_watchlist)
    val portfolioLabel = stringResource(R.string.tab_portfolio)
    val simulatorLabel = stringResource(R.string.tab_simulator)

    val tabs = remember(watchlistLabel, portfolioLabel, simulatorLabel) {
        listOf(
            TopLevelTab(
                route = WatchlistRoute,
                label = watchlistLabel,
                selectedIcon = Icons.AutoMirrored.Rounded.ShowChart,
                unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart,
                testTag = "nav_watchlist",
            ),
            TopLevelTab(
                route = PortfolioRoute,
                label = portfolioLabel,
                selectedIcon = Icons.Rounded.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                testTag = "nav_portfolio",
            ),
            TopLevelTab(
                route = SimulatorRoute,
                label = simulatorLabel,
                selectedIcon = Icons.Rounded.Science,
                unselectedIcon = Icons.Outlined.Science,
                testTag = "nav_simulator",
            ),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CanvasObsidian,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = SurfaceElevated,
                        contentColor = BrandSky,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandSky,
                                    selectedTextColor = BrandSky,
                                    indicatorColor = BrandSkyContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = SurfaceElevated,
                        contentColor = BrandSky,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = WatchlistRoute,
                    ) {
                    composable<WatchlistRoute> {
                        WatchlistScreen(
                            uiState = watchlistState,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onRefresh = { viewModel.refreshAll(sendNotifications = true) },
                            onAddClick = {
                                editingRule = null
                                viewModel.resetLookupState(null)
                                showAddEditRuleDialog = true
                            },
                            onEditRule = { item ->
                                editingRule = item.rule
                                viewModel.resetLookupState(item.quote)
                                showAddEditRuleDialog = true
                            },
                            onToggleEnabled = viewModel::toggleRuleEnabled,
                            onDeleteRule = viewModel::deleteRule,
                            onResetTrigger = viewModel::resetRuleTrigger,
                            onQuickBuyClick = viewModel::openQuickBuyFromRule,
                            onAboutAppClick = { showAboutDialog = true },
                        )
                    }

                    composable<PortfolioRoute> {
                        PortfolioScreen(
                            uiState = portfolioUiState,
                            onSelectTriggerFilter = viewModel::setPortfolioTriggerFilter,
                            onManualAddClick = {
                                editingRecord = null
                                showPortfolioRecordDialog = true
                            },
                            onEditRecordClick = { record ->
                                editingRecord = record
                                showPortfolioRecordDialog = true
                            },
                        )
                    }

                    composable<SimulatorRoute> {
                        SimulatorScreen(
                            uiState = watchlistState,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onDataSourceModeChange = viewModel::setDataSourceMode,
                            onBackgroundCheckToggle = viewModel::setBackgroundCheckEnabled,
                            onSimulatedPriceChange = viewModel::updateSimulatedPrice,
                            onSimulateDropBelowLevel = viewModel::simulateDropBelowLevel,
                            onSendTestNotification = viewModel::sendTestNotification,
                        )
                    }
                    }
                }
            }
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // About App & Check Update Dialog
    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false },
            onCheckUpdate = onCheckForUpdate,
            onExportBackup = {
                val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                exportBackupLauncher.launch("StockAlertLedger_Backup_$timestampStr.json")
            },
            onShareBackup = {
                val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                val filename = "StockAlertLedger_Backup_$timestampStr.json"
                coroutineScope.launch {
                    val uri = viewModel.getShareableBackupUri(filename) ?: return@launch
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "傳送或備份至雲端硬碟 / 郵件 / 通訊軟體"))
                }
            },
            onImportBackup = {
                importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
        )
    }

    // Confirm Restore Dialog
    pendingRestoreUri?.let { restoreUri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = {
                Text(
                    text = "確認還原備份資料？",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "警告：匯入還原將會清除現有的自選監控股與記帳資料，並以備份檔內容完全覆蓋。此動作無法復原，是否繼續？",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackup(restoreUri)
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TaiwanRed)
                ) {
                    Text("確認覆蓋還原")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Add / Edit Watchlist Rule Dialog
    if (showAddEditRuleDialog) {
        AddEditRuleDialog(
            editingRule = editingRule,
            lookupState = lookupState,
            onLookupTicker = viewModel::lookupTicker,
            onDismiss = {
                showAddEditRuleDialog = false
                editingRule = null
            },
            onSave = { existingId, symbol, displaySymbol, name, currency, condition, amount, quote ->
                viewModel.saveWatchlistRule(
                    existingId = existingId,
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    name = name,
                    currency = currency,
                    condition = condition,
                    plannedBuyAmount = amount,
                    initialQuote = quote,
                )
                showAddEditRuleDialog = false
                editingRule = null
            },
        )
    }

    val extraTriggerSuggestions = remember(watchlistState.items, portfolioUiState.triggerTagCounts) {
        val fromRules = watchlistState.items.mapNotNull { it.evaluation?.triggerTag }
        val fromRecords = portfolioUiState.triggerTagCounts.map { it.tag }
        (fromRules + fromRecords).distinct()
    }

    // Quick-Buy Confirmation Dialog (from Notification PendingIntent or Watchlist Card)
    quickBuyPrefill?.let { prefill ->
        BuyRecordDialog(
            prefill = prefill,
            editingRecord = null,
            extraTriggerSuggestions = extraTriggerSuggestions,
            onDismiss = viewModel::dismissQuickBuyDialog,
            onSave = { _, symbol, displaySymbol, name, currency, buyDate, shares, unitPrice, tag, resetRuleId ->
                viewModel.saveBuyRecord(
                    existingId = null,
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    name = name,
                    currency = currency,
                    buyDate = buyDate,
                    shares = shares,
                    unitPrice = unitPrice,
                    triggerTag = tag,
                    resetRuleId = resetRuleId,
                )
            },
        )
    }

    // Manual Add / Edit Portfolio Buy Record Dialog
    if (showPortfolioRecordDialog) {
        BuyRecordDialog(
            prefill = null,
            editingRecord = editingRecord,
            extraTriggerSuggestions = extraTriggerSuggestions,
            onDismiss = {
                showPortfolioRecordDialog = false
                editingRecord = null
            },
            onSave = { existingId, symbol, displaySymbol, name, currency, buyDate, shares, unitPrice, tag, _ ->
                viewModel.saveBuyRecord(
                    existingId = existingId,
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    name = name,
                    currency = currency,
                    buyDate = buyDate,
                    shares = shares,
                    unitPrice = unitPrice,
                    triggerTag = tag,
                    resetRuleId = null,
                )
                showPortfolioRecordDialog = false
                editingRecord = null
            },
            onDelete = { recordId ->
                viewModel.deleteBuyRecord(recordId)
                showPortfolioRecordDialog = false
                editingRecord = null
            },
        )
    }
}
