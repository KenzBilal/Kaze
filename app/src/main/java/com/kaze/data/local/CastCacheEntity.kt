package com.kaze.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "cast_cache",
    primaryKeys = ["imdbId", "actorName"],
    indices = [Index(value = ["imdbId"])]
)
data class CastCacheEntity(
    val imdbId: String,
    val actorName: String,
    val characterName: String = "",
    val imageUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface CastCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cast: List<CastCacheEntity>)

    @Query("SELECT * FROM cast_cache WHERE imdbId = :imdbId ORDER BY actorName ASC")
    suspend fun getByImdbId(imdbId: String): List<CastCacheEntity>

    @Query("SELECT COUNT(*) FROM cast_cache WHERE imdbId = :imdbId")
    suspend fun countByImdbId(imdbId: String): Int
}
