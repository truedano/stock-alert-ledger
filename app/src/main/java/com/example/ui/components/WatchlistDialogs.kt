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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.domain.model.RsiCondition
import com.example.domain.model.StockQuote
import com.example.notification.QuickBuyPrefill
import com.example.ui.LookupUiState
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandSkyContainer
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
    MOVING_AVERAGE("均線 (SMA)"),
    PEAK_DRAWDOWN("波段回撤抄底"),
    RSI("RSI 指標"),
    MARGIN("融資維持率斷頭"),
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
        is PeakDrawdownCondition -> RuleConditionCategory.PEAK_DRAWDOWN
        is RsiCondition -> RuleConditionCategory.RSI
        is com.example.domain.model.MarginMaintenanceCondition -> RuleConditionCategory.MARGIN
        else -> RuleConditionCategory.MOVING_AVERAGE
    }
    var selectedCategory by remember(editingRule) { mutableStateOf(initialCategory) }

    // MA 狀態
    val initialMa = initialCond as? MovingAverageCondition
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
    val initialDrawdown = initialCond as? PeakDrawdownCondition
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
    val plannedAmount = plannedAmountText.toDoubleOrNull() ?: 0.0
    val tolerance = toleranceText.toDoubleOrNull() ?: 1.0
    val effectiveLookback = drawdownLookbackDaysText.toIntOrNull() ?: 250
    val effectiveRsiPeriod = rsiPeriodText.toIntOrNull() ?: 14
    val effectiveRsiThreshold = rsiThresholdText.toDoubleOrNull() ?: 70.0

    val effectivePanic = marginPanicThresholdText.toDoubleOrNull() ?: 150.0
    val effectiveRebound = marginReboundPointsText.toDoubleOrNull() ?: 20.0
    val effectiveExit = marginExitThresholdText.toDoubleOrNull() ?: 170.0

    val isConditionValid = when (selectedCategory) {
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
                        placeholder = { Text("台股免填 .TW/.TWO") },
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
                    else -> {
                        if (editingRule == null) {
                            Text(
                                text = "輸入台股數字代號（例如 2330、0050、6488）將自動辨識上市 .TW / 上櫃 .TWO；美股輸入英文代號（如 AAPL、NVDA）即可。",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                // 策略類型切換 Tabs
                Text(
                    text = "監控策略類型",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandSky,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RuleConditionCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.title) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSkyContainer,
                                selectedLabelColor = BrandSky,
                            ),
                        )
                    }
                }

                when (selectedCategory) {
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
                            val condition: AlertCondition = when (selectedCategory) {
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

    var buyDate by remember(prefill, editingRecord) {
        mutableStateOf(
            editingRecord?.buyDate ?: prefill?.buyDate ?: Formatters.formatDateYmd()
        )
    }
    var symbolInput by remember(prefill, editingRecord) {
        mutableStateOf(editingRecord?.displaySymbol ?: prefill?.displaySymbol ?: "")
    }
    var nameInput by remember(prefill, editingRecord) {
        mutableStateOf(editingRecord?.name ?: prefill?.name ?: "")
    }
    var currency by remember(prefill, editingRecord) {
        mutableStateOf(editingRecord?.currency ?: prefill?.currency ?: MarketCurrency.TWD)
    }
    var unitPriceText by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> Formatters.formatRawPriceForInput(editingRecord.unitPrice, editingRecord.currency)
                prefill != null && prefill.unitPrice > 0.0 -> Formatters.formatRawPriceForInput(prefill.unitPrice, prefill.currency)
                else -> ""
            }
        )
    }
    var sharesText by remember(prefill, editingRecord) {
        mutableStateOf(
            when {
                editingRecord != null -> editingRecord.shares.toString()
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

    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val shares = sharesText.toDoubleOrNull() ?: 0.0
    val totalCost = unitPrice * shares

    val canSave = symbolInput.isNotBlank() && buyDate.isNotBlank() && unitPrice > 0.0 && shares > 0.0

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
                            Column {
                                Text(
                                    text = "${prefill.displaySymbol} · ${prefill.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "觸發標籤：${prefill.triggerTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AlertAmber,
                                )
                            }
                            MarketBadge(currency = currency)
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

                if (isManualNew || editingRecord != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = symbolInput,
                            onValueChange = {
                                symbolInput = it.uppercase()
                                if (it.any { c -> c.isDigit() } && !it.contains('.')) {
                                    currency = MarketCurrency.TWD
                                } else if (it.isNotEmpty() && it.all { c -> c.isLetter() }) {
                                    currency = MarketCurrency.USD
                                }
                            },
                            label = { Text("股票代號") },
                            placeholder = { Text("2330.TW / AAPL") },
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
                            label = { Text("股票名稱") },
                            placeholder = { Text("台積電") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
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
                                val resolvedSymbol = prefill?.symbol
                                    ?: editingRecord?.symbol
                                    ?: symbolInput.trim().uppercase()
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
