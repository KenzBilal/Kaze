package com.kaze.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an action that failed due to being offline.
 * WorkManager will retry these when connectivity is restored.
 */
@Entity(tableName = "pending_actions")
data class PendingAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // "FOLLOW", "UNFOLLOW", "SYNC_WATCHLIST", "ACTIVITY_FEED"
    val userId: String,
    val targetId: String = "",      // target user id for follows
    val payload: String = "",       // JSON payload for complex actions
    val createdAt: Long = System.currentTimeMillis()
)

object ActionType {
    const val FOLLOW = "FOLLOW"
    const val UNFOLLOW = "UNFOLLOW"
    const val SYNC_WATCHLIST = "SYNC_WATCHLIST"
    const val POST_ACTIVITY = "POST_ACTIVITY"
}
