package com.kaze.worker

import android.content.Context
import androidx.work.*
import com.kaze.data.local.ActionType
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.WatchItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
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
        val watchRepo = WatchItemRepository(db.watchItemDao(), db.episodeProgressDao())

        return withContext(Dispatchers.IO) {
            val pending = dao.getAll()
            if (pending.isEmpty()) return@withContext Result.success()

            var allSucceeded = true

            for (action in pending) {
                try {
                    when (action.actionType) {
                        ActionType.FOLLOW -> {
                            userRepo.followUser(action.userId, action.targetId, fromSyncWorker = true)
                            dao.delete(action)
                        }
                        ActionType.UNFOLLOW -> {
                            userRepo.unfollowUser(action.userId, action.targetId, fromSyncWorker = true)
                            dao.delete(action)
                        }
                        ActionType.POST_ACTIVITY -> {
                            // Parse payload and post activity (throws on error)
                            userRepo.postActivityFromPayload(action.payload)
                            dao.delete(action)
                        }
                        ActionType.DELETE_WATCHLIST -> {
                            val item = Json.decodeFromString<WatchItem>(action.payload)
                            userRepo.deleteFromWatchlist(action.userId, item, fromSyncWorker = true)
                            dao.delete(action)
                        }
                        ActionType.SYNC_WATCHLIST -> {
                            val allItems = watchRepo.getAllItemsSnapshot()
                            if (allItems.isNotEmpty()) {
                                userRepo.syncWatchlist(action.userId, allItems, fromSyncWorker = true)
                            }
                            dao.delete(action)
                        }
                        ActionType.SYNC_EPISODE_PROGRESS -> {
                            val allItems = watchRepo.getAllItemsSnapshot()
                            val progressList = db.episodeProgressDao().getAllEpisodeProgressOnce()
                            if (progressList.isNotEmpty() && allItems.isNotEmpty()) {
                                userRepo.syncEpisodeProgress(action.userId, progressList, allItems, fromSyncWorker = true)
                            }
                            dao.delete(action)
                        }
                        ActionType.UPDATE_PROFILE -> {
                            // Payload is a comma separated string: favMovie,favSeries,favGenre
                            val parts = action.payload.split("|||")
                            val favMovie = parts.getOrNull(0)?.ifBlank { null }
                            val favSeries = parts.getOrNull(1)?.ifBlank { null }
                            val favGenre = parts.getOrNull(2)?.ifBlank { null }
                            userRepo.updateProfile(action.userId, favMovie, favSeries, favGenre, fromSyncWorker = true)
                            dao.delete(action)
                        }
                        ActionType.SAVE_FCM_TOKEN -> {
                            val activityRepo = com.kaze.data.repository.ActivityRepository(applicationContext)
                            activityRepo.saveFcmToken(action.userId, action.payload, fromSyncWorker = true)
                            dao.delete(action)
                        }
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
