package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.PriceBand
import com.example.domain.model.StockValuationItem
import com.example.domain.model.ValuationCalculator
import com.example.domain.model.ValuationMarket
import com.example.domain.model.ValuationMethod
import com.example.domain.model.ValuationTier
import com.example.ui.ValuationUiState
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
import com.example.ui.theme.CanvasObsidian
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValuationScreen(
    uiState: ValuationUiState,
    onEnsureLoaded: () -> Unit,
    onSelectMarket: (ValuationMarket) -> Unit,
    onToggleSortMode: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onCreatePriceAlert: (StockValuationItem, Boolean, PriceBand?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onEnsureLoaded()
    }

    val rawItems = remember(uiState.snapshot, uiState.selectedMarket) {
        when (uiState.selectedMarket) {
            ValuationMarket.TW -> uiState.snapshot?.twTop10.orEmpty()
            ValuationMarket.US -> uiState.snapshot?.usTop10.orEmpty()
        }
    }

    val displayItems = remember(rawItems, uiState.sortByMarginOfSafety) {
        if (uiState.sortByMarginOfSafety) {
            rawItems.sortedBy { it.marginOfSafetyPercent }
        } else {
            rawItems.sortedBy { it.rank }
        }
    }

    val cheapCount = remember(rawItems) { rawItems.count { it.tier == ValuationTier.CHEAP } }
    val fairCount = remember(rawItems) {
        rawItems.count { it.tier == ValuationTier.FAIR_LOW || it.tier == ValuationTier.FAIR_HIGH }
    }
    val expensiveCount = remember(rawItems) { rawItems.count { it.tier == ValuationTier.EXPENSIVE } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasObsidian,
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ValuationHeaderCard(
                        updatedAtMs = uiState.snapshot?.updatedAtMs ?: 0L,
                        isFromCache = uiState.snapshot?.isFromCache == true,
                        isLoading = uiState.isLoading,
                        onRefresh = onRefresh,
                        onGuideClick = { showGuideDialog = true },
                    )
                }

                item {
                    ValuationControlBar(
                        selectedMarket = uiState.selectedMarket,
                        sortByMarginOfSafety = uiState.sortByMarginOfSafety,
                        cheapCount = cheapCount,
                        fairCount = fairCount,
                        expensiveCount = expensiveCount,
                        totalCount = rawItems.size,
                        onSelectMarket = onSelectMarket,
                        onToggleSortMode = onToggleSortMode,
                    )
                }

                if (uiState.isLoading && displayItems.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, HairlineBorder),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 42.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = BrandSky)
                                Text(
                                    text = "正在自動校正台美前十大市值名單並計算 5 年歷史估值分位…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                } else if (uiState.errorMessage != null && displayItems.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, TaiwanRed.copy(alpha = 0.45f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TaiwanRed,
                                )
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandSky,
                                        contentColor = CanvasObsidian,
                                    ),
                                ) {
                                    Text("重新抓取與校正", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = displayItems,
                        key = { "${it.market.name}_${it.symbol}" },
                    ) { item ->
                        ValuationStockCard(
                            item = item,
                            onCreateCheapAlert = { band -> onCreatePriceAlert(item, true, band) },
                            onCreateFairAlert = { band -> onCreatePriceAlert(item, false, band) },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showGuideDialog) {
        ValuationMethodologyDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun ValuationHeaderCard(
    updatedAtMs: Long,
    isFromCache: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onGuideClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BrandSkyContainer.copy(alpha = 0.5f), SurfaceCard)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "台美十強自動校正估值雷達",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val syncText = if (updatedAtMs > 0L) {
                        val cacheLabel = if (isFromCache) "（本地快取）" else "（即時校正）"
                        "更新：${Formatters.formatDateTime(updatedAtMs)}$cacheLabel"
                    } else {
                        "自動追蹤即時市值前十與 5 年歷史本益比/淨值比分位"
                    }
                    Text(
                        text = syncText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onGuideClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "估值邏輯說明",
                            tint = BrandSky,
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("valuation_refresh_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "強制重新校正",
                            tint = if (isLoading) TextMuted else BrandSky,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValuationControlBar(
    selectedMarket: ValuationMarket,
    sortByMarginOfSafety: Boolean,
    cheapCount: Int,
    fairCount: Int,
    expensiveCount: Int,
    totalCount: Int,
    onSelectMarket: (ValuationMarket) -> Unit,
    onToggleSortMode: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedMarket == ValuationMarket.TW,
                onClick = { onSelectMarket(ValuationMarket.TW) },
                label = { Text("🇹🇼 台股 Top 10", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandSkyContainer,
                    selectedLabelColor = BrandSky,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary,
                ),
            )
            FilterChip(
                selected = selectedMarket == ValuationMarket.US,
                onClick = { onSelectMarket(ValuationMarket.US) },
                label = { Text("🇺🇸 美股 Top 10", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandSkyContainer,
                    selectedLabelColor = BrandSky,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary,
                ),
            )
            FilterChip(
                selected = !sortByMarginOfSafety,
                onClick = { onToggleSortMode(false) },
                label = { Text("市值 #1~#10") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SurfaceCardHigh,
                    selectedLabelColor = TextPrimary,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary,
                ),
            )
            FilterChip(
                selected = sortByMarginOfSafety,
                onClick = { onToggleSortMode(true) },
                label = { Text("最被低估優先") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TaiwanGreenContainer,
                    selectedLabelColor = TaiwanGreen,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary,
                ),
            )
        }

        if (totalCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, HairlineBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "位階分佈 ($totalCount 檔)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "便宜 $cheapCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = TaiwanGreen,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "合理 $fairCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlertAmber,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "昂貴 $expensiveCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = TaiwanRed,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private enum class ValuationBasis(val label: String) {
    FORWARD_PE("預估 PE"),
    TRAILING_PE("過去 PE"),
    PB("淨值比 P/B"),
}

@Composable
private fun ValuationStockCard(
    item: StockValuationItem,
    onCreateCheapAlert: (PriceBand) -> Unit,
    onCreateFairAlert: (PriceBand) -> Unit,
) {
    val defaultBasis = remember(item.symbol, item.primaryMethod, item.forwardPePriceBand, item.trailingPePriceBand, item.pbPriceBand) {
        when {
            item.primaryMethod == ValuationMethod.PB_PRIORITY && item.pbPriceBand != null -> ValuationBasis.PB
            item.forwardPePriceBand != null -> ValuationBasis.FORWARD_PE
            item.trailingPePriceBand != null -> ValuationBasis.TRAILING_PE
            item.pbPriceBand != null -> ValuationBasis.PB
            else -> ValuationBasis.FORWARD_PE
        }
    }
    var selectedBasis by remember(item.symbol) { mutableStateOf(defaultBasis) }
    val selectedBand = when (selectedBasis) {
        ValuationBasis.FORWARD_PE -> item.forwardPePriceBand
        ValuationBasis.TRAILING_PE -> item.trailingPePriceBand
        ValuationBasis.PB -> item.pbPriceBand
    } ?: item.activePriceBand

    val activeTier = ValuationCalculator.computeTier(item.currentPrice, selectedBand)
    val activeMosPercent = ValuationCalculator.computeMarginOfSafetyPercent(item.currentPrice, selectedBand)

    val tierColor = when (activeTier) {
        ValuationTier.CHEAP -> TaiwanGreen
        ValuationTier.FAIR_LOW -> BrandSky
        ValuationTier.FAIR_HIGH -> AlertAmber
        ValuationTier.EXPENSIVE -> TaiwanRed
    }
    val tierContainer = when (activeTier) {
        ValuationTier.CHEAP -> TaiwanGreenContainer
        ValuationTier.FAIR_LOW -> BrandSkyContainer
        ValuationTier.FAIR_HIGH -> AlertAmberContainer
        ValuationTier.EXPENSIVE -> TaiwanRedContainer
    }

    val isTw = item.currency == MarketCurrency.TWD
    val changePct = item.changePercent ?: 0.0
    val changeColor = when {
        changePct > 0.001 -> if (isTw) TaiwanRed else TaiwanGreen
        changePct < -0.001 -> if (isTw) TaiwanGreen else TaiwanRed
        else -> TextSecondary
    }

    val mosText = if (activeMosPercent <= 0.0) {
        String.format(Locale.US, "折價 %.1f%%", kotlin.math.abs(activeMosPercent))
    } else {
        String.format(Locale.US, "溢價 +%.1f%%", activeMosPercent)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.tier == ValuationTier.CHEAP) TaiwanGreen.copy(alpha = 0.55f) else HairlineBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header row: rank, symbol, name, current price & tier badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandSkyContainer,
                        border = BorderStroke(1.dp, BrandSky.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "#${item.rank}",
                            style = FinancialTextStyles.tickerBadge,
                            color = BrandSky,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.displaySymbol,
                                style = FinancialTextStyles.numericSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = "· ${ValuationCalculator.formatMarketCap(item.marketCap, item.currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatPrice(item.currentPrice, item.currency),
                        style = FinancialTextStyles.priceLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (item.changePercent != null) {
                        Text(
                            text = Formatters.formatSignedPercent(item.changePercent),
                            style = FinancialTextStyles.numericSmall,
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Tier & Valuation Method Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tierContainer,
                        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = "${activeTier.label} · $mosText",
                            style = MaterialTheme.typography.labelSmall,
                            color = tierColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                val isCustomBasis = selectedBasis != defaultBasis
                Surface(
                    onClick = { selectedBasis = defaultBasis },
                    enabled = isCustomBasis,
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCustomBasis -> BrandSkyContainer
                        item.primaryMethod == ValuationMethod.PB_PRIORITY -> BrandIndigo.copy(alpha = 0.16f)
                        else -> SurfaceCardHigh
                    },
                    border = if (isCustomBasis) BorderStroke(1.dp, BrandSky.copy(alpha = 0.45f)) else null,
                ) {
                    Text(
                        text = when {
                            isCustomBasis -> "${selectedBasis.label}評估 · 點擊重置"
                            item.primaryMethod == ValuationMethod.PB_PRIORITY -> "★ P/B 淨值主導"
                            else -> "預估 PE 主導"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isCustomBasis -> BrandSky
                            item.primaryMethod == ValuationMethod.PB_PRIORITY -> BrandIndigo
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // 4-Metric Grid: Forward PE, Trailing PE, Implied EPS Growth, P/B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ValuationMetricCell(
                    label = if (defaultBasis == ValuationBasis.FORWARD_PE) "★ 預估 PE" else "預估 PE",
                    value = ValuationCalculator.formatMultiple(item.forwardPe),
                    subValue = "預估EPS ${ValuationCalculator.formatEps(item.forwardEps, item.currency)}",
                    highlight = selectedBasis == ValuationBasis.FORWARD_PE,
                    onClick = if (item.forwardPePriceBand != null) {
                        { selectedBasis = ValuationBasis.FORWARD_PE }
                    } else null,
                    modifier = Modifier.weight(1f),
                )
                ValuationMetricCell(
                    label = "過去 PE",
                    value = ValuationCalculator.formatMultiple(item.trailingPe),
                    subValue = "TTM ${ValuationCalculator.formatEps(item.trailingEps, item.currency)}",
                    highlight = selectedBasis == ValuationBasis.TRAILING_PE,
                    onClick = if (item.trailingPePriceBand != null) {
                        { selectedBasis = ValuationBasis.TRAILING_PE }
                    } else null,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val growth = item.impliedEpsGrowthPercent
                val growthColor = when {
                    growth == null -> TextSecondary
                    growth >= 0.0 -> TaiwanGreen
                    else -> AlertAmber
                }
                val growthText = growth?.let { Formatters.formatSignedPercent(it) } ?: "—"
                val growthSub = when {
                    growth == null -> "缺預估對比"
                    growth >= 15.0 -> "獲利高成長消化PE"
                    growth >= 0.0 -> "獲利溫和成長"
                    else -> "⚠️ 預估獲利衰退"
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "隱含獲利成長",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            if (growth != null) {
                                Icon(
                                    imageVector = if (growth >= 0.0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = growthColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Text(
                            text = growthText,
                            style = FinancialTextStyles.numericMedium,
                            color = growthColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = growthSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ValuationMetricCell(
                    label = if (defaultBasis == ValuationBasis.PB) "★ 淨值比 P/B" else "淨值比 P/B",
                    value = ValuationCalculator.formatMultiple(item.priceToBook),
                    subValue = "淨值 ${ValuationCalculator.formatEps(item.bookValuePerShare, item.currency)}",
                    highlight = selectedBasis == ValuationBasis.PB,
                    onClick = if (item.pbPriceBand != null) {
                        { selectedBasis = ValuationBasis.PB }
                    } else null,
                    modifier = Modifier.weight(1f),
                )
            }

            // Valuation River / Gauge Bar
            ValuationRiverBar(
                item = item,
                band = selectedBand,
                usePbBand = selectedBasis == ValuationBasis.PB,
            )

            // Cross-check secondary valuation reference
            val secondarySummary = buildSecondarySummary(item, selectedBasis)
            if (secondarySummary.isNotBlank()) {
                Text(
                    text = secondarySummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            HorizontalDivider(color = HairlineBorder)

            // Quick Alert Action Buttons
            val cheapRounded = ValuationCalculator.roundTargetPrice(selectedBand.cheapPrice, item.currency)
            val fairRounded = ValuationCalculator.roundTargetPrice(selectedBand.fairPrice, item.currency)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onCreateCheapAlert(selectedBand) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TaiwanGreen.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = TaiwanGreen,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "設便宜價",
                                style = MaterialTheme.typography.labelSmall,
                                color = TaiwanGreen,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = Formatters.formatPrice(cheapRounded, item.currency),
                            style = FinancialTextStyles.numericSmall,
                            color = TaiwanGreen,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onCreateFairAlert(selectedBand) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandSky.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = BrandSky,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "設合理價",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSky,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = Formatters.formatPrice(fairRounded, item.currency),
                            style = FinancialTextStyles.numericSmall,
                            color = BrandSky,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValuationMetricCell(
    label: String,
    value: String,
    subValue: String,
    highlight: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) BrandSkyContainer.copy(alpha = 0.45f) else SurfaceCardHigh,
        border = if (highlight) BorderStroke(1.dp, BrandSky.copy(alpha = 0.35f)) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) BrandSky else TextMuted,
            )
            Text(
                text = value,
                style = FinancialTextStyles.numericMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ValuationRiverBar(
    item: StockValuationItem,
    band: PriceBand,
    usePbBand: Boolean,
) {
    val minRange = band.cheapPrice * 0.85
    val maxRange = band.expensivePrice * 1.15
    val span = (maxRange - minRange).coerceAtLeast(1e-6)

    val cheapFrac = ((band.cheapPrice - minRange) / span).toFloat().coerceIn(0.05f, 0.9f)
    val fairFrac = ((band.fairPrice - minRange) / span).toFloat().coerceIn(cheapFrac + 0.05f, 0.92f)
    val expFrac = ((band.expensivePrice - minRange) / span).toFloat().coerceIn(fairFrac + 0.05f, 0.95f)
    val currentFrac = ((item.currentPrice - minRange) / span).toFloat().coerceIn(0.02f, 0.98f)

    val activeBand = if (usePbBand) item.pbBand else item.peBand
    val metricUnit = if (usePbBand) "PB" else "PE"
    val calSource = if (activeBand.isAutoCalibrated) {
        "5年歷史${metricUnit}自動校正(${activeBand.sampleCount}季)"
    } else {
        "基準${metricUnit}模型"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCardHigh, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = calSource,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "倍數區間",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%.1fx / %.1fx / %.1fx",
                        activeBand.lowMultiple,
                        activeBand.fairMultiple,
                        activeBand.highMultiple,
                    ),
                    style = FinancialTextStyles.numericSmall,
                    color = BrandSky,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val barTop = size.height * 0.3f
            val barHeight = size.height * 0.4f
            val r = barHeight / 2f

            // Cheap zone (Green)
            drawRoundRect(
                color = TaiwanGreen.copy(alpha = 0.75f),
                topLeft = Offset(0f, barTop),
                size = Size(size.width * cheapFrac, barHeight),
                cornerRadius = CornerRadius(r, r),
            )
            // Fair-low zone (Sky)
            drawRect(
                color = BrandSky.copy(alpha = 0.75f),
                topLeft = Offset(size.width * cheapFrac, barTop),
                size = Size(size.width * (fairFrac - cheapFrac), barHeight),
            )
            // Fair-high zone (Amber)
            drawRect(
                color = AlertAmber.copy(alpha = 0.75f),
                topLeft = Offset(size.width * fairFrac, barTop),
                size = Size(size.width * (expFrac - fairFrac), barHeight),
            )
            // Expensive zone (Red)
            drawRoundRect(
                color = TaiwanRed.copy(alpha = 0.75f),
                topLeft = Offset(size.width * expFrac, barTop),
                size = Size(size.width * (1f - expFrac), barHeight),
                cornerRadius = CornerRadius(r, r),
            )

            // Current Price Indicator Dot
            val cx = size.width * currentFrac
            val cy = size.height * 0.5f
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(cx, cy),
            )
            drawCircle(
                color = CanvasObsidian,
                radius = 4.dp.toPx(),
                center = Offset(cx, cy),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "便宜",
                    style = MaterialTheme.typography.labelSmall,
                    color = TaiwanGreen,
                )
                Text(
                    text = Formatters.formatPrice(band.cheapPrice, item.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = TaiwanGreen,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "合理",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandSky,
                )
                Text(
                    text = Formatters.formatPrice(band.fairPrice, item.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandSky,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "昂貴",
                    style = MaterialTheme.typography.labelSmall,
                    color = TaiwanRed,
                )
                Text(
                    text = Formatters.formatPrice(band.expensivePrice, item.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = TaiwanRed,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun buildSecondarySummary(
    item: StockValuationItem,
    selectedBasis: ValuationBasis,
): String {
    val parts = ArrayList<String>()
    if (selectedBasis != ValuationBasis.FORWARD_PE) {
        item.forwardPePriceBand?.let {
            parts.add("預估PE合理價 ${Formatters.formatPrice(it.fairPrice, item.currency)}")
        }
    }
    if (selectedBasis != ValuationBasis.TRAILING_PE) {
        item.trailingPePriceBand?.let {
            parts.add("過去PE合理價 ${Formatters.formatPrice(it.fairPrice, item.currency)}")
        }
    }
    if (selectedBasis != ValuationBasis.PB) {
        item.pbPriceBand?.let {
            parts.add("P/B淨值合理價 ${Formatters.formatPrice(it.fairPrice, item.currency)}")
        }
    }
    item.dividendYieldPercent?.takeIf { it > 0.0 }?.let {
        parts.add(String.format(Locale.US, "殖利率 %.2f%%", it))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun ValuationMethodologyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(
                text = "自動校正估值模型說明",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "1. 前十大名單自動校正：\n即時依市值抓取台美前十大權值股，並自動排除雙重股別重複佔位（如保留 GOOGL、BRK-B）與美國掛牌的台股 ADR（如 TSM），台股並自動對齊證交所官方中文名稱與 PE/PB。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "2. 5 年歷史分位數自動定錨：\n拉取各股過去 5 年季度 PE / PB 序列（過濾極端離群值），自動計算 P20（便宜倍數）、P50（合理中位倍數）、P80（昂貴倍數）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "3. 雙軌估值與金融股 P/B 保護：\n• 一般科技/傳產股：以「預估 EPS × 歷史本益比分位」為主，同時比對「過去 EPS」計算隱含獲利成長率。\n• 金融股與波克夏 (BRK-B)：受投資評價損益波動影響，自動切換為「每股淨值 × 歷史 P/B 分位」為主、PE 為輔。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("了解", color = BrandSky, fontWeight = FontWeight.Bold)
            }
        },
    )
}
