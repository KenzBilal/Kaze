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
    val username: String
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

    /**
     * Creates a new user in Supabase with the given username.
     * Validates format (4-12 letters only) before hitting the network.
     * Saves user ID and username locally on success.
     */
    suspend fun createUser(username: String): CreateUserResult {
        val trimmed = username.trim()

        // Client-side validation (mirrors DB constraint)
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

    // ── Watchlist Sync ────────────────────────────────────────────────────────

    /**
     * Bulk-syncs all local Room WatchItems to Supabase public_watchlist.
     * Uses upsert (insert or update on conflict) so re-calling this is safe.
     * Errors per item are swallowed — if one fails, the rest continue.
     */
    suspend fun syncWatchlist(userId: String, items: List<WatchItem>) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) {
            val payload = items.map { item ->
                PublicWatchlistItem(
                    user_id   = userId,
                    imdb_id   = item.imdbId,
                    title     = item.title,
                    year      = item.year,
                    type      = item.type.name,
                    is_watched = item.isWatched,
                    rating    = item.rating,
                    season    = item.season,
                    episode   = item.episode,
                    notes     = item.notes,
                    poster_url = item.posterUrl,
                    genres    = item.genres,
                    date_added = item.dateAdded
                )
            }

            // Batch upsert — Supabase handles conflicts via unique constraint
            try {
                SupabaseApi.client.from("public_watchlist").upsert(payload) {
                    onConflict = "user_id,title,year,type"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fall back to one-by-one upsert so partial success is possible
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
