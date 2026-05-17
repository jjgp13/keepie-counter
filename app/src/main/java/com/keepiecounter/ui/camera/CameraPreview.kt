package com.keepiecounter.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.keepiecounter.detection.ball.BallDetector
import com.keepiecounter.detection.ball.BallTracker
import com.keepiecounter.detection.pose.KickDetector
import com.keepiecounter.detection.pose.PoseAnalyzer

/**
 * Composable that displays a CameraX preview and manages camera lifecycle.
 *
 * Registers BallDetector and PoseAnalyzer as frame analyzers so that
 * the full detection pipeline runs on each camera frame.
 */
@Composable
fun CameraPreview(
    useFrontCamera: Boolean,
    isSessionActive: Boolean,
    ballTracker: BallTracker,
    kickDetector: KickDetector,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val ballDetector = remember { BallDetector(ballTracker) }
    val poseAnalyzer = remember { PoseAnalyzer(kickDetector) }

    val cameraManager = remember {
        CameraManager().also { manager ->
            manager.addAnalyzer(ballDetector)
            manager.addAnalyzer(poseAnalyzer)
        }
    }

    // Gate analysis on session state
    cameraManager.isAnalysisEnabled = isSessionActive

    // Bind/rebind camera when front/back changes
    DisposableEffect(useFrontCamera, lifecycleOwner) {
        cameraManager.bind(lifecycleOwner, previewView, useFrontCamera)
        onDispose {
            cameraManager.unbind()
        }
    }

    // Shut down when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
            ballDetector.close()
            poseAnalyzer.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
