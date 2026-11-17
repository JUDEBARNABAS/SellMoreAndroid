package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AuditLogEntity
import com.example.data.local.TransactionEntity
import com.example.model.OrderLineItem
import com.example.model.formatTimestamp
import com.example.model.formatUgx
import com.example.ui.theme.CocoaCopper
import com.example.ui.theme.DispenseEmerald
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.EspressoBorder
import com.example.ui.theme.EspressoSurface
import com.example.ui.theme.EspressoSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StrawberryPink
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VanillaAmber
import java.util.Calendar

@Composable
fun SalesAnalyticsTab(
    transactions: List<TransactionEntity>,
    decodeItems: (String) -> List<OrderLineItem>
) {
    val totalRevenue = transactions.sumOf { it.totalUgx }
    val allItems = remember(transactions) {
        transactions.flatMap { decodeItems(it.itemsJson) }
    }
    val totalBottles = allItems.sumOf { it.quantity }
    val avgOrder = if (transactions.isNotEmpty()) totalRevenue / transactions.size else 0

    // Hourly revenue buckets (last 6 active buckets or 8:00..20:00)
    val hourlyBuckets = remember(transactions) {
        val buckets = LinkedHashMap<String, Int>()
        listOf("09h", "12h", "15h", "18h", "21h").forEach { buckets[it] = 0 }
        val cal = Calendar.getInstance()
        transactions.forEach { txn ->
            cal.timeInMillis = txn.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val key = when {
                hour < 11 -> "09h"
                hour < 14 -> "12h"
                hour < 17 -> "15h"
                hour < 20 -> "18h"
                else -> "21h"
            }
            buckets[key] = (buckets[key] ?: 0) + txn.totalUgx
        }
        buckets.entries.map { it.key to it.value }
    }

    // Flavor breakdown
    val flavorTotals = remember(allItems) {
        val map = LinkedHashMap<String, Int>()
        allItems.forEach { item ->
            map[item.name] = (map[item.name] ?: 0) + item.quantity
        }
        map.entries.sortedByDescending { it.value }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsKpiCard("REVENUE", formatUgx(totalRevenue), DispenseEmerald, Modifier.weight(1f))
                AnalyticsKpiCard("AVG ORDER", formatUgx(avgOrder), TelemetryCyan, Modifier.weight(1f))
                AnalyticsKpiCard("BOTTLES", "$totalBottles vended", StrawberryPink, Modifier.weight(1f))
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hourly Revenue Distribution", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    val maxVal = (hourlyBuckets.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barCount = hourlyBuckets.size
                        val slotWidth = size.width / barCount
                        val barWidth = slotWidth * 0.52f
                        hourlyBuckets.forEachIndexed { idx, (_, value) ->
                            val ratio = value.toFloat() / maxVal.toFloat()
                            val barHeight = (size.height - 16f) * ratio.coerceAtLeast(0.06f)
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val top = size.height - barHeight
                            drawRoundRect(
                                color = if (value > 0) TelemetryCyan else EspressoBorder,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        hourlyBuckets.forEach { (label, amount) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = if (amount > 0) "${amount / 1000}k" else "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TelemetryCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Flavor Breakdown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (flavorTotals.isEmpty()) {
                        Text("No bottles vended yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    } else {
                        val maxQty = totalBottles.coerceAtLeast(1)
                        flavorTotals.forEach { (flavor, qty) ->
                            val barColor = when {
                                flavor.contains("Straw", true) -> StrawberryPink
                                flavor.contains("Choc", true) -> CocoaCopper
                                else -> VanillaAmber
                            }
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(flavor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text("$qty bottles", style = MaterialTheme.typography.labelMedium, color = barColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { qty.toFloat() / maxQty.toFloat() },
                                    color = barColor,
                                    trackColor = EspressoSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EspressoSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = accent
            )
        }
    }
}

@Composable
fun AuditLogsTab(
    logs: List<AuditLogEntity>,
    onClearLogs: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    val categories = listOf("ALL", "HARDWARE", "PAYMENT", "INVENTORY", "SHEETS", "SYSTEM")

    val filtered = logs.filter { selectedCategory == "ALL" || it.category.equals(selectedCategory, true) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) TelemetryCyan else EspressoSurfaceElevated,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCategory = cat }
                                .testTag("audit_filter_$cat")
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) EspressoSurface else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onClearLogs) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Audit Logs", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        items(filtered, key = { it.id }) { log ->
            val sevColor = when (log.severity.uppercase()) {
                "SUCCESS" -> DispenseEmerald
                "WARN" -> VanillaAmber
                "ERROR" -> ErrorRose
                else -> TelemetryCyan
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EspressoSurface,
                border = BorderStroke(1.dp, EspressoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${log.category}] ${log.action}",
                            style = MaterialTheme.typography.labelLarge,
                            color = sevColor
                        )
                        Text(
                            text = formatTimestamp(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.details, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
