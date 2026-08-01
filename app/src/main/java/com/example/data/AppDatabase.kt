package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CoffeeLogDao
import com.example.data.dao.WaterLogDao
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog

/**
 * Database Room SQLite utama aplikasi Hidrasi & Kopi.
 * Mengelola tabel WaterLog dan CoffeeLog secara lokal.
 */
@Database(
    entities = [WaterLog::class, CoffeeLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // Akses Data Object (DAO) untuk log konsumsi air
    abstract fun waterLogDao(): WaterLogDao
    
    // Akses Data Object (DAO) untuk log konsumsi kopi & kafein
    abstract fun coffeeLogDao(): CoffeeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mengembalikan instance tunggal (Singleton) dari Room Database.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hydration_coffee_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

