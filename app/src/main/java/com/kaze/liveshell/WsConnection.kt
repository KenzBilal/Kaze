package com.kaze.liveshell

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WsConnection(
    private val host: String,
    private val port: Int,
    private val deviceId: String,
    private val authToken: String,
    private val listener: Listener
) {
    interface Listener {
        fun onConnected(sessionId: String)
        fun onMessage(msg: JSONObject)
        fun onDisconnected(reason: String)
        fun onReconnecting(attempt: Int)
    }

    private var ws: WebSocket? = null
    private val closed = AtomicBoolean(false)
    private var attempt = 0
    private val maxAttempts = 50

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    fun connect() {
        if (closed.get()) return

        val protocol = if (port == 443) "wss" else "ws"
        val request = Request.Builder()
            .url("$protocol://$host:$port/ws/shell")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempt = 0
                val auth = JSONObject().apply {
                    put("type", "auth")
                    put("token", authToken)
                    put("device_id", deviceId)
                    put("platform", "android")
                }
                webSocket.send(auth.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = JSONObject(text)
                    when (msg.optString("type")) {
                        "auth_ok" -> listener.onConnected(msg.getString("session_id"))
                        "auth_fail" -> {
                            Log.e("LiveShell", "Auth failed: ${msg.optString("reason")}")
                            webSocket.close(1000, "auth failed")
                            closed.set(true)
                        }
                        else -> listener.onMessage(msg)
                    }
                } catch (e: Exception) {
                    Log.e("LiveShell", "Parse error", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!closed.get()) {
                    listener.onDisconnected(reason)
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!closed.get()) {
                    listener.onDisconnected(t.message ?: "unknown error")
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (closed.get()) return
        if (attempt >= maxAttempts) {
            Log.e("LiveShell", "Max reconnect attempts reached")
            return
        }
        attempt++
        val delay = (1L shl minOf(attempt, 5)) * 1000L  // 1s, 2s, 4s, 8s, 16s, 32s...
        Log.d("LiveShell", "Reconnecting in ${delay}ms (attempt $attempt/$maxAttempts)")
        listener.onReconnecting(attempt)
        Thread.sleep(delay)
        connect()
    }

    fun send(data: JSONObject) {
        ws?.send(data.toString())
    }

    fun close() {
        closed.set(true)
        ws?.close(1000, "client disconnect")
    }
}
