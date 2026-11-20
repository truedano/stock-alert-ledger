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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
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

                if (marketKeepRate != null && marketKeepRate > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isPanic = marketKeepRate < 150.0
                    val isWarm = marketKeepRate >= 170.0
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
                        isPanic -> "🚨 極端恐慌斷頭區 (<150%)"
                        isWarm -> "🟢 籌碼安全水位 (≥170%)"
                        else -> "⚠️ 融資警戒調整區 (150%~170%)"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeBg.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "全市場融資維持率：${String.format(java.util.Locale.US, "%.1f", marketKeepRate)}%",
                                    style = FinancialTextStyles.numericMedium,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            if (marketCallAccNum != null && marketExeAccNum != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "追繳 $marketCallAccNum 戶",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        text = "處分 $marketExeAccNum 戶",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (marketExeAccNum > 100) TaiwanRed else TextSecondary,
                                        fontWeight = if (marketExeAccNum > 100) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
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
