package com.keepiecounter.detection.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Detects kick motions from pose landmark data.
 *
 * Analyzes ankle velocity and knee extension angle over a sliding window
 * to identify keepie-uppie kicks. Pure Kotlin — fully JVM-testable.
 *
 * A kick is detected when:
 * 1. Ankle moves upward rapidly (velocity exceeds threshold)
 * 2. Knee angle is sufficiently extended (leg straightening)
 * Both signals are combined into a confidence score.
 */
class KickDetector {

    data class LandmarkPoint(val x: Float, val y: Float)

    data class FootSnapshot(
        val ankle: LandmarkPoint,
        val knee: LandmarkPoint,
        val hip: LandmarkPoint,
        val timestamp: Long
    )

    enum class Foot { LEFT, RIGHT }

    data class KickResult(
        val detected: Boolean,
        val foot: Foot,
        val confidence: Float,
        val timestamp: Long
    )

    private val leftHistory = ArrayDeque<FootSnapshot>()
    private val rightHistory = ArrayDeque<FootSnapshot>()
    private var lastKickTime: Long? = null

    var onKickDetected: ((KickResult) -> Unit)? = null

    companion object {
        const val MAX_HISTORY = 10
        const val MIN_FRAMES_FOR_KICK = 4
        const val VELOCITY_THRESHOLD = 12f
        const val KICK_CONFIDENCE_THRESHOLD = 0.45f
        const val MIN_KICK_INTERVAL_MS = 200L
        const val VELOCITY_NORMALIZER = 50f
        const val MIN_KNEE_ANGLE = 100f
        const val KNEE_ANGLE_RANGE = 80f
    }

    fun onPoseDetected(
        leftAnkle: LandmarkPoint, leftKnee: LandmarkPoint, leftHip: LandmarkPoint,
        rightAnkle: LandmarkPoint, rightKnee: LandmarkPoint, rightHip: LandmarkPoint,
        timestamp: Long
    ) {
        leftHistory.addLast(FootSnapshot(leftAnkle, leftKnee, leftHip, timestamp))
        rightHistory.addLast(FootSnapshot(rightAnkle, rightKnee, rightHip, timestamp))
        while (leftHistory.size > MAX_HISTORY) leftHistory.removeFirst()
        while (rightHistory.size > MAX_HISTORY) rightHistory.removeFirst()

        val leftScore = computeKickScore(leftHistory)
        val rightScore = computeKickScore(rightHistory)

        val bestScore = maxOf(leftScore, rightScore)
        val bestFoot = if (leftScore >= rightScore) Foot.LEFT else Foot.RIGHT

        if (bestScore >= KICK_CONFIDENCE_THRESHOLD) {
            val elapsed = lastKickTime?.let { timestamp - it }
            if (elapsed == null || elapsed > MIN_KICK_INTERVAL_MS) {
                lastKickTime = timestamp
                val result = KickResult(true, bestFoot, bestScore, timestamp)
                onKickDetected?.invoke(result)
            }
        }
    }

    fun computeKickScore(history: List<FootSnapshot>): Float {
        if (history.size < MIN_FRAMES_FOR_KICK) return 0f

        val recent = history.takeLast(MIN_FRAMES_FOR_KICK)
        val oldest = recent.first()
        val newest = recent.last()

        // Velocity: positive = ankle moving UP in image space (Y decreases)
        val velocityY = oldest.ankle.y - newest.ankle.y
        if (velocityY < VELOCITY_THRESHOLD) return 0f

        // Knee angle at the newest frame
        val kneeAngle = KickDetectorMath.calculateAngle(newest.hip, newest.knee, newest.ankle)

        val velocityScore = (velocityY / VELOCITY_NORMALIZER).coerceIn(0f, 1f)
        val angleScore = ((kneeAngle - MIN_KNEE_ANGLE) / KNEE_ANGLE_RANGE).coerceIn(0f, 1f)

        return velocityScore * 0.6f + angleScore * 0.4f
    }

    fun reset() {
        leftHistory.clear()
        rightHistory.clear()
        lastKickTime = null
    }
}

object KickDetectorMath {
    fun calculateAngle(
        a: KickDetector.LandmarkPoint,
        b: KickDetector.LandmarkPoint,
        c: KickDetector.LandmarkPoint
    ): Float {
        val abX = a.x - b.x; val abY = a.y - b.y
        val cbX = c.x - b.x; val cbY = c.y - b.y
        val dot = abX * cbX + abY * cbY
        val magAB = sqrt(abX * abX + abY * abY)
        val magCB = sqrt(cbX * cbX + cbY * cbY)
        if (magAB == 0f || magCB == 0f) return 0f
        val cosAngle = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }
}
