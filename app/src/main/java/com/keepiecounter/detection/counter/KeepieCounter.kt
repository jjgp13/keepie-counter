package com.keepiecounter.detection.counter

import com.keepiecounter.detection.pose.KickDetector
import kotlin.math.abs

/**
 * Combines ball trajectory and kick detection events to count keepie-uppies.
 *
 * A keepie-uppie is counted when:
 * 1. A ball-moving-up event occurs
 * 2. A kick-detected event occurs within ±CORRELATION_WINDOW_MS
 * 3. At least COOLDOWN_MS has passed since the last count
 *
 * Pure Kotlin — fully JVM-testable.
 */
class KeepieCounter {

    data class BallEvent(val timestamp: Long)
    data class KickEvent(val timestamp: Long, val foot: KickDetector.Foot, val confidence: Float)

    private val ballEvents = ArrayDeque<BallEvent>()
    private val kickEvents = ArrayDeque<KickEvent>()
    private var _count = 0
    private var lastCountTime: Long? = null

    val count: Int get() = _count

    var onCountChanged: ((Int) -> Unit)? = null

    companion object {
        const val CORRELATION_WINDOW_MS = 200L
        const val COOLDOWN_MS = 300L
        const val MAX_EVENT_AGE_MS = 1000L
    }

    fun onBallMovingUp(timestamp: Long) {
        ballEvents.addLast(BallEvent(timestamp))
        pruneOldEvents(timestamp)
        tryCount(timestamp)
    }

    fun onKickDetected(result: KickDetector.KickResult) {
        kickEvents.addLast(KickEvent(result.timestamp, result.foot, result.confidence))
        pruneOldEvents(result.timestamp)
        tryCount(result.timestamp)
    }

    private fun tryCount(now: Long) {
        // Check cooldown
        val elapsed = lastCountTime?.let { now - it }
        if (elapsed != null && elapsed < COOLDOWN_MS) return

        // Look for correlated events
        for (ball in ballEvents) {
            for (kick in kickEvents) {
                val timeDiff = abs(ball.timestamp - kick.timestamp)
                if (timeDiff <= CORRELATION_WINDOW_MS) {
                    _count++
                    lastCountTime = now
                    ballEvents.clear()
                    kickEvents.clear()
                    onCountChanged?.invoke(_count)
                    return
                }
            }
        }
    }

    private fun pruneOldEvents(now: Long) {
        val cutoff = now - MAX_EVENT_AGE_MS
        while (ballEvents.isNotEmpty() && ballEvents.first().timestamp < cutoff) {
            ballEvents.removeFirst()
        }
        while (kickEvents.isNotEmpty() && kickEvents.first().timestamp < cutoff) {
            kickEvents.removeFirst()
        }
    }

    fun reset() {
        _count = 0
        lastCountTime = null
        ballEvents.clear()
        kickEvents.clear()
    }
}
