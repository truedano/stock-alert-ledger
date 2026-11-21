package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AlertLogEntity
import com.example.domain.model.Formatters
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.BrandSky
import com.example.ui.theme.FinancialTextStyles
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TaiwanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AlertLogDialog(
    logs: List<AlertLogEntity>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteLog: (Long) -> Unit,
) {
    var selectedSymbolFilter by remember { mutableStateOf<String?>(null) }

    // 依股票代號動態產出篩選標籤清單
    val availableSymbols = remember(logs) {
        logs.map { it.symbol }.distinct()
    }

    // 依選定的標籤過濾紀錄
    val filteredLogs = remember(logs, selectedSymbolFilter) {
        if (selectedSymbolFilter == null) logs
        else logs.filter { it.symbol == selectedSymbolFilter }
    }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = BrandSky,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = if (selectedSymbolFilter == null) "警報歷史紀錄 (${logs.size})"
                            else "警報歷史 (${filteredLogs.size}/${logs.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                    }

                    if (logs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            colors = ButtonDefaults.textButtonColors(contentColor = TaiwanRed),
                        ) {
                            Text("清空紀錄")
                        }
                    }
                }

                Text(
                    text = "記錄每次條件觸發的精準時間、當下股價與策略指標",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )

                // 標籤篩選列（當有多檔股票時顯示 FilterChips）
                if (availableSymbols.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
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
                }

                HorizontalDivider(color = HairlineBorder)

                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            AlertLogItemCard(
                                log = log,
                                onDelete = { onDeleteLog(log.id) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("關閉")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertLogItemCard(
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
                .padding(12.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "刪除此紀錄",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
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
