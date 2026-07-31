package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Water log entity representing hydration entries.
 * Stores UUID as primary key and isSynced flag for offline-first synchronization.
 */
@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amountMl: Int,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val dateString: String = SimpleDateFormat("yyyy-MM-date", Locale.getDefault()).format(Date()).let {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    },
    val isSynced: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "amount_ml" to amountMl,
            "created_at" to createdAt,
            "date_string" to dateString,
            "is_synced" to isSynced
        )
    }

    companion object {
        fun getCurrentDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        fun getCurrentTimeString(): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }
}
