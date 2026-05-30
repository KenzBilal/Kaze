package com.watchlater.utils

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.watchlater.data.repository.WatchItemRepository
import com.watchlater.model.MediaType
import com.watchlater.model.WatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<WatchItemBackup>
)

data class WatchItemBackup(
    val title: String,
    val year: Int,
    val type: String,
    val isWatched: Boolean,
    val rating: Float,
    val season: Int?,
    val episode: Int?,
    val notes: String,
    val dateAdded: Long,
    val lastUpdated: Long
)

sealed class BackupResult {
    data class Success(val filePath: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val count: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

class BackupManager(
    private val context: Context,
    private val repository: WatchItemRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToUri(uri: android.net.Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val items = repository.getAllItemsSnapshot()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = com.google.gson.stream.JsonWriter(stream.writer())
                writer.setIndent("  ")
                writer.beginObject()
                writer.name("version").value(1)
                writer.name("exportedAt").value(System.currentTimeMillis())
                writer.name("items")
                writer.beginArray()

                for (item in items) {
                    gson.toJson(item.toBackup(), WatchItemBackup::class.java, writer)
                }

                writer.endArray()
                writer.endObject()
                writer.flush()
            } ?: throw Exception("Could not open output stream")

            BackupResult.Success("Backup saved successfully")
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Export failed", e)
            BackupResult.Error(e.message ?: "Export failed")
        }
    }

    suspend fun importFromJson(json: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<BackupPayload>() {}.type
            val payload: BackupPayload = gson.fromJson(json, type)
            val items = payload.items.map { it.toWatchItem() }
            repository.restoreItems(items)
            RestoreResult.Success(items.size)
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Invalid backup format")
        }
    }

    private fun WatchItem.toBackup() = WatchItemBackup(
        title = title, year = year, type = type.name,
        isWatched = isWatched, rating = rating,
        season = season, episode = episode, notes = notes,
        dateAdded = dateAdded, lastUpdated = lastUpdated
    )

    private fun WatchItemBackup.toWatchItem() = WatchItem(
        title = title, year = year,
        type = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.MOVIE),
        isWatched = isWatched, rating = rating,
        season = season, episode = episode, notes = notes,
        dateAdded = dateAdded, lastUpdated = lastUpdated
    )
}
