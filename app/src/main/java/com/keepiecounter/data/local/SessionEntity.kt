package com.keepiecounter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val count: Int,
    val durationMs: Long,
    val date: Long,
    val isPersonalBest: Boolean = false
)
