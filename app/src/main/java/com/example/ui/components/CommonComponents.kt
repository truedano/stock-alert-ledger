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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    fearAndGreed: com.example.domain.model.FearAndGreedData? = null,
    futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
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

                // 宏觀市場概況：美股恐懼貪婪 + 台股融資維持率 + 台指期籌碼
                val hasMargin = marketKeepRate != null && marketKeepRate > 0.0
                val hasFng = fearAndGreed != null
                if (hasFng || hasMargin || futuresChip != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MarketMacroSentimentPanel(
                        fearAndGreed = fearAndGreed,
                        marketKeepRate = marketKeepRate,
                        marketCallAccNum = marketCallAccNum,
                        marketExeAccNum = marketExeAccNum,
                        futuresChip = futuresChip,
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
    futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { showDialog = true },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 標題列
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌐 市場宏觀溫度計",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandSky,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "美股情緒 · 台股籌碼",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ⓘ",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandSky.copy(alpha = 0.8f),
                    )
                }
            }

            // 雙欄排版：左欄美股恐懼指數，右欄台股融資維持率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 美股 CNN 恐懼與貪婪指數
                Box(modifier = Modifier.weight(1f)) {
                    if (fearAndGreed != null) {
                        FearAndGreedMiniCard(data = fearAndGreed)
                    } else {
                        EmptySentimentPlaceholder(title = "美股恐懼與貪婪", subtitle = "暫無數據")
                    }
                }

                // 台股全市場融資維持率
                Box(modifier = Modifier.weight(1f)) {
                    if (marketKeepRate != null && marketKeepRate > 0.0) {
                        TaiwanMarginMiniCard(
                            keepRate = marketKeepRate,
                            callAccNum = marketCallAccNum,
                            exeAccNum = marketExeAccNum,
                        )
                    } else {
                        EmptySentimentPlaceholder(title = "台股融資維持率", subtitle = "台股休市或載入中")
                    }
                }
            }

            if (futuresChip != null) {
                TaiwanFuturesChipStrip(data = futuresChip)
            }
        }
    }

    if (showDialog) {
        MacroSentimentExplainDialog(
            fearAndGreed = fearAndGreed,
            marketKeepRate = marketKeepRate,
            futuresChip = futuresChip,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun FearAndGreedMiniCard(
    data: com.example.domain.model.FearAndGreedData,
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // 頂部標題
            Text(
                text = "美股恐懼貪婪",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )

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
                // 漸層底條
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

                // 游標指針 (白心外圈帶陰影)
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            // 頂部標題
            Text(
                text = "台股融資維持率",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )

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
private fun TaiwanFuturesChipStrip(
    data: com.example.domain.model.TaiwanFuturesChipSnapshot,
    modifier: Modifier = Modifier,
) {
    // 採台股紅多綠空慣例
    val foreignColor = when {
        data.foreignTxNetOi > 0L -> TaiwanRed
        data.foreignTxNetOi < 0L -> TaiwanGreen
        else -> TextSecondary
    }
    val foreignSign = if (data.foreignTxNetOi > 0L) "+" else ""
    val nf = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale.US) }
    val deltaText = if (data.foreignTxOiChange != null) {
        val sign = if (data.foreignTxOiChange > 0L) "+" else ""
        "(日 $sign${nf.format(data.foreignTxOiChange)})"
    } else {
        val sign = if (data.foreignTxTradingNet > 0L) "+" else ""
        "(交易 $sign${nf.format(data.foreignTxTradingNet)})"
    }

    fun ratioColor(ratio: Double?): Color = when {
        ratio == null -> TextPrimary
        ratio > 0.0 -> TaiwanRed
        ratio < 0.0 -> TaiwanGreen
        else -> TextSecondary
    }

    val mtxStr = data.mtxRetailRatio?.let { String.format(java.util.Locale.US, "%+.1f%%", it) } ?: "--"
    val tmfStr = data.tmfRetailRatio?.let { String.format(java.util.Locale.US, "%+.1f%%", it) } ?: "--"
    val retailAnnotated = buildAnnotatedString {
        withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
            append("小台 ")
        }
        withStyle(SpanStyle(color = ratioColor(data.mtxRetailRatio), fontWeight = FontWeight.Bold)) {
            append(mtxStr)
        }
        withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
            append(" · 微台 ")
        }
        withStyle(SpanStyle(color = ratioColor(data.tmfRetailRatio), fontWeight = FontWeight.Bold)) {
            append(tmfStr)
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "期交所盤後籌碼",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = TextMuted,
                )
                Text(
                    text = "${data.shortDateLabel} 盤後",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = BrandSky.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "外資台指期未平倉",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                        color = TextSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "$foreignSign${nf.format(data.foreignTxNetOi)} 口",
                            style = FinancialTextStyles.numericSmall,
                            color = foreignColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                            ),
                            color = TextMuted,
                            maxLines = 1,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "散戶多空比 (小台/微台)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                        color = TextSecondary,
                    )
                    Text(
                        text = retailAnnotated,
                        style = FinancialTextStyles.numericSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroSentimentExplainDialog(
    fearAndGreed: com.example.domain.model.FearAndGreedData?,
    marketKeepRate: Double?,
    futuresChip: com.example.domain.model.TaiwanFuturesChipSnapshot? = null,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌐 市場宏觀溫度計指南", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "【美股 CNN 恐懼與貪婪指數】",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandSky,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• 範圍 0 ~ 100，整合市場動能、波動率(VIX)、Put/Call Ratio 等 7 項宏觀數據。\n• < 25 為「極度恐慌」（巴菲特抄底區）；> 75 為「極度貪婪」（市場過熱警戒）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "【台股全市場融資維持率】",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandSky,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• < 130%：法定斷頭保證金追繳線。\n• < 150%：恐慌多殺多主跌段（融資萬人斷頭出清，經典長線抄底買點）。\n• ≥ 170%：籌碼健康安全水位，多頭格局熱絡。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "【台指期外資未平倉 ＆ 散戶多空比】",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandSky,
                    fontWeight = FontWeight.Bold
                )
                val futuresDetailText = if (futuresChip != null) {
                    val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
                    val mtxRatioStr = futuresChip.mtxRetailRatio?.let { String.format(java.util.Locale.US, "%+.2f%%", it) } ?: "--"
                    val tmfRatioStr = futuresChip.tmfRetailRatio?.let { String.format(java.util.Locale.US, "%+.2f%%", it) } ?: "--"
                    val oiChangeDesc = futuresChip.foreignTxOiChange?.let {
                        val sign = if (it > 0L) "+" else ""
                        "（日增減 $sign${nf.format(it)} 口，當日交易淨額 ${nf.format(futuresChip.foreignTxTradingNet)} 口）"
                    } ?: "（當日交易淨額 ${nf.format(futuresChip.foreignTxTradingNet)} 口）"
                    "• 資料日 ${futuresChip.formattedDate}（期交所每日一般盤盤後結算公布一次，約 15:15 後更新，非盤中即時報價）。\n" +
                        "• 大台未平倉：外資 ${nf.format(futuresChip.foreignTxNetOi)} 口$oiChangeDesc、投信 ${nf.format(futuresChip.trustTxNetOi)} 口、自營商 ${nf.format(futuresChip.dealerTxNetOi)} 口。\n" +
                        "• 小台散戶淨口數 ${nf.format(futuresChip.mtxRetailNetOi)} / 總 OI ${nf.format(futuresChip.mtxTotalOi)}（$mtxRatioStr）；微台散戶淨口數 ${nf.format(futuresChip.tmfRetailNetOi)} / 總 OI ${nf.format(futuresChip.tmfTotalOi)}（$tmfRatioStr）。\n" +
                        "• 採台股紅多綠空配色；散戶多空比常作反指標：正值紅字過高代表散戶偏多易震盪；負值綠字過低代表散戶大舉做空易軋空。"
                } else {
                    "• 統計期交所盤後三大法人期貨未平倉與全市場總未平倉（每日約 15:15 後更新，非盤中即時；小台/微台反推散戶淨部位）。"
                }
                Text(
                    text = futuresDetailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "了解", color = BrandSky)
            }
        },
        containerColor = SurfaceElevated,
    )
}

@Composable
private fun EmptySentimentPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}
