package com.keepiecounter.detection.counter

import com.keepiecounter.detection.pose.KickDetector
import com.keepiecounter.detection.pose.KickDetector.Foot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KeepieCounterTest {

    private lateinit var counter: KeepieCounter
    private var lastCount = 0

    @Before
    fun setup() {
        counter = KeepieCounter()
        lastCount = 0
        counter.onCountChanged = { lastCount = it }
    }

    private fun kick(timestamp: Long, foot: Foot = Foot.LEFT) =
        KickDetector.KickResult(true, foot, 0.8f, timestamp)

    @Test
    fun `counts when ball-up and kick correlate within window`() {
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)
        assertEquals(1, lastCount)
    }

    @Test
    fun `counts when kick comes before ball-up`() {
        counter.onKickDetected(kick(1000L))
        counter.onBallMovingUp(1100L)
        assertEquals(1, counter.count)
    }

    @Test
    fun `does not count ball-up alone`() {
        counter.onBallMovingUp(1000L)
        assertEquals(0, counter.count)
    }

    @Test
    fun `does not count kick alone`() {
        counter.onKickDetected(kick(1000L))
        assertEquals(0, counter.count)
    }

    @Test
    fun `does not count when events too far apart`() {
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1300L)) // 300ms apart, outside 200ms window
        assertEquals(0, counter.count)
    }

    @Test
    fun `counts at exactly correlation window boundary`() {
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1200L)) // exactly 200ms
        assertEquals(1, counter.count)
    }

    @Test
    fun `cooldown prevents double-counting`() {
        // First count
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)

        // Second events within cooldown (< 300ms from first count)
        counter.onBallMovingUp(1200L)
        counter.onKickDetected(kick(1250L))
        assertEquals(1, counter.count) // still 1
    }

    @Test
    fun `counts again after cooldown period`() {
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)

        // After cooldown
        counter.onBallMovingUp(1500L)
        counter.onKickDetected(kick(1600L))
        assertEquals(2, counter.count)
    }

    @Test
    fun `multiple rapid keepie-uppies counted correctly`() {
        // keepie 1
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1050L))
        assertEquals(1, counter.count)

        // keepie 2 (after cooldown)
        counter.onBallMovingUp(1500L)
        counter.onKickDetected(kick(1550L))
        assertEquals(2, counter.count)

        // keepie 3
        counter.onBallMovingUp(2000L)
        counter.onKickDetected(kick(2050L))
        assertEquals(3, counter.count)
    }

    @Test
    fun `old events are pruned after MAX_EVENT_AGE`() {
        counter.onBallMovingUp(1000L) // this ball event
        // 1100ms later — ball event is now > 1000ms old
        counter.onKickDetected(kick(2100L))
        assertEquals(0, counter.count) // pruned, no match
    }

    @Test
    fun `reset clears count and events`() {
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)

        counter.reset()
        assertEquals(0, counter.count)

        // Same events should work again
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)
    }

    @Test
    fun `callback not called without count change`() {
        var callbackCount = 0
        counter.onCountChanged = { callbackCount++ }

        counter.onBallMovingUp(1000L) // no match yet
        assertEquals(0, callbackCount)

        counter.onKickDetected(kick(1100L)) // now matches
        assertEquals(1, callbackCount)
    }

    @Test
    fun `works without callback set`() {
        counter.onCountChanged = null
        counter.onBallMovingUp(1000L)
        counter.onKickDetected(kick(1100L))
        assertEquals(1, counter.count)
    }
}
