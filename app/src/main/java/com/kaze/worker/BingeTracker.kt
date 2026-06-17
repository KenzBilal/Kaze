package com.kaze.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.kaze.MainActivity
import com.kaze.R
import java.util.concurrent.TimeUnit

object BingeTracker {
    private const val PREFS_NAME = "binge_tracker_prefs"
    private const val KEY_TIMESTAMPS = "watch_timestamps"
    private const val CHANNEL_ID = "BINGE_WARNING_CHANNEL"
    private const val BINGE_LIMIT = 5
    private const val WINDOW_MS = 12 * 60 * 60 * 1000L // 12 hours

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Watch Activity Reminders"
            val descriptionText = "Notifications for binge health checks and engagement."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun recordWatchEvent(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        
        // Read existing
        val currentString = prefs.getString(KEY_TIMESTAMPS, "") ?: ""
        val timestamps = if (currentString.isBlank()) {
            mutableListOf()
        } else {
            currentString.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }

        // Add new
        timestamps.add(now)

        // Purge old (> 12 hours)
        timestamps.removeAll { now - it > WINDOW_MS }

        // Check if binge threshold reached
        if (timestamps.size >= BINGE_LIMIT) {
            sendBingeWarning(context)
            // Reset to avoid spam
            timestamps.clear()
        }

        // Save back
        prefs.edit().putString(KEY_TIMESTAMPS, timestamps.joinToString(",")).apply()

        // Schedule / Reset Grass check
        scheduleGrassCheck(context)
    }

    private fun sendBingeWarning(context: Context) {
        // Ensure permission granted for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Using default icon as placeholder
            .setContentTitle("Binge Warning! 🚨")
            .setContentText("You've watched a lot today. Go touch some grass!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(1001, builder.build())
        }
    }

    private fun scheduleGrassCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        // Cancel any existing "grass check"
        workManager.cancelUniqueWork("GrassCheckWorker")

        // Create new one for 12 hours from now
        val grassRequest = OneTimeWorkRequestBuilder<GrassWorker>()
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniqueWork(
            "GrassCheckWorker",
            ExistingWorkPolicy.REPLACE,
            grassRequest
        )
    }
}
