package com.keepiecounter.detection.ball

/**
 * Tracks ball position across frames and detects upward trajectory.
 *
 * Uses a sliding window of recent ball positions to determine if the ball
 * is moving upward (indicating a keepie-uppie). In image coordinates,
 * Y decreases as the ball moves up.
 *
 * This class is pure Kotlin with no Android dependencies,
 * making it fully testable on JVM.
 */
class BallTracker {

    data class BallPosition(
        val centerX: Float,
        val centerY: Float,
        val timestamp: Long
    )

    enum class BallDirection { UP, DOWN, STATIONARY, UNKNOWN }

    private val history = ArrayDeque<BallPosition>()
    private var lastDirection = BallDirection.UNKNOWN
    private var lastPeakTime: Long? = null

    var onBallMovingUp: ((timestamp: Long) -> Unit)? = null

    companion object {
        const val MAX_HISTORY = 15
        const val MIN_FRAMES_FOR_DIRECTION = 3
        const val UPWARD_THRESHOLD = -15f
        const val DOWNWARD_THRESHOLD = 10f
        const val MIN_PEAK_INTERVAL_MS = 250L
    }

    fun onBallDetected(centerX: Float, centerY: Float, timestamp: Long) {
        val position = BallPosition(centerX, centerY, timestamp)

        history.addLast(position)
        while (history.size > MAX_HISTORY) {
            history.removeFirst()
        }

        val direction = computeDirection()

        // Detect upward movement transition
        if (direction == BallDirection.UP && lastDirection != BallDirection.UP) {
            val elapsed = lastPeakTime?.let { timestamp - it }
            if (elapsed == null || elapsed > MIN_PEAK_INTERVAL_MS) {
                lastPeakTime = timestamp
                onBallMovingUp?.invoke(timestamp)
            }
        }

        lastDirection = direction
    }

    fun computeDirection(): BallDirection {
        if (history.size < MIN_FRAMES_FOR_DIRECTION) return BallDirection.UNKNOWN

        val recent = history.toList().takeLast(MIN_FRAMES_FOR_DIRECTION)
        val dy = recent.last().centerY - recent.first().centerY

        return when {
            dy < UPWARD_THRESHOLD -> BallDirection.UP
            dy > DOWNWARD_THRESHOLD -> BallDirection.DOWN
            else -> BallDirection.STATIONARY
        }
    }

    fun reset() {
        history.clear()
        lastDirection = BallDirection.UNKNOWN
        lastPeakTime = null
    }

    fun getHistory(): List<BallPosition> = history.toList()
}
