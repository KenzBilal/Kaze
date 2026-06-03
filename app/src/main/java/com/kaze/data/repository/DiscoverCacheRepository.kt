package com.kaze.data.repository

import android.util.Log
import com.kaze.data.remote.DiscoverItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class DiscoverCacheDto(
    val imdb_id: String,
    val title: String,
    val year: Int? = null,
    val type: String? = null,
    val poster_url: String? = null,
    val rating: Float? = null,
    val notes: String? = null,
    val genres: String? = null,
    val created_at: String? = null
)

class DiscoverCacheRepository(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getCachedItems(imdbIds: List<String>): Map<String, DiscoverItem> {
        if (imdbIds.isEmpty()) return emptyMap()
        
        return try {
            val response = supabaseClient.postgrest["discover_cache"]
                .select(columns = Columns.ALL) {
                    filter {
                        isIn("imdb_id", imdbIds)
                    }
                }.decodeList<DiscoverCacheDto>()
                
            response.associate { dto ->
                dto.imdb_id to DiscoverItem(
                    imdbId = dto.imdb_id,
                    title = dto.title,
                    year = dto.year ?: 0,
                    type = dto.type ?: "MOVIE",
                    posterUrl = dto.poster_url,
                    rating = dto.rating ?: 0f,
                    notes = dto.notes ?: "",
                    genres = dto.genres ?: ""
                )
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("DiscoverCache", "Failed to fetch from cache", e)
            emptyMap()
        }
    }

    suspend fun cacheItem(item: DiscoverItem) {
        try {
            val dto = DiscoverCacheDto(
                imdb_id = item.imdbId,
                title = item.title,
                year = item.year,
                type = item.type,
                poster_url = item.posterUrl,
                rating = item.rating,
                notes = item.notes,
                genres = item.genres
            )
            // Upsert the item into cache
            supabaseClient.postgrest["discover_cache"].upsert(dto)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.e("DiscoverCache", "Failed to cache item ${item.imdbId}", e)
        }
    }
}
