package com.kaze.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// ── Entity ────────────────────────────────────────────────────────────────────

@Entity(tableName = "arc_item_progress", primaryKeys = ["arcItemId", "userId"])
data class ArcItemProgress(
    val arcItemId: String,
    val userId: String,
    val isMarked: Boolean = false,
    val markedAt: Long = System.currentTimeMillis()
)

// ── DAO ───────────────────────────────────────────────────────────────────────

@Dao
interface ArcProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ArcItemProgress)

    @Query("SELECT * FROM arc_item_progress WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<ArcItemProgress>

    @Query("SELECT * FROM arc_item_progress WHERE userId = :userId AND arcItemId IN (:arcItemIds)")
    suspend fun getForArc(userId: String, arcItemIds: List<String>): List<ArcItemProgress>

    @Query("DELETE FROM arc_item_progress WHERE userId = :userId AND arcItemId = :arcItemId")
    suspend fun delete(userId: String, arcItemId: String)
}
