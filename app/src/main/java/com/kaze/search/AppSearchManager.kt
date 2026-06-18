package com.kaze.search

import android.content.Context
import android.util.Log
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import androidx.appsearch.platformstorage.PlatformStorage
import com.kaze.model.WatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

/**
 * Manages the AppSearch session for on-device global search.
 *
 * Lifecycle:
 *  - Call [open] once in WatchLaterApp.onCreate()
 *  - Call [close] in WatchLaterApp.onTrimMemory() (TRIM_MEMORY_UI_HIDDEN) to free resources
 *  - All writes auto-sync via WatchItemRepository hooks (save/update/delete)
 *  - On fresh install, [rebuildIndex] is called automatically with the current Room snapshot
 */
object AppSearchManager {

    private const val TAG = "AppSearchManager"
    private const val DATABASE_NAME = "watchlater_appsearch"

    @Volatile
    private var session: AppSearchSession? = null

    suspend fun open(context: Context) = withContext(Dispatchers.IO) {
        if (session != null) return@withContext
        try {
            val sessionFuture = if (android.os.Build.VERSION.SDK_INT >= 31) {
                PlatformStorage.createSearchSessionAsync(
                    PlatformStorage.SearchContext.Builder(context.applicationContext, DATABASE_NAME).build()
                )
            } else {
                LocalStorage.createSearchSessionAsync(
                    LocalStorage.SearchContext.Builder(context.applicationContext, DATABASE_NAME).build()
                )
            }
            val s = sessionFuture.await()

            // Register schema — setForceOverride(false) so existing data is preserved on upgrade
            // setDocumentClassDisplayedBySystem makes it globally visible in launcher
            val schemaRequest = SetSchemaRequest.Builder()
                .addDocumentClasses(WatchItemDocument::class.java)
                .setDocumentClassDisplayedBySystem(WatchItemDocument::class.java, true)
                .setForceOverride(false)
                .build()
            s.setSchemaAsync(schemaRequest).await()

            session = s
            Log.d(TAG, "AppSearch session opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open AppSearch session", e)
        }
    }

    fun close() {
        session?.close()
        session = null
        Log.d(TAG, "AppSearch session closed")
    }

    /** Index or update a single WatchItem. Safe to call on save and update. */
    suspend fun indexItem(item: WatchItem) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val doc = item.toDocument()
            val request = PutDocumentsRequest.Builder().addDocuments(doc).build()
            s.putAsync(request).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to index item: ${item.title}", e)
        }
    }

    /** Remove a single item from the search index. */
    suspend fun removeItem(item: WatchItem) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val request = RemoveByDocumentIdRequest.Builder("watchlater")
                .addIds(item.id.toString())
                .build()
            s.removeAsync(request).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove item: ${item.title}", e)
        }
    }

    /** Rebuild the entire index from a list of items. Called on first launch after install. */
    suspend fun rebuildIndex(items: List<WatchItem>) = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext
        try {
            val docs = items.map { it.toDocument() }
            // Index in batches of 50 to avoid OOM
            docs.chunked(50).forEach { batch ->
                val request = PutDocumentsRequest.Builder().addDocuments(batch).build()
                s.putAsync(request).await()
            }
            Log.d(TAG, "AppSearch index rebuilt with ${items.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebuild index", e)
        }
    }

    private fun WatchItem.toDocument() = WatchItemDocument(
        id        = this.id.toString(),
        title     = this.title,
        year      = this.year.toLong(),
        mediaType = this.type.name,
        isWatched = this.isWatched,
        imdbId    = this.imdbId,
        posterUrl = this.posterUrl ?: ""
    )
}
