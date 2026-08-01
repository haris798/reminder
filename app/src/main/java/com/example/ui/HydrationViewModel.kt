package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import com.example.data.repository.HydrationRepository
import com.example.notification.DynamicScheduleInfo
import com.example.notification.WaterNotificationManager
import com.example.ui.theme.ThemeMode
import com.example.worker.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyStat(
    val dateIso: String,
    val dayLabel: String,
    val waterMl: Int,
    val caffeineMg: Int,
    val isTargetReached: Boolean
)

data class WeeklySummary(
    val totalWaterMl: Int = 0,
    val avgWaterMlPerDay: Int = 0,
    val targetReachedDays: Int = 0,
    val consecutiveStreakDays: Int = 0,
    val totalCaffeineMg: Int = 0,
    val avgCaffeineMgPerDay: Int = 0,
    val totalCoffeeCups: Int = 0,
    val insightMessage: String = "",
    val dailyStats: List<DailyStat> = emptyList()
)

data class HydrationUiState(
    val dailyWaterGoalMl: Int = 2000,
    val currentWaterMl: Int = 0,
    val waterLogsToday: List<WaterLog> = emptyList(),
    val coffeeLogsToday: List<CoffeeLog> = emptyList(),
    val totalCaffeineMg: Int = 0,
    val unsyncedCount: Int = 0,
    val isSyncing: Boolean = false,
    val currentDateFormatted: String = "",
    val currentDateIso: String = "",
    val dynamicSchedule: DynamicScheduleInfo = DynamicScheduleInfo(45, 0, "Sangat Sering (tiap 45m)", "Mulai hari ini", false),
    val weeklySummary: WeeklySummary = WeeklySummary(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val userMessage: String? = null
)

class HydrationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
    private val repository: HydrationRepository
    private val notificationManager: WaterNotificationManager
    private val todayIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val sevenDaysAgoIsoDate: String

    private val _isSyncing = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )

    val uiState: StateFlow<HydrationUiState>

    init {
        val db = AppDatabase.getInstance(application)
        repository = HydrationRepository(db.waterLogDao(), db.coffeeLogDao())
        notificationManager = WaterNotificationManager(application)

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        sevenDaysAgoIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        // Schedule periodic background sync worker
        SyncWorker.schedulePeriodicSync(application)

        val formattedDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())

        val todayFlow = combine(
            repository.getWaterLogsForDate(todayIsoDate),
            repository.getCoffeeLogsForDate(todayIsoDate)
        ) { water, coffee -> Pair(water, coffee) }

        val weeklyFlow = combine(
            repository.getWaterLogsFromDate(sevenDaysAgoIsoDate),
            repository.getCoffeeLogsFromDate(sevenDaysAgoIsoDate)
        ) { water7, coffee7 -> Pair(water7, coffee7) }

        uiState = combine(
            todayFlow,
            weeklyFlow,
            _isSyncing,
            _userMessage,
            _themeMode
        ) { (waterLogs, coffeeLogs), (water7Days, coffee7Days), syncing, message, themeMode ->
            val totalWater = waterLogs.sumOf { it.amountMl }
            val totalCaffeine = coffeeLogs.sumOf { it.caffeineMg }
            val unsyncedWater = waterLogs.count { !it.isSynced }
            val unsyncedCoffee = coffeeLogs.count { !it.isSynced }

            // Dynamically calculate dynamic schedule interval based on percentage
            val scheduleInfo = notificationManager.updateDynamicReminderSchedule(
                currentMl = totalWater,
                targetMl = 2000
            )

            val weeklySummary = calculateWeeklySummary(water7Days, coffee7Days, 2000)

            HydrationUiState(
                dailyWaterGoalMl = 2000,
                currentWaterMl = totalWater,
                waterLogsToday = waterLogs,
                coffeeLogsToday = coffeeLogs,
                totalCaffeineMg = totalCaffeine,
                unsyncedCount = unsyncedWater + unsyncedCoffee,
                isSyncing = syncing,
                currentDateFormatted = formattedDate,
                currentDateIso = todayIsoDate,
                dynamicSchedule = scheduleInfo,
                weeklySummary = weeklySummary,
                themeMode = themeMode,
                userMessage = message
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationUiState(
                currentDateFormatted = formattedDate,
                currentDateIso = todayIsoDate,
                themeMode = _themeMode.value
            )
        )
    }

    private fun calculateWeeklySummary(
        waterLogs: List<WaterLog>,
        coffeeLogs: List<CoffeeLog>,
        dailyGoalMl: Int
    ): WeeklySummary {
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale.forLanguageTag("id-ID"))

        val dailyStatsList = mutableListOf<DailyStat>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateIso = sdfIso.format(cal.time)
            val dayLabel = sdfDay.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val dayWater = waterLogs.filter { it.dateString == dateIso }.sumOf { it.amountMl }
            val dayCaffeine = coffeeLogs.filter { it.dateString == dateIso }.sumOf { it.caffeineMg }

            dailyStatsList.add(
                DailyStat(
                    dateIso = dateIso,
                    dayLabel = dayLabel,
                    waterMl = dayWater,
                    caffeineMg = dayCaffeine,
                    isTargetReached = dayWater >= dailyGoalMl
                )
            )
        }

        val totalWaterMl = waterLogs.sumOf { it.amountMl }
        val totalCaffeineMg = coffeeLogs.sumOf { it.caffeineMg }
        val totalCoffeeCups = coffeeLogs.size
        val avgWaterMl = totalWaterMl / 7
        val avgCaffeineMg = totalCaffeineMg / 7
        val targetReachedDays = dailyStatsList.count { it.isTargetReached }

        // Calculate consecutive streak (from today going backwards)
        var consecutiveStreak = 0
        for (stat in dailyStatsList) { // dailyStatsList is ordered 6 days ago -> today
            if (stat.isTargetReached) {
                consecutiveStreak++
            } else {
                consecutiveStreak = 0
            }
        }

        val insightText = when {
            targetReachedDays >= 5 -> "🔥 Luar biasa! Target hidrasi 2.000 ml tercapai dalam $targetReachedDays dari 7 hari terakhir."
            targetReachedDays >= 3 -> "👍 Bagus! Anda memenuhi target hidrasi $targetReachedDays hari dalam seminggu. Pertahankan!"
            avgWaterMl >= dailyGoalMl -> "💧 Mantap! Rata-rata asupan air harian ($avgWaterMl ml) memenuhi target."
            totalWaterMl > 0 -> "💡 Rata-rata asupan air 7 hari terakhir ($avgWaterMl ml/hari) belum mencapai target 2.000 ml."
            else -> "📝 Belum ada catatan asupan air dalam 7 hari terakhir. Mulai minum & catat hari ini!"
        }

        val caffeineNote = if (avgCaffeineMg > 300) " ☕ Perhatian: Rata-rata kafein ($avgCaffeineMg mg/hari) cukup tinggi." else ""

        return WeeklySummary(
            totalWaterMl = totalWaterMl,
            avgWaterMlPerDay = avgWaterMl,
            targetReachedDays = targetReachedDays,
            consecutiveStreakDays = consecutiveStreak,
            totalCaffeineMg = totalCaffeineMg,
            avgCaffeineMgPerDay = avgCaffeineMg,
            totalCoffeeCups = totalCoffeeCups,
            insightMessage = insightText + caffeineNote,
            dailyStats = dailyStatsList
        )
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWaterLog(amountMl, todayIsoDate)
            _userMessage.value = "+$amountMl ml air berhasil dicatat"
            com.example.widget.HydrationWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun addCoffee(coffeeType: String, caffeineMg: Int) {
        viewModelScope.launch {
            repository.addCoffeeLog(coffeeType, caffeineMg, todayIsoDate)
            _userMessage.value = "Kopi $coffeeType ($caffeineMg mg kafein) berhasil dicatat"
            com.example.widget.HydrationWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteWaterLog(id: String) {
        viewModelScope.launch {
            repository.deleteWaterLog(id)
            _userMessage.value = "Catatan air dihapus"
            com.example.widget.HydrationWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteCoffeeLog(id: String) {
        viewModelScope.launch {
            repository.deleteCoffeeLog(id)
            _userMessage.value = "Catatan kopi dihapus"
            com.example.widget.HydrationWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _userMessage.value = "Memulai sinkronisasi data..."
            try {
                val unsyncedWater = repository.getUnsyncedWaterLogs()
                val unsyncedCoffee = repository.getUnsyncedCoffeeLogs()

                if (unsyncedWater.isEmpty() && unsyncedCoffee.isEmpty()) {
                    _userMessage.value = "Semua data sudah tersinkronisasi"
                } else {
                    // Simulation of Supabase upsert batch sync
                    val waterIds = unsyncedWater.map { it.id }
                    val coffeeIds = unsyncedCoffee.map { it.id }

                    repository.markWaterLogsSynced(waterIds)
                    repository.markCoffeeLogsSynced(coffeeIds)

                    _userMessage.value = "Berhasil sinkronisasi ${waterIds.size + coffeeIds.size} data ke cloud"
                }
            } catch (e: Exception) {
                _userMessage.value = "Gagal sinkronisasi: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun sendTestNotification() {
        val state = uiState.value
        notificationManager.showHydrationReminderNotification(
            currentMl = state.currentWaterMl,
            targetMl = state.dailyWaterGoalMl
        )
        _userMessage.value = "Notifikasi pengingat dikirim (${state.dynamicSchedule.progressPercent}%)"
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
        _userMessage.value = when (mode) {
            ThemeMode.SYSTEM -> "Tema disesuaikan dengan sistem"
            ThemeMode.LIGHT -> "Mode Terang diaktifkan"
            ThemeMode.DARK -> "Mode Gelap diaktifkan"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

