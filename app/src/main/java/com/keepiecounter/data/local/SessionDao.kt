package com.keepiecounter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT MAX(count) FROM sessions")
    suspend fun getPersonalBest(): Int?

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
}
