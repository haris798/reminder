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

        // Otomatis download data Supabase ke lokal saat pertama kali terkoneksi
        checkAndDownloadInitialSupabaseData()
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

    fun checkAndDownloadInitialSupabaseData(force: Boolean = false) {
        viewModelScope.launch {
            val settingsManager = com.example.data.SupabaseSettingsManager(getApplication())
            val config = settingsManager.loadConfig()
            if (!config.isConnected) return@launch

            if (force || !settingsManager.isInitialDownloadComplete()) {
                _isSyncing.value = true
                _userMessage.value = "Mengunduh data Supabase ke lokal..."
                com.example.util.AppLogger.i("HydrationViewModel", "Koneksi Supabase terdeteksi - Mengunduh data cloud ke lokal (force=$force)...")

                try {
                    val syncService = com.example.data.SupabaseSyncService()
                    val waterRes = syncService.downloadWaterLogs(config)
                    val coffeeRes = syncService.downloadCoffeeLogs(config)

                    var waterCount = 0
                    var coffeeCount = 0

                    if (waterRes.isSuccess) {
                        val waterLogs = waterRes.getOrThrow()
                        waterCount = waterLogs.size
                        if (waterLogs.isNotEmpty()) {
                            repository.insertDownloadedWaterLogs(waterLogs)
                        }
                    }
                    if (coffeeRes.isSuccess) {
                        val coffeeLogs = coffeeRes.getOrThrow()
                        coffeeCount = coffeeLogs.size
                        if (coffeeLogs.isNotEmpty()) {
                            repository.insertDownloadedCoffeeLogs(coffeeLogs)
                        }
                    }

                    if (waterRes.isSuccess && coffeeRes.isSuccess) {
                        settingsManager.setInitialDownloadComplete(true)
                        val msg = "Berhasil mengunduh $waterCount log air & $coffeeCount log kopi dari Supabase!"
                        _userMessage.value = msg
                        com.example.util.AppLogger.s("HydrationViewModel", msg)
                    } else {
                        val errMsg = "Gagal mengunduh beberapa data dari Supabase."
                        _userMessage.value = errMsg
                        com.example.util.AppLogger.e("HydrationViewModel", errMsg)
                    }
                } catch (e: Exception) {
                    val errMsg = "Gagal download awal Supabase: ${e.message}"
                    _userMessage.value = errMsg
                    com.example.util.AppLogger.e("HydrationViewModel", errMsg, e.stackTraceToString(), e)
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _userMessage.value = "Memulai sinkronisasi data ke Supabase..."
            com.example.util.AppLogger.i("HydrationViewModel", "Manual sync dipicu dari UI...")
            try {
                val settingsManager = com.example.data.SupabaseSettingsManager(getApplication())
                val config = settingsManager.loadConfig()

                if (config.supabaseUrl.isBlank()) {
                    val msg = "URL Supabase belum diisi di Pengaturan!"
                    _userMessage.value = msg
                    com.example.util.AppLogger.w("HydrationViewModel", msg)
                    return@launch
                }
                if (config.apiKey.isBlank() || config.apiKey == "YOUR_SUPABASE_API_KEY_HERE") {
                    val msg = "API Key Supabase belum diisi di Pengaturan!"
                    _userMessage.value = msg
                    com.example.util.AppLogger.w("HydrationViewModel", msg)
                    return@launch
                }

                var waterToSync = repository.getUnsyncedWaterLogs()
                var coffeeToSync = repository.getUnsyncedCoffeeLogs()

                // If no unsynced records found, fallback to sync all logs to populate Supabase table if empty
                if (waterToSync.isEmpty() && coffeeToSync.isEmpty()) {
                    com.example.util.AppLogger.i("HydrationViewModel", "Tidak ada log dengan is_synced=0, mengambil seluruh log SQLite...")
                    waterToSync = repository.getAllWaterLogs()
                    coffeeToSync = repository.getAllCoffeeLogs()
                }

                if (waterToSync.isEmpty() && coffeeToSync.isEmpty()) {
                    val msg = "Belum ada catatan air atau kopi untuk disinkronkan"
                    _userMessage.value = msg
                    com.example.util.AppLogger.i("HydrationViewModel", msg)
                    return@launch
                }

                val syncService = com.example.data.SupabaseSyncService()
                var totalSynced = 0
                val errors = mutableListOf<String>()

                // Unduh data dari Supabase ke lokal saat pertama kali terhubung
                if (!settingsManager.isInitialDownloadComplete()) {
                    com.example.util.AppLogger.i("HydrationViewModel", "Koneksi pertama terdeteksi - mengunduh data Supabase ke lokal")

                    val waterDownload = syncService.downloadWaterLogs(config)
                    if (waterDownload.isSuccess) {
                        val downloadedWater = waterDownload.getOrThrow()
                        if (downloadedWater.isNotEmpty()) {
                            repository.insertDownloadedWaterLogs(downloadedWater)
                            com.example.util.AppLogger.s("HydrationViewModel", "Download ${downloadedWater.size} water_logs ke lokal")
                        }
                    } else {
                        errors.add(waterDownload.exceptionOrNull()?.message ?: "Gagal download water_logs")
                    }

                    val coffeeDownload = syncService.downloadCoffeeLogs(config)
                    if (coffeeDownload.isSuccess) {
                        val downloadedCoffee = coffeeDownload.getOrThrow()
                        if (downloadedCoffee.isNotEmpty()) {
                            repository.insertDownloadedCoffeeLogs(downloadedCoffee)
                            com.example.util.AppLogger.s("HydrationViewModel", "Download ${downloadedCoffee.size} coffee_logs ke lokal")
                        }
                    } else {
                        errors.add(coffeeDownload.exceptionOrNull()?.message ?: "Gagal download coffee_logs")
                    }

                    if (waterDownload.isSuccess && coffeeDownload.isSuccess) {
                        settingsManager.setInitialDownloadComplete(true)
                        com.example.util.AppLogger.s("HydrationViewModel", "Download awal Supabase selesai - data kini tersedia offline")
                    }
                }

                if (waterToSync.isNotEmpty()) {
                    val waterResult = syncService.syncWaterLogs(config, waterToSync)
                    waterResult.fold(
                        onSuccess = { count ->
                            totalSynced += count
                            repository.markWaterLogsSynced(waterToSync.map { it.id })
                        },
                        onFailure = { error ->
                            errors.add(error.message ?: "Gagal sync water_logs")
                        }
                    )
                }

                if (coffeeToSync.isNotEmpty()) {
                    val coffeeResult = syncService.syncCoffeeLogs(config, coffeeToSync)
                    coffeeResult.fold(
                        onSuccess = { count ->
                            totalSynced += count
                            repository.markCoffeeLogsSynced(coffeeToSync.map { it.id })
                        },
                        onFailure = { error ->
                            errors.add(error.message ?: "Gagal sync coffee_logs")
                        }
                    )
                }

                if (errors.isNotEmpty()) {
                    val errMsg = "Gagal: ${errors.joinToString("; ")}"
                    _userMessage.value = errMsg
                    com.example.util.AppLogger.e("HydrationViewModel", "Manual sync selesai dengan error: $errMsg")
                } else {
                    val succMsg = "Berhasil sinkronisasi $totalSynced data ke Supabase!"
                    _userMessage.value = succMsg
                    com.example.util.AppLogger.s("HydrationViewModel", succMsg)
                }
            } catch (e: Exception) {
                val errMsg = "Gagal sinkronisasi: ${e.message}"
                _userMessage.value = errMsg
                com.example.util.AppLogger.e("HydrationViewModel", errMsg, e.stackTraceToString(), e)
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
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

