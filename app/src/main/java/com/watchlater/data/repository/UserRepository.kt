package com.watchlater.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.watchlater.data.remote.SupabaseApi
import com.watchlater.model.WatchItem
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
    val date_added: Long
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
                val newId = UUID.randomUUID().toString()
                val user = SupabaseUser(id = newId, username = trimmed)
                SupabaseApi.client.from("users").insert(user)
                saveLocalUser(newId, trimmed)
                CreateUserResult(success = true)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("unique", ignoreCase = true) == true ->
                        "Username \"$trimmed\" is already taken. Try another."
                    e.message?.contains("check", ignoreCase = true) == true ->
                        "Username doesn't meet the requirements."
                    else -> "Connection error. Please check your internet and try again."
                }
                CreateUserResult(success = false, errorMessage = msg)
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

    suspend fun updateProfile(userId: String, favMovie: String?, favSeries: String?, favGenre: String?) {
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
                e.printStackTrace()
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchUsers(query: String): List<SupabaseUser> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("users")
                    .select {
                        filter {
                            ilike("username", "%${query.trim()}%")
                        }
                        limit(20)
                    }
                    .decodeList<SupabaseUser>()
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
                relations.mapNotNull { getUserById(it.follower_id) }
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun getFollowingList(userId: String): List<SupabaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val relations = SupabaseApi.client.from("follows")
                    .select { filter { eq("follower_id", userId) } }
                    .decodeList<FollowRelation>()
                relations.mapNotNull { getUserById(it.following_id) }
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

    suspend fun followUser(followerId: String, followingId: String) {
        withContext(Dispatchers.IO) {
            try {
                val rel = FollowRelation(follower_id = followerId, following_id = followingId)
                SupabaseApi.client.from("follows").insert(rel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("follows").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Watchlist Sync ────────────────────────────────────────────────────────

    suspend fun syncWatchlist(userId: String, items: List<WatchItem>) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) {
            val payload = items.map { item ->
                PublicWatchlistItem(
                    user_id    = userId,
                    imdb_id    = item.imdbId,
                    title      = item.title,
                    year       = item.year,
                    type       = item.type.name,
                    is_watched = item.isWatched,
                    rating     = item.rating,
                    season     = item.season,
                    episode    = item.episode,
                    notes      = item.notes,
                    poster_url = item.posterUrl,
                    genres     = item.genres,
                    date_added = item.dateAdded
                )
            }

            try {
                SupabaseApi.client.from("public_watchlist").upsert(payload) {
                    onConflict = "user_id,title,year,type"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                payload.forEach { entry ->
                    try {
                        SupabaseApi.client.from("public_watchlist").upsert(entry) {
                            onConflict = "user_id,title,year,type"
                        }
                    } catch (inner: Exception) {
                        inner.printStackTrace()
                    }
                }
            }
        }
    }
}
