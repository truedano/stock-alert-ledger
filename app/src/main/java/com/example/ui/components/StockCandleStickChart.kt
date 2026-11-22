package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AlertCondition
import com.example.domain.model.CandleStickData
import com.example.domain.model.ChartTimeframe
import com.example.domain.model.CompositeCondition
import com.example.domain.model.ConditionEvaluationResult
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.PeakDrawdownCondition
import com.example.domain.model.PriceTargetCondition
import com.example.domain.model.RsiCondition
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AlertMarker(
    val label: String,
    val price: Double,
    val color: Color,
    val isDashed: Boolean = true,
    val periodDays: Int? = null,
)

@Composable
fun StockCandleStickChart(
    symbol: String,
    currency: MarketCurrency,
    candleHistory: List<CandleStickData>,
    closeHistory: List<Double>,
    ruleCondition: AlertCondition,
    evaluation: ConditionEvaluationResult?,
    marginInfo: com.example.data.remote.StockMarginInfo? = null,
    modifier: Modifier = Modifier,
) {
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.THREE_MONTHS) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // 根據選取天數裁切顯示資料
    val displayCandles = remember(candleHistory, closeHistory, selectedTimeframe) {
        val baseCandles = if (candleHistory.isNotEmpty()) {
            candleHistory
        } else {
            // 若歷史資料僅有 closeHistory，自動合成單純收盤價蠟燭
            closeHistory.mapIndexed { idx, c ->
                val prev = if (idx > 0) closeHistory[idx - 1] else c
                CandleStickData(
                    timestamp = System.currentTimeMillis() - (closeHistory.size - idx) * 86400000L,
                    open = prev,
                    high = maxOf(prev, c),
                    low = minOf(prev, c),
                    close = c,
                    volume = 0L,
                )
            }
        }
        if (selectedTimeframe.days >= baseCandles.size) {
            baseCandles
        } else {
            baseCandles.takeLast(selectedTimeframe.days)
        }
    }

// 計算警報條件防線與融資 130% 追繳防線
    val alertMarkers = remember(ruleCondition, evaluation, closeHistory, currency, marginInfo) {
        val list = extractAlertMarkers(ruleCondition, evaluation, closeHistory, currency).toMutableList()
        // 若為台股且有融資資料/均線，將融資 130% 斷頭追繳線繪製於價格主圖
        if (currency == MarketCurrency.TWD && closeHistory.size >= 20) {
            val ma20 = com.example.domain.model.MovingAverageCalculator.calculateSma(closeHistory, 20)
            if (ma20 != null && ma20 > 0.0) {
                // 融資成本 = MA20，借款 6 成，130% 追繳價 = (MA20 * 0.6) * 1.3 = MA20 * 0.78
                val marginCallPrice = ma20 * 0.78
                val priceStr = Formatters.formatPrice(marginCallPrice, currency)
                list.add(
                    AlertMarker(
                        label = "融資追繳 130% $priceStr",
                        price = marginCallPrice,
                        color = Color(0xFFF97316), // 亮橘色斷頭防線
                    )
                )
            }
        }
        list
    }

    // 計算個股推估融資維持率歷史序列（依每日收盤價與當時 20MA 計算）
    val estMarginSeries = remember(closeHistory, displayCandles, currency) {
        if (currency != MarketCurrency.TWD || closeHistory.size < 20) emptyList()
        else {
            val totalLen = closeHistory.size
            val displayLen = displayCandles.size
            val startOffset = (totalLen - displayLen).coerceAtLeast(0)

            val list = mutableListOf<Double?>()
            for (i in startOffset until totalLen) {
                if (i >= 19) {
                    val sub = closeHistory.subList(0, i + 1)
                    val priceAtI = closeHistory[i]
                    list.add(com.example.domain.model.MovingAverageCalculator.calculateEstimatedMarginKeepRate(priceAtI, sub))
                } else {
                    list.add(null)
                }
            }
            list
        }
    }
    val marginCondition = remember(ruleCondition) {
        when (ruleCondition) {
            is com.example.domain.model.MarginMaintenanceCondition -> ruleCondition
            is CompositeCondition -> ruleCondition.conditions.filterIsInstance<com.example.domain.model.MarginMaintenanceCondition>().firstOrNull()
            else -> null
        }
    }

    // 檢查條件中是否包含 RSI
    val rsiCondition = remember(ruleCondition) {
        when (ruleCondition) {
            is RsiCondition -> ruleCondition
            is CompositeCondition -> ruleCondition.conditions.filterIsInstance<RsiCondition>().firstOrNull()
            else -> null
        }
    }

    // 計算顯示區間的 RSI(14) 歷史序列
    val rsiSeries = remember(closeHistory, displayCandles, rsiCondition) {
        if (rsiCondition == null) emptyList()
        else {
            val period = rsiCondition.periodDays
            val totalLen = closeHistory.size
            val displayLen = displayCandles.size
            val startOffset = (totalLen - displayLen).coerceAtLeast(0)

            val list = mutableListOf<Double?>()
            for (i in startOffset until totalLen) {
                if (i >= period) {
                    val sub = closeHistory.subList(0, i + 1)
                    list.add(com.example.domain.model.MovingAverageCalculator.calculateRsi(sub, period))
                } else {
                    list.add(null)
                }
            }
            list
        }
    }
    val maSeriesMap = remember(alertMarkers, closeHistory, displayCandles) {
        val result = mutableMapOf<Int, List<Double?>>()
        val totalLen = closeHistory.size
        val displayLen = displayCandles.size
        val startOffset = (totalLen - displayLen).coerceAtLeast(0)

        alertMarkers.mapNotNull { it.periodDays }.distinct().forEach { period ->
            val maList = mutableListOf<Double?>()
            for (i in startOffset until totalLen) {
                if (i + 1 >= period) {
                    val sub = closeHistory.subList(i + 1 - period, i + 1)
                    maList.add(sub.average())
                } else {
                    maList.add(null)
                }
            }
            result[period] = maList
        }
        result
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 頂部列：週期切換與警報條件指示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartTimeframe.values().forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTimeframe = tf
                                selectedIndex = null
                            },
                            label = {
                                Text(
                                    text = tf.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSkyContainer,
                                selectedLabelColor = BrandSky,
                                containerColor = SurfaceCardHigh.copy(alpha = 0.5f),
                                labelColor = TextSecondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = HairlineBorder,
                                selectedBorderColor = BrandSky.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }

                // 標記提示徽章
                if (alertMarkers.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = AlertAmber,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "警報防線已標記",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // 十字游標詳細資訊面板（自適應兩欄排列，避免右側數值截斷）
            val activeCandle = selectedIndex?.let { displayCandles.getOrNull(it) }
                ?: displayCandles.lastOrNull()

            if (activeCandle != null) {
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
                val dateStr = remember(activeCandle.timestamp) {
                    val ts = activeCandle.timestamp
                    if (ts > 0L) {
                        val millis = if (ts < 10000000000L) ts * 1000L else ts
                        dateFormat.format(Date(millis))
                    } else "--"
                }
                val isUp = activeCandle.close >= activeCandle.open
                val kColor = if (isUp) TaiwanRed else TaiwanGreen

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardHigh.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 第一行：日期與收盤價（高亮主要狀態）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "日期：$dateStr",
                            style = FinancialTextStyles.numericSmall,
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "收盤",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp,
                            )
                            Text(
                                text = Formatters.formatPrice(activeCandle.close, currency),
                                style = FinancialTextStyles.numericSmall,
                                color = kColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    // 第二行：開盤、最高、最低三項輔助數據平分展示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "開 ${Formatters.formatPrice(activeCandle.open, currency)}",
                            style = FinancialTextStyles.numericSmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "高 ${Formatters.formatPrice(activeCandle.high, currency)}",
                            style = FinancialTextStyles.numericSmall,
                            color = TaiwanRed,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "低 ${Formatters.formatPrice(activeCandle.low, currency)}",
                            style = FinancialTextStyles.numericSmall,
                            color = TaiwanGreen,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            // K 線主圖 Canvas
            if (displayCandles.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .pointerInput(displayCandles) {
                                detectTapGestures { offset ->
                                    val count = displayCandles.size
                                    val candleSlotWidth = size.width / count
                                    val idx = (offset.x / candleSlotWidth).toInt().coerceIn(0, count - 1)
                                    selectedIndex = idx
                                }
                            }
                            .pointerInput(displayCandles) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val count = displayCandles.size
                                        val candleSlotWidth = size.width / count
                                        selectedIndex = (offset.x / candleSlotWidth).toInt().coerceIn(0, count - 1)
                                    },
                                    onDrag = { change, _ ->
                                        val count = displayCandles.size
                                        val candleSlotWidth = size.width / count
                                        selectedIndex = (change.position.x / candleSlotWidth).toInt().coerceIn(0, count - 1)
                                    },
                                )
                            }
                    ) {
                        drawCandleStickChart(
                            candles = displayCandles,
                            alertMarkers = alertMarkers,
                            maSeriesMap = maSeriesMap,
                            selectedIndex = selectedIndex,
                            currency = currency,
                        )
                    }
                }

                // 若包含 RSI 條件，自動滑出 60dp 專業 RSI 副圖與超賣/超買防線
                if (rsiCondition != null && rsiSeries.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val activeRsi = selectedIndex?.let { rsiSeries.getOrNull(it) }
                            ?: rsiSeries.lastOrNull()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "RSI(${rsiCondition.periodDays})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSky,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = activeRsi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                                    style = FinancialTextStyles.numericSmall,
                                    color = if ((activeRsi ?: 50.0) <= rsiCondition.threshold && !rsiCondition.isAbove) TaiwanGreen
                                    else if ((activeRsi ?: 50.0) >= rsiCondition.threshold && rsiCondition.isAbove) TaiwanRed
                                    else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "門檻：${if (rsiCondition.isAbove) "≥" else "≤"} ${rsiCondition.threshold.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRsiSubChart(
                                    series = rsiSeries,
                                    condition = rsiCondition,
                                    selectedIndex = selectedIndex,
                                )
                            }
                        }
                    }
                }

                // 若為台股且包含融資維持率條件或個股融資資訊，自動滑出 65dp 維持率副圖與走勢折線
                val showMarginSubChart = marginCondition != null || (currency == MarketCurrency.TWD && estMarginSeries.isNotEmpty())
                if (showMarginSubChart) {
                    val activeEstRate = selectedIndex?.let { estMarginSeries.getOrNull(it) }
                        ?: estMarginSeries.lastOrNull()
                        ?: marginInfo?.estimatedKeepRate
                    val marketKeepRate = evaluation?.targetReferencePrice
                    val threshold = marginCondition?.let { if (it.isExitMode) it.exitThreshold else it.panicThreshold } ?: 130.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = if (marginCondition != null) "全市場/個股維持率" else "個股推估維持率",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlertAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                activeEstRate?.let { r ->
                                    val rColor = if (r < 135.0) TaiwanRed else if (r < 150.0) AlertAmber else TaiwanGreen
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", r)}%",
                                        style = FinancialTextStyles.numericSmall,
                                        color = rColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (marginCondition != null && marketKeepRate != null) {
                                    Text(
                                        text = "(大盤 ${String.format(Locale.US, "%.1f", marketKeepRate)}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                text = if (marginCondition != null) {
                                    if (marginCondition.isExitMode) "安全停泊門檻：≥ ${threshold.toInt()}%" else "恐慌抄底門檻：≤ ${threshold.toInt()}%"
                                } else "追繳警戒線：130%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawMarginSubChart(
                                    series = estMarginSeries,
                                    threshold = threshold,
                                    selectedIndex = selectedIndex,
                                    isExitMode = marginCondition?.isExitMode == true,
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "歷史 K 線資料載入中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCandleStickChart(
    candles: List<CandleStickData>,
    alertMarkers: List<AlertMarker>,
    maSeriesMap: Map<Int, List<Double?>>,
    selectedIndex: Int?,
    currency: MarketCurrency,
) {
    val count = candles.size
    if (count == 0) return

    val paddingRight = 72.dp.toPx()
    val chartWidth = size.width - paddingRight
    val chartHeight = size.height - 24.dp.toPx()

    // 計算價格極值
    var minPrice = candles.minOf { it.low }
    var maxPrice = candles.maxOf { it.high }

    alertMarkers.forEach { marker ->
        if (marker.price > 0.0) {
            minPrice = minOf(minPrice, marker.price)
            maxPrice = maxOf(maxPrice, marker.price)
        }
    }

    maSeriesMap.values.forEach { series ->
        series.filterNotNull().forEach { p ->
            minPrice = minOf(minPrice, p)
            maxPrice = maxOf(maxPrice, p)
        }
    }

    // 留上下 5% padding
    val priceDiff = (maxPrice - minPrice).coerceAtLeast(0.01)
    val paddedMin = minPrice - priceDiff * 0.05
    val paddedMax = maxPrice + priceDiff * 0.05
    val totalRange = paddedMax - paddedMin

    fun priceToY(price: Double): Float {
        return (chartHeight - ((price - paddedMin) / totalRange * chartHeight)).toFloat()
    }

    // 1. 繪製水平網格線與 Y 軸標籤 (3條水平線)
    val gridCount = 3
    for (i in 0..gridCount) {
        val y = chartHeight * (i.toFloat() / gridCount)
        val p = paddedMax - (totalRange * (i.toFloat() / gridCount))
        drawLine(
            color = HairlineBorder.copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f,
        )
        // 刻度文字
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 26f
                isAntiAlias = true
            }
            drawText(
                Formatters.formatPrice(p, currency),
                chartWidth + 8f,
                y + 9f,
                textPaint
            )
        }
    }

    val candleSlotWidth = chartWidth / count
    val candleWidth = (candleSlotWidth * 0.65f).coerceAtLeast(2f)

    // 2. 繪製 K 棒
    candles.forEachIndexed { index, candle ->
        val xCenter = index * candleSlotWidth + candleSlotWidth / 2f
        val isUp = candle.close >= candle.open
        val color = if (isUp) TaiwanRed else TaiwanGreen

        val highY = priceToY(candle.high)
        val lowY = priceToY(candle.low)
        val openY = priceToY(candle.open)
        val closeY = priceToY(candle.close)

        // 上下影線
        drawLine(
            color = color,
            start = Offset(xCenter, highY),
            end = Offset(xCenter, lowY),
            strokeWidth = 1.5f,
        )

        // 實體
        val topY = minOf(openY, closeY)
        val bodyHeight = kotlin.math.abs(closeY - openY).coerceAtLeast(1.5f)
        drawRect(
            color = color,
            topLeft = Offset(xCenter - candleWidth / 2f, topY),
            size = Size(candleWidth, bodyHeight),
        )
    }

    // 3. 繪製均線路徑 (MA Lines)
    maSeriesMap.forEach { (period, series) ->
        val maPath = Path()
        var hasStarted = false
        val maColor = when (period) {
            5 -> Color(0xFFFBBF24) // 琥珀黃
            20 -> BrandSky         // 月線天藍
            60 -> Color(0xFFA855F7) // 季線紫色
            else -> Color(0xFFFB923C) // 長期橘色
        }

        series.forEachIndexed { idx, price ->
            if (price != null && idx < count) {
                val x = idx * candleSlotWidth + candleSlotWidth / 2f
                val y = priceToY(price)
                if (!hasStarted) {
                    maPath.moveTo(x, y)
                    hasStarted = true
                } else {
                    maPath.lineTo(x, y)
                }
            }
        }

        if (hasStarted) {
            drawPath(
                path = maPath,
                color = maColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2f,
                    pathEffect = PathEffect.cornerPathEffect(4f)
                ),
            )
        }
    }

    // 4. 繪製警報防線 (Alert Markers: 虛線 + 右側徽章)
    alertMarkers.forEach { marker ->
        if (marker.price > 0.0) {
            val y = priceToY(marker.price)
            if (y in 0f..chartHeight) {
                // 貫穿水平虛線
                drawLine(
                    color = marker.color.copy(alpha = 0.9f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                )

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 20f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val text = marker.label
                val textWidth = paint.measureText(text)
                val badgeWidth = (textWidth + 16f).coerceAtLeast(paddingRight - 4f)
                val badgeHeight = 32f
                val badgeTop = (y - badgeHeight / 2f).coerceIn(0f, chartHeight - badgeHeight)
                val badgeLeft = (size.width - badgeWidth - 2f).coerceAtLeast(chartWidth + 2f)

                drawRoundRect(
                    color = marker.color.copy(alpha = 0.25f),
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )

                drawRoundRect(
                    color = marker.color,
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f),
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(text, badgeLeft + 8f, badgeTop + 22f, paint)
                }
            }
        }
    }

    // 5. 十字游標 (Crosshair)
    if (selectedIndex != null && selectedIndex in 0 until count) {
        val candle = candles[selectedIndex]
        val xCenter = selectedIndex * candleSlotWidth + candleSlotWidth / 2f
        val closeY = priceToY(candle.close)

        // 垂直虛線
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(xCenter, 0f),
            end = Offset(xCenter, chartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
        )

        // 水平虛線
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(0f, closeY),
            end = Offset(chartWidth, closeY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
        )

        // 交叉圓點
        drawCircle(
            color = BrandSky,
            radius = 4.dp.toPx(),
            center = Offset(xCenter, closeY),
        )
    }
}

private fun DrawScope.drawRsiSubChart(
    series: List<Double?>,
    condition: RsiCondition,
    selectedIndex: Int?,
) {
    val count = series.size
    if (count == 0) return

    val paddingRight = 72.dp.toPx()
    val chartWidth = size.width - paddingRight
    val chartHeight = size.height

    fun rsiToY(rsi: Double): Float {
        // RSI 固定 0 ~ 100 區間
        return (chartHeight - (rsi.coerceIn(0.0, 100.0) / 100.0 * chartHeight)).toFloat()
    }

    // 1. 繪製 30 與 70 基準格線
    val y30 = rsiToY(30.0)
    val y70 = rsiToY(70.0)
    val yThreshold = rsiToY(condition.threshold)

    // 30 / 70 灰色參考虛線
    drawLine(
        color = HairlineBorder.copy(alpha = 0.5f),
        start = Offset(0f, y70),
        end = Offset(chartWidth, y70),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
    )
    drawLine(
        color = HairlineBorder.copy(alpha = 0.5f),
        start = Offset(0f, y30),
        end = Offset(chartWidth, y30),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
    )

    // 2. 警報警戒防線（桃紅超賣線或亮橘超買線）
    val alertColor = if (condition.isAbove) TaiwanRed else TaiwanGreen
    drawLine(
        color = alertColor,
        start = Offset(0f, yThreshold),
        end = Offset(chartWidth, yThreshold),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
    )

    // 警戒標籤
    val alertBadgeTop = (yThreshold - 12f).coerceIn(0f, chartHeight - 24f)
    drawRoundRect(
        color = alertColor.copy(alpha = 0.2f),
        topLeft = Offset(chartWidth + 4f, alertBadgeTop),
        size = Size(paddingRight - 8f, 24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )
    drawRoundRect(
        color = alertColor,
        topLeft = Offset(chartWidth + 4f, alertBadgeTop),
        size = Size(paddingRight - 8f, 24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 18f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val label = "${if (condition.isAbove) "≥" else "≤"} ${condition.threshold.toInt()}"
        drawText(label, chartWidth + 8f, alertBadgeTop + 17f, paint)
    }

    // 3. 繪製 RSI 連續平滑折線
    val slotWidth = chartWidth / count
    val rsiPath = Path()
    var hasStarted = false

    series.forEachIndexed { idx, rsiVal ->
        if (rsiVal != null) {
            val x = idx * slotWidth + slotWidth / 2f
            val y = rsiToY(rsiVal)
            if (!hasStarted) {
                rsiPath.moveTo(x, y)
                hasStarted = true
            } else {
                rsiPath.lineTo(x, y)
            }
        }
    }

    if (hasStarted) {
        drawPath(
            path = rsiPath,
            color = Color(0xFFA855F7), // 紫色 RSI 線
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f,
                pathEffect = PathEffect.cornerPathEffect(3f)
            ),
        )
    }

    // 4. 十字游標連動
    if (selectedIndex != null && selectedIndex in 0 until count) {
        val rsiVal = series[selectedIndex]
        val xCenter = selectedIndex * slotWidth + slotWidth / 2f
        // 垂直虛線
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(xCenter, 0f),
            end = Offset(xCenter, chartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
        )
        if (rsiVal != null) {
            val rsiY = rsiToY(rsiVal)
            drawCircle(
                color = Color(0xFFA855F7),
                radius = 3.dp.toPx(),
                center = Offset(xCenter, rsiY),
            )
        }
    }
}

private fun DrawScope.drawMarginSubChart(
    series: List<Double?>,
    threshold: Double,
    selectedIndex: Int?,
    isExitMode: Boolean,
) {
    val paddingRight = 72.dp.toPx()
    val chartWidth = size.width - paddingRight
    val chartHeight = size.height

    // 融資維持率常態觀察區間 110% ~ 190%
    fun rateToY(rate: Double): Float {
        val minRate = 110.0
        val maxRate = 190.0
        val clamped = rate.coerceIn(minRate, maxRate)
        return (chartHeight - ((clamped - minRate) / (maxRate - minRate) * chartHeight)).toFloat()
    }

    val y130 = rateToY(130.0)
    val y160 = rateToY(160.0)
    val yThreshold = rateToY(threshold)

    // 1. 繪製 130% 斷頭追繳與 160% 正常水平參考線
    drawLine(
        color = TaiwanRed.copy(alpha = 0.5f),
        start = Offset(0f, y130),
        end = Offset(chartWidth, y130),
        strokeWidth = 1.2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
    )
    drawLine(
        color = HairlineBorder.copy(alpha = 0.5f),
        start = Offset(0f, y160),
        end = Offset(chartWidth, y160),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
    )

    // 2. 警報門檻防線（警戒線）
    val thresholdColor = if (isExitMode) TaiwanGreen else AlertAmber
    drawLine(
        color = thresholdColor,
        start = Offset(0f, yThreshold),
        end = Offset(chartWidth, yThreshold),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
    )

    // 門檻右側標籤
    val badgeTop = (yThreshold - 12f).coerceIn(0f, chartHeight - 24f)
    drawRoundRect(
        color = thresholdColor.copy(alpha = 0.2f),
        topLeft = Offset(chartWidth + 4f, badgeTop),
        size = Size(paddingRight - 8f, 24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )
    drawRoundRect(
        color = thresholdColor,
        topLeft = Offset(chartWidth + 4f, badgeTop),
        size = Size(paddingRight - 8f, 24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 17f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val label = "門檻 ${threshold.toInt()}%"
        drawText(label, chartWidth + 6f, badgeTop + 17f, paint)
    }

    // 3. 繪製個股推估維持率連續折線
    val count = series.size
    if (count > 0) {
        val slotWidth = chartWidth / count
        val marginPath = Path()
        var hasStarted = false

        series.forEachIndexed { idx, rateVal ->
            if (rateVal != null) {
                val x = idx * slotWidth + slotWidth / 2f
                val y = rateToY(rateVal)
                if (!hasStarted) {
                    marginPath.moveTo(x, y)
                    hasStarted = true
                } else {
                    marginPath.lineTo(x, y)
                }
            }
        }

        if (hasStarted) {
            drawPath(
                path = marginPath,
                color = AlertAmber, // 琥珀橙色維持率曲線
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2f,
                    pathEffect = PathEffect.cornerPathEffect(3f)
                ),
            )
        }

        // 4. 十字游標連動
        if (selectedIndex != null && selectedIndex in 0 until count) {
            val rVal = series[selectedIndex]
            val xCenter = selectedIndex * slotWidth + slotWidth / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(xCenter, 0f),
                end = Offset(xCenter, chartHeight),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
            )
            if (rVal != null) {
                val rY = rateToY(rVal)
                val dotColor = if (rVal < 135.0) TaiwanRed else if (rVal < 150.0) AlertAmber else TaiwanGreen
                drawCircle(
                    color = dotColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(xCenter, rY),
                )
            }
        }
    }
}

private fun extractAlertMarkers(
    ruleCondition: AlertCondition,
    evaluation: ConditionEvaluationResult?,
    closeHistory: List<Double>,
    currency: MarketCurrency,
): List<AlertMarker> {
    val markers = mutableListOf<AlertMarker>()

    fun processCondition(cond: AlertCondition) {
        when (cond) {
            is MovingAverageCondition -> {
                val targetPrice = evaluation?.targetReferencePrice
                    ?: com.example.domain.model.MovingAverageCalculator.calculateSma(closeHistory, cond.periodDays)
                if (targetPrice != null && targetPrice > 0.0) {
                    val priceStr = Formatters.formatPrice(targetPrice, currency)
                    markers.add(
                        AlertMarker(
                            label = "${cond.periodDays}MA $priceStr",
                            price = targetPrice,
                            color = AlertAmber,
                            periodDays = cond.periodDays,
                        )
                    )
                }
            }
            is PriceTargetCondition -> {
                val dirSymbol = if (cond.direction == com.example.domain.model.PriceTargetDirection.BELOW_OR_EQUAL) "≤" else "≥"
                val priceStr = Formatters.formatPrice(cond.targetPrice, currency)
                markers.add(
                    AlertMarker(
                        label = "$dirSymbol $priceStr",
                        price = cond.targetPrice,
                        color = Color(0xFF38BDF8), // 亮天藍
                    )
                )
            }
            is PeakDrawdownCondition -> {
                val targetPrice = evaluation?.targetReferencePrice
                if (targetPrice != null && targetPrice > 0.0) {
                    val priceStr = Formatters.formatPrice(targetPrice, currency)
                    markers.add(
                        AlertMarker(
                            label = "-${cond.drawdownPercent.toInt()}% $priceStr",
                            price = targetPrice,
                            color = Color(0xFFF43F5E), // 桃紅抄底線
                        )
                    )
                }
            }
            is CompositeCondition -> {
                cond.conditions.forEach { processCondition(it) }
            }
            else -> {}
        }
    }

    processCondition(ruleCondition)
    return markers
}
