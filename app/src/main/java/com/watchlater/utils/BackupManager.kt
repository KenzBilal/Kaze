package com.watchlater.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.watchlater.data.local.EpisodeProgress
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private val repository: WatchItemRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToUri(uri: android.net.Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val items = repository.getAllItemsSnapshot()

            // Collect episode progress for all series items
            val episodeProgressBackup = items
                .filter { it.imdbId.isNotBlank() }
                .flatMap { item ->
                    repository.getEpisodeProgressSnapshot(item.id).map { progress ->
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
