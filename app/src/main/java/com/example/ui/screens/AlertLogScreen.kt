package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AlertLogEntity
import com.example.domain.model.Formatters
import com.example.ui.components.MarketBadge
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.BrandSky
import com.example.ui.theme.CanvasObsidian
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertLogScreen(
    logs: List<AlertLogEntity>,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSymbolFilter by remember { mutableStateOf<String?>(null) }

    val availableSymbols = remember(logs) {
        logs.map { it.symbol }.distinct()
    }

    val filteredLogs = remember(logs, selectedSymbolFilter) {
        if (selectedSymbolFilter == null) logs
        else logs.filter { it.symbol == selectedSymbolFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasObsidian,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedSymbolFilter == null) {
                                "警報歷史紀錄 (${logs.size})"
                            } else {
                                "警報歷史 (${filteredLogs.size}/${logs.size})"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                        Text(
                            text = "記錄每次條件觸發的時間、成交價與策略指標",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                        )
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            colors = ButtonDefaults.textButtonColors(contentColor = TaiwanRed),
                        ) {
                            Text("清空紀錄")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceElevated,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
            ) {
                if (availableSymbols.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSymbolFilter == null,
                                onClick = { selectedSymbolFilter = null },
                                label = { Text("全部 (${logs.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSky.copy(alpha = 0.2f),
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }
                        items(availableSymbols) { symbol ->
                            val symbolCount = logs.count { it.symbol == symbol }
                            val display = logs.firstOrNull { it.symbol == symbol }?.let {
                                if (it.name.isNotBlank() && it.name.any { ch -> ch.code in 0x4e00..0x9fff }) it.name
                                else it.displaySymbol
                            } ?: symbol

                            FilterChip(
                                selected = selectedSymbolFilter == symbol,
                                onClick = {
                                    selectedSymbolFilter = if (selectedSymbolFilter == symbol) null else symbol
                                },
                                label = { Text("$display ($symbolCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandSky.copy(alpha = 0.2f),
                                    selectedLabelColor = BrandSky,
                                ),
                            )
                        }
                    }
                    HorizontalDivider(color = HairlineBorder)
                }

                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (logs.isEmpty()) "目前尚無歷史觸發紀錄" else "無符合篩選條件的紀錄",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                        Text(
                            text = if (logs.isEmpty()) "當股價或指標達到您設定的條件時，此處將自動記錄觸發歷程"
                            else "點擊「全部」可查看所有警報歷程",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            AlertLogFullCard(
                                log = log,
                                onDelete = { onDeleteLog(log.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertLogFullCard(
    log: AlertLogEntity,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AlertAmberContainer,
                    ) {
                        Text(
                            text = log.triggerTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertAmber,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = Formatters.formatDateTime(log.triggeredTimestamp),
                        style = FinancialTextStyles.numericSmall,
                        color = TextSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarketBadge(currency = log.currency)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "刪除此紀錄",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${log.displaySymbol} · ${log.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (log.note.isNotBlank()) {
                        Text(
                            text = log.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "觸發價",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = Formatters.formatPrice(log.triggeredPrice, log.currency),
                        style = FinancialTextStyles.numericMedium,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
