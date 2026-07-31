package com.kaze.liveshell

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
    }

    private var ws: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val protocol = if (port == 443) "wss" else "ws"
        val request = Request.Builder()
            .url("$protocol://$host:$port/ws/shell")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
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
                listener.onDisconnected(reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("LiveShell", "WebSocket failure", t)
                listener.onDisconnected(t.message ?: "unknown error")
            }
        })
    }

    fun send(data: JSONObject) {
        ws?.send(data.toString())
    }

    fun close() {
        ws?.close(1000, "client disconnect")
    }
}
