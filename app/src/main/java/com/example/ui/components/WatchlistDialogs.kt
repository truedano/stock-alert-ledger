package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BuyRecordEntity
import com.example.data.local.WatchlistRuleEntity
import com.example.domain.model.AlertCondition
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import com.example.domain.model.MovingAverageCalculator
import com.example.domain.model.MovingAverageCondition
import com.example.domain.model.MovingAverageTriggerMode
import com.example.domain.model.PeakDrawdownCondition
import com.example.domain.model.ProxyBuyTarget
import com.example.domain.model.RsiCondition
import com.example.domain.model.StockQuote
import com.example.domain.model.TickerLookupResult
import com.example.domain.model.isMarketIndexSymbol
import com.example.domain.model.recommendedProxyTargetsForIndex
import com.example.notification.QuickBuyPrefill
import kotlinx.coroutines.launch
import com.example.ui.LookupUiState
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
import com.example.ui.theme.CanvasObsidian
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TaiwanGreen
import com.example.ui.theme.TaiwanGreenContainer
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TaiwanRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class RuleConditionCategory(val title: String) {
    PRICE_TARGET("🎯 目標價"),
    MOVING_AVERAGE("📈 均線 (SMA)"),
    PEAK_DRAWDOWN("📉 波段回撤"),
    RSI("⚡ RSI 指標"),
    MARGIN("🏦 融資維持率"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditRuleDialog(
    editingRule: WatchlistRuleEntity?,
    lookupState: LookupUiState,
    onLookupTicker: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        symbol: String,
        displaySymbol: String,
        name: String,
        currency: MarketCurrency,
        condition: AlertCondition,
        plannedBuyAmount: Double,
        initialQuote: StockQuote?,
    ) -> Unit,
) {
    val initialCond = editingRule?.condition
    val initialCategory = when (initialCond) {
        is MovingAverageCondition -> RuleConditionCategory.MOVING_AVERAGE
        is PeakDrawdownCondition -> RuleConditionCategory.PEAK_DRAWDOWN
        is RsiCondition -> RuleConditionCategory.RSI
        is com.example.domain.model.MarginMaintenanceCondition -> RuleConditionCategory.MARGIN
        is com.example.domain.model.CompositeCondition -> {
            val lead = initialCond.conditions.firstOrNull()
            when (lead) {
                is MovingAverageCondition -> RuleConditionCategory.MOVING_AVERAGE
                is PeakDrawdownCondition -> RuleConditionCategory.PEAK_DRAWDOWN
                is com.example.domain.model.PriceTargetCondition -> RuleConditionCategory.PRICE_TARGET
                else -> RuleConditionCategory.MOVING_AVERAGE
            }
        }
        else -> RuleConditionCategory.PRICE_TARGET
    }
    var selectedCategory by remember(editingRule) { mutableStateOf(initialCategory) }

    // 目標價狀態
    val initialPriceTarget = when (initialCond) {
        is com.example.domain.model.PriceTargetCondition -> initialCond
        is com.example.domain.model.CompositeCondition -> initialCond.conditions.filterIsInstance<com.example.domain.model.PriceTargetCondition>().firstOrNull()
        else -> null
    }
    var targetPriceText by remember(editingRule) {
        mutableStateOf(initialPriceTarget?.targetPrice?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var targetDirection by remember(editingRule) {
        mutableStateOf(initialPriceTarget?.direction ?: com.example.domain.model.PriceTargetDirection.BELOW_OR_EQUAL)
    }
    var targetToleranceText by remember(editingRule) {
        mutableStateOf((initialPriceTarget?.tolerancePercent ?: 0.0).toString())
    }
    var autoDisableOnTrigger by remember(editingRule) {
        mutableStateOf(initialPriceTarget?.autoDisableOnTrigger ?: true)
    }

    // MA 狀態
    val initialMa = when (initialCond) {
        is MovingAverageCondition -> initialCond
        is com.example.domain.model.CompositeCondition -> initialCond.conditions.filterIsInstance<MovingAverageCondition>().firstOrNull()
        else -> null
    }
    val presetPeriods = listOf(5, 10, 20, 60, 120, 240, 720, 1200, 2400)
    val initialPeriod = initialMa?.periodDays ?: 20
    var selectedPeriod by remember(editingRule) { mutableStateOf(initialPeriod) }
    var isCustomPeriod by remember(editingRule) {
        mutableStateOf(initialPeriod !in presetPeriods)
    }
    var customPeriodText by remember(editingRule) {
        mutableStateOf(if (initialPeriod !in presetPeriods) initialPeriod.toString() else "")
    }
    var triggerMode by remember(editingRule) {
        mutableStateOf(initialMa?.triggerMode ?: MovingAverageTriggerMode.BELOW_OR_EQUAL)
    }
    var toleranceText by remember(editingRule) {
        mutableStateOf((initialMa?.tolerancePercent ?: 1.0).toString())
    }

    // 波段回撤抄底狀態 (預設 -20%)
    val initialDrawdown = when (initialCond) {
        is PeakDrawdownCondition -> initialCond
        is com.example.domain.model.CompositeCondition -> initialCond.conditions.filterIsInstance<PeakDrawdownCondition>().firstOrNull()
        else -> null
    }
    val presetDrawdowns = listOf(10, 15, 20, 25, 30)
    var selectedDrawdownPercent by remember(editingRule) {
        mutableStateOf(initialDrawdown?.drawdownPercent?.toInt() ?: 20)
    }
    var drawdownLookbackDaysText by remember(editingRule) {
        mutableStateOf((initialDrawdown?.lookbackDays ?: 250).toString())
    }

    // RSI 狀態 (預設 RSI 14 > 70 獲利了結)
    val initialRsi = initialCond as? RsiCondition
    var rsiPeriodText by remember(editingRule) {
        mutableStateOf((initialRsi?.periodDays ?: 14).toString())
    }
    var rsiThresholdText by remember(editingRule) {
        mutableStateOf((initialRsi?.threshold?.toInt() ?: 70).toString())
    }
    var rsiIsAbove by remember(editingRule) {
        mutableStateOf(initialRsi?.isAbove ?: true)
    }

    // 融資維持率條件狀態
    val initialMargin = initialCond as? com.example.domain.model.MarginMaintenanceCondition
    var isMarginExitMode by remember(editingRule) {
        mutableStateOf(initialMargin?.isExitMode ?: false)
    }
    var marginPanicThresholdText by remember(editingRule) {
        mutableStateOf((initialMargin?.panicThreshold?.toInt() ?: 150).toString())
    }
    var marginReboundPointsText by remember(editingRule) {
        mutableStateOf((initialMargin?.reboundPoints?.toInt() ?: 20).toString())
    }
    var marginExitThresholdText by remember(editingRule) {
        mutableStateOf((initialMargin?.exitThreshold?.toInt() ?: 170).toString())
    }

    // 複合條件輔助確認 (AND 輔助確認)
    val initialComposite = initialCond as? com.example.domain.model.CompositeCondition
    val initialSubRsi = initialComposite?.conditions?.filterIsInstance<RsiCondition>()?.firstOrNull()
    val initialSubMargin = initialComposite?.conditions?.filterIsInstance<com.example.domain.model.MarginMaintenanceCondition>()?.firstOrNull()
    var enableRsiFilter by remember(editingRule) {
        mutableStateOf(initialSubRsi != null)
    }
    var subRsiThresholdText by remember(editingRule) {
        mutableStateOf((initialSubRsi?.threshold?.toInt() ?: 30).toString())
    }
    var enableMarginFilter by remember(editingRule) {
        mutableStateOf(initialSubMargin != null)
    }
    var subMarginThresholdText by remember(editingRule) {
        mutableStateOf((initialSubMargin?.panicThreshold?.toInt() ?: 150).toString())
    }

    var tickerInput by remember(editingRule) {
        mutableStateOf(editingRule?.displaySymbol ?: "")
    }
    var plannedAmountText by remember(editingRule) {
        mutableStateOf(
            editingRule?.plannedBuyAmount?.let {
                if (editingRule.currency == MarketCurrency.TWD) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }

    val verifiedQuote: StockQuote? = (lookupState as? LookupUiState.Verified)?.quote
    val activeCurrency = verifiedQuote?.currency ?: editingRule?.currency ?: MarketCurrency.TWD
    val activePrice = verifiedQuote?.currentPrice

    LaunchedEffect(verifiedQuote) {
        if (verifiedQuote != null && plannedAmountText.isBlank()) {
            plannedAmountText = if (verifiedQuote.currency == MarketCurrency.TWD) "30000" else "500"
        }
    }

    val effectivePeriod = if (isCustomPeriod) {
        customPeriodText.toIntOrNull() ?: 0
    } else {
        selectedPeriod
    }
    val effectiveTargetPrice = targetPriceText.toDoubleOrNull() ?: 0.0
    val targetTolerance = targetToleranceText.toDoubleOrNull() ?: 0.0
    val plannedAmount = plannedAmountText.toDoubleOrNull() ?: 0.0
    val tolerance = toleranceText.toDoubleOrNull() ?: 1.0
    val effectiveLookback = drawdownLookbackDaysText.toIntOrNull() ?: 250
    val effectiveRsiPeriod = rsiPeriodText.toIntOrNull() ?: 14
    val effectiveRsiThreshold = rsiThresholdText.toDoubleOrNull() ?: 70.0

    val effectivePanic = marginPanicThresholdText.toDoubleOrNull() ?: 150.0
    val effectiveRebound = marginReboundPointsText.toDoubleOrNull() ?: 20.0
    val effectiveExit = marginExitThresholdText.toDoubleOrNull() ?: 170.0

    val isConditionValid = when (selectedCategory) {
        RuleConditionCategory.PRICE_TARGET -> {
            effectiveTargetPrice > 0.0 && targetTolerance >= 0.0
        }
        RuleConditionCategory.MOVING_AVERAGE -> {
            effectivePeriod in 1..3000 &&
                (triggerMode == MovingAverageTriggerMode.BELOW_OR_EQUAL || tolerance >= 0.0)
        }
        RuleConditionCategory.PEAK_DRAWDOWN -> {
            effectiveLookback in 10..3000 && selectedDrawdownPercent > 0
        }
        RuleConditionCategory.RSI -> {
            effectiveRsiPeriod in 2..200 && effectiveRsiThreshold in 1.0..99.0
        }
        RuleConditionCategory.MARGIN -> {
            if (isMarginExitMode) effectiveExit in 100.0..300.0
            else effectivePanic in 100.0..200.0 && effectiveRebound in 1.0..50.0
        }
    }

    val canSave = (verifiedQuote != null || editingRule != null) &&
        plannedAmount > 0.0 &&
        isConditionValid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, HairlineBorder),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (editingRule == null) "新增監控條件" else "編輯監控條件",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )

                // Ticker Lookup Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("股票代號 (如 2330 或 AAPL)") },
                        placeholder = { Text("支援台股/美股自動辨識") },
                        supportingText = if (editingRule == null) {
                            { Text("台股數字代號自動辨識上市櫃；美股直接輸入代號", style = MaterialTheme.typography.labelSmall) }
                        } else null,
                        singleLine = true,
                        enabled = editingRule == null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ticker_input"),
                    )
                    if (editingRule == null) {
                        Button(
                            onClick = { onLookupTicker(tickerInput) },
                            enabled = tickerInput.isNotBlank() && lookupState !is LookupUiState.Loading,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("lookup_ticker_button"),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (lookupState is LookupUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "查詢驗證",
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("查詢")
                            }
                        }
                    }
                }

                // Quick Preset Chips for Index & Major ETFs
                if (editingRule == null) {
                    val presets = listOf(
                        "加權指數" to "^TWII",
                        "櫃買指數" to "^TWOII",
                        "0050" to "0050",
                        "標普500" to "^GSPC",
                        "那斯達克" to "^IXIC",
                        "費半" to "^SOX",
                        "道瓊" to "^DJI",
                        "VIX恐慌" to "^VIX",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "熱門/大盤：",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        presets.forEach { (label, symbol) ->
                            AssistChip(
                                onClick = {
                                    tickerInput = symbol
                                    onLookupTicker(symbol)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Lookup Feedback Banner
                when (lookupState) {
                    is LookupUiState.Verified -> {
                        val q = lookupState.quote
                        val previewSma = if (effectivePeriod > 0) {
                            MovingAverageCalculator.calculateSma(q.closeHistory, effectivePeriod)
                        } else null
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TaiwanGreenContainer,
                            border = BorderStroke(1.dp, TaiwanGreen.copy(alpha = 0.4f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "驗證成功",
                                            tint = TaiwanGreen,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${q.displaySymbol} · ${q.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                        )
                                    }
                                    MarketBadge(currency = q.currency)
                                }
                                Text(
                                    text = "目前現價：${Formatters.formatPrice(q.currentPrice, q.currency)}" +
                                        (previewSma?.let {
                                            " ｜ ${effectivePeriod}MA：${Formatters.formatPrice(it, q.currency)}"
                                        } ?: ""),
                                    style = FinancialTextStyles.numericSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "可用歷史交易日：${q.closeHistory.size} 日",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                    is LookupUiState.Failed -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TaiwanRedContainer,
                            border = BorderStroke(1.dp, TaiwanRed.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = lookupState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaiwanRed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            )
                        }
                    }
                    else -> {}
                }

                // 策略類型切換 Tabs
                Text(
                    text = "監控策略類型",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandSky,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RuleConditionCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSkyContainer,
                                selectedLabelColor = BrandSky,
                            ),
                        )
                    }
                }

                when (selectedCategory) {
                    RuleConditionCategory.PRICE_TARGET -> {
                        Text(
                            text = "觸發方向",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = targetDirection == com.example.domain.model.PriceTargetDirection.BELOW_OR_EQUAL,
                                onClick = { targetDirection = com.example.domain.model.PriceTargetDirection.BELOW_OR_EQUAL },
                                label = { Text("跌破/觸及 (≤ 逢低買)") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                            FilterChip(
                                selected = targetDirection == com.example.domain.model.PriceTargetDirection.ABOVE_OR_EQUAL,
                                onClick = { targetDirection = com.example.domain.model.PriceTargetDirection.ABOVE_OR_EQUAL },
                                label = { Text("突破/觸及 (≥ 停利追價)") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }

                        OutlinedTextField(
                            value = targetPriceText,
                            onValueChange = { targetPriceText = it },
                            label = { Text("設定目標價 (${activeCurrency.prefix})") },
                            prefix = { Text("${activeCurrency.prefix} ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // 快捷填入目標價輔助按鈕
                        if (activePrice != null && activePrice > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "現價快速換算:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                                val presets = listOf(-5 to "-5%", -10 to "-10%", 5 to "+5%", 10 to "+10%")
                                for ((pct, label) in presets) {
                                    val calcPrice = activePrice * (1.0 + pct / 100.0)
                                    val formatted = if (calcPrice >= 100) String.format(java.util.Locale.US, "%.1f", calcPrice)
                                    else String.format(java.util.Locale.US, "%.2f", calcPrice)
                                    AssistChip(
                                        onClick = { targetPriceText = formatted },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = targetToleranceText,
                            onValueChange = { targetToleranceText = it },
                            label = { Text("接近預警容許度 (±%)") },
                            placeholder = { Text("0 表示剛好到達才提醒") },
                            supportingText = {
                                Text(
                                    text = if (targetToleranceText.toDoubleOrNull() ?: 0.0 > 0.0) "距離目標價 ±$targetToleranceText% 時提前預警"
                                    else "0 表示到達或穿越目標價時提醒",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "觸發一次後自動停用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "發送通知後自動標為完成並停用，避免重複提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = autoDisableOnTrigger,
                                onCheckedChange = { autoDisableOnTrigger = it },
                            )
                        }
                    }

                    RuleConditionCategory.MOVING_AVERAGE -> {
                        // MA Period Selection
                        Text(
                            text = "均線天數 (SMA)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            presetPeriods.forEach { period ->
                                val selected = !isCustomPeriod && selectedPeriod == period
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        isCustomPeriod = false
                                        selectedPeriod = period
                                    },
                                    label = {
                                        Text(
                                            text = MovingAverageCondition.maPeriodLabel(period),
                                            style = FinancialTextStyles.numericSmall,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandSkyContainer,
                                        selectedLabelColor = BrandSky,
                                    ),
                                )
                            }
                            FilterChip(
                                selected = isCustomPeriod,
                                onClick = { isCustomPeriod = true },
                                label = { Text("自訂") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                    ),
                            )
                        }

                        if (isCustomPeriod) {
                            OutlinedTextField(
                                value = customPeriodText,
                                onValueChange = { customPeriodText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("自訂均線天數 (1~3000)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Trigger Mode Selection
                        Text(
                            text = "觸發條件模式",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = triggerMode == MovingAverageTriggerMode.BELOW_OR_EQUAL,
                                onClick = { triggerMode = MovingAverageTriggerMode.BELOW_OR_EQUAL },
                                label = { Text("跌破或觸及 (<=)") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                            FilterChip(
                                selected = triggerMode == MovingAverageTriggerMode.WITHIN_PERCENT,
                                onClick = { triggerMode = MovingAverageTriggerMode.WITHIN_PERCENT },
                                label = { Text("接近均線誤差內") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }

                        if (triggerMode == MovingAverageTriggerMode.WITHIN_PERCENT) {
                            OutlinedTextField(
                                value = toleranceText,
                                onValueChange = { toleranceText = it },
                                label = { Text("接近均線誤差百分比 (%)") },
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    RuleConditionCategory.PEAK_DRAWDOWN -> {
                        Text(
                            text = "恐慌抄底回撤幅度（自波段高點下跌）",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            presetDrawdowns.forEach { pct ->
                                FilterChip(
                                    selected = selectedDrawdownPercent == pct,
                                    onClick = { selectedDrawdownPercent = pct },
                                    label = { Text("-$pct%${if (pct == 20) " (黃金勝率)" else ""}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandSkyContainer,
                                        selectedLabelColor = BrandSky,
                                    ),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = drawdownLookbackDaysText,
                            onValueChange = { drawdownLookbackDaysText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("回撤計算週期天數 (預設 250 日 / 52 週)") },
                            suffix = { Text("日") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = "💡 歷史統計（2003~2026）：0050 自波段高點回撤達 -20% 時進場持有 1 年，勝率高達 85.7%。建議可分 2~3 批進場（例如 -20%、-25%、-30%）。",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertAmber,
                        )
                    }

                    RuleConditionCategory.RSI -> {
                        Text(
                            text = "RSI 判斷方向",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = rsiIsAbove,
                                onClick = {
                                    rsiIsAbove = true
                                    // 點擊超買時，若原本是超賣的 30/20 或預設，自動帶入標準超買 70
                                    if (rsiThresholdText == "30" || rsiThresholdText == "20") {
                                        rsiThresholdText = "70"
                                    }
                                },
                                label = { Text("高於閾值（超買/獲利反彈）") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                            FilterChip(
                                selected = !rsiIsAbove,
                                onClick = {
                                    rsiIsAbove = false
                                    // 點擊超賣時，若原本是超買的 70/80，自動帶入標準超賣 30
                                    if (rsiThresholdText == "70" || rsiThresholdText == "80") {
                                        rsiThresholdText = "30"
                                    }
                                },
                                label = { Text("低於閾值（超賣恐慌）") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = rsiPeriodText,
                                onValueChange = { rsiPeriodText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("RSI 週期") },
                                suffix = { Text("日") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = rsiThresholdText,
                                onValueChange = { rsiThresholdText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("閾值") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Text(
                            text = if (rsiIsAbove) "💡 常見出場策略：當 RSI(14) 突破 70 時，表示短線急速反彈過熱，可作為衛星部位（如 00631L）獲利了結退回防守之提醒。"
                                   else "💡 抄底輔助：當 RSI(14) 跌破 30 或 20 時，代表極端超賣恐慌。",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandSky,
                        )
                    }

                    RuleConditionCategory.MARGIN -> {
                        Text(
                            text = "維持率監控模式",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = !isMarginExitMode,
                                onClick = { isMarginExitMode = false },
                                label = { Text("斷頭反彈抄底") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                            FilterChip(
                                selected = isMarginExitMode,
                                onClick = { isMarginExitMode = true },
                                label = { Text("回升安全停利") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }

                        if (!isMarginExitMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = marginPanicThresholdText,
                                    onValueChange = { marginPanicThresholdText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("恐慌斷頭線 (%)") },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = marginReboundPointsText,
                                    onValueChange = { marginReboundPointsText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("低點反彈點數") },
                                    suffix = { Text("點") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = "💡 實證經典參數：全市場融資維持率跌破 150% 後自低點強彈 +20 點（或 0050 維持率破 155% 彈 5 點），2005~2026 長期實證能最精準捕捉台股急殺大底！",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertAmber,
                            )
                        } else {
                            OutlinedTextField(
                                value = marginExitThresholdText,
                                onValueChange = { marginExitThresholdText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("安全出場水位 (%)") },
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "💡 出場停泊策略：當全市場融資維持率回升至 170% 以上，代表籌碼全面回溫、多頭脫離風險，賣出衛星部位，資金回防現金停泊。",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSky,
                            )
                        }
                    }
                }

                // 複合雙重確認區塊 (支援在 目標價 / 均線 / 波段回撤 上疊加輔助過濾)
                if (selectedCategory != RuleConditionCategory.RSI && selectedCategory != RuleConditionCategory.MARGIN) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, HairlineBorder),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "🛡️ 複合策略雙重確認 (選用)",
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandSky,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "同時滿足以下輔助條件才發出警報，有效過濾空頭破線接飛刀的假訊號：",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )

                            // RSI 輔助開關
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "搭配 RSI < 30 超賣過濾",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = "股價達到目標/均線時，RSI 必須低於閾值才通知",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                                Switch(
                                    checked = enableRsiFilter,
                                    onCheckedChange = { enableRsiFilter = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CanvasObsidian,
                                        checkedTrackColor = BrandSky,
                                    ),
                                )
                            }

                            if (enableRsiFilter) {
                                OutlinedTextField(
                                    value = subRsiThresholdText,
                                    onValueChange = { subRsiThresholdText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("RSI 超賣門檻 (建議 30)") },
                                    suffix = { Text("分以下") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            // 台股融資維持率輔助開關 (限台股)
                            if (activeCurrency == MarketCurrency.TWD) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "搭配全市場融資維持率恐慌抄底",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                        )
                                        Text(
                                            text = "全市場融資維持率跌破指定門檻且反彈時才提醒",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    Switch(
                                        checked = enableMarginFilter,
                                        onCheckedChange = { enableMarginFilter = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CanvasObsidian,
                                            checkedTrackColor = AlertAmber,
                                        ),
                                    )
                                }

                                if (enableMarginFilter) {
                                    OutlinedTextField(
                                        value = subMarginThresholdText,
                                        onValueChange = { subMarginThresholdText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("維持率恐慌線 (%)") },
                                        suffix = { Text("% 以下") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }

                // Planned Buy Amount
                OutlinedTextField(
                    value = plannedAmountText,
                    onValueChange = { plannedAmountText = it },
                    label = { Text("預定買入金額 (${activeCurrency.prefix})") },
                    prefix = { Text("${activeCurrency.prefix} ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planned_amount_input"),
                )

                if (activePrice != null && activePrice > 0.0 && plannedAmount > 0.0) {
                    val estShares = plannedAmount / activePrice
                    Text(
                        text = "以目前現價估算：約可買入 ${Formatters.formatShares(estShares)} 股",
                        style = FinancialTextStyles.numericSmall,
                        color = AlertAmber,
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = canSave,
                        modifier = Modifier.testTag("save_rule_button"),
                        onClick = {
                            val symbol = verifiedQuote?.symbol ?: editingRule?.symbol ?: return@Button
                            val displaySym = verifiedQuote?.displaySymbol ?: editingRule?.displaySymbol ?: symbol
                            val name = verifiedQuote?.name ?: editingRule?.name ?: displaySym
                            val baseCondition: AlertCondition = when (selectedCategory) {
                                RuleConditionCategory.PRICE_TARGET -> com.example.domain.model.PriceTargetCondition(
                                    targetPrice = effectiveTargetPrice,
                                    direction = targetDirection,
                                    tolerancePercent = targetTolerance.coerceAtLeast(0.0),
                                    autoDisableOnTrigger = autoDisableOnTrigger,
                                )
                                RuleConditionCategory.MOVING_AVERAGE -> MovingAverageCondition(
                                    periodDays = effectivePeriod,
                                    triggerMode = triggerMode,
                                    tolerancePercent = tolerance.coerceAtLeast(0.0),
                                )
                                RuleConditionCategory.PEAK_DRAWDOWN -> PeakDrawdownCondition(
                                    lookbackDays = effectiveLookback,
                                    drawdownPercent = selectedDrawdownPercent.toDouble(),
                                )
                                RuleConditionCategory.RSI -> RsiCondition(
                                    periodDays = effectiveRsiPeriod,
                                    threshold = effectiveRsiThreshold,
                                    isAbove = rsiIsAbove,
                                )
                                RuleConditionCategory.MARGIN -> com.example.domain.model.MarginMaintenanceCondition(
                                    panicThreshold = effectivePanic,
                                    reboundPoints = effectiveRebound,
                                    isExitMode = isMarginExitMode,
                                    exitThreshold = effectiveExit,
                                )
                            }

                            val condition: AlertCondition = if (selectedCategory != RuleConditionCategory.RSI && selectedCategory != RuleConditionCategory.MARGIN) {
                                val extras = mutableListOf<AlertCondition>()
                                if (enableRsiFilter) {
                                    val rsiTh = subRsiThresholdText.toDoubleOrNull() ?: 30.0
                                    extras.add(RsiCondition(periodDays = 14, threshold = rsiTh, isAbove = false))
                                }
                                if (enableMarginFilter && activeCurrency == MarketCurrency.TWD) {
                                    val marginTh = subMarginThresholdText.toDoubleOrNull() ?: 150.0
                                    extras.add(com.example.domain.model.MarginMaintenanceCondition(panicThreshold = marginTh, reboundPoints = 20.0))
                                }
                                if (extras.isNotEmpty()) {
                                    com.example.domain.model.CompositeCondition(conditions = listOf(baseCondition) + extras)
                                } else {
                                    baseCondition
                                }
                            } else {
                                baseCondition
                            }
                            onSave(
                                editingRule?.id,
                                symbol,
                                displaySym,
                                name,
                                activeCurrency,
                                condition,
                                plannedAmount,
                                verifiedQuote,
                            )
                        },
                    ) {
                        Text("儲存監控規則")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuyRecordDialog(
    prefill: QuickBuyPrefill?,
    editingRecord: BuyRecordEntity?,
    extraTriggerSuggestions: List<String> = emptyList(),
    watchlistProxyTargets: List<ProxyBuyTarget> = emptyList(),
    onLookupTargetQuote: (suspend (String) -> TickerLookupResult)? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        symbol: String,
        displaySymbol: String,
        name: String,
        currency: MarketCurrency,
        buyDate: String,
        shares: Double,
        unitPrice: Double,
        triggerTag: String,
        resetRuleId: Long?,
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
) {
    val isManualNew = prefill == null && editingRecord == null
    val isIndexSignal = prefill != null && editingRecord == null && isMarketIndexSymbol(prefill.symbol)
    val scope = rememberCoroutineScope()

    var buyDate by remember(prefill, editingRecord) {
        mutableStateOf(
            editingRecord?.buyDate ?: prefill?.buyDate ?: Formatters.formatDateYmd()
        )
    }
    var symbolInput by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> editingRecord.displaySymbol
                isIndexSignal -> ""
                prefill != null -> prefill.displaySymbol
                else -> ""
            }
        )
    }
    var resolvedTargetSymbol by remember(prefill, editingRecord) {
        mutableStateOf<String?>(null)
    }
    var nameInput by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> editingRecord.name
                isIndexSignal -> ""
                prefill != null -> prefill.name
                else -> ""
            }
        )
    }
    var currency by remember(prefill, editingRecord) {
        mutableStateOf(editingRecord?.currency ?: prefill?.currency ?: MarketCurrency.TWD)
    }
    var unitPriceText by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> Formatters.formatRawPriceForInput(editingRecord.unitPrice, editingRecord.currency)
                isIndexSignal -> ""
                prefill != null && prefill.unitPrice > 0.0 -> Formatters.formatRawPriceForInput(prefill.unitPrice, prefill.currency)
                else -> ""
            }
        )
    }
    var sharesText by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> editingRecord.shares.toString()
                isIndexSignal -> ""
                prefill != null && prefill.estimatedShares > 0.0 -> {
                    String.format(java.util.Locale.US, "%.2f", prefill.estimatedShares)
                }
                else -> ""
            }
        )
    }
    var triggerTag by remember(prefill, editingRecord) {
        mutableStateOf(
            editingRecord?.triggerTag ?: prefill?.triggerTag ?: "手動買入"
        )
    }
    var isLookingUpTarget by remember { mutableStateOf(false) }
    var targetLookupHint by remember { mutableStateOf<String?>(null) }

    val applyProxyTarget: (String, String?, MarketCurrency?) -> Unit = { rawSymbol, fallbackName, fallbackCurrency ->
        val cleaned = rawSymbol.trim().uppercase()
        symbolInput = cleaned
        resolvedTargetSymbol = cleaned
        if (!fallbackName.isNullOrBlank()) nameInput = fallbackName
        if (fallbackCurrency != null) currency = fallbackCurrency
        targetLookupHint = null

        if (onLookupTargetQuote != null && cleaned.isNotEmpty() && !isMarketIndexSymbol(cleaned)) {
            isLookingUpTarget = true
            scope.launch {
                when (val result = onLookupTargetQuote(cleaned)) {
                    is TickerLookupResult.Success -> {
                        val quote = result.quote
                        resolvedTargetSymbol = quote.symbol
                        symbolInput = quote.displaySymbol
                        nameInput = quote.name
                        currency = quote.currency
                        if (quote.currentPrice > 0.0) {
                            unitPriceText = Formatters.formatRawPriceForInput(quote.currentPrice, quote.currency)
                            val planned = prefill?.plannedAmount ?: 0.0
                            if (planned > 0.0) {
                                sharesText = String.format(java.util.Locale.US, "%.2f", planned / quote.currentPrice)
                            }
                        }
                        targetLookupHint = "已帶入 ${quote.displaySymbol} 最新報價"
                    }
                    is TickerLookupResult.NotFound -> targetLookupHint = result.message
                    is TickerLookupResult.RateLimited -> targetLookupHint = result.message
                    is TickerLookupResult.Error -> targetLookupHint = result.message
                }
                isLookingUpTarget = false
            }
        }
    }

    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val shares = sharesText.toDoubleOrNull() ?: 0.0
    val totalCost = unitPrice * shares

    val canSave = symbolInput.isNotBlank() &&
        !isMarketIndexSymbol(symbolInput) &&
        buyDate.isNotBlank() &&
        unitPrice > 0.0 &&
        shares > 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, HairlineBorder),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = when {
                        editingRecord != null -> "編輯成交紀錄"
                        prefill != null -> "記錄買入確認"
                        else -> "手動新增買入紀錄"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )

                if (prefill != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isIndexSignal) {
                                        "觸發訊號：${prefill.displaySymbol} · ${prefill.name}"
                                    } else {
                                        "${prefill.displaySymbol} · ${prefill.name}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "觸發標籤：${prefill.triggerTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AlertAmber,
                                )
                                if (isIndexSignal) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "大盤指數無法直接買賣，請選擇下方對應 ETF 或輸入實際買入標的。",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            MarketBadge(currency = currency)
                        }
                    }
                }

                if (isIndexSignal && prefill != null) {
                    val proxyOptions = remember(prefill.symbol, watchlistProxyTargets) {
                        (recommendedProxyTargetsForIndex(prefill.symbol) + watchlistProxyTargets)
                            .filter { !isMarketIndexSymbol(it.symbol) }
                            .distinctBy { it.symbol.uppercase() }
                    }
                    Text(
                        text = "選擇實際買入標的",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        proxyOptions.forEach { target ->
                            val isSelected = symbolInput.trim().equals(target.symbol, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    applyProxyTarget(target.symbol, target.name, target.currency)
                                },
                                label = { Text("${target.symbol} ${target.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSkyContainer,
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = buyDate,
                    onValueChange = { buyDate = it },
                    label = { Text("買入日期 (YYYY/MM/DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isManualNew || editingRecord != null || isIndexSignal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = symbolInput,
                            onValueChange = {
                                symbolInput = it.uppercase()
                                resolvedTargetSymbol = null
                                if (it.any { c -> c.isDigit() } && !it.contains('.')) {
                                    currency = MarketCurrency.TWD
                                } else if (it.isNotEmpty() && it.all { c -> c.isLetter() }) {
                                    currency = MarketCurrency.USD
                                }
                            },
                            label = { Text("買入代號") },
                            placeholder = { Text("0050.TW / 00631L.TW") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("標的名稱") },
                            placeholder = { Text("元大台灣50") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (isIndexSignal && onLookupTargetQuote != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = targetLookupHint ?: "輸入任意代號後可點擊右側帶入最新市價",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetLookupHint?.startsWith("已帶入") == true) BrandSky else TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                enabled = symbolInput.isNotBlank() && !isMarketIndexSymbol(symbolInput) && !isLookingUpTarget,
                                onClick = { applyProxyTarget(symbolInput, nameInput.ifBlank { null }, null) },
                            ) {
                                if (isLookingUpTarget) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("帶入現價")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = currency == MarketCurrency.TWD,
                            onClick = { currency = MarketCurrency.TWD },
                            label = { Text("台股 (NT$)") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = currency == MarketCurrency.USD,
                            onClick = { currency = MarketCurrency.USD },
                            label = { Text("美股 (US$)") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("成交單價 (${currency.prefix})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("buy_unit_price_input"),
                    )
                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it },
                        label = { Text("買入股數 (支援零股/小數)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("buy_shares_input"),
                    )
                }

                val presetTriggerOptions = remember(extraTriggerSuggestions) {
                    (
                        listOf(
                            "手動買入",
                            "融資破150%反彈+20",
                            "維持率回升>170%",
                            "波段高點回撤 -20%",
                            "波段高點回撤 -25%",
                            "波段高點回撤 -30%",
                            "RSI>70",
                            "週線 5MA",
                            "雙週線 10MA",
                            "月線 20MA",
                            "季線 60MA",
                            "半年線 120MA",
                            "年線 240MA",
                            "停泊資金調度",
                            "觸及目標價",
                            "技術指標訊號",
                        ) + extraTriggerSuggestions
                    )
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                }

                Text(
                    text = "買入觸發情況",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetTriggerOptions.forEach { option ->
                        val isSelected = triggerTag.trim() == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { triggerTag = option },
                            label = { Text(option) },
                            modifier = Modifier.testTag("trigger_chip_$option"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSkyContainer,
                                selectedLabelColor = BrandSky,
                            ),
                        )
                    }
                }

                OutlinedTextField(
                    value = triggerTag,
                    onValueChange = { triggerTag = it },
                    label = { Text("觸發條件標籤（可選上方或自訂）") },
                    placeholder = { Text("例如：年線 240MA、跌破目標價") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buy_trigger_tag_input"),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, HairlineBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "預估總花費成本",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                        Text(
                            text = Formatters.formatAmount(totalCost, currency),
                            style = FinancialTextStyles.priceLarge,
                            color = TextPrimary,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editingRecord != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(editingRecord.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = TaiwanRed),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "刪除紀錄",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刪除")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = canSave,
                            modifier = Modifier.testTag("confirm_buy_record_button"),
                            onClick = {
                                val resolvedSymbol = when {
                                    isIndexSignal -> resolvedTargetSymbol ?: symbolInput.trim().uppercase()
                                    prefill != null -> prefill.symbol
                                    editingRecord != null -> editingRecord.symbol
                                    else -> symbolInput.trim().uppercase()
                                }
                                val resolvedDisplay = symbolInput.trim().uppercase()
                                onSave(
                                    editingRecord?.id,
                                    resolvedSymbol,
                                    resolvedDisplay,
                                    nameInput.trim().ifEmpty { resolvedDisplay },
                                    currency,
                                    buyDate,
                                    shares,
                                    unitPrice,
                                    triggerTag,
                                    prefill?.ruleId,
                                )
                            },
                        ) {
                            Text(
                                text = if (editingRecord != null) "儲存修改" else "確認寫入成交紀錄",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
