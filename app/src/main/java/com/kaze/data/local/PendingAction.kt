package com.kaze.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an action that failed due to being offline.
 * WorkManager will retry these when connectivity is restored.
 */
@Entity(
    tableName = "pending_actions",
    indices = [
        androidx.room.Index(value = ["actionType", "userId", "targetId", "payload"], unique = true)
    ]
)
data class PendingAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: ActionType,
    val userId: String,
    val targetId: String = "",      // target user id for follows
    val payload: String = "",       // JSON payload for complex actions
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActionType { 
    FOLLOW, 
    UNFOLLOW, 
    SYNC_WATCHLIST, 
    POST_ACTIVITY,
    DELETE_WATCHLIST,
    SYNC_EPISODE_PROGRESS,
    UPDATE_PROFILE
}
