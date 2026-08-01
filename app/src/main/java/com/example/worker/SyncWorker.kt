package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * SyncWorker runs as an isolated background task.
 * Re-initializes SQLite database connection, queries unsynced records (is_synced = 0),
 * executes upsert logic to Supabase / Remote DB, and updates SQLite batch is_synced = 1.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "hydration_coffee_sync_worker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            AppLogger.i(TAG, "WorkManager background periodic sync dijadwalkan (tiap 15m)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = com.example.data.SupabaseSettingsManager(applicationContext).loadConfig()
            if (!settings.autoUpload) {
                AppLogger.i(TAG, "Auto upload mati di pengaturan. Melewati background sync.")
                return@withContext Result.success()
            }

            AppLogger.i(TAG, "SyncWorker dimulai untuk Supabase URL: ${settings.supabaseUrl}")

            // Re-initialize Database in background isolate context
            val db = AppDatabase.getInstance(applicationContext)
            val waterDao = db.waterLogDao()
            val coffeeDao = db.coffeeLogDao()

            // 1. Ambil data water_logs dengan is_synced = 0
            val unsyncedWaterLogs = waterDao.getUnsyncedWaterLogs()

            // 2. Ambil data coffee_logs dengan is_synced = 0
            val unsyncedCoffeeLogs = coffeeDao.getUnsyncedCoffeeLogs()

            AppLogger.i(TAG, "Ditemukan ${unsyncedWaterLogs.size} log air & ${unsyncedCoffeeLogs.size} log kopi yang belum ter-sync")

            if (unsyncedWaterLogs.isEmpty() && unsyncedCoffeeLogs.isEmpty()) {
                AppLogger.i(TAG, "Tidak ada data baru untuk di-sync. Selesai.")
                return@withContext Result.success()
            }

            val syncService = com.example.data.SupabaseSyncService()

            if (unsyncedWaterLogs.isNotEmpty()) {
                val waterResult = syncService.syncWaterLogs(settings, unsyncedWaterLogs)
                if (waterResult.isSuccess) {
                    val syncedWaterIds = unsyncedWaterLogs.map { it.id }
                    waterDao.markWaterLogsAsSynced(syncedWaterIds)
                    AppLogger.s(TAG, "Batch update ${syncedWaterIds.size} water logs -> is_synced = 1")
                } else {
                    AppLogger.e(TAG, "Gagal sync background water logs", waterResult.exceptionOrNull()?.message)
                }
            }

            if (unsyncedCoffeeLogs.isNotEmpty()) {
                val coffeeResult = syncService.syncCoffeeLogs(settings, unsyncedCoffeeLogs)
                if (coffeeResult.isSuccess) {
                    val syncedCoffeeIds = unsyncedCoffeeLogs.map { it.id }
                    coffeeDao.markCoffeeLogsAsSynced(syncedCoffeeIds)
                    AppLogger.s(TAG, "Batch update ${syncedCoffeeIds.size} coffee logs -> is_synced = 1")
                } else {
                    AppLogger.e(TAG, "Gagal sync background coffee logs", coffeeResult.exceptionOrNull()?.message)
                }
            }

            Result.success()
        } catch (e: Exception) {
            AppLogger.e(TAG, "SyncWorker exception: ${e.message}", e.stackTraceToString(), e)
            Result.retry()
        }
    }
}
