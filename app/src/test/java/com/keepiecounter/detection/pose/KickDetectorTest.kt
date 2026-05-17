package com.keepiecounter.detection.pose

import com.keepiecounter.detection.pose.KickDetector.Foot
import com.keepiecounter.detection.pose.KickDetector.LandmarkPoint
import com.keepiecounter.detection.pose.KickDetector.FootSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KickDetectorTest {

    private lateinit var detector: KickDetector
    private var lastKick: KickDetector.KickResult? = null

    @Before
    fun setup() {
        detector = KickDetector()
        lastKick = null
        detector.onKickDetected = { result -> lastKick = result }
    }

    // Helper: create a pose frame with ankle at given Y, knee extended
    private fun pose(
        ankleY: Float,
        timestamp: Long,
        kneeAngle: Float = 160f // extended leg
    ): Unit {
        // Position knee and hip to create the desired angle
        // Hip at (100, 100), Knee at (100, 200), Ankle moves
        val hip = LandmarkPoint(100f, 100f)
        val knee = LandmarkPoint(100f, 200f)
        val ankle = LandmarkPoint(100f, ankleY)

        detector.onPoseDetected(
            leftAnkle = ankle, leftKnee = knee, leftHip = hip,
            rightAnkle = LandmarkPoint(200f, 400f), // stationary right foot
            rightKnee = LandmarkPoint(200f, 300f),
            rightHip = LandmarkPoint(200f, 200f),
            timestamp = timestamp
        )
    }

    @Test
    fun `no kick detected with fewer than MIN_FRAMES`() {
        pose(300f, 0L)
        pose(280f, 33L)
        pose(260f, 66L)
        assertNull(lastKick)
    }

    @Test
    fun `detects kick from rapid upward ankle movement`() {
        // Ankle moves up 80px over 4 frames — well above threshold
        pose(400f, 0L)
        pose(380f, 33L)
        pose(350f, 66L)
        pose(320f, 100L)
        assertTrue("Kick should be detected", lastKick?.detected == true)
        assertEquals(Foot.LEFT, lastKick?.foot)
    }

    @Test
    fun `does not detect kick from slow movement`() {
        // Ankle moves up only 4px — below velocity threshold
        pose(300f, 0L)
        pose(299f, 33L)
        pose(298f, 66L)
        pose(296f, 100L)
        assertNull("Slow movement should not trigger kick", lastKick)
    }

    @Test
    fun `does not detect kick from downward movement`() {
        // Ankle moves DOWN
        pose(300f, 0L)
        pose(320f, 33L)
        pose(340f, 66L)
        pose(360f, 100L)
        assertNull("Downward movement should not trigger kick", lastKick)
    }

    @Test
    fun `debounce prevents rapid-fire kicks`() {
        // First kick
        pose(400f, 0L)
        pose(380f, 33L)
        pose(350f, 66L)
        pose(320f, 100L)
        assertEquals(100L, lastKick?.timestamp)

        // Second kick too soon (within 200ms)
        lastKick = null
        pose(400f, 150L)
        pose(380f, 183L)
        pose(350f, 216L)
        pose(320f, 250L)
        assertNull("Should debounce rapid kicks", lastKick)
    }

    @Test
    fun `allows kick after debounce period`() {
        // First kick
        pose(400f, 0L)
        pose(380f, 33L)
        pose(350f, 66L)
        pose(320f, 100L)
        assertEquals(100L, lastKick?.timestamp)

        // Reset foot to stationary, then kick again after 200ms
        lastKick = null
        pose(400f, 200L)
        pose(400f, 250L)
        pose(400f, 300L)
        pose(400f, 350L)
        pose(380f, 400L)
        pose(350f, 433L)
        pose(320f, 466L)
        assertTrue("Should allow kick after debounce", lastKick != null)
    }

    @Test
    fun `reset clears state`() {
        pose(400f, 0L)
        pose(380f, 33L)
        pose(350f, 66L)
        pose(320f, 100L)
        assertTrue(lastKick != null)

        detector.reset()
        lastKick = null

        // Same motion should trigger again after reset
        pose(400f, 0L)
        pose(380f, 33L)
        pose(350f, 66L)
        pose(320f, 100L)
        assertTrue("Should detect kick after reset", lastKick != null)
    }

    @Test
    fun `calculateAngle returns 180 for straight line`() {
        val a = LandmarkPoint(0f, 0f)
        val b = LandmarkPoint(0f, 100f)
        val c = LandmarkPoint(0f, 200f)
        val angle = KickDetectorMath.calculateAngle(a, b, c)
        assertEquals(180f, angle, 0.1f)
    }

    @Test
    fun `calculateAngle returns 90 for right angle`() {
        val a = LandmarkPoint(0f, 0f)
        val b = LandmarkPoint(0f, 100f)
        val c = LandmarkPoint(100f, 100f)
        val angle = KickDetectorMath.calculateAngle(a, b, c)
        assertEquals(90f, angle, 0.1f)
    }

    @Test
    fun `calculateAngle handles zero-length vector`() {
        val a = LandmarkPoint(100f, 100f)
        val b = LandmarkPoint(100f, 100f) // same as a
        val c = LandmarkPoint(200f, 200f)
        val angle = KickDetectorMath.calculateAngle(a, b, c)
        assertEquals(0f, angle, 0.1f)
    }

    @Test
    fun `kick confidence increases with faster movement`() {
        val slow = mutableListOf<FootSnapshot>()
        val fast = mutableListOf<FootSnapshot>()
        val hip = LandmarkPoint(100f, 100f)
        val knee = LandmarkPoint(100f, 200f)

        // Slow: 20px over 4 frames
        for (i in 0..3) {
            slow.add(FootSnapshot(LandmarkPoint(100f, 350f - i * 5f), knee, hip, i * 33L))
        }
        // Fast: 80px over 4 frames
        for (i in 0..3) {
            fast.add(FootSnapshot(LandmarkPoint(100f, 400f - i * 20f), knee, hip, i * 33L))
        }

        val slowScore = detector.computeKickScore(slow)
        val fastScore = detector.computeKickScore(fast)
        assertTrue("Fast kick should score higher than slow", fastScore > slowScore)
    }
}
