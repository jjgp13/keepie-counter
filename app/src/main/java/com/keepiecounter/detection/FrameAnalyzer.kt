package com.keepiecounter.detection

import com.google.mlkit.vision.common.InputImage

/**
 * Metadata-rich wrapper for a camera frame.
 * Carries everything detectors need without coupling them to CameraX's ImageProxy.
 */
data class CameraFrame(
    val image: InputImage,
    val timestamp: Long,
    val rotationDegrees: Int,
    val width: Int,
    val height: Int,
    val lensFacing: Int
)

/**
 * Interface for frame analysis in the detection pipeline.
 *
 * Implementations (BallDetector, PoseDetector) process the frame asynchronously
 * via ML Kit and report results through their own callbacks/flows.
 *
 * The returned Task signals when this analyzer is done with the frame,
 * allowing the pipeline to close the ImageProxy only after all analyzers finish.
 */
interface FrameAnalyzer {
    fun analyze(frame: CameraFrame): com.google.android.gms.tasks.Task<*>
}
