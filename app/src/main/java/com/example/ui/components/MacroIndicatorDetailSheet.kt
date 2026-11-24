package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.MarketMarginSnapshot
import com.example.domain.model.FearAndGreedData
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.TaiwanFuturesChipSnapshot
import com.example.domain.model.classifySentiment
import com.example.ui.WatchlistItemUi
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class MacroIndicatorTab(
    val title: String,
    val shortTitle: String,
) {
    FEAR_AND_GREED("美股恐懼與貪婪指數", "恐懼貪婪"),
    TW_MARGIN("台股全市場融資維持率", "融資維持率"),
    FOREIGN_FUTURES("外資台指期未平倉", "外資期貨"),
    RETAIL_RATIO("散戶多空比 (小台/微台)", "散戶多空比"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroIndicatorDetailSheet(
    initialTab: MacroIndicatorTab,
    fearAndGreed: FearAndGreedData?,
    marketMargin: MarketMarginSnapshot?,
    marketKeepRate: Double?,
    marketCallAccNum: Long?,
    marketExeAccNum: Long?,
    futuresChip: TaiwanFuturesChipSnapshot?,
    watchlistItems: List<WatchlistItemUi>,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceElevated,
        contentColor = TextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🌐 ${selectedTab.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "點擊上方標籤可快速切換四項宏觀指標明細",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "關閉明細",
                        tint = TextSecondary,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MacroIndicatorTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        modifier = Modifier.testTag("macro_tab_${tab.name}"),
                        label = {
                            Text(
                                text = tab.shortTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandSkyContainer,
                            selectedLabelColor = BrandSky,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary,
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    MacroIndicatorTab.FEAR_AND_GREED -> FearAndGreedDetailContent(fearAndGreed)
                    MacroIndicatorTab.TW_MARGIN -> TaiwanMarginDetailContent(
                        marketMargin = marketMargin,
                        fallbackKeepRate = marketKeepRate,
                        fallbackCallAccNum = marketCallAccNum,
                        fallbackExeAccNum = marketExeAccNum,
                        watchlistItems = watchlistItems,
                    )
                    MacroIndicatorTab.FOREIGN_FUTURES -> ForeignFuturesDetailContent(futuresChip)
                    MacroIndicatorTab.RETAIL_RATIO -> RetailRatioDetailContent(futuresChip)
                }
            }
        }
    }
}

@Composable
private fun FearAndGreedDetailContent(
    data: FearAndGreedData?,
) {
    if (data == null) {
        DetailEmptyCard(
            title = "尚無美股恐懼與貪婪指數資料",
            description = "請點擊主畫面上方的「立即整理」或確認網路連線後再試一次。",
        )
        return
    }

    val classification = data.ratingClassification
    val primaryColor = classification.primaryColor
    val scoreFraction = (data.roundedScore / 100f).coerceIn(0f, 1f)

    // 主分數卡片
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = classification.containerColor.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "CNN 即時市場情緒分數",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${data.roundedScore}",
                            style = FinancialTextStyles.priceHero,
                            color = primaryColor,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = classification.displayNameZh,
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (data.timestamp.isNotBlank()) {
                    Text(
                        text = data.timestamp.take(10),
                        style = FinancialTextStyles.numericSmall,
                        color = TextMuted,
                    )
                }
            }

            // 漸層進度條 + 指針
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        TaiwanRed,
                                        AlertAmber,
                                        Color(0xFF64748B),
                                        Color(0xFF34D399),
                                        TaiwanGreen,
                                    )
                                ),
                                shape = RoundedCornerShape(3.dp),
                            )
                    )
                    Box(modifier = Modifier.fillMaxWidth(fraction = scoreFraction)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0 極度恐慌", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("50 中立", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("100 極度貪婪", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }

    // 歷史跨度對比
    DetailSectionCard(title = "歷史時間軸對比") {
        val comparisons = listOf(
            "昨收" to data.previousClose,
            "上週" to data.previousOneWeek,
            "上個月" to data.previousOneMonth,
            "去年同期" to data.previousOneYear,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            comparisons.forEach { (label, prevScore) ->
                val prevInt = prevScore?.toInt()?.coerceIn(0, 100)
                val delta = prevScore?.let { data.score - it }
                val prevClass = prevScore?.let { classifySentiment(it) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCardHigh.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = prevInt?.toString() ?: "--",
                            style = FinancialTextStyles.numericMedium,
                            color = prevClass?.primaryColor ?: TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        val deltaStr = delta?.let {
                            val rounded = it.roundToInt()
                            if (rounded > 0) "+$rounded" else "$rounded"
                        } ?: "--"
                        val deltaColor = when {
                            delta == null -> TextMuted
                            delta > 0.5 -> TaiwanGreen
                            delta < -0.5 -> TaiwanRed
                            else -> TextSecondary
                        }
                        Text(
                            text = deltaStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    // 7 大組成子指標
    DetailSectionCard(title = "7 大組成子指標拆解") {
        if (data.subIndicators.isEmpty()) {
            Text(
                text = "目前尚無子指標細項（請點擊「立即整理」更新最新完整數據）。",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.subIndicators.forEachIndexed { index, sub ->
                    val subClass = sub.classification
                    val barColor = subClass.primaryColor
                    val fraction = (sub.roundedScore / 100f).coerceIn(0.03f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.nameZh,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = sub.descriptionZh,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${sub.roundedScore}",
                                    style = FinancialTextStyles.numericMedium,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = barColor.copy(alpha = 0.16f),
                                ) {
                                    Text(
                                        text = subClass.displayNameZh,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = barColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(SurfaceCardHigh, shape = RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(5.dp)
                                    .background(barColor, shape = RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    if (index < data.subIndicators.lastIndex) {
                        HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    DetailGuideCard(
        title = "指標判讀指南",
        bullets = listOf(
            "分數介於 0 ~ 100，綜合 S&P500 動能、VIX 波動率、Put/Call 比與債市利差。",
            "< 25「極度恐慌」：市場情緒過度悲觀，往往是長線分批布局區間。",
            "> 75「極度貪婪」：追價過熱，適合檢視部位停利或提高警覺。",
        ),
    )
}

@Composable
private fun TaiwanMarginDetailContent(
    marketMargin: MarketMarginSnapshot?,
    fallbackKeepRate: Double?,
    fallbackCallAccNum: Long?,
    fallbackExeAccNum: Long?,
    watchlistItems: List<WatchlistItemUi>,
) {
    val keepRate = marketMargin?.keepRate ?: fallbackKeepRate
    if (keepRate == null || keepRate <= 0.0) {
        DetailEmptyCard(
            title = "台股融資維持率載入中或休市",
            description = "證交所每日收盤後公布全市場信用交易統計，請點擊「立即整理」重試。",
        )
        return
    }

    val callAcc = marketMargin?.callAccNum ?: fallbackCallAccNum
    val exeAcc = marketMargin?.exeAccNum ?: fallbackExeAccNum
    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }

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

    // 主維持率卡片
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = badgeBg.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
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
                Column {
                    Text(
                        text = "全市場整戶擔保維持率",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", keepRate)}%",
                            style = FinancialTextStyles.priceHero,
                            color = badgeColor,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    marketMargin?.date?.takeIf { it.isNotBlank() }?.let { dateStr ->
                        Text(
                            text = dateStr,
                            style = FinancialTextStyles.numericSmall,
                            color = TextMuted,
                        )
                    }
                    marketMargin?.keepRateChange?.let { diff ->
                        val sign = if (diff > 0.0) "+" else ""
                        val diffColor = if (diff >= 0.0) TaiwanGreen else TaiwanRed
                        Text(
                            text = "日變動 $sign${String.format(Locale.US, "%.2f", diff)}%",
                            style = FinancialTextStyles.numericSmall,
                            color = diffColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    // 近期維持率走勢圖
    val trendRates = remember(marketMargin) {
        val pts = marketMargin?.historyPoints.orEmpty()
        if (pts.size >= 2) {
            pts.map { it.date to it.keepRate }
        } else {
            marketMargin?.recentKeepRates.orEmpty().mapIndexed { idx, rate -> "#${idx + 1}" to rate }
        }
    }
    if (trendRates.size >= 2) {
        DetailSectionCard(title = "近期維持率走勢 (${trendRates.size} 交易日)") {
            MarginKeepRateTrendChart(points = trendRates)
        }
    }

    // 信用壓力與戶數明細 (2x2)
    DetailSectionCard(title = "全市場信用壓力明細") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    label = "低於警戒戶數",
                    value = marketMargin?.belowAccNum?.takeIf { it > 0L }?.let { "${nf.format(it)} 戶" } ?: "--",
                    valueColor = if ((marketMargin?.belowAccNum ?: 0L) > 0L) AlertAmber else TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "發出追繳戶數",
                    value = callAcc?.let { "${nf.format(it)} 戶" } ?: "--",
                    subValue = marketMargin?.callAmt?.takeIf { it > 0L }?.let { formatTwAmt(it) },
                    valueColor = if ((callAcc ?: 0L) > 0L) AlertAmber else TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    label = "處分 (斷頭) 戶數",
                    value = exeAcc?.let { "${nf.format(it)} 戶" } ?: "--",
                    subValue = marketMargin?.exeAmt?.takeIf { it > 0L }?.let { formatTwAmt(it) },
                    valueColor = if ((exeAcc ?: 0L) > 100L) TaiwanRed else TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                val minRate = trendRates.minOfOrNull { it.second }
                val maxRate = trendRates.maxOfOrNull { it.second }
                val crdLabel = if ((marketMargin?.crdAmt ?: 0L) > 0L) "全市場融資餘額" else "近期維持率區間"
                val crdValue = if ((marketMargin?.crdAmt ?: 0L) > 0L) {
                    formatTwAmt(marketMargin!!.crdAmt)
                } else if (minRate != null && maxRate != null) {
                    "${String.format(Locale.US, "%.1f", minRate)}~${String.format(Locale.US, "%.1f", maxRate)}%"
                } else {
                    "--"
                }
                val crdSub = marketMargin?.crdAmtChange?.let { chg ->
                    val sign = if (chg > 0L) "+" else ""
                    "日增減 $sign${formatTwAmt(chg)}"
                }
                MetricTile(
                    label = crdLabel,
                    value = crdValue,
                    subValue = crdSub,
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // 自選台股融資速覽
    val twMarginStocks = remember(watchlistItems) {
        watchlistItems
            .filter { it.rule.currency == MarketCurrency.TWD && it.quote?.marginInfo != null }
            .distinctBy { it.rule.symbol }
    }
    if (twMarginStocks.isNotEmpty()) {
        DetailSectionCard(title = "自選台股融資籌碼速覽") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                twMarginStocks.forEachIndexed { idx, item ->
                    val info = item.quote!!.marginInfo!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.rule.displaySymbol} ${item.rule.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val balStr = if (info.marginBalance > 0L) {
                                val chgSign = if (info.marginChange > 0L) "+" else ""
                                val chgPart = if (info.marginChange != 0L) " ($chgSign${nf.format(info.marginChange)})" else ""
                                "資餘 ${nf.format(info.marginBalance)} 張$chgPart"
                            } else {
                                "尚無個股融資餘額"
                            }
                            Text(
                                text = balStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }

                        info.estimatedKeepRate?.let { est ->
                            val rateColor = when {
                                est < 135.0 -> TaiwanRed
                                est < 150.0 -> AlertAmber
                                else -> TaiwanGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = rateColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, rateColor.copy(alpha = 0.35f)),
                            ) {
                                Text(
                                    text = "推估 ${String.format(Locale.US, "%.1f", est)}%",
                                    style = FinancialTextStyles.numericSmall,
                                    color = rateColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    if (idx < twMarginStocks.lastIndex) {
                        HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    DetailGuideCard(
        title = "維持率判讀指南",
        bullets = listOf(
            "130% 為法定整戶追繳門檻；低於 130% 且未補繳將面臨斷頭處分。",
            "< 150%：市場進入多殺多洗盤區，斷頭出清後常孕育波段低點。",
            "≥ 170%：融資戶普遍獲利、籌碼結構安全穩健。",
        ),
    )
}

@Composable
private fun MarginKeepRateTrendChart(
    points: List<Pair<String, Double>>,
) {
    val values = points.map { it.second }
    val rawMin = values.minOrNull() ?: 130.0
    val rawMax = values.maxOrNull() ?: 180.0
    val chartMin = minOf(rawMin - 3.0, 145.0).coerceAtLeast(115.0)
    val chartMax = maxOf(rawMax + 3.0, 172.0)
    val span = (chartMax - chartMin).coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "低點 ${String.format(Locale.US, "%.1f", rawMin)}%",
                style = FinancialTextStyles.numericSmall,
                color = TextSecondary,
            )
            Text(
                text = "高點 ${String.format(Locale.US, "%.1f", rawMax)}%",
                style = FinancialTextStyles.numericSmall,
                color = BrandSky,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCardHigh.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            val w = size.width
            val h = size.height
            fun yFor(rate: Double): Float {
                val ratio = ((rate - chartMin) / span).toFloat().coerceIn(0f, 1f)
                return h - ratio * h
            }

            // 參考水位線：170% 安全線、150% 警戒線、130% 追繳線
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(
                170.0 to TaiwanGreen.copy(alpha = 0.45f),
                150.0 to AlertAmber.copy(alpha = 0.55f),
                130.0 to TaiwanRed.copy(alpha = 0.6f),
            ).forEach { (level, lineColor) ->
                if (level in chartMin..chartMax) {
                    val y = yFor(level)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f,
                        pathEffect = dashEffect,
                    )
                }
            }

            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, (_, rate) ->
                val x = idx * stepX
                val y = yFor(rate)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo((points.size - 1) * stepX, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(BrandSky.copy(alpha = 0.28f), Color.Transparent),
                ),
            )
            drawPath(
                path = linePath,
                color = BrandSky,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            points.forEachIndexed { idx, (_, rate) ->
                val x = idx * stepX
                val y = yFor(rate)
                val dotColor = when {
                    rate < 150.0 -> TaiwanRed
                    rate >= 170.0 -> TaiwanGreen
                    else -> AlertAmber
                }
                drawCircle(
                    color = dotColor,
                    radius = if (idx == points.lastIndex) 7f else 4.5f,
                    center = Offset(x, y),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.first().first,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Text(
                text = "虛線：170%安全 · 150%恐慌 · 130%追繳",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted,
            )
            Text(
                text = points.last().first,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun ForeignFuturesDetailContent(
    data: TaiwanFuturesChipSnapshot?,
) {
    if (data == null) {
        DetailEmptyCard(
            title = "尚無期交所盤後籌碼資料",
            description = "期交所每日約 15:15 後更新一般盤結算籌碼，請稍後點擊「立即整理」。",
        )
        return
    }

    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val foreignColor = directionalColor(data.foreignTxNetOi)

    // 主摘要卡片
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, foreignColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = "外資大台 (TX) 未平倉淨口數",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Text(
                    text = "${data.formattedDate} 盤後",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandSky,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${formatSigned(data.foreignTxNetOi, nf)} 口",
                    style = FinancialTextStyles.priceHero,
                    color = foreignColor,
                )
                Column(horizontalAlignment = Alignment.End) {
                    data.foreignTxOiChange?.let { chg ->
                        Text(
                            text = "日增減 ${formatSigned(chg, nf)} 口",
                            style = FinancialTextStyles.numericSmall,
                            color = directionalColor(chg),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "當日交易淨額 ${formatSigned(data.foreignTxTradingNet, nf)} 口",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }

    // 三大法人大台部位對照表
    DetailSectionCard(title = "三大法人臺股期貨 (TX) 部位明細") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val rows = listOf(
                InstitutionalRowData(
                    name = "外資及陸資",
                    netOi = data.foreignTxNetOi,
                    oiChange = data.foreignTxOiChange,
                    tradingNet = data.foreignTxTradingNet,
                    longOi = data.foreignTxLongOi,
                    shortOi = data.foreignTxShortOi,
                ),
                InstitutionalRowData(
                    name = "投信",
                    netOi = data.trustTxNetOi,
                    oiChange = data.trustTxOiChange,
                    tradingNet = data.trustTxTradingNet,
                    longOi = data.trustTxLongOi,
                    shortOi = data.trustTxShortOi,
                ),
                InstitutionalRowData(
                    name = "自營商",
                    netOi = data.dealerTxNetOi,
                    oiChange = data.dealerTxOiChange,
                    tradingNet = data.dealerTxTradingNet,
                    longOi = data.dealerTxLongOi,
                    shortOi = data.dealerTxShortOi,
                ),
                InstitutionalRowData(
                    name = "三大法人合計",
                    netOi = data.totalInstitutionalTxNetOi,
                    oiChange = data.totalInstitutionalTxOiChange,
                    tradingNet = data.totalInstitutionalTxTradingNet,
                    longOi = data.foreignTxLongOi + data.trustTxLongOi + data.dealerTxLongOi,
                    shortOi = data.foreignTxShortOi + data.trustTxShortOi + data.dealerTxShortOi,
                    isTotal = true,
                ),
            )

            rows.forEachIndexed { index, row ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.name,
                            style = if (row.isTotal) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                            color = if (row.isTotal) BrandSky else TextPrimary,
                            fontWeight = if (row.isTotal) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${formatSigned(row.netOi, nf)} 口",
                                style = FinancialTextStyles.numericMedium,
                                color = directionalColor(row.netOi),
                                fontWeight = FontWeight.Bold,
                            )
                            val deltaLabel = row.oiChange?.let { "日 ${formatSigned(it, nf)}" }
                                ?: "交 ${formatSigned(row.tradingNet, nf)}"
                            Text(
                                text = deltaLabel,
                                style = FinancialTextStyles.numericSmall,
                                color = directionalColor(row.oiChange ?: row.tradingNet),
                            )
                        }
                    }

                    val totalLs = row.longOi + row.shortOi
                    if (totalLs > 0L) {
                        val longFrac = (row.longOi.toFloat() / totalLs.toFloat()).coerceIn(0.02f, 0.98f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "多單 ${nf.format(row.longOi)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TaiwanRed.copy(alpha = 0.9f),
                            )
                            Text(
                                text = "空單 ${nf.format(row.shortOi)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TaiwanGreen.copy(alpha = 0.9f),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(longFrac)
                                    .height(4.dp)
                                    .background(TaiwanRed)
                            )
                            Box(
                                modifier = Modifier
                                    .weight((1f - longFrac).coerceAtLeast(0.02f))
                                    .height(4.dp)
                                    .background(TaiwanGreen)
                            )
                        }
                    }
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))
                }
            }
        }
    }

    // 近 10 日外資未平倉變化
    if (data.historyPoints.isNotEmpty()) {
        DetailSectionCard(title = "近 ${data.historyPoints.size} 日外資未平倉變化") {
            val maxAbs = data.historyPoints.maxOfOrNull { abs(it.foreignTxNetOi) }?.coerceAtLeast(1L) ?: 1L
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.historyPoints.asReversed().forEach { pt ->
                    val barFraction = (abs(pt.foreignTxNetOi).toFloat() / maxAbs.toFloat()).coerceIn(0.05f, 1f)
                    val color = directionalColor(pt.foreignTxNetOi)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = pt.shortDateLabel,
                            style = FinancialTextStyles.numericSmall,
                            color = TextSecondary,
                            modifier = Modifier.width(44.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceCardHigh),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barFraction)
                                    .height(8.dp)
                                    .background(color.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            text = formatSigned(pt.foreignTxNetOi, nf),
                            style = FinancialTextStyles.numericSmall,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(74.dp),
                        )
                        Text(
                            text = pt.foreignTxOiChange?.let { formatSigned(it, nf) } ?: "--",
                            style = MaterialTheme.typography.labelSmall,
                            color = pt.foreignTxOiChange?.let { directionalColor(it) } ?: TextMuted,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(56.dp),
                        )
                    }
                }
            }
        }
    }

    DetailGuideCard(
        title = "法人期貨籌碼判讀",
        bullets = listOf(
            "採台股紅多綠空配色；外資大台常含現貨避險空單，觀察重點在「連續多日增減趨勢」。",
            "投信大台多單通常對應 ETF 避險與主動部位，可與外資合計對照法人整體多空傾向。",
        ),
    )
}

@Composable
private fun RetailRatioDetailContent(
    data: TaiwanFuturesChipSnapshot?,
) {
    if (data == null) {
        DetailEmptyCard(
            title = "尚無散戶多空比資料",
            description = "期交所每日約 15:15 後更新一般盤結算籌碼，請稍後點擊「立即整理」。",
        )
        return
    }

    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val combinedRatio = data.combinedRetailRatio
    val combinedColor = ratioColor(combinedRatio)
    val signalLabel = when {
        combinedRatio == null -> "資料不足"
        combinedRatio >= 15.0 -> "散戶顯著偏多 · 留意震盪壓回"
        combinedRatio > 5.0 -> "散戶溫和偏多"
        combinedRatio <= -15.0 -> "散戶顯著偏空 · 具軋空動能"
        combinedRatio < -5.0 -> "散戶溫和偏空"
        else -> "散戶多空均衡"
    }

    // 5:1 加權綜合散戶多空比主卡
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, combinedColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = "5:1 資金加權綜合散戶多空比",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BrandSkyContainer,
                ) {
                    Text(
                        text = "1 小台 = 5 微台",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BrandSky,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = combinedRatio?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "--",
                    style = FinancialTextStyles.priceHero,
                    color = combinedColor,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = combinedColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, combinedColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = signalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = combinedColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                text = "等值小台散戶淨口數 ${formatSigned(data.combinedMtxEquivalentRetailNetOi.roundToInt().toLong(), nf)} 口 / 等值總 OI ${nf.format(data.combinedMtxEquivalentTotalOi.roundToInt())} 口（${data.formattedDate} 盤後）",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }

    // 小台 vs 微台拆解雙卡
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RetailContractBreakdownCard(
            title = "小台指 (MTX)",
            subtitle = "50 元/點",
            ratio = data.mtxRetailRatio,
            retailNetOi = data.mtxRetailNetOi,
            totalOi = data.mtxTotalOi,
            nf = nf,
            modifier = Modifier.weight(1f),
        )
        RetailContractBreakdownCard(
            title = "微台指 (TMF)",
            subtitle = "10 元/點",
            ratio = data.tmfRetailRatio,
            retailNetOi = data.tmfRetailNetOi,
            totalOi = data.tmfTotalOi,
            nf = nf,
            modifier = Modifier.weight(1f),
        )
    }

    // 近 10 日散戶多空比歷史趨勢
    if (data.historyPoints.isNotEmpty()) {
        DetailSectionCard(title = "近 ${data.historyPoints.size} 日散戶多空比趨勢") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("日期", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(0.9f))
                    Text("小台比", style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text("微台比", style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text("加權綜合", style = MaterialTheme.typography.labelSmall, color = BrandSky, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                }
                HorizontalDivider(color = HairlineBorder.copy(alpha = 0.6f))
                data.historyPoints.asReversed().forEach { pt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = pt.shortDateLabel,
                            style = FinancialTextStyles.numericSmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(0.9f),
                        )
                        Text(
                            text = pt.mtxRetailRatio?.let { String.format(Locale.US, "%+.1f%%", it) } ?: "--",
                            style = FinancialTextStyles.numericSmall,
                            color = ratioColor(pt.mtxRetailRatio),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = pt.tmfRetailRatio?.let { String.format(Locale.US, "%+.1f%%", it) } ?: "--",
                            style = FinancialTextStyles.numericSmall,
                            color = ratioColor(pt.tmfRetailRatio),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = pt.combinedRetailRatio?.let { String.format(Locale.US, "%+.1f%%", it) } ?: "--",
                            style = FinancialTextStyles.numericSmall,
                            color = ratioColor(pt.combinedRetailRatio),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.1f),
                        )
                    }
                }
            }
        }
    }

    DetailGuideCard(
        title = "散戶反指標判讀指南",
        bullets = listOf(
            "散戶淨部位由「－(三大法人小台/微台未平倉淨口數)」反推得出。",
            "紅字正值過高：代表散戶持續攤平作多，籌碼凌亂較易引發震盪壓回。",
            "綠字負值過低：代表散戶大舉作空，籌碼集中於法人手上，行情上攻時容易出現軋空。",
        ),
    )
}

@Composable
private fun RetailContractBreakdownCard(
    title: String,
    subtitle: String,
    ratio: Double?,
    retailNetOi: Long,
    totalOi: Long,
    nf: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val color = ratioColor(ratio)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted,
                )
            }
            Text(
                text = ratio?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "--",
                style = FinancialTextStyles.priceLarge,
                color = color,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))
            Text(
                text = "散戶淨額 ${formatSigned(retailNetOi, nf)} 口",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Text(
                text = "市場總 OI ${nf.format(totalOi)} 口",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

private data class InstitutionalRowData(
    val name: String,
    val netOi: Long,
    val oiChange: Long?,
    val tradingNet: Long,
    val longOi: Long,
    val shortOi: Long,
    val isTotal: Boolean = false,
)

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = BrandSky,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardHigh.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Text(
                text = value,
                style = FinancialTextStyles.numericMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subValue.isNullOrBlank()) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailGuideCard(
    title: String,
    bullets: List<String>,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardHigh.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "💡 $title",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun DetailEmptyCard(
    title: String,
    description: String,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

private fun directionalColor(value: Long): Color = when {
    value > 0L -> TaiwanRed
    value < 0L -> TaiwanGreen
    else -> TextSecondary
}

private fun ratioColor(ratio: Double?): Color = when {
    ratio == null -> TextPrimary
    ratio > 0.0 -> TaiwanRed
    ratio < 0.0 -> TaiwanGreen
    else -> TextSecondary
}

private fun formatSigned(value: Long, nf: NumberFormat): String {
    val sign = if (value > 0L) "+" else ""
    return "$sign${nf.format(value)}"
}

private fun formatTwAmt(amountInThousandsOrUnit: Long): String {
    // 證交所 BFIJ3U_TREND 的 crdAmt/callAmt/exeAmt 單位為千元；若數值較大換算為億元
    val absVal = abs(amountInThousandsOrUnit)
    return if (absVal >= 100_000L) {
        val yi = amountInThousandsOrUnit / 100_000.0
        String.format(Locale.US, "%.1f 億", yi)
    } else if (absVal >= 1_000L) {
        val wan = amountInThousandsOrUnit / 10.0
        String.format(Locale.US, "%.0f 萬", wan)
    } else {
        "$amountInThousandsOrUnit 千元"
    }
}
