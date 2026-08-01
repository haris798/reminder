package com.example.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.data.SupabaseConfig
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import com.example.notification.DynamicScheduleInfo
import com.example.ui.DailyStat
import com.example.ui.HydrationUiState
import com.example.ui.WeeklySummary
import com.example.ui.theme.ThemeMode

class HydrationPreviewParameterProvider : PreviewParameterProvider<HydrationUiState> {
    override val values: Sequence<HydrationUiState> = sequenceOf(
        // 1. Initial / Normal State
        HydrationUiState(
            dailyWaterGoalMl = 2000,
            currentWaterMl = 1250,
            currentDateFormatted = "Sabtu, 1 Agustus 2026",
            currentDateIso = "2026-08-01",
            totalCaffeineMg = 95,
            waterLogsToday = listOf(
                WaterLog(id = "w1", amountMl = 250, dateString = "2026-08-01", createdAt = "2026-08-01 08:00:00", isSynced = true),
                WaterLog(id = "w2", amountMl = 500, dateString = "2026-08-01", createdAt = "2026-08-01 10:30:00", isSynced = true),
                WaterLog(id = "w3", amountMl = 500, dateString = "2026-08-01", createdAt = "2026-08-01 14:15:00", isSynced = false)
            ),
            coffeeLogsToday = listOf(
                CoffeeLog(id = "c1", coffeeType = "Americano", caffeineMg = 95, dateString = "2026-08-01", createdAt = "2026-08-01 09:00:00", isSynced = true)
            ),

            unsyncedCount = 1,
            isSyncing = false,
            dynamicSchedule = DynamicScheduleInfo(
                intervalMinutes = 60,
                progressPercent = 62,
                frequencyLabel = "Sedang (tiap 60m)",
                statusDescription = "Progres hidrasi berjalan lancar",
                isGoalReached = false
            ),
            weeklySummary = WeeklySummary(
                totalWaterMl = 13500,
                avgWaterMlPerDay = 1928,
                targetReachedDays = 5,
                consecutiveStreakDays = 4,
                totalCaffeineMg = 450,
                avgCaffeineMgPerDay = 64,
                totalCoffeeCups = 5,
                insightMessage = "Performa konsistensi hidrasi kamu minggu ini sangat baik! Pertahankan pola minum teratur.",
                dailyStats = listOf(
                    DailyStat("2026-07-26", "Min", 2000, 95, true),
                    DailyStat("2026-07-27", "Sen", 2200, 0, true),
                    DailyStat("2026-07-28", "Sel", 1800, 120, false),
                    DailyStat("2026-07-29", "Rab", 2100, 95, true),
                    DailyStat("2026-07-30", "Kam", 2000, 60, true),
                    DailyStat("2026-07-31", "Jum", 2150, 80, true),
                    DailyStat("2026-08-01", "Sab", 1250, 95, false)
                )
            ),
            themeMode = ThemeMode.SYSTEM
        ),
        // 2. Goal Achieved State
        HydrationUiState(
            dailyWaterGoalMl = 2000,
            currentWaterMl = 2250,
            currentDateFormatted = "Sabtu, 1 Agustus 2026",
            currentDateIso = "2026-08-01",
            totalCaffeineMg = 150,
            waterLogsToday = listOf(
                WaterLog(id = "w1", amountMl = 750, dateString = "2026-08-01", createdAt = "2026-08-01 07:30:00", isSynced = true),
                WaterLog(id = "w2", amountMl = 750, dateString = "2026-08-01", createdAt = "2026-08-01 12:00:00", isSynced = true),
                WaterLog(id = "w3", amountMl = 750, dateString = "2026-08-01", createdAt = "2026-08-01 17:00:00", isSynced = true)
            ),
            coffeeLogsToday = listOf(
                CoffeeLog(id = "c1", coffeeType = "Espresso Double", caffeineMg = 150, dateString = "2026-08-01", createdAt = "2026-08-01 08:15:00", isSynced = true)
            ),

            unsyncedCount = 0,
            isSyncing = false,
            dynamicSchedule = DynamicScheduleInfo(
                intervalMinutes = 180,
                progressPercent = 100,
                frequencyLabel = "Jarang (tiap 180m)",
                statusDescription = "Selamat! Target hidrasi harian telah tercapai.",
                isGoalReached = true
            ),
            weeklySummary = WeeklySummary(
                totalWaterMl = 15000,
                avgWaterMlPerDay = 2142,
                targetReachedDays = 7,
                consecutiveStreakDays = 7,
                totalCaffeineMg = 600,
                avgCaffeineMgPerDay = 85,
                totalCoffeeCups = 6,
                insightMessage = "Luar biasa! Target hidrasi 7 hari berturut-turut tercapai sempurna.",
                dailyStats = listOf(
                    DailyStat("2026-07-26", "Min", 2000, 95, true),
                    DailyStat("2026-07-27", "Sen", 2100, 95, true),
                    DailyStat("2026-07-28", "Sel", 2050, 95, true),
                    DailyStat("2026-07-29", "Rab", 2200, 95, true),
                    DailyStat("2026-07-30", "Kam", 2000, 60, true),
                    DailyStat("2026-07-31", "Jum", 2400, 80, true),
                    DailyStat("2026-08-01", "Sab", 2250, 150, true)
                )
            ),
            themeMode = ThemeMode.DARK
        )
    )
}

class SupabaseConfigPreviewParameterProvider : PreviewParameterProvider<SupabaseConfig> {
    override val values: Sequence<SupabaseConfig> = sequenceOf(
        SupabaseConfig(
            supabaseUrl = "https://pcoyvfhcniscynjkndlw.supabase.co",
            userEmail = "haris443@gmail.com",
            userPassword = "••••••••••••",
            apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            autoUpload = true,
            uploadIntervalMinutes = 15
        ),
        SupabaseConfig(
            supabaseUrl = "",
            userEmail = "",
            userPassword = "",
            apiKey = "",
            autoUpload = false,
            uploadIntervalMinutes = 30
        )
    )
}
