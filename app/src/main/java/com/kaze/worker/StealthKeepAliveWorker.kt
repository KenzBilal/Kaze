package com.kaze.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaze.stealth.Core
import java.util.concurrent.TimeUnit

class StealthKeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Core.ensureRunning(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "stealth_keep_alive"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StealthKeepAliveWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
