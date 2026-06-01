package com.kaze.worker

import android.content.Context
import androidx.work.*
import com.kaze.data.local.ActionType
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that processes queued offline actions (follows, unfollows)
 * when network connectivity is restored.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = WatchLaterDatabase.getInstance(applicationContext)
        val dao = db.pendingActionDao()
        val userRepo = UserRepository(applicationContext)

        return withContext(Dispatchers.IO) {
            val pending = dao.getAll()
            if (pending.isEmpty()) return@withContext Result.success()

            var allSucceeded = true

            for (action in pending) {
                try {
                    when (action.actionType) {
                        ActionType.FOLLOW -> {
                            userRepo.followUser(action.userId, action.targetId)
                            dao.delete(action)
                        }
                        ActionType.UNFOLLOW -> {
                            userRepo.unfollowUser(action.userId, action.targetId)
                            dao.delete(action)
                        }
                        ActionType.POST_ACTIVITY -> {
                            // Parse payload and post activity
                            userRepo.postActivityFromPayload(action.payload)
                            dao.delete(action)
                        }
                        ActionType.SYNC_WATCHLIST -> dao.delete(action) // handled elsewhere
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSucceeded = false
                }
            }

            if (allSucceeded) Result.success() else Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "kaze_sync_worker"

        /**
         * Enqueue a one-time sync when network is available.
         * Safe to call multiple times — WorkManager deduplicates.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
