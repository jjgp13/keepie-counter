package com.keepiecounter.detection.pose

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.keepiecounter.detection.CameraFrame
import com.keepiecounter.detection.FrameAnalyzer

/**
 * Analyzes camera frames for pose/kick detection using ML Kit Pose Detection.
 *
 * Extracts lower-body landmarks (ankle, knee, hip) from each frame and
 * forwards them to [KickDetector] for motion analysis.
 */
class PoseAnalyzer(
    private val kickDetector: KickDetector
) : FrameAnalyzer {

    private val poseDetector: com.google.mlkit.vision.pose.PoseDetector

    init {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)
    }

    override fun analyze(frame: CameraFrame): Task<*> {
        return poseDetector.process(frame.image)
            .addOnSuccessListener { pose ->
                extractAndForward(pose, frame.timestamp)
            }
    }

    private fun extractAndForward(pose: Pose, timestamp: Long) {
        val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return
        val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return
        val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE) ?: return
        val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE) ?: return
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP) ?: return

        fun toPoint(landmark: PoseLandmark) =
            KickDetector.LandmarkPoint(landmark.position.x, landmark.position.y)

        kickDetector.onPoseDetected(
            leftAnkle = toPoint(lAnkle), leftKnee = toPoint(lKnee), leftHip = toPoint(lHip),
            rightAnkle = toPoint(rAnkle), rightKnee = toPoint(rKnee), rightHip = toPoint(rHip),
            timestamp = timestamp
        )
    }

    fun close() {
        poseDetector.close()
    }
}
