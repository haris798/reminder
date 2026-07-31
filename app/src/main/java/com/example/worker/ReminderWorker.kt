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

/**
 * Background worker that executes periodic hydration reminders dynamically.
 * Checks total water for current date and fires notification if target not reached.
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
                val notificationManager = WaterNotificationManager(applicationContext)
                notificationManager.showHydrationReminderNotification(
                    currentMl = totalWater,
                    targetMl = DEFAULT_TARGET_ML
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
