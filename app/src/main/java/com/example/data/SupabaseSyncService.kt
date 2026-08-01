package com.example.data

import android.util.Log
import com.example.data.model.CoffeeLog
import com.example.data.model.WaterLog
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SupabaseSyncService {

    companion object {
        private const val TAG = "SupabaseSyncService"
    }

    suspend fun syncWaterLogs(config: SupabaseConfig, waterLogs: List<WaterLog>): Result<Int> = withContext(Dispatchers.IO) {
        if (waterLogs.isEmpty()) return@withContext Result.success(0)
        
        val urlString = config.supabaseUrl.trimEnd('/')
        if (urlString.isBlank()) {
            return@withContext Result.failure(IllegalStateException("URL Supabase belum diisi"))
        }
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank() || apiKey == "YOUR_SUPABASE_API_KEY_HERE") {
            return@withContext Result.failure(IllegalStateException("API Key Supabase belum diisi di Pengaturan"))
        }

        try {
            val endpoint = URL("$urlString/rest/v1/water_logs")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
            }

            val jsonArray = JSONArray()
            for (log in waterLogs) {
                val jsonObject = JSONObject().apply {
                    put("id", log.id)
                    put("amount_ml", log.amountMl)
                    put("date_string", log.dateString)
                    put("created_at", log.createdAt)
                }
                jsonArray.put(jsonObject)
            }

            val jsonString = jsonArray.toString()
            val maskedKey = if (apiKey.length > 8) "${apiKey.take(8)}..." else "***"
            val reqDetails = "URL: $endpoint\nMasked Key: $maskedKey\nPayload (${waterLogs.size} items):\n$jsonString"
            AppLogger.i(TAG, "POST /rest/v1/water_logs - Mengirim ${waterLogs.size} data air", reqDetails)

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            if (responseCode in 200..299) {
                val responseText = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                AppLogger.s(TAG, "HTTP $responseCode - Supabase water_logs berhasil di-sync!", "Response: $responseText")
                Result.success(waterLogs.size)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val parsedDetails = parseSupabaseError(errorMsg)
                AppLogger.e(TAG, "HTTP $responseCode $responseMessage - Gagal sync water_logs", "Payload: $jsonString\nError Body: $errorMsg\nParsed: $parsedDetails")

                val detailedError = when (responseCode) {
                    401, 403 -> "Akses Supabase ditolak (HTTP $responseCode): ${parsedDetails.ifBlank { "Cek API Key & Kebijakan RLS di Supabase" }}"
                    404 -> "Tabel 'water_logs' tidak ditemukan di Supabase (HTTP 404). Pastikan tabel 'water_logs' sudah dibuat di SQL Editor."
                    409, 422 -> "Format data/konflik pada 'water_logs' (HTTP $responseCode): $parsedDetails"
                    else -> "Gagal sync water_logs (HTTP $responseCode): $parsedDetails"
                }
                Result.failure(Exception(detailedError))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception saat sync water_logs: ${e.localizedMessage}", e.stackTraceToString(), e)
            Result.failure(e)
        }
    }

    suspend fun syncCoffeeLogs(config: SupabaseConfig, coffeeLogs: List<CoffeeLog>): Result<Int> = withContext(Dispatchers.IO) {
        if (coffeeLogs.isEmpty()) return@withContext Result.success(0)

        val urlString = config.supabaseUrl.trimEnd('/')
        if (urlString.isBlank()) {
            return@withContext Result.failure(IllegalStateException("URL Supabase belum diisi"))
        }
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank() || apiKey == "YOUR_SUPABASE_API_KEY_HERE") {
            return@withContext Result.failure(IllegalStateException("API Key Supabase belum diisi di Pengaturan"))
        }

        try {
            val endpoint = URL("$urlString/rest/v1/coffee_logs")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
            }

            val jsonArray = JSONArray()
            for (log in coffeeLogs) {
                val jsonObject = JSONObject().apply {
                    put("id", log.id)
                    put("coffee_type", log.coffeeType)
                    put("caffeine_mg", log.caffeineMg)
                    put("date_string", log.dateString)
                    put("created_at", log.createdAt)
                }
                jsonArray.put(jsonObject)
            }

            val jsonString = jsonArray.toString()
            val maskedKey = if (apiKey.length > 8) "${apiKey.take(8)}..." else "***"
            val reqDetails = "URL: $endpoint\nMasked Key: $maskedKey\nPayload (${coffeeLogs.size} items):\n$jsonString"
            AppLogger.i(TAG, "POST /rest/v1/coffee_logs - Mengirim ${coffeeLogs.size} data kopi", reqDetails)

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            if (responseCode in 200..299) {
                val responseText = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                AppLogger.s(TAG, "HTTP $responseCode - Supabase coffee_logs berhasil di-sync!", "Response: $responseText")
                Result.success(coffeeLogs.size)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val parsedDetails = parseSupabaseError(errorMsg)
                AppLogger.e(TAG, "HTTP $responseCode $responseMessage - Gagal sync coffee_logs", "Payload: $jsonString\nError Body: $errorMsg\nParsed: $parsedDetails")

                val detailedError = when (responseCode) {
                    401, 403 -> "Akses Supabase ditolak (HTTP $responseCode): ${parsedDetails.ifBlank { "Cek API Key & Kebijakan RLS di Supabase" }}"
                    404 -> "Tabel 'coffee_logs' tidak ditemukan di Supabase (HTTP 404). Pastikan tabel 'coffee_logs' sudah dibuat di SQL Editor."
                    409, 422 -> "Format data/konflik pada 'coffee_logs' (HTTP $responseCode): $parsedDetails"
                    else -> "Gagal sync coffee_logs (HTTP $responseCode): $parsedDetails"
                }
                Result.failure(Exception(detailedError))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception saat sync coffee_logs: ${e.localizedMessage}", e.stackTraceToString(), e)
            Result.failure(e)
        }
    }

    private fun parseSupabaseError(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            val msg = json.optString("message", "")
            val details = json.optString("details", "")
            val hint = json.optString("hint", "")
            val code = json.optString("code", "")

            listOf(msg, details, hint, if (code.isNotBlank()) "Code: $code" else "")
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { rawJson }
        } catch (_: Exception) {
            rawJson
        }
    }
}
