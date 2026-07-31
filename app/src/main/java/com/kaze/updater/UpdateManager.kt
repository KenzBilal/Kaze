package com.kaze.updater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.kaze.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val sha256: String = ""
)

enum class UpdateState {
    IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, ERROR, UP_TO_DATE
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _updateState = MutableStateFlow(UpdateState.IDLE)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val prefs = context.getSharedPreferences("kaze_update_prefs", Context.MODE_PRIVATE)

    // Persisted across process death — survives system installer killing the app
    private var dismissedVersionCode: Int
        get() = prefs.getInt("dismissed_version", 0)
        set(value) = prefs.edit().putInt("dismissed_version", value).apply()

    private var lastCheckTime: Long
        get() = prefs.getLong("last_check_time", 0L)
        set(value) = prefs.edit().putLong("last_check_time", value).apply()

    private var lastCheckedVersionCode: Int
        get() = prefs.getInt("last_checked_version", 0)
        set(value) = prefs.edit().putInt("last_checked_version", value).apply()

    private val CHECK_COOLDOWN_MS = 30 * 60 * 1000L

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Called when user taps "Later" OR "Update Now".
     * Prevents re-showing dialog for the same version after process death.
     */
    fun dismissUpdate() {
        dismissedVersionCode = _updateInfo.value?.versionCode ?: 0
    }

    suspend fun checkForUpdates() {
        if (BuildConfig.UPDATE_JSON_URL.isBlank()) return
        val now = System.currentTimeMillis()
        val versionChanged = BuildConfig.VERSION_CODE != lastCheckedVersionCode
        if (!versionChanged && now - lastCheckTime < CHECK_COOLDOWN_MS) return
        lastCheckTime = now
        lastCheckedVersionCode = BuildConfig.VERSION_CODE
        if (versionChanged) {
            dismissedVersionCode = 0
        }

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
            if (info.versionCode > BuildConfig.VERSION_CODE &&
                info.versionCode > dismissedVersionCode) {
                _updateState.value = UpdateState.AVAILABLE
            } else {
                _updateState.value = UpdateState.UP_TO_DATE
                if (info.versionCode <= BuildConfig.VERSION_CODE) {
                    dismissedVersionCode = 0
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check for updates", e)
            _updateState.value = UpdateState.ERROR
        }
    }

    suspend fun downloadUpdate() {
        val url = _updateInfo.value?.apkUrl ?: return
        // Dismiss immediately so process death doesn't re-trigger for same version
        dismissUpdate()
        _updateState.value = UpdateState.DOWNLOADING

        try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("UpdateManager", "Download failed: HTTP ${response.code}")
                    _updateState.value = UpdateState.ERROR
                    return@withContext
                }

                val body = response.body ?: run {
                    Log.e("UpdateManager", "Download failed: empty response body")
                    _updateState.value = UpdateState.ERROR
                    return@withContext
                }

                val apkFile = File(context.filesDir, "update.apk")
                apkFile.outputStream().use { out ->
                    body.byteStream().use { inp ->
                        inp.copyTo(out)
                    }
                }

                val magic = ByteArray(4)
                apkFile.inputStream().use { it.read(magic) }
                if (magic[0] != 0x50.toByte() || magic[1] != 0x4B.toByte()) {
                    Log.e("UpdateManager", "Downloaded file is not a valid APK")
                    apkFile.delete()
                    _updateState.value = UpdateState.ERROR
                    return@withContext
                }

                val expectedHash = _updateInfo.value?.sha256.orEmpty()
                if (expectedHash.isNotBlank()) {
                    val actual = sha256Hex(apkFile)
                    if (!actual.equals(expectedHash, ignoreCase = true)) {
                        Log.e("UpdateManager", "SHA-256 mismatch! expected=$expectedHash actual=$actual")
                        apkFile.delete()
                        _updateState.value = UpdateState.ERROR
                        return@withContext
                    }
                }

                Log.d("UpdateManager", "APK downloaded: ${apkFile.length()} bytes")
                _updateState.value = UpdateState.READY_TO_INSTALL
                installApk()
                // Reset to IDLE after launching installer — prevents re-trigger on return
                _updateState.value = UpdateState.IDLE
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download failed", e)
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

            val file = File(context.filesDir, "update.apk")
            if (!file.exists() || file.length() == 0L) {
                Log.e("UpdateManager", "APK file missing or empty")
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
