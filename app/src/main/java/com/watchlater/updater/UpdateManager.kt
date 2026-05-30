package com.watchlater.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.watchlater.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val sha256: String = ""   // hex SHA-256 of APK — optional, verified if present
)

enum class UpdateState {
    IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, ERROR, UP_TO_DATE
}

class UpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow(UpdateState.IDLE)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private var downloadId: Long = -1L
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var receiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                // BUG-06 fix: verify SHA-256 before triggering install
                val expectedHash = _updateInfo.value?.sha256.orEmpty()
                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                if (expectedHash.isNotBlank() && apkFile.exists()) {
                    val actual = sha256Hex(apkFile)
                    if (!actual.equals(expectedHash, ignoreCase = true)) {
                        Log.e("UpdateManager", "SHA-256 mismatch! expected=$expectedHash actual=$actual")
                        apkFile.delete()
                        _updateState.value = UpdateState.ERROR
                        return
                    }
                    Log.d("UpdateManager", "SHA-256 verified OK")
                }
                _updateState.value = UpdateState.READY_TO_INSTALL
                installApk()
            }
        }
    }

    init {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(downloadReceiver, filter)
        }
        receiverRegistered = true
    }

    /** Call from ViewModel.onCleared() to prevent BroadcastReceiver leak (BUG-01 fix). */
    fun release() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(downloadReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w("UpdateManager", "Receiver already unregistered")
            }
            receiverRegistered = false
        }
    }

    suspend fun checkForUpdates() {
        if (BuildConfig.UPDATE_JSON_URL.isBlank()) return

        _updateState.value = UpdateState.CHECKING
        try {
            val info = withContext(Dispatchers.IO) {
                val url = URL(BuildConfig.UPDATE_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5000
                    connection.readTimeout    = 5000
                    val jsonStr = connection.inputStream.bufferedReader().readText()
                    val json    = JSONObject(jsonStr)
                    UpdateInfo(
                        versionCode  = json.getInt("versionCode"),
                        versionName  = json.getString("versionName"),
                        apkUrl       = json.getString("apkUrl"),
                        releaseNotes = json.optString("releaseNotes", ""),
                        sha256       = json.optString("sha256", "")
                    )
                } finally {
                    connection.disconnect()
                }
            }

            _updateInfo.value = info
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _updateState.value = UpdateState.AVAILABLE
            } else {
                _updateState.value = UpdateState.UP_TO_DATE
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("UpdateManager", "Failed to check for updates", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    fun downloadUpdate() {
        val url = _updateInfo.value?.apkUrl ?: return
        _updateState.value = UpdateState.DOWNLOADING

        try {
            val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (destination.exists()) destination.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Kaze Update")
                .setDescription("Downloading latest version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))
                .setMimeType("application/vnd.android.package-archive")

            downloadId = downloadManager.enqueue(request)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("UpdateManager", "Failed to start download", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    fun installApk() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data  = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (!file.exists()) {
                _updateState.value = UpdateState.ERROR
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("UpdateManager", "Failed to install APK", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    // ── SHA-256 helper ─────────────────────────────────────────────────────

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
