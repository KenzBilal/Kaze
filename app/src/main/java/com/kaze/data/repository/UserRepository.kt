package com.kaze.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kaze.data.local.ActionType
import com.kaze.data.local.PendingAction
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.remote.SupabaseApi
import com.kaze.model.WatchItem
import com.kaze.worker.SyncWorker
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

// ── Models ────────────────────────────────────────────────────────────────────

@Serializable
data class SupabaseUser(
    val id: String,
    val username: String,
    val fav_movie: String? = null,
    val fav_series: String? = null,
    val fav_genre: String? = null
)

@Serializable
data class PublicWatchlistItem(
    val user_id: String,
    val imdb_id: String,
    val title: String,
    val year: Int,
    val type: String,
    val is_watched: Boolean,
    val rating: Float,
    val season: Int?,
    val episode: Int?,
    val notes: String,
    val poster_url: String?,
    val genres: String,
    val date_added: Long,
    val last_updated: Long = 0L
)

@Serializable
data class PublicEpisodeProgress(
    val user_id: String,
    val imdb_id: String,
    val series_title: String,
    val season: Int,
    val episode_number: Int,
    val is_watched: Boolean,
    val watched_at: Long?,
    val last_updated: Long
)

@Serializable
data class FollowRelation(
    val follower_id: String,
    val following_id: String
)

data class CreateUserResult(
    val success: Boolean,
    val errorMessage: String? = null
)

// ── Repository ────────────────────────────────────────────────────────────────

class UserRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // ── Local State ──────────────────────────────────────────────────────────

    suspend fun getLocalUserId(): String? = withContext(Dispatchers.IO) {
        prefs.getString("user_id", null)
    }

    suspend fun getLocalUsername(): String? = withContext(Dispatchers.IO) {
        prefs.getString("username", null)
    }

    private fun saveLocalUser(id: String, username: String) {
        prefs.edit()
            .putString("user_id", id)
            .putString("username", username)
            .apply()
    }

    // ── Account Creation ─────────────────────────────────────────────────────

    suspend fun createUser(username: String): CreateUserResult {
        val trimmed = username.trim()

        if (trimmed.length < 4 || trimmed.length > 12) {
            return CreateUserResult(false, "Username must be 4–12 characters.")
        }
        if (!trimmed.matches(Regex("^[A-Za-z]+$"))) {
            return CreateUserResult(false, "Username can only contain letters.")
        }

        return withContext(Dispatchers.IO) {
            try {
                // First check if user exists to login instead of create
                val existingUsers = SupabaseApi.client.from("users")
                    .select { filter { eq("username", trimmed) } }
                    .decodeList<SupabaseUser>()

                if (existingUsers.isNotEmpty()) {
                    // Account recovery: login to existing username
                    saveLocalUser(existingUsers.first().id, trimmed)
                    return@withContext CreateUserResult(success = true)
                }

                // If not exists, create new
                val newId = UUID.randomUUID().toString()
                val user = SupabaseUser(id = newId, username = trimmed)
                SupabaseApi.client.from("users").insert(user)
                
                saveLocalUser(newId, trimmed)
                CreateUserResult(success = true)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("unique", ignoreCase = true) == true ->
                        "Username \"$trimmed\" is already taken. Try another."
                    else -> "Network error. Please try again."
                }
                CreateUserResult(false, msg)
            }
        }
    }

    // ── Profile Update ────────────────────────────────────────────────────────

    @Serializable
    private data class ProfileUpdate(
        val fav_movie: String?,
        val fav_series: String?,
        val fav_genre: String?
    )

    suspend fun updateProfile(userId: String, favMovie: String?, favSeries: String?, favGenre: String?, fromSyncWorker: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val update = ProfileUpdate(
                    fav_movie  = favMovie?.ifBlank { null },
                    fav_series = favSeries?.ifBlank { null },
                    fav_genre  = favGenre?.ifBlank { null }
                )
                SupabaseApi.client.from("users").update(update) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                val payload = "${favMovie.orEmpty()}|||${favSeries.orEmpty()}|||${favGenre.orEmpty()}"
                val dao = com.kaze.data.local.WatchLaterDatabase.getInstance(context).pendingActionDao()
                dao.insert(
                    com.kaze.data.local.PendingAction(
                        actionType = com.kaze.data.local.ActionType.UPDATE_PROFILE,
                        userId = userId,
                        payload = payload
                    )
                )
                com.kaze.worker.SyncWorker.enqueue(context)
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchUsers(query: String): List<SupabaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    // Return all users (limited) for friends list default view
                    SupabaseApi.client.from("users")
                        .select { limit(50) }
                        .decodeList<SupabaseUser>()
                } else {
                    SupabaseApi.client.from("users")
                        .select {
                            filter {
                                ilike("username", "%${query.trim()}%")
                            }
                            limit(20)
                        }
                        .decodeList<SupabaseUser>()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun getUserById(userId: String): SupabaseUser? {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("users")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<SupabaseUser>()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): List<SupabaseUser> {
        if (userIds.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("users")
                    .select { filter { isIn("id", userIds) } }
                    .decodeList<SupabaseUser>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getWatchlistByUserId(userId: String): List<PublicWatchlistItem> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("public_watchlist")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<PublicWatchlistItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getWatchlistsByUserIds(userIds: Collection<String>): List<PublicWatchlistItem> {
        if (userIds.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("public_watchlist")
                    .select { filter { isIn("user_id", userIds.toList()) } }
                    .decodeList<PublicWatchlistItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // ── Follows ───────────────────────────────────────────────────────────────

    suspend fun getFollowersCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("follows")
                    .select { filter { eq("following_id", userId) } }
                    .decodeList<FollowRelation>().size
            } catch (e: Exception) { 0 }
        }
    }

    suspend fun getFollowingCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("follows")
                    .select { filter { eq("follower_id", userId) } }
                    .decodeList<FollowRelation>().size
            } catch (e: Exception) { 0 }
        }
    }

    suspend fun getFollowersList(userId: String): List<SupabaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val relations = SupabaseApi.client.from("follows")
                    .select { filter { eq("following_id", userId) } }
                    .decodeList<FollowRelation>()
                val followerIds = relations.map { it.follower_id }
                getUsersByIds(followerIds)
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun getFollowingList(userId: String): List<SupabaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val relations = SupabaseApi.client.from("follows")
                    .select { filter { eq("follower_id", userId) } }
                    .decodeList<FollowRelation>()
                val followingIds = relations.map { it.following_id }
                getUsersByIds(followingIds)
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = SupabaseApi.client.from("follows")
                    .select {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<FollowRelation>()
                result.isNotEmpty()
            } catch (e: Exception) { false }
        }
    }

    suspend fun getFollowedIds(followerId: String): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                val relations = SupabaseApi.client.from("follows")
                    .select { filter { eq("follower_id", followerId) } }
                    .decodeList<FollowRelation>()
                relations.map { it.following_id }.toSet()
            } catch (e: Exception) { emptySet() }
        }
    }

    suspend fun followUser(followerId: String, followingId: String, fromSyncWorker: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val rel = FollowRelation(follower_id = followerId, following_id = followingId)
                SupabaseApi.client.from("follows").insert(rel)
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                // Queue for offline sync
                val db = WatchLaterDatabase.getInstance(context)
                db.pendingActionDao().insert(
                    PendingAction(
                        actionType = ActionType.FOLLOW,
                        userId = followerId,
                        targetId = followingId
                    )
                )
                SyncWorker.enqueue(context)
            }
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String, fromSyncWorker: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("follows").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                // Queue for offline sync
                val db = WatchLaterDatabase.getInstance(context)
                db.pendingActionDao().insert(
                    PendingAction(
                        actionType = ActionType.UNFOLLOW,
                        userId = followerId,
                        targetId = followingId
                    )
                )
                SyncWorker.enqueue(context)
            }
        }
    }

    /**
     * Called by SyncWorker to replay a queued activity event payload.
     */
    suspend fun postActivityFromPayload(payload: String) {
        val entry = Json.decodeFromString<com.kaze.data.repository.ActivityFeedEntry>(payload)
        withContext(Dispatchers.IO) {
            SupabaseApi.client.from("activity_feed").insert(entry)
        }
    }

    // ── Paginated Watchlist ───────────────────────────────────────────────────

    suspend fun getWatchlistByUserIdPaged(
        userId: String,
        page: Int = 0,
        pageSize: Int = 30
    ): List<PublicWatchlistItem> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("public_watchlist")
                    .select {
                        filter { eq("user_id", userId) }
                        range(
                            from = (page * pageSize).toLong(),
                            to = ((page + 1) * pageSize - 1).toLong()
                        )
                    }
                    .decodeList<PublicWatchlistItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // ── Watchlist Sync ────────────────────────────────────────────────────────

    suspend fun pushWatchItem(userId: String, item: WatchItem) {
        syncWatchlist(userId, listOf(item))
    }

    suspend fun deleteFromWatchlist(userId: String, item: WatchItem, fromSyncWorker: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("public_watchlist").delete {
                    filter {
                        eq("user_id", userId)
                        if (item.imdbId.isNotBlank()) {
                            eq("imdb_id", item.imdbId)
                        } else {
                            eq("title", item.title)
                            eq("year", item.year)
                            eq("type", item.type.name)
                        }
                    }
                }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                val dao = com.kaze.data.local.WatchLaterDatabase.getInstance(context).pendingActionDao()
                dao.insert(
                    com.kaze.data.local.PendingAction(
                        actionType = com.kaze.data.local.ActionType.DELETE_WATCHLIST,
                        userId = userId,
                        payload = kotlinx.serialization.json.Json.encodeToString(item)
                    )
                )
                com.kaze.worker.SyncWorker.enqueue(context)
            }
        }
    }

    suspend fun syncWatchlist(userId: String, items: List<WatchItem>, fromSyncWorker: Boolean = false) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            // Fetch current server last_updated timestamps to avoid overwriting newer data
            val serverTimestamps: Map<String, Long> = try {
                SupabaseApi.client.from("public_watchlist")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("imdb_id", "title", "year", "type", "last_updated")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<PublicWatchlistItem>()
                    .associateBy(
                        keySelector   = { it.imdb_id.ifBlank { "${it.title}_${it.year}_${it.type}" } },
                        valueTransform = { it.last_updated }
                    )
            } catch (_: Exception) { emptyMap() }

            // Only upsert items newer than what the server has
            val payload = items.filter { item ->
                val key = item.imdbId.ifBlank { "${item.title}_${item.year}_${item.type.name}" }
                val serverTs = serverTimestamps[key] ?: 0L
                item.lastUpdated > serverTs
            }.map { item ->
                PublicWatchlistItem(
                    user_id      = userId,
                    imdb_id      = item.imdbId,
                    title        = item.title,
                    year         = item.year,
                    type         = item.type.name,
                    is_watched   = item.isWatched,
                    rating       = item.rating,
                    season       = item.season,
                    episode      = item.episode,
                    notes        = item.notes,
                    poster_url   = item.posterUrl,
                    genres       = item.genres,
                    date_added   = item.dateAdded,
                    last_updated = item.lastUpdated
                )
            }

            if (payload.isEmpty()) return@withContext

            try {
                SupabaseApi.client.from("public_watchlist").upsert(payload) {
                    onConflict = "user_id,title,year,type"
                }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                payload.forEach { entry ->
                    try {
                        SupabaseApi.client.from("public_watchlist").upsert(entry) {
                            onConflict = "user_id,title,year,type"
                        }
                    } catch (inner: Exception) {
                        inner.printStackTrace()
                        val dao = com.kaze.data.local.WatchLaterDatabase.getInstance(context).pendingActionDao()
                        dao.insert(
                            com.kaze.data.local.PendingAction(
                                actionType = com.kaze.data.local.ActionType.SYNC_WATCHLIST,
                                userId = userId
                            )
                        )
                        com.kaze.worker.SyncWorker.enqueue(context)
                    }
                }
            }
        }
    }

    suspend fun syncEpisodeProgress(userId: String, progressList: List<com.kaze.data.local.EpisodeProgress>, items: List<WatchItem>, fromSyncWorker: Boolean = false) {
        if (progressList.isEmpty()) return
        withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            val itemMap = items.associateBy { it.id }
            
            // Map to cloud models
            val payload = progressList.mapNotNull { local ->
                val item = itemMap[local.watchItemId] ?: return@mapNotNull null
                val stableId = item.imdbId.ifBlank { "${item.title}_${item.year}_${item.type.name}" }
                PublicEpisodeProgress(
                    user_id = userId,
                    imdb_id = stableId,
                    series_title = item.title,
                    season = local.season,
                    episode_number = local.episodeNumber,
                    is_watched = local.isWatched,
                    watched_at = local.watchedAt,
                    last_updated = local.watchedAt ?: System.currentTimeMillis() // Assuming watchedAt is updated when toggled
                )
            }

            try {
                // Fetch server timestamps
                val serverTimestamps: Map<String, Long> = try {
                    SupabaseApi.client.from("public_episode_progress")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("imdb_id", "season", "episode_number", "last_updated")) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<PublicEpisodeProgress>()
                        .associateBy(
                            keySelector = { "${it.imdb_id}_${it.season}_${it.episode_number}" },
                            valueTransform = { it.last_updated }
                        )
                } catch (_: Exception) { emptyMap() }

                // Only upload if local is newer or not present on server
                val upsertPayload = payload.filter {
                    val key = "${it.imdb_id}_${it.season}_${it.episode_number}"
                    val serverTs = serverTimestamps[key] ?: 0L
                    it.last_updated > serverTs
                }

                if (upsertPayload.isNotEmpty()) {
                    SupabaseApi.client.from("public_episode_progress").upsert(upsertPayload) {
                        onConflict = "user_id,imdb_id,season,episode_number"
                    }
                }
            } catch (e: Exception) {
                if (fromSyncWorker) throw e
                e.printStackTrace()
                val dao = com.kaze.data.local.WatchLaterDatabase.getInstance(context).pendingActionDao()
                dao.insert(
                    com.kaze.data.local.PendingAction(
                        actionType = com.kaze.data.local.ActionType.SYNC_EPISODE_PROGRESS,
                        userId = userId
                    )
                )
                com.kaze.worker.SyncWorker.enqueue(context)
            }
        }
    }
}
