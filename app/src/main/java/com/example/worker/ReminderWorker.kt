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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Worker background WorkManager untuk menjalankan pengingat minum air periodik.
 * Mengecek jam aktif pengguna (08:00 - 22:00) dan mengirim notifikasi setiap 2 jam
 * apabila target asupan harian belum tercapai.
 */
class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReminderWorker"
        private const val DEFAULT_TARGET_ML = 2000
        private const val ACTIVE_HOUR_START = 8  // 08:00 AM
        private const val ACTIVE_HOUR_END = 22  // 10:00 PM
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // 1. Cek apakah waktu saat ini berada dalam jam aktif pengguna (08:00 - 22:00)
            if (currentHour < ACTIVE_HOUR_START || currentHour >= ACTIVE_HOUR_END) {
                Log.d(TAG, "Di luar jam aktif pengguna ($currentHour:00). Notifikasi pengingat dilewati.")
                return@withContext Result.success()
            }

            // 2. Ambil total konsumsi air harian dari Room Database
            val db = AppDatabase.getInstance(applicationContext)
            val todayDate = WaterLog.getCurrentDateString()
            val totalWater = db.waterLogDao().getTotalWaterForDateDirect(todayDate) ?: 0

            Log.d(TAG, "WorkManager Reminder running: Air hari ini = $totalWater ml / $DEFAULT_TARGET_ML ml")

            // 3. Jika belum mencapai target harian, hitung selisih waktu & kirim notifikasi
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
                        Log.e(TAG, "Gagal mengolah timestamp log terakhir", e)
                    }
                }

                // Kirim notifikasi pengingat minum
                val notificationManager = WaterNotificationManager(applicationContext)
                notificationManager.showHydrationReminderNotification(
                    currentMl = totalWater,
                    targetMl = DEFAULT_TARGET_ML,
                    hoursSinceLastLog = hoursSinceLastLog
                )
            } else {
                Log.d(TAG, "Target hidrasi hari ini sudah tercapai ($totalWater ml). Notifikasi tidak dikirim.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Terjadi kesalahan pada ReminderWorker: ${e.message}", e)
            Result.failure()
        }
    }
}


