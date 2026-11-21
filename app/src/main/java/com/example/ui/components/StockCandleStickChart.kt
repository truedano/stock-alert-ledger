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

    // 計算警報條件防線
    val alertMarkers = remember(ruleCondition, evaluation, closeHistory) {
        extractAlertMarkers(ruleCondition, evaluation, closeHistory)
    }

    // 計算特定均線之全歷史序列（若條件包含均線）
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

            // 十字游標詳細資訊面板
            val activeCandle = selectedIndex?.let { displayCandles.getOrNull(it) }
                ?: displayCandles.lastOrNull()

            if (activeCandle != null) {
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
                val dateStr = remember(activeCandle.timestamp) {
                    if (activeCandle.timestamp > 0) dateFormat.format(Date(activeCandle.timestamp * 1000L.takeIf { activeCandle.timestamp < 10000000000L } ?: activeCandle.timestamp))
                    else "--"
                }
                val isUp = activeCandle.close >= activeCandle.open
                val kColor = if (isUp) TaiwanRed else TaiwanGreen

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardHigh.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateStr,
                        style = FinancialTextStyles.numericSmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(
                            text = "收 ${Formatters.formatPrice(activeCandle.close, currency)}",
                            style = FinancialTextStyles.numericSmall,
                            color = kColor,
                            fontWeight = FontWeight.Bold,
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

    val paddingRight = 60.dp.toPx()
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

                // 右側醒目徽章背景與文字
                val badgeWidth = paddingRight - 4f
                val badgeHeight = 36f
                val badgeTop = (y - badgeHeight / 2f).coerceIn(0f, chartHeight - badgeHeight)

                drawRoundRect(
                    color = marker.color.copy(alpha = 0.2f),
                    topLeft = Offset(chartWidth + 4f, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )

                drawRoundRect(
                    color = marker.color,
                    topLeft = Offset(chartWidth + 4f, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f),
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 22f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val text = marker.label
                    drawText(text, chartWidth + 8f, badgeTop + 24f, paint)
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

private fun extractAlertMarkers(
    ruleCondition: AlertCondition,
    evaluation: ConditionEvaluationResult?,
    closeHistory: List<Double>,
): List<AlertMarker> {
    val markers = mutableListOf<AlertMarker>()

    fun processCondition(cond: AlertCondition) {
        when (cond) {
            is MovingAverageCondition -> {
                val targetPrice = evaluation?.targetReferencePrice
                    ?: com.example.domain.model.MovingAverageCalculator.calculateSma(closeHistory, cond.periodDays)
                if (targetPrice != null && targetPrice > 0.0) {
                    markers.add(
                        AlertMarker(
                            label = "${cond.periodDays}MA",
                            price = targetPrice,
                            color = AlertAmber,
                            periodDays = cond.periodDays,
                        )
                    )
                }
            }
            is PriceTargetCondition -> {
                val dirSymbol = if (cond.direction == com.example.domain.model.PriceTargetDirection.BELOW_OR_EQUAL) "≤" else "≥"
                markers.add(
                    AlertMarker(
                        label = "目標 $dirSymbol",
                        price = cond.targetPrice,
                        color = Color(0xFF38BDF8), // 亮天藍
                    )
                )
            }
            is PeakDrawdownCondition -> {
                val targetPrice = evaluation?.targetReferencePrice
                if (targetPrice != null && targetPrice > 0.0) {
                    markers.add(
                        AlertMarker(
                            label = "-${cond.drawdownPercent.toInt()}%",
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
