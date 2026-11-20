package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WatchlistRuleEntity
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.ui.WatchlistItemUi
import com.example.ui.WatchlistUiState
import com.example.ui.components.MarketBadge
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.WatchlistStatusHeader
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    uiState: WatchlistUiState,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRefresh: () -> Unit,
    onAddClick: () -> Unit,
    onEditRule: (WatchlistItemUi) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onResetTrigger: (Long) -> Unit,
    onQuickBuyClick: (WatchlistItemUi) -> Unit,
    onAboutAppClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasObsidian,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = BrandSky,
                contentColor = CanvasObsidian,
                modifier = Modifier.testTag("add_watchlist_fab"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "新增監控項目",
                )
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                        bottom = 92.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header_title") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "價格與指標監控",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "台股／美股即時追蹤，觸發價格或技術指標條件立即推播",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                            IconButton(onClick = onAboutAppClick) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "軟體資訊與檢查更新",
                                    tint = BrandSky
                                )
                            }
                        }
                    }

                    if (!hasNotificationPermission) {
                        item(key = "permission_banner") {
                            NotificationPermissionBanner(
                                onGrantClick = onRequestNotificationPermission,
                            )
                        }
                    }

                    item(key = "status_header") {
                        WatchlistStatusHeader(
                            mode = uiState.dataSourceMode,
                            lastSyncTimestamp = uiState.lastSyncTimestamp,
                            lastSyncMessage = uiState.lastSyncMessage,
                            isRefreshing = uiState.isRefreshing,
                            hasRateLimitWarning = uiState.hasRateLimitWarning,
                            onRefreshClick = onRefresh,
                            marketKeepRate = uiState.marketKeepRate,
                            marketCallAccNum = uiState.marketCallAccNum,
                            marketExeAccNum = uiState.marketExeAccNum,
                        )
                    }

                    if (uiState.items.isEmpty()) {
                        item(key = "empty_watchlist") {
                            EmptyWatchlistCard(onAddClick = onAddClick)
                        }
                    } else {
                        items(
                            items = uiState.items,
                            key = { it.rule.id },
                            contentType = { "watchlist_rule_card" },
                        ) { item ->
                            WatchlistRuleCard(
                                item = item,
                                onToggleEnabled = { enabled ->
                                    onToggleEnabled(item.rule.id, enabled)
                                },
                                onEdit = { onEditRule(item) },
                                onDelete = { onDeleteRule(item.rule.id) },
                                onResetTrigger = { onResetTrigger(item.rule.id) },
                                onQuickBuy = { onQuickBuyClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchlistCard(
    onAddClick: () -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardHigh,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                        contentDescription = null,
                        tint = BrandSky,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Text(
                text = "尚未建立任何監控規則",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "點擊右下角「+」加入台股（如 2330、0050）或美股（如 AAPL、NVDA），也可先至「資料與模擬」體驗測試通知流程。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("新增第一檔監控")
            }
        }
    }
}

@Composable
private fun WatchlistRuleCard(
    item: WatchlistItemUi,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetTrigger: () -> Unit,
    onQuickBuy: () -> Unit,
) {
    val rule: WatchlistRuleEntity = item.rule
    val quote = item.quote
    val eval = item.evaluation
    val isTriggered = rule.isTriggered

    val borderColor = when {
        isTriggered -> AlertAmber
        !rule.isEnabled -> HairlineBorder.copy(alpha = 0.5f)
        else -> HairlineBorder
    }

    val cardBrush = if (isTriggered) {
        Brush.verticalGradient(
            colors = listOf(
                AlertAmber.copy(alpha = 0.14f),
                SurfaceCard,
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SurfaceCard, SurfaceCard)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watchlist_card_${rule.id}"),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(if (isTriggered) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = rule.displaySymbol,
                            style = FinancialTextStyles.tickerBadge,
                            color = BrandSky,
                        )
                        MarketBadge(currency = rule.currency)
                        if (isTriggered) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AlertAmberContainer,
                                border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = "已觸及條件",
                                        tint = AlertAmber,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "已觸及條件！",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AlertAmber,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (rule.isEnabled) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("rule_switch_${rule.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CanvasObsidian,
                        checkedTrackColor = BrandSky,
                    ),
                )
            }

            // Price & Moving Average Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCardHigh.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "目前現價",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = quote?.let { Formatters.formatPrice(it.currentPrice, rule.currency) }
                            ?: "尚無報價",
                        style = FinancialTextStyles.priceLarge,
                        color = TextPrimary,
                    )
                }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (rule.condition) {
                                is com.example.domain.model.PriceTargetCondition -> "目標價"
                                else -> eval?.targetReferenceLabel ?: rule.condition.conditionDisplayName
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = when {
                                rule.condition is com.example.domain.model.RsiCondition ||
                                rule.condition is com.example.domain.model.MarginMaintenanceCondition -> {
                                    eval?.targetReferencePrice?.let {
                                        "${String.format(java.util.Locale.US, "%.1f", it)}%"
                                    } ?: "--"
                                }
                                else -> {
                                    eval?.targetReferencePrice?.let {
                                        Formatters.formatPrice(it, rule.currency)
                                    } ?: "--"
                                }
                            },
                            style = FinancialTextStyles.numericMedium,
                            color = BrandSky,
                        )
                        val dist = eval?.distancePercent
                        if (dist != null) {
                            val isPositive = dist > 0.0001
                            val pillColor = if (isPositive) TaiwanRed else TaiwanGreen
                            val pillBg = if (isPositive) TaiwanRedContainer else TaiwanGreenContainer
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pillBg,
                            ) {
                                val distLabel = when (rule.condition) {
                                    is com.example.domain.model.RsiCondition -> {
                                        val sign = if (dist > 0) "+" else ""
                                        "差額 $sign${String.format(java.util.Locale.US, "%.1f", dist)}"
                                    }
                                    is com.example.domain.model.MarginMaintenanceCondition -> {
                                        val sign = if (dist > 0) "+" else ""
                                        "反彈 $sign${String.format(java.util.Locale.US, "%.1f", dist)} 點"
                                    }
                                    else -> {
                                        val prefix = when {
                                            rule.condition is com.example.domain.model.PriceTargetCondition -> "距目標 "
                                            else -> "距MA "
                                        }
                                        "$prefix${Formatters.formatSignedPercent(dist)}"
                                    }
                                }
                                Text(
                                    text = distLabel,
                                    style = FinancialTextStyles.numericSmall,
                                    color = pillColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        } else if (eval?.isInsufficientData == true) {
                            Text(
                                text = eval.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertAmber,
                            )
                        }
                    }
            }

            // 個股融資資訊與推估維持率區塊 (方案 A + 方案 B)
            val marginInfo = quote?.marginInfo
            if (rule.currency == MarketCurrency.TWD && marginInfo != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCardHigh.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, HairlineBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // 方案 A：融資餘額與日增減
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrandSkyContainer,
                            ) {
                                Text(
                                    text = "資",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSky,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                            val marginBalStr = if (marginInfo.marginBalance > 0L) {
                                "${Formatters.formatShares(marginInfo.marginBalance.toDouble())} 張"
                            } else {
                                "--"
                            }
                            val changeSign = if (marginInfo.marginChange > 0) "+" else ""
                            val changeColor = when {
                                marginInfo.marginChange > 0 -> TaiwanRed
                                marginInfo.marginChange < 0 -> TaiwanGreen
                                else -> TextSecondary
                            }
                            Text(
                                text = marginBalStr,
                                style = FinancialTextStyles.numericSmall,
                                color = TextPrimary,
                            )
                            if (marginInfo.marginBalance > 0L && marginInfo.marginChange != 0L) {
                                Text(
                                    text = "($changeSign${marginInfo.marginChange})",
                                    style = FinancialTextStyles.numericSmall,
                                    color = changeColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // 方案 B：推估個股融資維持率
                        val estRate = marginInfo.estimatedKeepRate
                        if (estRate != null) {
                            val rateColor = when {
                                estRate < 135.0 -> TaiwanRed
                                estRate < 150.0 -> AlertAmber
                                else -> TaiwanGreen
                            }
                            val rateBg = when {
                                estRate < 135.0 -> TaiwanRedContainer
                                estRate < 150.0 -> AlertAmberContainer
                                else -> TaiwanGreenContainer
                            }
                            val tagText = when {
                                estRate < 130.0 -> "面臨追繳"
                                estRate < 140.0 -> "高度警戒"
                                estRate < 150.0 -> "調整警戒"
                                else -> "安全"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = rateBg,
                                border = BorderStroke(1.dp, rateColor.copy(alpha = 0.35f)),
                            ) {
                                Text(
                                    text = "推估維持率 ${String.format(java.util.Locale.US, "%.1f", estRate)}% · $tagText",
                                    style = FinancialTextStyles.numericSmall,
                                    color = rateColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Condition & Planned Budget Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 條件標籤化 (Badge) & 觸發狀態聯動高亮
                    val conditionBadgeBg = if (isTriggered) AlertAmberContainer else SurfaceCardHigh
                    val conditionBadgeBorder = if (isTriggered) AlertAmber else HairlineBorder
                    val conditionTextColor = if (isTriggered) AlertAmber else TextPrimary
                    val conditionLabel = when (val cond = rule.condition) {
                        is com.example.domain.model.MovingAverageCondition -> {
                            val modeStr = if (cond.triggerMode == com.example.domain.model.MovingAverageTriggerMode.BELOW_OR_EQUAL) {
                                "跌破/觸及"
                            } else {
                                "接近(±${cond.tolerancePercent}%)"
                            }
                            val periodName = com.example.domain.model.MovingAverageCondition.maPeriodLabel(cond.periodDays)
                            "$modeStr $periodName"
                        }
                        else -> rule.condition.conditionDisplayName
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = conditionBadgeBg,
                        border = BorderStroke(1.dp, conditionBadgeBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (isTriggered) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = AlertAmber,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            Text(
                                text = conditionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = conditionTextColor,
                                fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }

                    // 預定買入與股數主次分離
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "預定買入",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = Formatters.formatAmount(rule.plannedBuyAmount, rule.currency),
                            style = FinancialTextStyles.numericSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        item.estimatedShares?.let { shares ->
                            Text(
                                text = "(${Formatters.formatShares(shares)} 股)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "編輯規則",
                            tint = TextSecondary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "刪除規則",
                            tint = TextSecondary,
                        )
                    }
                }
            }

            // Triggered Quick-Action Bar
            AnimatedVisibility(visible = isTriggered) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = AlertAmber.copy(alpha = 0.25f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onQuickBuy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_buy_button_${rule.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertAmber,
                                contentColor = CanvasObsidian,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = "立即記錄買入",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        OutlinedButton(
                            onClick = onResetTrigger,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reset_trigger_button_${rule.id}"),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "重置提醒狀態",
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置提醒")
                        }
                    }
                }
            }
        }
    }
}
