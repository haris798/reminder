package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE date_string = :date ORDER BY created_at DESC")
    fun getWaterLogsByDate(date: String): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE date_string >= :startDateIso ORDER BY created_at DESC")
    fun getWaterLogsFromDate(startDateIso: String): Flow<List<WaterLog>>

    @Query("SELECT SUM(amount_ml) FROM water_logs WHERE date_string = :date")
    fun getTotalWaterForDate(date: String): Flow<Int?>

    @Query("SELECT SUM(amount_ml) FROM water_logs WHERE date_string = :date")
    suspend fun getTotalWaterForDateDirect(date: String): Int?

    @Query("SELECT * FROM water_logs WHERE is_synced = 0")
    suspend fun getUnsyncedWaterLogs(): List<WaterLog>

    @Query("SELECT * FROM water_logs")
    suspend fun getAllWaterLogs(): List<WaterLog>

    @Query("SELECT * FROM water_logs ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestWaterLog(): WaterLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWaterLogsIgnore(waterLogs: List<WaterLog>)

    @Query("UPDATE water_logs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markWaterLogsAsSynced(ids: List<String>)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: String)
}
