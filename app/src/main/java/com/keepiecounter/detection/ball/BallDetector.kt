package com.keepiecounter.detection.ball

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.keepiecounter.detection.CameraFrame
import com.keepiecounter.detection.FrameAnalyzer

/**
 * Detects soccer balls in camera frames using ML Kit Object Detection.
 *
 * Uses ML Kit's built-in model in STREAM_MODE for real-time tracking.
 * When a ball is detected, its bounding box is forwarded to [BallTracker]
 * for trajectory analysis.
 *
 * In a future iteration, this can be swapped to use a custom TFLite model
 * fine-tuned specifically for soccer balls.
 */
class BallDetector(
    private val ballTracker: BallTracker
) : FrameAnalyzer {

    private val detector: ObjectDetector

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        detector = ObjectDetection.getClient(options)
    }

    override fun analyze(frame: CameraFrame): Task<*> {
        return detector.process(frame.image)
            .addOnSuccessListener { detectedObjects ->
                val ball = findBall(detectedObjects, frame)
                if (ball != null) {
                    ballTracker.onBallDetected(ball.centerX(), ball.centerY(), frame.timestamp)
                }
            }
    }

    /**
     * Find the most likely soccer ball among detected objects.
     *
     * ML Kit's built-in model detects generic objects with labels like
     * "Sports equipment", "Ball", etc. We look for the best match.
     * Falls back to the largest "Unknown" object as a heuristic, since
     * the ball is typically the most prominent moving object.
     */
    private fun findBall(
        objects: List<DetectedObject>,
        frame: CameraFrame
    ): android.graphics.RectF? {
        if (objects.isEmpty()) return null

        // Priority 1: Look for objects with ball-related labels
        val ballLabels = setOf("sports equipment", "ball", "sports ball")
        val ballByLabel = objects.firstOrNull { obj ->
            obj.labels.any { label ->
                ballLabels.any { keyword ->
                    label.text.lowercase().contains(keyword)
                }
            }
        }
        if (ballByLabel != null) {
            return android.graphics.RectF(ballByLabel.boundingBox)
        }

        // Priority 2: If only one object detected in stream mode, likely the ball
        if (objects.size == 1) {
            return android.graphics.RectF(objects[0].boundingBox)
        }

        // Priority 3: Pick the smallest object (ball is usually smaller than the person)
        val smallest = objects.minByOrNull {
            it.boundingBox.width() * it.boundingBox.height()
        }
        return smallest?.let { android.graphics.RectF(it.boundingBox) }
    }

    fun close() {
        detector.close()
    }
}
