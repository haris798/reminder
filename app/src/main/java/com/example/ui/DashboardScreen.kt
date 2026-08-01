package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.FloatingActionButton
import com.example.data.model.WaterLog
import com.example.ui.components.AchievementsCard
import com.example.ui.components.AddCoffeeDialog
import com.example.ui.components.AddWaterDialog
import com.example.ui.components.CircularWaterProgress
import com.example.ui.components.QuickWaterBottomSheet
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.ui.components.WaterBarChartCard
import com.example.ui.preview.HydrationPreviewParameterProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode

@Composable
fun DashboardScreen(
    viewModel: HydrationViewModel,
    isSupabaseConnected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    DashboardContent(
        state = state,
        isSupabaseConnected = isSupabaseConnected,
        onAddWater = { viewModel.addWater(it) },
        onAddCoffee = { type, caffeine -> viewModel.addCoffee(type, caffeine) },
        onToggleTheme = {
            val nextTheme = if (state.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            viewModel.setThemeMode(nextTheme)
        },
        onTriggerManualSync = { viewModel.triggerManualSync() },
        onSendTestNotification = { viewModel.sendTestNotification() },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: HydrationUiState,
    isSupabaseConnected: Boolean = false,
    onAddWater: (Int) -> Unit = {},
    onAddCoffee: (type: String, caffeineMg: Int) -> Unit = { _, _ -> },
    onToggleTheme: () -> Unit = {},
    onTriggerManualSync: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showAddCoffeeDialog by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }
    var showQuickWaterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickWaterSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("quick_water_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Tambah Air"
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Minum.ku",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = state.currentDateFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Sun / Moon Theme Toggle Action Button
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (state.themeMode == ThemeMode.DARK) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Supabase Online Connected Badge (only shown when connected)
                    if (isSupabaseConnected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF3ECF8E).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("supabase_online_badge")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3ECF8E))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Supabase Online",
                                tint = Color(0xFF3ECF8E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Supabase",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF3ECF8E)
                            )
                        }
                    }

                    // Test notification action button
                    IconButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.testTag("test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Kirim Pengingat Notifikasi",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Manual background sync button
                    IconButton(
                        onClick = onTriggerManualSync,
                        enabled = !state.isSyncing,
                        modifier = Modifier.testTag("manual_sync_button")
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronisasi Cloud",
                                tint = if (state.unsyncedCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hydration Progress Section
            item {
                WaterHydrationCard(
                    currentMl = state.currentWaterMl,
                    targetMl = state.dailyWaterGoalMl,
                    onQuickAdd250 = { onAddWater(250) },
                    onQuickAdd500 = { onAddWater(500) },
                    onCustomAdd = { showCustomWaterDialog = true }
                )
            }

            // 2. 7-Day Water Consumption Bar Chart
            item {
                WaterBarChartCard(
                    dailyStats = state.weeklySummary.dailyStats,
                    dailyGoalMl = state.dailyWaterGoalMl
                )
            }

            // 3. Hydration & Streak Achievements / Badges
            item {
                AchievementsCard(
                    consecutiveStreakDays = state.weeklySummary.consecutiveStreakDays,
                    targetReachedDays7Days = state.weeklySummary.targetReachedDays,
                    isCaffeineSafe = state.totalCaffeineMg <= 400
                )
            }

            // 4. Coffee Tracker Section
            item {
                CoffeeTrackerHeaderCard(
                    totalCaffeineMg = state.totalCaffeineMg,
                    coffeeCount = state.coffeeLogsToday.size,
                    onAddCoffeeClick = { showAddCoffeeDialog = true }
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Quick Water & Coffee Bottom Sheet
    if (showQuickWaterSheet) {
        QuickWaterBottomSheet(
            onDismissRequest = { showQuickWaterSheet = false },
            onPresetSelected = { amount ->
                onAddWater(amount)
                showQuickWaterSheet = false
            },
            onCoffeePresetSelected = { type, caffeine ->
                onAddCoffee(type, caffeine)
                showQuickWaterSheet = false
            }
        )
    }

    // Dialogs
    if (showAddCoffeeDialog) {
        AddCoffeeDialog(
            onDismiss = { showAddCoffeeDialog = false },
            onConfirm = { type, caffeine ->
                onAddCoffee(type, caffeine)
                showAddCoffeeDialog = false
            }
        )
    }

    if (showCustomWaterDialog) {
        AddWaterDialog(
            onDismiss = { showCustomWaterDialog = false },
            onConfirm = { amount ->
                onAddWater(amount)
                showCustomWaterDialog = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview(
    @PreviewParameter(HydrationPreviewParameterProvider::class) state: HydrationUiState
) {
    MyApplicationTheme(themeMode = state.themeMode) {
        DashboardContent(
            state = state,
            isSupabaseConnected = true
        )
    }
}



@Composable
fun DynamicNotificationScheduleBanner(
    scheduleInfo: com.example.notification.DynamicScheduleInfo,
    onTestNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dynamic_notification_schedule_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (scheduleInfo.isGoalReached) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Dynamic Notification Schedule",
                        tint = if (scheduleInfo.isGoalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pengingat Adaptif: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = scheduleInfo.frequencyLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = scheduleInfo.statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onTestNotificationClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("trigger_test_notification_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Test Notification",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WaterHydrationCard(
    currentMl: Int,
    targetMl: Int,
    onQuickAdd250: () -> Unit,
    onQuickAdd500: () -> Unit,
    onCustomAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progres Air Minum",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Target: $targetMl ml",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Progress Component
            CircularWaterProgress(
                currentMl = currentMl,
                targetMl = targetMl
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Quick Add (Tambah Cepat)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Add Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickAdd250,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_250_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+250 ml", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onQuickAdd500,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_500_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+500 ml", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCustomAdd,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_custom_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Kustom...", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CoffeeTrackerHeaderCard(
    totalCaffeineMg: Int,
    coffeeCount: Int,
    onAddCoffeeClick: () -> Unit
) {
    val fdaLimitMg = 400
    val progress = (totalCaffeineMg.toFloat() / fdaLimitMg.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Konsumsi Kopi Hari Ini",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$coffeeCount cangkir tercatat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onAddCoffeeClick,
                    modifier = Modifier.testTag("add_coffee_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Kopi",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Caffeine Safety Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Kafein: $totalCaffeineMg mg",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Batas aman: $fdaLimitMg mg/hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (totalCaffeineMg > fdaLimitMg) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun WaterLogRowCard(
    waterLog: WaterLog,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("water_log_card_${waterLog.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "+${waterLog.amountMl} ml",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = waterLog.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (waterLog.isSynced) Icons.Default.CloudDone else Icons.Default.CloudSync,
                    contentDescription = if (waterLog.isSynced) "Synced" else "Offline",
                    tint = if (waterLog.isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onDelete(waterLog.id) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_water_${waterLog.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Log Air",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
