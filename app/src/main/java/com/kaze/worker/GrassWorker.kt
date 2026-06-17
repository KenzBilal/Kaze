package com.kaze.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaze.MainActivity
import com.kaze.R

class GrassWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        sendGrassWarning(appContext)
        return Result.success()
    }

    private fun sendGrassWarning(context: Context) {
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

        val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        val color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_blue)

        val builder = NotificationCompat.Builder(context, "BINGE_WARNING_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(color)
            .setContentTitle("Enough touching grass! 🍿")
            .setContentText("It's been 12 hours since your last watch.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's been 12 hours since your last watch. Time for the next episode! Grab some popcorn and let's go."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Open App", pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(1002, builder.build())
        }
    }
}
