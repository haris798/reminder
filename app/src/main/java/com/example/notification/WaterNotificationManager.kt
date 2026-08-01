package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import com.example.worker.ReminderWorker
import java.util.concurrent.TimeUnit

/**
 * Information on dynamic notification scheduling based on hydration progress.
 */
data class DynamicScheduleInfo(
    val intervalMinutes: Long,
    val progressPercent: Int,
    val frequencyLabel: String,
    val statusDescription: String,
    val isGoalReached: Boolean
)

/**
 * Handles creation of Notification Channel and dynamic local periodic hydration reminders.
 */
class WaterNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "water_hydration_reminder_channel"
        const val CHANNEL_NAME = "Pengingat Minum Air"
        const val NOTIFICATION_ID = 1001
        const val DYNAMIC_REMINDER_WORK_NAME = "dynamic_hydration_reminder_work"
        private const val TAG = "WaterNotificationMgr"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Notifikasi pengingat minum air periodik adaptif berbasis progres harian."
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Computes dynamic notification interval based on progress percentage:
     * - < 25%: 45 minutes (High frequency)
     * - 25% - 49%: 60 minutes (Standard frequency)
     * - 50% - 74%: 90 minutes (Moderate frequency)
     * - 75% - 99%: 120 minutes (Relaxed frequency)
     * - >= 100%: 0 minutes (Reminder stopped as target reached)
     */
    fun calculateDynamicSchedule(currentMl: Int, targetMl: Int): DynamicScheduleInfo {
        if (targetMl <= 0) {
            return DynamicScheduleInfo(0, 0, "Nonaktif", "Target belum diatur", false)
        }

        val percent = ((currentMl.toFloat() / targetMl.toFloat()) * 100).toInt().coerceIn(0, 100)

        return when {
            percent >= 100 -> DynamicScheduleInfo(
                intervalMinutes = 0,
                progressPercent = percent,
                frequencyLabel = "Target Tercapai 🎉",
                statusDescription = "Notifikasi pengingat dihentikan karena target hidrasi harian sudah terpenuhi.",
                isGoalReached = true
            )
            percent < 25 -> DynamicScheduleInfo(
                intervalMinutes = 45,
                progressPercent = percent,
                frequencyLabel = "Sangat Sering (tiap 45m)",
                statusDescription = "Progres awal ($percent%). Pengingat dijadwalkan lebih sering untuk mengejar hidrasi.",
                isGoalReached = false
            )
            percent in 25..49 -> DynamicScheduleInfo(
                intervalMinutes = 60,
                progressPercent = percent,
                frequencyLabel = "Standar (tiap 60m)",
                statusDescription = "Progres $percent%. Pengingat dijadwalkan setiap 1 jam.",
                isGoalReached = false
            )
            percent in 50..74 -> DynamicScheduleInfo(
                intervalMinutes = 90,
                progressPercent = percent,
                frequencyLabel = "Moderat (tiap 90m)",
                statusDescription = "Progres $percent%. Pengingat dijadwalkan setiap 1,5 jam.",
                isGoalReached = false
            )
            else -> DynamicScheduleInfo(
                intervalMinutes = 120,
                progressPercent = percent,
                frequencyLabel = "Santai (tiap 120m)",
                statusDescription = "Hampir mencapai target ($percent%). Pengingat dijadwalkan setiap 2 jam.",
                isGoalReached = false
            )
        }
    }

    /**
     * Dynamically updates or cancels periodic reminder schedule based on current progress.
     */
    fun updateDynamicReminderSchedule(currentMl: Int, targetMl: Int): DynamicScheduleInfo {
        val schedule = calculateDynamicSchedule(currentMl, targetMl)
        val workManager = WorkManager.getInstance(context)

        if (schedule.isGoalReached || schedule.intervalMinutes <= 0) {
            workManager.cancelUniqueWork(DYNAMIC_REMINDER_WORK_NAME)
            Log.d(TAG, "Goal reached ($currentMl / $targetMl ml). Cancelled periodic reminder work.")
        } else {
            // Minimum WorkManager periodic interval is 15 mins.
            val interval = schedule.intervalMinutes.coerceAtLeast(15)
            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(interval, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DYNAMIC_REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
            Log.d(TAG, "Scheduled dynamic reminder: ${schedule.frequencyLabel} (interval: ${interval}m)")
        }

        return schedule
    }

    fun showHydrationReminderNotification(
        currentMl: Int,
        targetMl: Int,
        hoursSinceLastLog: Double? = null
    ) {
        val remainingMl = targetMl - currentMl
        if (remainingMl <= 0) {
            return
        }

        val scheduleInfo = calculateDynamicSchedule(currentMl, targetMl)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            hoursSinceLastLog != null && hoursSinceLastLog >= 2.0 -> {
                val roundedHours = hoursSinceLastLog.toInt()
                "Sudah $roundedHours jam Anda belum mencatat minum air. Yuk, minum air putih sekarang! (Terpenuhi $currentMl / $targetMl ml)"
            }
            else -> {
                "Anda sudah minum $currentMl ml. Tinggal $remainingMl ml lagi menuju target $targetMl ml."
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Waktunya Minum Air! 💧 (${scheduleInfo.progressPercent}%)")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

