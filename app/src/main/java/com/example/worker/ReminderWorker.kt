package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.model.WaterLog
import com.example.notification.WaterNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Background worker that executes periodic hydration reminders dynamically.
 * Checks total water for current date & elapsed time since last water log,
 * firing notification if target not reached or user hasn't logged recently.
 */
class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReminderWorker"
        private const val DEFAULT_TARGET_ML = 2000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val todayDate = WaterLog.getCurrentDateString()
            val totalWater = db.waterLogDao().getTotalWaterForDateDirect(todayDate) ?: 0

            Log.d(TAG, "ReminderWorker executing: Today's water = $totalWater ml / $DEFAULT_TARGET_ML ml")

            if (totalWater < DEFAULT_TARGET_ML) {
                val latestLog = db.waterLogDao().getLatestWaterLog()
                var hoursSinceLastLog: Double? = null

                if (latestLog != null) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val logDate = sdf.parse(latestLog.createdAt)
                        if (logDate != null) {
                            val diffMs = Date().time - logDate.time
                            hoursSinceLastLog = diffMs.toDouble() / (1000 * 60 * 60)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing log timestamp", e)
                    }
                }

                val notificationManager = WaterNotificationManager(applicationContext)
                notificationManager.showHydrationReminderNotification(
                    currentMl = totalWater,
                    targetMl = DEFAULT_TARGET_ML,
                    hoursSinceLastLog = hoursSinceLastLog
                )
            } else {
                Log.d(TAG, "Target reached ($totalWater ml). Skipping notification.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ReminderWorker error: ${e.message}", e)
            Result.failure()
        }
    }
}

