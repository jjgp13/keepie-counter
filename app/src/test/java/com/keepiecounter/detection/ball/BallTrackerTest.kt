package com.keepiecounter.detection.ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BallTrackerTest {

    private lateinit var tracker: BallTracker
    private var lastBallUpTimestamp: Long? = null

    @Before
    fun setup() {
        tracker = BallTracker()
        lastBallUpTimestamp = null
        tracker.onBallMovingUp = { timestamp -> lastBallUpTimestamp = timestamp }
    }

    @Test
    fun `direction is UNKNOWN with fewer than 3 frames`() {
        tracker.onBallDetected(100f, 200f, 0L)
        tracker.onBallDetected(100f, 190f, 33L)
        assertEquals(BallTracker.BallDirection.UNKNOWN, tracker.computeDirection())
    }

    @Test
    fun `detects upward movement from decreasing Y coordinates`() {
        tracker.onBallDetected(100f, 300f, 0L)
        tracker.onBallDetected(100f, 280f, 33L)
        tracker.onBallDetected(100f, 260f, 66L)
        assertEquals(BallTracker.BallDirection.UP, tracker.computeDirection())
    }

    @Test
    fun `detects downward movement from increasing Y coordinates`() {
        tracker.onBallDetected(100f, 200f, 0L)
        tracker.onBallDetected(100f, 220f, 33L)
        tracker.onBallDetected(100f, 240f, 66L)
        assertEquals(BallTracker.BallDirection.DOWN, tracker.computeDirection())
    }

    @Test
    fun `stationary when Y barely changes`() {
        tracker.onBallDetected(100f, 200f, 0L)
        tracker.onBallDetected(100f, 202f, 33L)
        tracker.onBallDetected(100f, 198f, 66L)
        assertEquals(BallTracker.BallDirection.STATIONARY, tracker.computeDirection())
    }

    @Test
    fun `onBallMovingUp callback fires on upward transition`() {
        // Start with downward
        tracker.onBallDetected(100f, 200f, 0L)
        tracker.onBallDetected(100f, 220f, 33L)
        tracker.onBallDetected(100f, 240f, 66L)
        assertNull(lastBallUpTimestamp)

        // Now move up — last-3 window becomes UP when dy < -15
        tracker.onBallDetected(100f, 220f, 400L)
        tracker.onBallDetected(100f, 200f, 433L)
        // At this point last 3 are: (240,66), (220,400), (200,433) → dy = 200-240 = -40 → UP
        assertEquals(433L, lastBallUpTimestamp)
    }

    @Test
    fun `does not double-fire within MIN_PEAK_INTERVAL`() {
        // First upward event — need transition from non-UP to UP
        // Start with UNKNOWN (fresh tracker, first 3 frames go up)
        tracker.onBallDetected(100f, 300f, 0L)
        tracker.onBallDetected(100f, 280f, 33L)
        tracker.onBallDetected(100f, 260f, 66L)
        // First 3 frames: direction becomes UP, transition from UNKNOWN->UP fires
        assertEquals(66L, lastBallUpTimestamp)

        // Go down briefly
        lastBallUpTimestamp = null
        tracker.onBallDetected(100f, 280f, 100L)
        tracker.onBallDetected(100f, 300f, 133L)
        tracker.onBallDetected(100f, 320f, 166L)

        // Now up again but within 250ms of last peak (66ms + 250ms = 316ms)
        tracker.onBallDetected(100f, 300f, 200L)
        tracker.onBallDetected(100f, 280f, 233L)
        tracker.onBallDetected(100f, 260f, 266L)
        assertNull("Should not fire within MIN_PEAK_INTERVAL", lastBallUpTimestamp)
    }

    @Test
    fun `fires again after MIN_PEAK_INTERVAL elapses`() {
        // First upward event at t=66
        tracker.onBallDetected(100f, 300f, 0L)
        tracker.onBallDetected(100f, 280f, 33L)
        tracker.onBallDetected(100f, 260f, 66L)
        assertEquals(66L, lastBallUpTimestamp)

        // Go down
        lastBallUpTimestamp = null
        tracker.onBallDetected(100f, 280f, 200L)
        tracker.onBallDetected(100f, 300f, 300L)
        tracker.onBallDetected(100f, 320f, 400L)

        // Now up again well after 250ms from first peak
        // last-3 window becomes UP at the 2nd up-frame
        // (320,400), (300,500), (280,533) → dy = 280-320 = -40 → UP fires at t=533
        tracker.onBallDetected(100f, 300f, 500L)
        tracker.onBallDetected(100f, 280f, 533L)
        assertEquals(533L, lastBallUpTimestamp)
    }

    @Test
    fun `reset clears history and state`() {
        tracker.onBallDetected(100f, 300f, 0L)
        tracker.onBallDetected(100f, 280f, 33L)
        tracker.onBallDetected(100f, 260f, 66L)

        tracker.reset()

        assertTrue(tracker.getHistory().isEmpty())
        assertEquals(BallTracker.BallDirection.UNKNOWN, tracker.computeDirection())
    }

    @Test
    fun `history is capped at MAX_HISTORY`() {
        repeat(20) { i ->
            tracker.onBallDetected(100f, 300f - i, i.toLong() * 33)
        }
        assertEquals(BallTracker.MAX_HISTORY, tracker.getHistory().size)
    }

    @Test
    fun `no callback fires without listener set`() {
        tracker.onBallMovingUp = null
        tracker.onBallDetected(100f, 300f, 0L)
        tracker.onBallDetected(100f, 280f, 33L)
        tracker.onBallDetected(100f, 260f, 66L)
        // No exception thrown
    }
}

