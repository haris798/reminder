package com.example.data.repository

import com.example.data.dao.CoffeeLogDao
import com.example.data.dao.WaterLogDao
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HydrationRepository(
    private val waterLogDao: WaterLogDao,
    private val coffeeLogDao: CoffeeLogDao
) {
    fun getWaterLogsForDate(date: String): Flow<List<WaterLog>> {
        return waterLogDao.getWaterLogsByDate(date)
    }

    fun getTotalWaterForDate(date: String): Flow<Int?> {
        return waterLogDao.getTotalWaterForDate(date)
    }

    suspend fun getTotalWaterForDateDirect(date: String): Int {
        return waterLogDao.getTotalWaterForDateDirect(date) ?: 0
    }

    suspend fun addWaterLog(amountMl: Int, dateString: String): WaterLog {
        val waterLog = WaterLog(
            id = UUID.randomUUID().toString(),
            amountMl = amountMl,
            dateString = dateString,
            isSynced = false
        )
        waterLogDao.insertWaterLog(waterLog)
        return waterLog
    }

    suspend fun deleteWaterLog(id: String) {
        waterLogDao.deleteWaterLogById(id)
    }

    fun getCoffeeLogsForDate(date: String): Flow<List<CoffeeLog>> {
        return coffeeLogDao.getCoffeeLogsByDate(date)
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
        return coffeeLog
    }

    suspend fun deleteCoffeeLog(id: String) {
        coffeeLogDao.deleteCoffeeLogById(id)
    }

    suspend fun getUnsyncedWaterLogs(): List<WaterLog> = waterLogDao.getUnsyncedWaterLogs()

    suspend fun getUnsyncedCoffeeLogs(): List<CoffeeLog> = coffeeLogDao.getUnsyncedCoffeeLogs()

    suspend fun markWaterLogsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            waterLogDao.markWaterLogsAsSynced(ids)
        }
    }

    suspend fun markCoffeeLogsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            coffeeLogDao.markCoffeeLogsAsSynced(ids)
        }
    }
}
