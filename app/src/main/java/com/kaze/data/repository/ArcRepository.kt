package com.kaze.data.repository

import android.content.Context
import com.kaze.data.local.ArcItemProgress
import com.kaze.data.local.WatchLaterDatabase
import com.kaze.data.remote.SupabaseApi
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// ── Remote models ─────────────────────────────────────────────────────────────

@Serializable
data class Arc(
    val id: String,
    val name: String,
    val description: String = "",
    val aliases: String = "",           // comma-separated aliases for search
    val cover_url: String? = null,
    val is_published: Boolean = false,
    val owner_id: String? = null
)

@Serializable
data class ArcShare(
    val id: String = "",
    val arc_id: String,
    val sender_id: String,
    val receiver_id: String,
    val status: String = "pending",
    val created_at: String = "",
    // Denormalized display fields (populated locally when possible)
    val arc_name: String = "",
    val sender_username: String = ""
)

@Serializable
data class ArcItem(
    val id: String = "",
    val arc_id: String = "",
    val order_index: Double = 0.0,
    val imdb_id: String = "",
    val title: String = "",
    val year: Int = 0,
    val type: String,         // "MOVIE" or "SERIES" (no default to ensure it serializes)
    val poster_url: String? = null,
    val total_seasons: Int? = null,
    val start_season: Int? = null,
    val start_episode: Int? = null,
    val end_season: Int? = null,
    val end_episode: Int? = null,
    val phase_label: String? = null,
    val notes: String? = null,
    val is_optional: Boolean = false
)

// ── UI model ──────────────────────────────────────────────────────────────────

enum class ArcRowState { NOT_IN_WATCHLIST, IN_WATCHLIST, WATCHED, MANUALLY_MARKED }

data class ArcItemUiState(
    val arcItem: ArcItem,
    val rowState: ArcRowState
)

// ── Repository ────────────────────────────────────────────────────────────────

class ArcRepository(private val context: Context) {

    private val db by lazy { WatchLaterDatabase.getInstance(context) }
    private val arcProgressDao by lazy { db.arcProgressDao() }

    // ── Cache ─────────────────────────────────────────────────────────────────

    private var cachedArcs: List<Arc> = emptyList()
    private var arcItemsCache: MutableMap<String, List<ArcItem>> = mutableMapOf()
    private var lastFetchTime: Long = 0L
    private val cacheTtlMs = 24L * 60 * 60 * 1000  // 24 hours

    private fun isCacheValid() = System.currentTimeMillis() - lastFetchTime < cacheTtlMs

    // ── User-facing ───────────────────────────────────────────────────────────

    suspend fun getPublishedArcs(forceRefresh: Boolean = false): List<Arc> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && isCacheValid() && cachedArcs.isNotEmpty()) return@withContext cachedArcs
            try {
                val userId = UserRepository(context).getLocalUserId()
                val publishedArcs = SupabaseApi.client.from("arcs")
                    .select { filter { eq("is_published", true) } }
                    .decodeList<Arc>()
                val personalArcs = if (userId != null) {
                    SupabaseApi.client.from("arcs")
                        .select { filter { eq("owner_id", userId) } }
                        .decodeList<Arc>()
                } else emptyList()
                val arcs = (publishedArcs + personalArcs).distinctBy { it.id }
                cachedArcs = arcs
                lastFetchTime = System.currentTimeMillis()
                arcs
            } catch (e: Exception) {
                e.printStackTrace()
                cachedArcs  // return stale cache on failure
            }
        }

    suspend fun getArcWithItems(arcId: String, forceRefresh: Boolean = false): Pair<Arc?, List<ArcItem>> =
        withContext(Dispatchers.IO) {
            val cachedItems = arcItemsCache[arcId]
            if (!forceRefresh && isCacheValid() && cachedItems != null) {
                return@withContext Pair(cachedArcs.firstOrNull { it.id == arcId }, cachedItems)
            }
            try {
                val arc = SupabaseApi.client.from("arcs")
                    .select { filter { eq("id", arcId) } }
                    .decodeSingleOrNull<Arc>()
                val items = SupabaseApi.client.from("arc_items")
                    .select { filter { eq("arc_id", arcId) }; order("order_index", Order.ASCENDING) }
                    .decodeList<ArcItem>()
                arcItemsCache[arcId] = items
                Pair(arc, items)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(null, emptyList())
            }
        }

    /** Fuzzy search: matches arc name, aliases, and any item title inside the arc */
    fun searchArcs(
        query: String,
        allArcs: List<Arc>,
        allItems: Map<String, List<ArcItem>>
    ): List<Arc> {
        if (query.isBlank()) return allArcs
        val q = query.trim().lowercase()
        return allArcs.filter { arc ->
            arc.name.lowercase().contains(q) ||
            arc.aliases.lowercase().split(",").any { it.trim().contains(q) } ||
            allItems[arc.id]?.any { item -> item.title.lowercase().contains(q) } == true
        }
    }

    // ── User progress (local Room) ────────────────────────────────────────────

    suspend fun markArcItem(arcItemId: String, userId: String, marked: Boolean) {
        if (marked) {
            arcProgressDao.upsert(ArcItemProgress(arcItemId = arcItemId, userId = userId, isMarked = true))
        } else {
            arcProgressDao.delete(userId, arcItemId)
        }
    }

    suspend fun getProgressForArc(arcItemIds: List<String>, userId: String): Map<String, Boolean> {
        if (arcItemIds.isEmpty() || userId.isBlank()) return emptyMap()
        return arcProgressDao.getForArc(userId, arcItemIds)
            .associate { it.arcItemId to it.isMarked }
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    suspend fun getAllArcsAdmin(): List<Arc> = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arcs").select().decodeList<Arc>()
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun createArc(arc: Arc): Arc? = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arcs").insert(arc)
            invalidateCache()
            arc
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun updateArc(arc: Arc) = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arcs").update(arc) { filter { eq("id", arc.id) } }
            invalidateCache()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteArc(arcId: String) = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arcs").delete { filter { eq("id", arcId) } }
            invalidateCache()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun publishArc(arcId: String, publish: Boolean) = withContext(Dispatchers.IO) {
        try {
            @Serializable data class PublishUpdate(val is_published: Boolean)
            SupabaseApi.client.from("arcs").update(PublishUpdate(publish)) {
                filter { eq("id", arcId) }
            }
            invalidateCache()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun getArcItemsAdmin(arcId: String): List<ArcItem> = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arc_items")
                .select { filter { eq("arc_id", arcId) }; order("order_index", Order.ASCENDING) }
                .decodeList<ArcItem>()
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun addArcItem(item: ArcItem): ArcItem? = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arc_items").insert(item)
            arcItemsCache.remove(item.arc_id)
            item
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun updateArcItem(item: ArcItem) = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arc_items").update(item) { filter { eq("id", item.id) } }
            arcItemsCache.remove(item.arc_id)
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteArcItem(itemId: String, arcId: String) = withContext(Dispatchers.IO) {
        try {
            SupabaseApi.client.from("arc_items").delete { filter { eq("id", itemId) } }
            arcItemsCache.remove(arcId)
        } catch (e: Exception) { e.printStackTrace() }
    }

    @Serializable
    private data class OrderUpdate(val id: String, val order_index: Double)

    suspend fun reorderItems(updates: List<Pair<String, Double>>, arcId: String) =
        withContext(Dispatchers.IO) {
            updates.forEach { (id, idx) ->
                try {
                    @Serializable data class OUpdate(val order_index: Double)
                    SupabaseApi.client.from("arc_items")
                        .update(OUpdate(idx)) { filter { eq("id", id) } }
                } catch (e: Exception) { e.printStackTrace() }
            }
            arcItemsCache.remove(arcId)
        }

    /** Returns the last arc_item with this imdbId in the arc — for resume logic */
    suspend fun getLastOccurrence(arcId: String, imdbId: String): ArcItem? =
        withContext(Dispatchers.IO) {
            try {
                SupabaseApi.client.from("arc_items")
                    .select {
                        filter { eq("arc_id", arcId); eq("imdb_id", imdbId) }
                        order("order_index", Order.DESCENDING)
                        limit(1)
                    }
                    .decodeList<ArcItem>()
                    .firstOrNull()
            } catch (e: Exception) { e.printStackTrace(); null }
        }

    /** Returns the next available order_index for a new item at the end */
    suspend fun getNextOrderIndex(arcId: String): Double = withContext(Dispatchers.IO) {
        try {
            val last = SupabaseApi.client.from("arc_items")
                .select {
                    filter { eq("arc_id", arcId) }
                    order("order_index", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<ArcItem>()
                .firstOrNull()
            (last?.order_index ?: 0.0) + 1000.0
        } catch (e: Exception) { 1000.0 }
    }

    private fun invalidateCache() {
        lastFetchTime = 0L
        cachedArcs = emptyList()
        arcItemsCache.clear()
    }

    // ── Personal Arcs & Sharing ───────────────────────────────────────────────

    suspend fun shareArc(arcId: String, senderId: String, receiverId: String) = withContext(Dispatchers.IO) {
        try {
            val share = ArcShare(
                arc_id = arcId,
                sender_id = senderId,
                receiver_id = receiverId
            )
            SupabaseApi.client.from("arc_shares").insert(share)
            
            val arc = getArcWithItems(arcId, forceRefresh = true).first
            if (arc != null) {
                val activityRepo = ActivityRepository(context)
                activityRepo.postActivity(
                    ActivityFeedEntry(
                        user_id = senderId,
                        action_type = "shared_arc",
                        item_title = arc.name,
                        target_user_id = receiverId
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun getPendingShares(userId: String): List<ArcShare> = withContext(Dispatchers.IO) {
        try {
            val shares = SupabaseApi.client.from("arc_shares")
                .select { filter { eq("receiver_id", userId); eq("status", "pending") } }
                .decodeList<ArcShare>()
            // Enrich with arc name and sender username for display
            val userRepo = UserRepository(context)
            shares.map { share ->
                val arcName = try {
                    SupabaseApi.client.from("arcs")
                        .select { filter { eq("id", share.arc_id) } }
                        .decodeSingleOrNull<Arc>()?.name ?: ""
                } catch (e: Exception) { "" }
                val senderName = try {
                    userRepo.getUsersByIds(listOf(share.sender_id)).firstOrNull()?.username ?: ""
                } catch (e: Exception) { "" }
                share.copy(arc_name = arcName, sender_username = senderName)
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun acceptShare(share: ArcShare) = withContext(Dispatchers.IO) {
        try {
            val (originalArc, originalItems) = getArcWithItems(share.arc_id, forceRefresh = true)
            if (originalArc == null) return@withContext

            val newArcId = java.util.UUID.randomUUID().toString()
            val clonedArc = originalArc.copy(
                id = newArcId,
                owner_id = share.receiver_id,
                is_published = false
            )
            SupabaseApi.client.from("arcs").insert(clonedArc)

            val clonedItems = originalItems.map { it.copy(id = java.util.UUID.randomUUID().toString(), arc_id = newArcId) }
            if (clonedItems.isNotEmpty()) {
                SupabaseApi.client.from("arc_items").insert(clonedItems)
            }

            @Serializable data class StatusUpdate(val status: String)
            SupabaseApi.client.from("arc_shares").update(StatusUpdate("accepted")) {
                filter { eq("id", share.id) }
            }
            invalidateCache()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun rejectShare(shareId: String) = withContext(Dispatchers.IO) {
        try {
            @Serializable data class StatusUpdate(val status: String)
            SupabaseApi.client.from("arc_shares").update(StatusUpdate("rejected")) {
                filter { eq("id", shareId) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
