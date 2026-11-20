package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BuyRecordEntity
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.StockHoldingSummary
import com.example.ui.PortfolioUiState
import com.example.ui.components.MarketBadge
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
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioScreen(
    uiState: PortfolioUiState,
    onSelectTriggerFilter: (String?) -> Unit,
    onManualAddClick: () -> Unit,
    onEditRecordClick: (BuyRecordEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary
    val records = uiState.filteredRecords
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasObsidian,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onManualAddClick,
                containerColor = BrandSky,
                contentColor = CanvasObsidian,
                modifier = Modifier.testTag("manual_buy_record_fab"),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "手動記帳",
                    )
                },
                text = {
                    Text(
                        text = "手動記帳",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "portfolio_title") {
                    Column {
                        Text(
                            text = "買入紀錄與成本統計",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "台股與美股分幣別獨立計算總投入成本與平均持股均價",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }

                if (uiState.triggerTagCounts.isNotEmpty()) {
                    item(key = "trigger_filter_row") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "依觸發情況篩選統計",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FilterChip(
                                    selected = uiState.selectedTriggerFilter == null,
                                    onClick = { onSelectTriggerFilter(null) },
                                    label = { Text("全部 (${uiState.totalRecordCount})") },
                                    modifier = Modifier.testTag("filter_trigger_all"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandSkyContainer,
                                        selectedLabelColor = BrandSky,
                                    ),
                                )
                                uiState.triggerTagCounts.forEach { tagCount ->
                                    val selected = uiState.selectedTriggerFilter == tagCount.tag
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            onSelectTriggerFilter(if (selected) null else tagCount.tag)
                                        },
                                        label = { Text("${tagCount.tag} (${tagCount.count})") },
                                        modifier = Modifier.testTag("filter_trigger_${tagCount.tag}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandSkyContainer,
                                            selectedLabelColor = BrandSky,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "currency_totals_card") {
                    DualCurrencySummaryCard(
                        twdTotal = summary.twdTotalInvested,
                        usdTotal = summary.usdTotalInvested,
                        activeFilter = uiState.selectedTriggerFilter,
                    )
                }

                if (summary.holdings.isNotEmpty()) {
                    item(key = "holdings_section_header") {
                        Text(
                            text = "個股持倉平均成本 (${summary.holdings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    items(
                        items = summary.holdings,
                        key = { "holding_${it.symbol}_${it.currency.name}" },
                        contentType = { "holding_summary_card" },
                    ) { holding ->
                        HoldingSummaryCard(holding = holding)
                    }
                }

                item(key = "records_section_header") {
                    Text(
                        text = "買入歷史明細 (${records.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (records.isEmpty()) {
                    item(key = "empty_portfolio") {
                        EmptyPortfolioCard(onManualAddClick = onManualAddClick)
                    }
                } else {
                    items(
                        items = records,
                        key = { "record_${it.id}" },
                        contentType = { "buy_record_card" },
                    ) { record ->
                        BuyRecordCard(
                            record = record,
                            onClick = { onEditRecordClick(record) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualCurrencySummaryCard(
    twdTotal: Double,
    usdTotal: Double,
    activeFilter: String?,
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
                        colors = listOf(
                            BrandSky.copy(alpha = 0.10f),
                            SurfaceCard,
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = BrandSky,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (activeFilter == null) {
                        "累計總投入資金（依幣別獨立統計）"
                    } else {
                        "篩選統計：$activeFilter"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activeFilter == null) TextSecondary else BrandSky,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CurrencyTotalBox(
                    label = "台幣總投入 (NT$)",
                    formattedValue = Formatters.formatAmount(twdTotal, MarketCurrency.TWD),
                    accentColor = BrandSky,
                    modifier = Modifier.weight(1f),
                )
                CurrencyTotalBox(
                    label = "美金總投入 (US$)",
                    formattedValue = Formatters.formatAmount(usdTotal, MarketCurrency.USD),
                    accentColor = AlertAmber,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CurrencyTotalBox(
    label: String,
    formattedValue: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardHigh,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                text = formattedValue,
                style = FinancialTextStyles.priceLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HoldingSummaryCard(
    holding: StockHoldingSummary,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        text = holding.symbol,
                        style = FinancialTextStyles.tickerBadge,
                        color = BrandSky,
                    )
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MarketBadge(currency = holding.currency)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "累計總股數",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = "${Formatters.formatShares(holding.totalShares)} 股 (${holding.recordCount} 筆)",
                        style = FinancialTextStyles.numericMedium,
                        color = TextPrimary,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "平均每股成本",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = Formatters.formatPrice(holding.averageCost, holding.currency),
                        style = FinancialTextStyles.numericMedium,
                        color = AlertAmber,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "總投入成本",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = Formatters.formatAmount(holding.totalCost, holding.currency),
                        style = FinancialTextStyles.numericMedium,
                        color = TextPrimary,
                    )
                }
            }

            val pnlPct = holding.unrealizedPnlPercent
            val cachedPrice = holding.latestCachedPrice
            if (pnlPct != null && cachedPrice != null) {
                val isProfit = pnlPct >= 0.0
                val pnlColor = if (isProfit) TaiwanRed else TaiwanGreen
                val pnlBg = if (isProfit) TaiwanRedContainer else TaiwanGreenContainer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "參考現價：${Formatters.formatPrice(cachedPrice, holding.currency)}",
                        style = FinancialTextStyles.numericSmall,
                        color = TextSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pnlBg,
                    ) {
                        Text(
                            text = "帳面報酬 ${Formatters.formatSignedPercent(pnlPct)}",
                            style = FinancialTextStyles.numericSmall,
                            color = pnlColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyRecordCard(
    record: BuyRecordEntity,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("buy_record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = record.buyDate,
                        style = FinancialTextStyles.numericSmall,
                        color = TextSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AlertAmberContainer,
                    ) {
                        Text(
                            text = record.triggerTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                MarketBadge(currency = record.currency)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${record.displaySymbol} · ${record.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${Formatters.formatShares(record.shares)} 股 × ${
                            Formatters.formatPrice(record.unitPrice, record.currency)
                        }",
                        style = FinancialTextStyles.numericSmall,
                        color = TextSecondary,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatAmount(record.totalCost, record.currency),
                        style = FinancialTextStyles.numericMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "點擊編輯/刪除",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandSky,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPortfolioCard(
    onManualAddClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = null,
                tint = BrandSky,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = "尚無成交買入紀錄",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "當監控股票觸發買入提醒時點擊「立即記錄買入」，或點擊右下角「+ 手動記帳」開始追蹤分批買入成本。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
