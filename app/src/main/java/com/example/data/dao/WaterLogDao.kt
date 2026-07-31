package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE dateString = :date ORDER BY createdAt DESC")
    fun getWaterLogsByDate(date: String): Flow<List<WaterLog>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateString = :date")
    fun getTotalWaterForDate(date: String): Flow<Int?>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateString = :date")
    suspend fun getTotalWaterForDateDirect(date: String): Int?

    @Query("SELECT * FROM water_logs WHERE isSynced = 0")
    suspend fun getUnsyncedWaterLogs(): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog)

    @Query("UPDATE water_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markWaterLogsAsSynced(ids: List<String>)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: String)
}
