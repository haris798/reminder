package com.example.data.repository

import com.example.data.dao.CoffeeLogDao
import com.example.data.dao.WaterLogDao
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import com.example.util.AppLogger
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HydrationRepository(
    private val waterLogDao: WaterLogDao,
    private val coffeeLogDao: CoffeeLogDao
) {
    companion object {
        private const val TAG = "SQLiteDB"
    }

    fun getWaterLogsForDate(date: String): Flow<List<WaterLog>> {
        return waterLogDao.getWaterLogsByDate(date)
    }

    fun getWaterLogsFromDate(startDateIso: String): Flow<List<WaterLog>> {
        return waterLogDao.getWaterLogsFromDate(startDateIso)
    }

    fun getTotalWaterForDate(date: String): Flow<Int?> {
        return waterLogDao.getTotalWaterForDate(date)
    }

    suspend fun getTotalWaterForDateDirect(date: String): Int {
        return waterLogDao.getTotalWaterForDateDirect(date) ?: 0
    }

    suspend fun getTotalCaffeineForDateDirect(date: String): Int {
        return coffeeLogDao.getTotalCaffeineForDateDirect(date) ?: 0
    }

    suspend fun addWaterLog(amountMl: Int, dateString: String): WaterLog {
        val waterLog = WaterLog(
            id = UUID.randomUUID().toString(),
            amountMl = amountMl,
            dateString = dateString,
            isSynced = false
        )
        waterLogDao.insertWaterLog(waterLog)
        AppLogger.i(TAG, "INSERT water_logs - +$amountMl ml", "ID=${waterLog.id}, Date=$dateString, isSynced=false")
        return waterLog
    }

    suspend fun deleteWaterLog(id: String) {
        waterLogDao.deleteWaterLogById(id)
        AppLogger.i(TAG, "DELETE water_logs - ID=$id")
    }

    fun getCoffeeLogsForDate(date: String): Flow<List<CoffeeLog>> {
        return coffeeLogDao.getCoffeeLogsByDate(date)
    }

    fun getCoffeeLogsFromDate(startDateIso: String): Flow<List<CoffeeLog>> {
        return coffeeLogDao.getCoffeeLogsFromDate(startDateIso)
    }

    fun getTotalCaffeineForDate(date: String): Flow<Int?> {
        return coffeeLogDao.getTotalCaffeineForDate(date)
    }

    suspend fun addCoffeeLog(coffeeType: String, caffeineMg: Int, dateString: String): CoffeeLog {
        val coffeeLog = CoffeeLog(
            id = UUID.randomUUID().toString(),
            coffeeType = coffeeType,
            caffeineMg = caffeineMg,
            dateString = dateString,
            isSynced = false
        )
        coffeeLogDao.insertCoffeeLog(coffeeLog)
        AppLogger.i(TAG, "INSERT coffee_logs - $coffeeType ($caffeineMg mg)", "ID=${coffeeLog.id}, Date=$dateString, isSynced=false")
        return coffeeLog
    }

    suspend fun deleteCoffeeLog(id: String) {
        coffeeLogDao.deleteCoffeeLogById(id)
        AppLogger.i(TAG, "DELETE coffee_logs - ID=$id")
    }

    suspend fun getUnsyncedWaterLogs(): List<WaterLog> {
        val logs = waterLogDao.getUnsyncedWaterLogs()
        AppLogger.i(TAG, "QUERY unsynced water_logs - Total: ${logs.size}")
        return logs
    }

    suspend fun getUnsyncedCoffeeLogs(): List<CoffeeLog> {
        val logs = coffeeLogDao.getUnsyncedCoffeeLogs()
        AppLogger.i(TAG, "QUERY unsynced coffee_logs - Total: ${logs.size}")
        return logs
    }

    suspend fun getAllWaterLogs(): List<WaterLog> {
        val logs = waterLogDao.getAllWaterLogs()
        AppLogger.i(TAG, "QUERY all water_logs - Total: ${logs.size}")
        return logs
    }

    suspend fun getAllCoffeeLogs(): List<CoffeeLog> {
        val logs = coffeeLogDao.getAllCoffeeLogs()
        AppLogger.i(TAG, "QUERY all coffee_logs - Total: ${logs.size}")
        return logs
    }

    suspend fun markWaterLogsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            waterLogDao.markWaterLogsAsSynced(ids)
            AppLogger.s(TAG, "UPDATE water_logs - Set is_synced=1 untuk ${ids.size} baris", "IDs: ${ids.joinToString()}")
        }
    }

    suspend fun markCoffeeLogsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            coffeeLogDao.markCoffeeLogsAsSynced(ids)
            AppLogger.s(TAG, "UPDATE coffee_logs - Set is_synced=1 untuk ${ids.size} baris", "IDs: ${ids.joinToString()}")
        }
    }

    /**
     * Menyimpan data water_logs hasil download dari Supabase ke lokal.
     * Baris dengan ID yang sudah ada tidak akan ditimpa (IGNORE).
     */
    suspend fun insertDownloadedWaterLogs(logs: List<WaterLog>) {
        if (logs.isNotEmpty()) {
            waterLogDao.insertWaterLogsIgnore(logs)
            AppLogger.i(TAG, "INSERT downloaded water_logs - ${logs.size} baris (is_synced=1)")
        }
    }

    /**
     * Menyimpan data coffee_logs hasil download dari Supabase ke lokal.
     * Baris dengan ID yang sudah ada tidak akan ditimpa (IGNORE).
     */
    suspend fun insertDownloadedCoffeeLogs(logs: List<CoffeeLog>) {
        if (logs.isNotEmpty()) {
            coffeeLogDao.insertCoffeeLogsIgnore(logs)
            AppLogger.i(TAG, "INSERT downloaded coffee_logs - ${logs.size} baris (is_synced=1)")
        }
    }
}
