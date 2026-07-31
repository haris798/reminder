package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CoffeeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeLogDao {
    @Query("SELECT * FROM coffee_logs WHERE dateString = :date ORDER BY createdAt DESC")
    fun getCoffeeLogsByDate(date: String): Flow<List<CoffeeLog>>

    @Query("SELECT SUM(caffeineMg) FROM coffee_logs WHERE dateString = :date")
    fun getTotalCaffeineForDate(date: String): Flow<Int?>

    @Query("SELECT * FROM coffee_logs WHERE isSynced = 0")
    suspend fun getUnsyncedCoffeeLogs(): List<CoffeeLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffeeLog(coffeeLog: CoffeeLog)

    @Query("UPDATE coffee_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCoffeeLogsAsSynced(ids: List<String>)

    @Query("DELETE FROM coffee_logs WHERE id = :id")
    suspend fun deleteCoffeeLogById(id: String)
}
