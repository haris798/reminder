package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Coffee log entity representing coffee consumption entries.
 * Stores coffee type, caffeine estimation, timestamp, and sync status.
 */
@Entity(tableName = "coffee_logs")
data class CoffeeLog(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "coffee_type")
    val coffeeType: String,

    @ColumnInfo(name = "caffeine_mg")
    val caffeineMg: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),

    @ColumnInfo(name = "date_string")
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "coffee_type" to coffeeType,
            "caffeine_mg" to caffeineMg,
            "created_at" to createdAt,
            "date_string" to dateString,
            "is_synced" to isSynced
        )
    }

    val formattedTime: String
        get() {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val parsedDate = parser.parse(createdAt)
                if (parsedDate != null) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)
                } else {
                    createdAt
                }
            } catch (e: Exception) {
                createdAt
            }
        }
}
