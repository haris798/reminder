package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CoffeeLogDao
import com.example.data.dao.WaterLogDao
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog

@Database(
    entities = [WaterLog::class, CoffeeLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
    abstract fun coffeeLogDao(): CoffeeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hydration_coffee_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
