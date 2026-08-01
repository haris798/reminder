package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CoffeeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeLogDao {
    @Query("SELECT * FROM coffee_logs WHERE date_string = :date ORDER BY created_at DESC")
    fun getCoffeeLogsByDate(date: String): Flow<List<CoffeeLog>>

    @Query("SELECT * FROM coffee_logs WHERE date_string >= :startDateIso ORDER BY created_at DESC")
    fun getCoffeeLogsFromDate(startDateIso: String): Flow<List<CoffeeLog>>

    @Query("SELECT SUM(caffeine_mg) FROM coffee_logs WHERE date_string = :date")
    fun getTotalCaffeineForDate(date: String): Flow<Int?>

    @Query("SELECT SUM(caffeine_mg) FROM coffee_logs WHERE date_string = :date")
    suspend fun getTotalCaffeineForDateDirect(date: String): Int?

    @Query("SELECT * FROM coffee_logs WHERE is_synced = 0")
    suspend fun getUnsyncedCoffeeLogs(): List<CoffeeLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffeeLog(coffeeLog: CoffeeLog)

    @Query("UPDATE coffee_logs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markCoffeeLogsAsSynced(ids: List<String>)

    @Query("DELETE FROM coffee_logs WHERE id = :id")
    suspend fun deleteCoffeeLogById(id: String)
}
