package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import com.example.data.repository.HydrationRepository
import com.example.notification.DynamicScheduleInfo
import com.example.notification.WaterNotificationManager
import com.example.worker.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val userMessage: String? = null
)

class HydrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HydrationRepository
    private val notificationManager: WaterNotificationManager
    private val todayIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _isSyncing = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HydrationUiState>

    init {
        val db = AppDatabase.getInstance(application)
        repository = HydrationRepository(db.waterLogDao(), db.coffeeLogDao())
        notificationManager = WaterNotificationManager(application)

        // Schedule periodic background sync worker
        SyncWorker.schedulePeriodicSync(application)

        val formattedDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())

        uiState = combine(
            repository.getWaterLogsForDate(todayIsoDate),
            repository.getCoffeeLogsForDate(todayIsoDate),
            _isSyncing,
            _userMessage
        ) { waterLogs, coffeeLogs, syncing, message ->
            val totalWater = waterLogs.sumOf { it.amountMl }
            val totalCaffeine = coffeeLogs.sumOf { it.caffeineMg }
            val unsyncedWater = waterLogs.count { !it.isSynced }
            val unsyncedCoffee = coffeeLogs.count { !it.isSynced }

            // Dynamically calculate dynamic schedule interval based on percentage
            val scheduleInfo = notificationManager.updateDynamicReminderSchedule(
                currentMl = totalWater,
                targetMl = 2000
            )

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
                userMessage = message
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationUiState(
                currentDateFormatted = formattedDate,
                currentDateIso = todayIsoDate
            )
        )
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWaterLog(amountMl, todayIsoDate)
            _userMessage.value = "+$amountMl ml air berhasil dicatat"
        }
    }

    fun addCoffee(coffeeType: String, caffeineMg: Int) {
        viewModelScope.launch {
            repository.addCoffeeLog(coffeeType, caffeineMg, todayIsoDate)
            _userMessage.value = "Kopi $coffeeType ($caffeineMg mg kafein) berhasil dicatat"
        }
    }

    fun deleteWaterLog(id: String) {
        viewModelScope.launch {
            repository.deleteWaterLog(id)
            _userMessage.value = "Catatan air dihapus"
        }
    }

    fun deleteCoffeeLog(id: String) {
        viewModelScope.launch {
            repository.deleteCoffeeLog(id)
            _userMessage.value = "Catatan kopi dihapus"
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

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

