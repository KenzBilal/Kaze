package com.kaze.data.local

import androidx.room.*

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(action: PendingAction)

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingAction>

    @Delete
    suspend fun delete(action: PendingAction)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM pending_actions")
    suspend fun count(): Int
}
