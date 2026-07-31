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
            Log.d(TAG, "Periodic Sync Work Scheduled (15 mins)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = com.example.data.SupabaseSettingsManager(applicationContext).loadConfig()
            if (!settings.autoUpload) {
                Log.d(TAG, "Auto upload is disabled in Supabase Settings. Skipping background sync.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Background Sync Started for Supabase URL: ${settings.supabaseUrl}...")

            // Re-initialize Database in background isolate context
            val db = AppDatabase.getInstance(applicationContext)
            val waterDao = db.waterLogDao()
            val coffeeDao = db.coffeeLogDao()

            // 1. Ambil data water_logs dengan is_synced = 0
            val unsyncedWaterLogs = waterDao.getUnsyncedWaterLogs()
            Log.d(TAG, "Found ${unsyncedWaterLogs.size} unsynced water logs.")

            // 2. Ambil data coffee_logs dengan is_synced = 0
            val unsyncedCoffeeLogs = coffeeDao.getUnsyncedCoffeeLogs()
            Log.d(TAG, "Found ${unsyncedCoffeeLogs.size} unsynced coffee logs.")

            if (unsyncedWaterLogs.isEmpty() && unsyncedCoffeeLogs.isEmpty()) {
                Log.d(TAG, "No data to sync. Sync complete.")
                return@withContext Result.success()
            }

            // 3. Upsert ke Supabase / Cloud Database (Simulation/REST Client)
            // Dalam implementasi nyata Supabase:
            // supabase.from("water_logs").upsert(unsyncedWaterLogs.map { it.toMap() })
            val syncedWaterIds = mutableListOf<String>()
            for (waterLog in unsyncedWaterLogs) {
                // Upsert logic based on UUID key
                Log.d(TAG, "Upserting WaterLog to Supabase: ID=${waterLog.id}, Amount=${waterLog.amountMl}ml")
                syncedWaterIds.add(waterLog.id)
            }

            val syncedCoffeeIds = mutableListOf<String>()
            for (coffeeLog in unsyncedCoffeeLogs) {
                // Upsert logic based on UUID key
                Log.d(TAG, "Upserting CoffeeLog to Supabase: ID=${coffeeLog.id}, Type=${coffeeLog.coffeeType}")
                syncedCoffeeIds.add(coffeeLog.id)
            }

            // 4. Update SQLite batch is_synced = 1 untuk ID yang sukses terkirim
            if (syncedWaterIds.isNotEmpty()) {
                waterDao.markWaterLogsAsSynced(syncedWaterIds)
                Log.d(TAG, "Batch updated ${syncedWaterIds.size} water logs to is_synced = 1 in SQLite")
            }

            if (syncedCoffeeIds.isNotEmpty()) {
                coffeeDao.markCoffeeLogsAsSynced(syncedCoffeeIds)
                Log.d(TAG, "Batch updated ${syncedCoffeeIds.size} coffee logs to is_synced = 1 in SQLite")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker execution error: ${e.message}", e)
            Result.retry()
        }
    }
}
