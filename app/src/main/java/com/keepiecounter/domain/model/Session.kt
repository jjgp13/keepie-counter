package com.keepiecounter.domain.model

data class Session(
    val id: Long = 0,
    val count: Int,
    val durationMs: Long,
    val date: Long = System.currentTimeMillis(),
    val isPersonalBest: Boolean = false
)
