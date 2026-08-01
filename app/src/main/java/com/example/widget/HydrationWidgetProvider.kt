package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.repository.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_ADD_WATER_250 = "com.example.widget.ACTION_QUICK_ADD_WATER_250"
        const val ACTION_QUICK_ADD_WATER_500 = "com.example.widget.ACTION_QUICK_ADD_WATER_500"
        const val ACTION_QUICK_ADD_COFFEE = "com.example.widget.ACTION_QUICK_ADD_COFFEE"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            waterMl: Int = 0,
            caffeineMg: Int = 0
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_hydration_quick_log)

            views.setTextViewText(R.id.widget_text_water, "$waterMl ml")
            views.setTextViewText(R.id.widget_text_caffeine, "$caffeineMg mg")

            // Intent to open MainActivity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            // PendingIntents for quick log actions
            views.setOnClickPendingIntent(
                R.id.widget_btn_water_250,
                getPendingSelfIntent(context, ACTION_QUICK_ADD_WATER_250)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_water_500,
                getPendingSelfIntent(context, ACTION_QUICK_ADD_WATER_500)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_coffee,
                getPendingSelfIntent(context, ACTION_QUICK_ADD_COFFEE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_refresh,
                getPendingSelfIntent(context, ACTION_REFRESH_WIDGET)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, HydrationWidgetProvider::class.java).apply {
                this.action = action
            }
            val requestCode = action.hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HydrationWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val repository = HydrationRepository(db.waterLogDao(), db.coffeeLogDao())
                val todayIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val waterTotal = repository.getTotalWaterForDateDirect(todayIsoDate)
                val caffeineTotal = repository.getTotalCaffeineForDateDirect(todayIsoDate)

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, waterTotal, caffeineTotal)
                    }
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action ?: return
        if (action in listOf(
                ACTION_QUICK_ADD_WATER_250,
                ACTION_QUICK_ADD_WATER_500,
                ACTION_QUICK_ADD_COFFEE,
                ACTION_REFRESH_WIDGET
            )
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val repository = HydrationRepository(db.waterLogDao(), db.coffeeLogDao())
                    val todayIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    var toastMsg: String? = null

                    when (action) {
                        ACTION_QUICK_ADD_WATER_250 -> {
                            repository.addWaterLog(250, todayIsoDate)
                            toastMsg = "💧 +250 ml air dicatat!"
                        }
                        ACTION_QUICK_ADD_WATER_500 -> {
                            repository.addWaterLog(500, todayIsoDate)
                            toastMsg = "💧 +500 ml air dicatat!"
                        }
                        ACTION_QUICK_ADD_COFFEE -> {
                            repository.addCoffeeLog("Espresso Widget", 80, todayIsoDate)
                            toastMsg = "☕ Kopi dicatat (+80 mg kafein)!"
                        }
                        ACTION_REFRESH_WIDGET -> {
                            toastMsg = "🔄 Widget diperbarui"
                        }
                    }

                    val waterTotal = repository.getTotalWaterForDateDirect(todayIsoDate)
                    val caffeineTotal = repository.getTotalCaffeineForDateDirect(todayIsoDate)

                    withContext(Dispatchers.Main) {
                        toastMsg?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val componentName = ComponentName(context, HydrationWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        for (appWidgetId in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId, waterTotal, caffeineTotal)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
