package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import java.util.Locale
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DailyStat

/**
 * Komponen Grafik Batang Native Jetpack Compose untuk memvisualisasikan konsumsi Air Minum & Kopi/Kafein harian selama 7 hari.
 */
@Composable
fun WaterBarChartCard(
    dailyStats: List<DailyStat>,
    dailyGoalMl: Int = 2000,
    modifier: Modifier = Modifier
) {
    var selectedStat by remember { mutableStateOf<DailyStat?>(null) }

    val waterColor = MaterialTheme.colorScheme.primary
    val targetReachedColor = Color(0xFF10B981) // Emerald Green
    val coffeeColor = Color(0xFFD97706) // Amber / Coffee Brown

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_bar_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Grafik Konsumsi Air & Kopi (7 Hari)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Target harian: $dailyGoalMl ml air",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = targetReachedColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(targetReachedColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${dailyStats.count { it.isTargetReached }}/7 Capai Target",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = targetReachedColor
                        )
                    }
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Water Legend Item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(waterColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "💧 Air (L)",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Coffee Legend Item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(coffeeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "☕ Kafein (mg)",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive Dual Bar Chart Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                val maxWaterMl = (dailyStats.maxOfOrNull { it.waterMl } ?: dailyGoalMl).coerceAtLeast(dailyGoalMl)
                val maxCaffeineMg = (dailyStats.maxOfOrNull { it.caffeineMg } ?: 400).coerceAtLeast(200)

                val goalRatio = (dailyGoalMl.toFloat() / maxWaterMl.toFloat()).coerceIn(0.1f, 1f)

                // Goal reference line for water
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = (110 * goalRatio).dp + 22.dp)
                        .height(1.5.dp)
                        .background(targetReachedColor.copy(alpha = 0.5f))
                        .align(Alignment.BottomStart)
                )

                // Row of 7 Days
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyStats.forEach { stat ->
                        val isSelected = selectedStat?.dateIso == stat.dateIso

                        val rawWaterRatio = if (maxWaterMl > 0) stat.waterMl.toFloat() / maxWaterMl.toFloat() else 0f
                        val animatedWaterHeight by animateFloatAsState(
                            targetValue = rawWaterRatio.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 600),
                            label = "water_bar_${stat.dateIso}"
                        )

                        val rawCoffeeRatio = if (maxCaffeineMg > 0) stat.caffeineMg.toFloat() / maxCaffeineMg.toFloat() else 0f
                        val animatedCoffeeHeight by animateFloatAsState(
                            targetValue = rawCoffeeRatio.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 600),
                            label = "coffee_bar_${stat.dateIso}"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedStat = if (isSelected) null else stat
                                }
                        ) {
                            // Amount label above bars
                            Text(
                                text = if (stat.waterMl > 0) String.format(Locale.US, "%.1fL", stat.waterMl / 1000f) else "-",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (stat.isTargetReached) FontWeight.Bold else FontWeight.Normal,
                                color = if (stat.isTargetReached) targetReachedColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dual Bar Group Container
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .height(105.dp)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 2.dp)
                            ) {
                                // 1. Water Bar (Air)
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animatedWaterHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (stat.isTargetReached) targetReachedColor else waterColor
                                            )
                                    )
                                }

                                // 2. Coffee/Caffeine Bar (Kopi)
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animatedCoffeeHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(coffeeColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Day Label with optional target checkmark
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stat.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (stat.isTargetReached) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Target reached",
                                        tint = targetReachedColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detail Card when a day is tapped
            selectedStat?.let { stat ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detail Hari ${stat.dayLabel} (${stat.dateIso}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "💧 ${stat.waterMl} ml",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = waterColor
                            )
                            Text(
                                text = "☕ ${stat.caffeineMg} mg",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = coffeeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

