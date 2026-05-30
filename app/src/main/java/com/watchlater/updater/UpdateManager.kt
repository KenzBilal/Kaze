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

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
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

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
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
            context.registerReceiver(downloadReceiver, filter)
        }
    }

    suspend fun checkForUpdates() {
        if (BuildConfig.UPDATE_JSON_URL.isBlank()) return

        _updateState.value = UpdateState.CHECKING
        try {
            val info = withContext(Dispatchers.IO) {
                val url = URL(BuildConfig.UPDATE_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val jsonStr = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(jsonStr)

                UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    apkUrl = json.getString("apkUrl"),
                    releaseNotes = json.optString("releaseNotes", "")
                )
            }

            _updateInfo.value = info
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _updateState.value = UpdateState.AVAILABLE
            } else {
                _updateState.value = UpdateState.UP_TO_DATE
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check for updates", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    fun downloadUpdate() {
        val url = _updateInfo.value?.apkUrl ?: return
        _updateState.value = UpdateState.DOWNLOADING

        try {
            // Remove old APK if exists
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
            Log.e("UpdateManager", "Failed to start download", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    fun installApk() {
        try {
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
            Log.e("UpdateManager", "Failed to install APK", e)
            _updateState.value = UpdateState.ERROR
        }
    }
}
