package com.keepiecounter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {

    @Test
    fun `default session has zero id`() {
        val session = Session(count = 10, durationMs = 5000L)
        assertEquals(0L, session.id)
    }

    @Test
    fun `default session is not personal best`() {
        val session = Session(count = 10, durationMs = 5000L)
        assertFalse(session.isPersonalBest)
    }

    @Test
    fun `session date defaults to current time`() {
        val before = System.currentTimeMillis()
        val session = Session(count = 5, durationMs = 3000L)
        val after = System.currentTimeMillis()
        assertTrue(session.date in before..after)
    }

    @Test
    fun `sessions with same data are equal`() {
        val s1 = Session(id = 1, count = 10, durationMs = 5000L, date = 1000L)
        val s2 = Session(id = 1, count = 10, durationMs = 5000L, date = 1000L)
        assertEquals(s1, s2)
    }

    @Test
    fun `sessions with different counts are not equal`() {
        val s1 = Session(id = 1, count = 10, durationMs = 5000L, date = 1000L)
        val s2 = Session(id = 1, count = 20, durationMs = 5000L, date = 1000L)
        assertNotEquals(s1, s2)
    }

    @Test
    fun `personal best flag can be set`() {
        val session = Session(count = 50, durationMs = 30000L, isPersonalBest = true)
        assertTrue(session.isPersonalBest)
    }
}
