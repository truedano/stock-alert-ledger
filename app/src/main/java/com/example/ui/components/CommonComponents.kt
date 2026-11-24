package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataSourceMode
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MarketBadge(
    currency: MarketCurrency,
    modifier: Modifier = Modifier,
) {
    val bg = if (currency == MarketCurrency.TWD) BrandSkyContainer else SurfaceCardHigh
    val fg = if (currency == MarketCurrency.TWD) BrandSky else TextSecondary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
    ) {
        Text(
            text = "${currency.marketTag} ${currency.code}",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun DataModeBadge(
    mode: DataSourceMode,
    modifier: Modifier = Modifier,
) {
    val isSim = mode == DataSourceMode.SIMULATED
    val dotColor = if (isSim) AlertAmber else TaiwanGreen
    val bgColor = if (isSim) AlertAmberContainer else TaiwanGreenContainer
    val label = if (isSim) "模擬測試中" else "真實報價"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = dotColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun NotificationPermissionBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AlertAmberContainer,
        border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = "通知權限提醒",
                    tint = AlertAmber,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "尚未開啟系統通知權限",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "開啟後才能在股價觸發監控條件時即時彈出提醒",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onGrantClick,
                modifier = Modifier.testTag("grant_notification_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertAmber,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "開啟通知權限",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun WatchlistStatusHeader(
    mode: DataSourceMode,
    lastSyncTimestamp: Long,
    lastSyncMessage: String,
    isRefreshing: Boolean,
    hasRateLimitWarning: Boolean,
    onRefreshClick: () -> Unit,
    marketKeepRate: Double? = null,
    marketCallAccNum: Long? = null,
    marketExeAccNum: Long? = null,
    marketMargin: com.example.data.remote.MarketMarginSnapshot? = null,
    fearAndGreed: com.example.domain.model.FearAndGreedData? = null,
    futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
    watchlistItems: List<com.example.ui.WatchlistItemUi> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BrandSky.copy(alpha = 0.08f),
                            SurfaceCard,
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DataModeBadge(mode = mode)
                    FilledTonalButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_watchlist_button"),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BrandSky,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "立即重新整理",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "更新中..." else "立即整理",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "最後更新：${Formatters.formatDateTime(lastSyncTimestamp)}",
                    style = FinancialTextStyles.numericSmall,
                    color = TextSecondary,
                )

                // 宏觀市場概況：2x2 獨立可點擊四宮格（美股恐懼貪婪 + 台股融資維持率 + 外資期貨未平倉 + 散戶多空比）
                val effectiveKeepRate = marketMargin?.keepRate ?: marketKeepRate
                val hasMargin = effectiveKeepRate != null && effectiveKeepRate > 0.0
                val hasFng = fearAndGreed != null
                if (hasFng || hasMargin || futuresChip != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MarketMacroSentimentPanel(
                        fearAndGreed = fearAndGreed,
                        marketKeepRate = effectiveKeepRate,
                        marketCallAccNum = marketMargin?.callAccNum ?: marketCallAccNum,
                        marketExeAccNum = marketMargin?.exeAccNum ?: marketExeAccNum,
                        marketMargin = marketMargin,
                        futuresChip = futuresChip,
                        watchlistItems = watchlistItems,
                    )
                }

                if (hasRateLimitWarning) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TaiwanRedContainer,
                        border = BorderStroke(1.dp, TaiwanRed.copy(alpha = 0.35f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "連線提示",
                                tint = TaiwanRed,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lastSyncMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Text(
                        text = lastSyncMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun MarketMacroSentimentPanel(
    fearAndGreed: com.example.domain.model.FearAndGreedData?,
    marketKeepRate: Double?,
    marketCallAccNum: Long?,
    marketExeAccNum: Long?,
    marketMargin: com.example.data.remote.MarketMarginSnapshot? = null,
    futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
    watchlistItems: List<com.example.ui.WatchlistItemUi> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var selectedDetailTab by remember { mutableStateOf<MacroIndicatorTab?>(null) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 標題列
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "🌐 市場宏觀溫度計",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandSky,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "點擊任一指標看細項 ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            // 第一列：美股恐懼貪婪 ｜ 台股融資維持率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (fearAndGreed != null) {
                        FearAndGreedMiniCard(
                            data = fearAndGreed,
                            onClick = { selectedDetailTab = MacroIndicatorTab.FEAR_AND_GREED },
                            modifier = Modifier.testTag("macro_card_fng"),
                        )
                    } else {
                        EmptySentimentPlaceholder(
                            title = "美股恐懼貪婪",
                            subtitle = "暫無數據",
                            onClick = { selectedDetailTab = MacroIndicatorTab.FEAR_AND_GREED },
                            modifier = Modifier.testTag("macro_card_fng"),
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (marketKeepRate != null && marketKeepRate > 0.0) {
                        TaiwanMarginMiniCard(
                            keepRate = marketKeepRate,
                            callAccNum = marketCallAccNum,
                            exeAccNum = marketExeAccNum,
                            onClick = { selectedDetailTab = MacroIndicatorTab.TW_MARGIN },
                            modifier = Modifier.testTag("macro_card_margin"),
                        )
                    } else {
                        EmptySentimentPlaceholder(
                            title = "台股融資維持率",
                            subtitle = "台股休市或載入中",
                            onClick = { selectedDetailTab = MacroIndicatorTab.TW_MARGIN },
                            modifier = Modifier.testTag("macro_card_margin"),
                        )
                    }
                }
            }

            // 第二列：外資台指期未平倉 ｜ 散戶多空比 (小台/微台)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (futuresChip != null) {
                        ForeignFuturesMiniCard(
                            data = futuresChip,
                            onClick = { selectedDetailTab = MacroIndicatorTab.FOREIGN_FUTURES },
                            modifier = Modifier.testTag("macro_card_foreign_futures"),
                        )
                    } else {
                        EmptySentimentPlaceholder(
                            title = "外資台指期未平倉",
                            subtitle = "盤後載入中",
                            onClick = { selectedDetailTab = MacroIndicatorTab.FOREIGN_FUTURES },
                            modifier = Modifier.testTag("macro_card_foreign_futures"),
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (futuresChip != null) {
                        RetailFuturesMiniCard(
                            data = futuresChip,
                            onClick = { selectedDetailTab = MacroIndicatorTab.RETAIL_RATIO },
                            modifier = Modifier.testTag("macro_card_retail_futures"),
                        )
                    } else {
                        EmptySentimentPlaceholder(
                            title = "散戶多空比",
                            subtitle = "盤後載入中",
                            onClick = { selectedDetailTab = MacroIndicatorTab.RETAIL_RATIO },
                            modifier = Modifier.testTag("macro_card_retail_futures"),
                        )
                    }
                }
            }
        }
    }

    selectedDetailTab?.let { initialTab ->
        MacroIndicatorDetailSheet(
            initialTab = initialTab,
            fearAndGreed = fearAndGreed,
            marketMargin = marketMargin,
            marketKeepRate = marketKeepRate,
            marketCallAccNum = marketCallAccNum,
            marketExeAccNum = marketExeAccNum,
            futuresChip = futuresChip,
            watchlistItems = watchlistItems,
            onDismiss = { selectedDetailTab = null },
        )
    }
}

@Composable
private fun MiniCardHeaderRow(
    title: String,
    accentColor: Color = BrandSky,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.labelMedium,
            color = accentColor.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FearAndGreedMiniCard(
    data: com.example.domain.model.FearAndGreedData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classification = data.ratingClassification
    val primaryColor = classification.primaryColor
    val containerColor = classification.containerColor
    val scoreFraction = (data.roundedScore / 100f).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            MiniCardHeaderRow(title = "美股恐懼貪婪", accentColor = primaryColor)

            Spacer(modifier = Modifier.height(4.dp))

            // 數值 + Mini Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${data.roundedScore}",
                    style = FinancialTextStyles.priceLarge,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = primaryColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = classification.displayNameZh,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 漸層色階底條 + 當前指標指針
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    TaiwanRed,
                                    AlertAmber,
                                    Color(0xFF64748B),
                                    Color(0xFF34D399),
                                    TaiwanGreen
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = scoreFraction)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(8.dp)
                            .background(Color.White, shape = CircleShape)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部對比數值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = data.previousClose?.let { "昨收 ${it.toInt()}" } ?: "--",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Text(
                    text = data.previousOneWeek?.let { "上週 ${it.toInt()}" } ?: "--",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun TaiwanMarginMiniCard(
    keepRate: Double,
    callAccNum: Long?,
    exeAccNum: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPanic = keepRate < 150.0
    val isWarm = keepRate >= 170.0
    val badgeColor = when {
        isPanic -> TaiwanRed
        isWarm -> TaiwanGreen
        else -> AlertAmber
    }
    val badgeBg = when {
        isPanic -> TaiwanRedContainer
        isWarm -> TaiwanGreenContainer
        else -> AlertAmberContainer
    }
    val statusText = when {
        isPanic -> "斷頭恐慌"
        isWarm -> "籌碼安全"
        else -> "警戒整理"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = badgeBg.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            MiniCardHeaderRow(title = "台股融資維持率", accentColor = badgeColor)

            Spacer(modifier = Modifier.height(4.dp))

            // 數值 + Mini Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${String.format(java.util.Locale.US, "%.1f", keepRate)}%",
                    style = FinancialTextStyles.numericMedium,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 維持率警戒刻度指示條 (基準 130% ~ 180%)
            val progressFraction = ((keepRate - 130.0) / (180.0 - 130.0)).toFloat().coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xFF27354F), shape = RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(4.dp)
                            .background(badgeColor, shape = RoundedCornerShape(2.dp))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(8.dp)
                            .background(Color.White, shape = CircleShape)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部追繳與處分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = callAccNum?.let { "追繳 ${it}戶" } ?: "--",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = TextMuted,
                    maxLines = 1,
                )
                Text(
                    text = exeAccNum?.let { "處分 ${it}戶" } ?: "--",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = if (exeAccNum != null && exeAccNum > 100) TaiwanRed else TextMuted,
                    fontWeight = if (exeAccNum != null && exeAccNum > 100) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ForeignFuturesMiniCard(
    data: com.example.domain.model.TaiwanFuturesChipSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foreignColor = when {
        data.foreignTxNetOi > 0L -> TaiwanRed
        data.foreignTxNetOi < 0L -> TaiwanGreen
        else -> TextSecondary
    }
    val foreignSign = if (data.foreignTxNetOi > 0L) "+" else ""
    val nf = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale.US) }
    val deltaText = if (data.foreignTxOiChange != null) {
        val sign = if (data.foreignTxOiChange > 0L) "+" else ""
        "日 $sign${nf.format(data.foreignTxOiChange)}"
    } else {
        val sign = if (data.foreignTxTradingNet > 0L) "+" else ""
        "交 $sign${nf.format(data.foreignTxTradingNet)}"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, foreignColor.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniCardHeaderRow(title = "外資台指期未平倉", accentColor = foreignColor)

            Text(
                text = "$foreignSign${nf.format(data.foreignTxNetOi)} 口",
                style = FinancialTextStyles.numericMedium,
                color = foreignColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = TextMuted,
                    maxLines = 1,
                )
                Text(
                    text = "${data.shortDateLabel} 盤後",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = BrandSky.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun RetailFuturesMiniCard(
    data: com.example.domain.model.TaiwanFuturesChipSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val combined = data.combinedRetailRatio
    fun ratioColor(ratio: Double?): Color = when {
        ratio == null -> TextPrimary
        ratio > 0.0 -> TaiwanRed
        ratio < 0.0 -> TaiwanGreen
        else -> TextSecondary
    }

    val combinedColor = ratioColor(combined)
    val combinedStr = combined?.let { String.format(java.util.Locale.US, "%+.1f%%", it) } ?: "--"
    val mtxStr = data.mtxRetailRatio?.let { String.format(java.util.Locale.US, "%+.1f%%", it) } ?: "--"
    val tmfStr = data.tmfRetailRatio?.let { String.format(java.util.Locale.US, "%+.1f%%", it) } ?: "--"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, combinedColor.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniCardHeaderRow(title = "散戶多空比 (綜合)", accentColor = combinedColor)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = combinedStr,
                    style = FinancialTextStyles.numericMedium,
                    color = combinedColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = combinedColor.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = when {
                            combined == null -> "--"
                            combined > 5.0 -> "偏多"
                            combined < -5.0 -> "偏空"
                            else -> "中性"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                        color = combinedColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "小台 $mtxStr",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = ratioColor(data.mtxRetailRatio),
                    maxLines = 1,
                )
                Text(
                    text = "微台 $tmfStr",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = ratioColor(data.tmfRetailRatio),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptySentimentPlaceholder(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            MiniCardHeaderRow(title = title, accentColor = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}
