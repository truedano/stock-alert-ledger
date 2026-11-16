package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataSourceMode
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.ui.SimulatedStockUi
import com.example.ui.SimulatedTargetLevel
import com.example.ui.WatchlistUiState
import com.example.ui.components.MarketBadge
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
import com.example.ui.theme.CanvasObsidian
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SimulatorScreen(
    uiState: WatchlistUiState,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDataSourceModeChange: (DataSourceMode) -> Unit,
    onBackgroundCheckToggle: (Boolean) -> Unit,
    onHighFrequencyCheckToggle: (Boolean) -> Unit,
    onSimulatedPriceChange: (String, Double) -> Unit,
    onSimulateDropBelowLevel: (SimulatedStockUi, SimulatedTargetLevel?) -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasWatchlistStocks = uiState.watchlistSimulatedStocks.isNotEmpty()
    var showBuiltInWhenWatchlistPresent by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "sim_title") {
                Column {
                    Text(
                        text = "資料來源與模擬測試",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "可直接模擬您監控清單中的真實股票，驗證通知與一鍵記帳",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }

            if (!hasNotificationPermission) {
                item(key = "sim_perm_banner") {
                    NotificationPermissionBanner(
                        onGrantClick = onRequestNotificationPermission,
                    )
                }
            }

            // Data Source Mode Switch Card
            item(key = "data_source_switch_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, HairlineBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = null,
                                tint = BrandSky,
                            )
                            Text(
                                text = "資料來源總開關",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilterChip(
                                selected = uiState.dataSourceMode == DataSourceMode.REAL,
                                onClick = { onDataSourceModeChange(DataSourceMode.REAL) },
                                label = { Text("真實股市報價") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("mode_real_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                            FilterChip(
                                selected = uiState.dataSourceMode == DataSourceMode.SIMULATED,
                                onClick = { onDataSourceModeChange(DataSourceMode.SIMULATED) },
                                label = { Text("模擬測試模式") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("mode_sim_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertAmberContainer,
                                    selectedLabelColor = AlertAmber,
                                ),
                            )
                        }

                        Text(
                            text = if (uiState.dataSourceMode == DataSourceMode.REAL) {
                                "目前使用 Yahoo Finance 即時與 1 年期日收盤價計算價格與技術指標。"
                            } else {
                                "目前已啟用模擬測試模式！可用滑桿、輸入精確價格或選擇目標週期一鍵觸發通知。"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Watchlist-linked simulated stocks (Scheme A)
            if (hasWatchlistStocks) {
                item(key = "sim_watchlist_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "監控清單標的模擬 (${uiState.watchlistSimulatedStocks.size} 檔)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        TextButton(
                            onClick = {
                                showBuiltInWhenWatchlistPresent = !showBuiltInWhenWatchlistPresent
                            },
                        ) {
                            Text(
                                text = if (showBuiltInWhenWatchlistPresent) "隱藏內建範本" else "顯示內建範本",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                items(
                    items = uiState.watchlistSimulatedStocks,
                    key = { "sim_watchlist_${it.symbol}" },
                ) { simStock ->
                    SimulatedStockControlCard(
                        stock = simStock,
                        isSimulatedModeActive = uiState.dataSourceMode == DataSourceMode.SIMULATED,
                        onPriceChange = { newPrice ->
                            onSimulatedPriceChange(simStock.symbol, newPrice)
                        },
                        onDropBelowLevelClick = { level ->
                            onSimulateDropBelowLevel(simStock, level)
                        },
                    )
                }
            }

            // Built-in offline templates (shown automatically when watchlist is empty, or toggled)
            if (!hasWatchlistStocks || showBuiltInWhenWatchlistPresent) {
                item(key = "sim_panel_header") {
                    Text(
                        text = if (hasWatchlistStocks) {
                            "內建離線測試範本 (2 檔)"
                        } else {
                            "內建離線測試範本 (新增監控股票後將自動連動)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }

                items(
                    items = uiState.builtInSimulatedStocks,
                    key = { "sim_builtin_${it.symbol}" },
                ) { simStock ->
                    SimulatedStockControlCard(
                        stock = simStock,
                        isSimulatedModeActive = uiState.dataSourceMode == DataSourceMode.SIMULATED,
                        onPriceChange = { newPrice ->
                            onSimulatedPriceChange(simStock.symbol, newPrice)
                        },
                        onDropBelowLevelClick = { level ->
                            onSimulateDropBelowLevel(simStock, level)
                        },
                    )
                }
            }

            // Notification & WorkManager Settings
            item(key = "notification_schedule_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, HairlineBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = BrandSky,
                            )
                            Text(
                                text = "通知與背景排程設定",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "盤中每分鐘高頻監控 (前景常駐服務)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "切換到其他 App 或鎖定螢幕仍每 60 秒即時輪詢報價，達成最即時推播通知",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.highFrequencyCheckEnabled,
                                onCheckedChange = onHighFrequencyCheckToggle,
                                modifier = Modifier.testTag("high_frequency_service_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CanvasObsidian,
                                    checkedTrackColor = BrandSky,
                                ),
                            )
                        }

                        HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "背景定期檢查 (WorkManager)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "每 15 分鐘自動檢查啟用中的監控條件，同日僅提醒一次避免洗版",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.backgroundCheckEnabled,
                                onCheckedChange = onBackgroundCheckToggle,
                                modifier = Modifier.testTag("background_worker_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CanvasObsidian,
                                    checkedTrackColor = BrandSky,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_test_notification_button"),
                            border = BorderStroke(1.dp, BrandSky.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "發送測試通知",
                                tint = BrandSky,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "發送測試通知（驗證橫幅響鈴與點擊跳轉）",
                                color = BrandSky,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimulatedStockControlCard(
    stock: SimulatedStockUi,
    isSimulatedModeActive: Boolean,
    onPriceChange: (Double) -> Unit,
    onDropBelowLevelClick: (SimulatedTargetLevel?) -> Unit,
) {
    var selectedPeriod by remember(stock.symbol, stock.targetLevels) {
        mutableStateOf(stock.targetLevels.firstOrNull()?.periodDays ?: 20)
    }
    val activeLevel = stock.targetLevels.firstOrNull { it.periodDays == selectedPeriod }
        ?: stock.targetLevels.firstOrNull()

    var exactPriceText by remember(stock.symbol) {
        mutableStateOf(Formatters.formatRawPriceForInput(stock.currentPrice, stock.currency))
    }

    LaunchedEffect(stock.currentPrice) {
        exactPriceText = Formatters.formatRawPriceForInput(stock.currentPrice, stock.currency)
    }

    val isBelowSelected = activeLevel?.let { stock.currentPrice <= it.referencePrice } ?: false
    val statusColor = if (isBelowSelected) TaiwanGreen else TaiwanRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sim_card_${stock.symbol}"),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSimulatedModeActive) AlertAmber.copy(alpha = 0.45f) else HairlineBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stock.displaySymbol,
                        style = FinancialTextStyles.tickerBadge,
                        color = BrandSky,
                    )
                    Text(
                        text = stock.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MarketBadge(currency = stock.currency)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "模擬現價",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = Formatters.formatPrice(stock.currentPrice, stock.currency),
                        style = FinancialTextStyles.priceLarge,
                        color = statusColor,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (stock.targetLevels.isEmpty()) {
                        Text(
                            text = "尚未累積足夠歷史收盤日",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    } else {
                        stock.targetLevels.take(3).forEach { level ->
                            Text(
                                text = "${level.label}：${Formatters.formatPrice(level.referencePrice, stock.currency)}",
                                style = FinancialTextStyles.numericSmall,
                                color = if (level.periodDays == activeLevel?.periodDays) TextPrimary else TextSecondary,
                            )
                        }
                    }
                }
            }

            val sliderMin = minOf(stock.minSliderPrice, stock.currentPrice * 0.95).toFloat()
            val sliderMax = maxOf(stock.maxSliderPrice, stock.currentPrice * 1.05).toFloat()
                .coerceAtLeast(sliderMin + 1f)

            Slider(
                value = stock.currentPrice.toFloat().coerceIn(sliderMin, sliderMax),
                onValueChange = { rawValue ->
                    val snapped = if (stock.currency == MarketCurrency.TWD) {
                        (rawValue * 2f).toInt() / 2.0
                    } else {
                        (rawValue * 100f).toInt() / 100.0
                    }
                    onPriceChange(snapped)
                },
                valueRange = sliderMin..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = AlertAmber,
                    activeTrackColor = AlertAmber,
                    inactiveTrackColor = SurfaceCardHigh,
                ),
                modifier = Modifier.testTag("sim_slider_${stock.symbol}"),
            )

            // Exact Price Input Row (Scheme C)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = exactPriceText,
                    onValueChange = { exactPriceText = it },
                    label = { Text("指定模擬價格 (${stock.currency.prefix})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_exact_price_input_${stock.symbol}"),
                )
                val parsedExact = exactPriceText.toDoubleOrNull()
                FilledTonalButton(
                    onClick = {
                        if (parsedExact != null && parsedExact > 0.0) {
                            onPriceChange(parsedExact)
                        }
                    },
                    enabled = parsedExact != null && parsedExact > 0.0,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("sim_apply_price_button_${stock.symbol}"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("套用價格")
                }
            }

            // Target Period Selector (Scheme C)
            if (stock.targetLevels.isNotEmpty()) {
                Text(
                    text = "選擇要模擬測試的觸發條件",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    stock.targetLevels.forEach { level ->
                        val selected = level.periodDays == activeLevel?.periodDays
                        FilterChip(
                            selected = selected,
                            onClick = { selectedPeriod = level.periodDays },
                            label = { Text(level.label) },
                            modifier = Modifier.testTag("sim_level_chip_${stock.symbol}_${level.periodDays}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSkyContainer,
                                selectedLabelColor = BrandSky,
                            ),
                        )
                    }
                }
            }

            val buttonLabel = if (activeLevel != null) {
                if (stock.hasWatchlistRule) {
                    "一鍵模擬跌破 ${activeLevel.label} 並觸發通知"
                } else {
                    "建立 ${activeLevel.label} 示範條件並一鍵通知"
                }
            } else {
                "一鍵模擬下探價格並觸發通知"
            }

            Button(
                onClick = { onDropBelowLevelClick(activeLevel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sim_drop_button_${stock.symbol}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertAmber,
                    contentColor = CanvasObsidian,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = "模擬觸發買入條件",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
