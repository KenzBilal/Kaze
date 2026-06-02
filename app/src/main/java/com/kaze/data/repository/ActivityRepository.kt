package com.kaze.data.repository

import android.content.Context
import com.kaze.data.local.ActionType
import com.kaze.data.local.PendingAction
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.remote.SupabaseApi
import com.kaze.worker.SyncWorker
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Models ────────────────────────────────────────────────────────────────────

@Serializable
data class ActivityFeedItem(
    val id: String = "",
    val user_id: String,
    val action_type: String,
    val item_title: String? = null,
    val item_type: String? = null,
    val item_poster_url: String? = null,
    val item_imdb_id: String? = null,
    val target_user_id: String? = null,
    val created_at: String = ""
)

@Serializable
data class ActivityFeedEntry(
    val user_id: String,
    val action_type: String,
    val item_title: String? = null,
    val item_type: String? = null,
    val item_poster_url: String? = null,
    val item_imdb_id: String? = null,
    val target_user_id: String? = null
)

@Serializable
data class FcmToken(
    val user_id: String,
    val token: String
)

// ── Enriched feed item (with user info) ───────────────────────────────────────

data class FeedEvent(
    val actorUsername: String,
    val actorUserId: String,
    val actionType: String,
    val itemTitle: String?,
    val itemType: String?,
    val itemPosterUrl: String?,
    val targetUserId: String?,
    val targetUsername: String?,
    val createdAt: String
)

// ── Repository ────────────────────────────────────────────────────────────────

class ActivityRepository(private val context: Context) {

    private val db by lazy { WatchLaterDatabase.getInstance(context) }
    private val pendingDao by lazy { db.pendingActionDao() }
    private val userRepo = UserRepository(context)

    /**
     * Post an activity event.
     * If offline, queue it for later via WorkManager.
     */
    suspend fun postActivity(entry: ActivityFeedEntry) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("activity_feed").insert(entry)
            } catch (e: Exception) {
                e.printStackTrace()
                // Queue offline — WorkManager will retry
                val payload = Json.encodeToString(entry)
                val userId = userRepo.getLocalUserId() ?: return@withContext
                pendingDao.insert(
                    PendingAction(
                        actionType = ActionType.POST_ACTIVITY,
                        userId = userId,
                        payload = payload
                    )
                )
                SyncWorker.enqueue(context)
            }
        }
    }

    /**
     * Fetch the activity feed for a given set of user IDs (i.e., users you follow).
     * Returns newest events first, limited to 50 per load.
     */
    suspend fun getFeedForUsers(
        userIds: List<String>,
        page: Int = 0,
        pageSize: Int = 30
    ): List<ActivityFeedItem> {
        if (userIds.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("activity_feed")
                    .select {
                        filter {
                            isIn("user_id", userIds)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        range(
                            from = (page * pageSize).toLong(),
                            to = ((page + 1) * pageSize - 1).toLong()
                        )
                    }
                    .decodeList<ActivityFeedItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Get social suggestions — items added by followed users that the current user
     * hasn't watched or added yet.
     */
    suspend fun getSocialSuggestions(
        followedUserIds: List<String>,
        ownImdbIds: Set<String>
    ): List<ActivityFeedItem> {
        if (followedUserIds.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val feedItems = SupabaseApi.client.from("activity_feed")
                    .select {
                        filter {
                            isIn("user_id", followedUserIds)
                            eq("action_type", "added_item")
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(100)
                    }
                    .decodeList<ActivityFeedItem>()

                feedItems.filter { item -> item.item_imdb_id != null && !ownImdbIds.contains(item.item_imdb_id) }
                .distinctBy { it.item_imdb_id }
                .take(50)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Store the FCM push notification token in Supabase.
     */
    suspend fun saveFcmToken(userId: String, token: String, fromSyncWorker: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("fcm_tokens").upsert(
                    FcmToken(user_id = userId, token = token)
                ) { onConflict = "user_id" }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                pendingDao.insert(
                    PendingAction(
                        actionType = ActionType.SAVE_FCM_TOKEN,
                        userId = userId,
                        payload = token
                    )
                )
                SyncWorker.enqueue(context)
            }
        }
    }
}
