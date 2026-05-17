package com.keepiecounter.data.repository

import com.keepiecounter.data.local.SessionDao
import com.keepiecounter.data.local.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun saveSession(count: Int, durationMs: Long) {
        val currentBest = sessionDao.getPersonalBest() ?: 0
        val isNewBest = count > currentBest

        sessionDao.insert(
            SessionEntity(
                count = count,
                durationMs = durationMs,
                date = System.currentTimeMillis(),
                isPersonalBest = isNewBest
            )
        )
    }

    suspend fun getPersonalBest(): Int = sessionDao.getPersonalBest() ?: 0
}
