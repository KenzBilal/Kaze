package com.kaze.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.kaze.data.local.EpisodeProgress
import com.kaze.data.remote.SupabaseApi
import com.kaze.data.repository.UserRepository
import com.kaze.data.repository.WatchItemRepository
import com.kaze.model.MediaType
import com.kaze.model.WatchItem
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

import androidx.annotation.Keep

// ─── Backup data classes ────────────────────────────────────────────────────

@Keep
data class BackupPayload(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<WatchItemBackup>,
    val episodeProgress: List<EpisodeProgressBackup> = emptyList()
)

/**
 * BUG-02 fix: posterUrl, genres, and imdbId are now included in the backup.
 */
@Keep
data class WatchItemBackup(
    val title: String,
    val year: Int,
    val type: String,
    val isWatched: Boolean,
    val rating: Float,
    val season: Int?,
    val episode: Int?,
    val notes: String,
    val posterUrl: String?,
    val genres: String,
    val imdbId: String,
    val dateAdded: Long,
    val lastUpdated: Long
)

/**
 * BUG-03 fix: Episode progress is exported/imported using imdbId as the stable key
 * so that it survives the autoincrement ID reassignment on restore.
 */
@Keep
data class EpisodeProgressBackup(
    val imdbId: String,         // stable foreign key — not the DB autoincrement id
    val season: Int,
    val episodeNumber: Int,
    val isWatched: Boolean,
    val watchedAt: Long?
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val count: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

// ─── BackupManager ──────────────────────────────────────────────────────────

class BackupManager(
    private val context: Context,
    private val repository: WatchItemRepository,
    private val userRepository: UserRepository? = null
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToUri(uri: android.net.Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val items = repository.getAllItemsSnapshot()
            val allProgress = repository.getAllEpisodeProgressSnapshot()
            val progressByWatchItemId = allProgress.groupBy { it.watchItemId }

            // Collect episode progress for all series items
            val episodeProgressBackup = items
                .filter { it.imdbId.isNotBlank() }
                .flatMap { item ->
                    progressByWatchItemId[item.id].orEmpty().map { progress ->
                        EpisodeProgressBackup(
                            imdbId        = item.imdbId,
                            season        = progress.season,
                            episodeNumber = progress.episodeNumber,
                            isWatched     = progress.isWatched,
                            watchedAt     = progress.watchedAt
                        )
                    }
                }

            val payload = BackupPayload(
                items           = items.map { it.toBackup() },
                episodeProgress = episodeProgressBackup
            )

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.writer().use { it.write(gson.toJson(payload)) }
            } ?: throw Exception("Could not open output stream")

            BackupResult.Success("Backup saved — ${items.size} items, ${episodeProgressBackup.size} episode records")
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Export failed", e)
            BackupResult.Error(e.message ?: "Export failed")
        }
    }

    /**
     * BUG-03 + BUG-09 fix:
     * 1. Restore is now atomic (replaceAll uses @Transaction).
     * 2. Episode progress is restored by mapping imdbId → new DB id.
     */
    suspend fun importFromJson(json: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val type    = object : TypeToken<BackupPayload>() {}.type
            val payload: BackupPayload = gson.fromJson(json, type)
            val watchItems = payload.items.map { it.toWatchItem() }

            // Atomic replace — returns new DB ids in insertion order
            val newIds: List<Long> = repository.restoreItems(watchItems)

            // Build imdbId → new DB id map for episode progress remapping
            val imdbToNewId: Map<String, Long> = watchItems
                .mapIndexed { index, item -> item.imdbId to newIds[index] }
                .filter { it.first.isNotBlank() }
                .toMap()

            // Restore episode progress with remapped IDs
            if (payload.episodeProgress.isNotEmpty() && imdbToNewId.isNotEmpty()) {
                val progressRows = payload.episodeProgress.mapNotNull { ep ->
                    val newId = imdbToNewId[ep.imdbId] ?: return@mapNotNull null
                    EpisodeProgress(
                        watchItemId   = newId,
                        season        = ep.season,
                        episodeNumber = ep.episodeNumber,
                        isWatched     = ep.isWatched,
                        watchedAt     = ep.watchedAt
                    )
                }
                repository.restoreEpisodeProgress(progressRows)
            }

            RestoreResult.Success(watchItems.size)
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Invalid backup format")
        }
    }

    // ── Converters ─────────────────────────────────────────────────────────

    // ── Cloud Backup (Bug 43) ───────────────────────────────────────────────

    /**
     * Uploads all local watchlist items to Supabase for the given user.
     * Returns the number of items pushed, or throws on failure.
     */
    suspend fun uploadToCloud(userId: String): Int = withContext(Dispatchers.IO) {
        val repo = userRepository ?: error("UserRepository not provided")
        val items = repository.getAllItemsSnapshot()
        repo.syncWatchlist(userId, items)
        items.size
    }

    /**
     * Restores watchlist items from Supabase for the given user.
     * Only inserts items not already present locally (keyed by imdbId or title+year+type).
     * Returns the number of new items inserted.
     */
    @Serializable
    private data class CloudItem(
        val imdb_id: String = "",
        val title: String,
        val year: Int,
        val type: String,
        val is_watched: Boolean,
        val rating: Float,
        val season: Int? = null,
        val episode: Int? = null,
        val notes: String? = null,
        val poster_url: String? = null,
        val genres: String? = null,
        val date_added: Long
    )

    suspend fun restoreFromCloud(userId: String): Int = withContext(Dispatchers.IO) {
        val remoteItems = SupabaseApi.client.from("public_watchlist")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudItem>()

        val localItems = repository.getAllItemsSnapshot()
        val localSet = buildSet<String> {
            localItems.forEach { item ->
                if (item.imdbId.isNotBlank()) add(item.imdbId)
                add("${item.title}_${item.year}_${item.type.name}")
            }
        }

        val newItems = remoteItems.filter { r ->
            val dedupeKey = "${r.title}_${r.year}_${r.type}"
            r.imdb_id.isBlank() || r.imdb_id !in localSet && dedupeKey !in localSet
        }.map { r ->
            WatchItem(
                imdbId    = r.imdb_id,
                title     = r.title,
                year      = r.year,
                type      = runCatching { MediaType.valueOf(r.type) }.getOrDefault(MediaType.MOVIE),
                isWatched = r.is_watched,
                rating    = r.rating,
                season    = r.season,
                episode   = r.episode,
                notes     = r.notes.orEmpty(),
                posterUrl = r.poster_url,
                genres    = r.genres.orEmpty(),
                dateAdded = r.date_added
            )
        }

        if (newItems.isNotEmpty()) {
            repository.restoreItems(newItems)
        }
        newItems.size
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private fun WatchItem.toBackup() = WatchItemBackup(
        title       = title,
        year        = year,
        type        = type.name,
        isWatched   = isWatched,
        rating      = rating,
        season      = season,
        episode     = episode,
        notes       = notes,
        posterUrl   = posterUrl,
        genres      = genres,
        imdbId      = imdbId,
        dateAdded   = dateAdded,
        lastUpdated = lastUpdated
    )

    private fun WatchItemBackup.toWatchItem() = WatchItem(
        title       = title,
        year        = year,
        type        = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.MOVIE),
        isWatched   = isWatched,
        rating      = rating,
        season      = season,
        episode     = episode,
        notes       = notes,
        posterUrl   = posterUrl,
        genres      = genres,
        imdbId      = imdbId,
        dateAdded   = dateAdded,
        lastUpdated = lastUpdated
    )
}
