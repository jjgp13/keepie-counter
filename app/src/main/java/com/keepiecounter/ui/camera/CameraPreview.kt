package com.keepiecounter.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Composable that displays a CameraX preview and manages camera lifecycle.
 *
 * - Creates a PreviewView and a CameraManager.
 * - Binds the camera in a DisposableEffect keyed on [useFrontCamera], so toggling
 *   the camera triggers unbind → rebind automatically.
 * - Shuts down the CameraManager when leaving composition.
 *
 * @param useFrontCamera Whether to use the front-facing camera.
 * @param isSessionActive Whether detection analysis is active (gates frame dispatch).
 * @param cameraManagerReady Callback providing the CameraManager so the parent can
 *                           register FrameAnalyzers (used in Phase 3+).
 */
@Composable
fun CameraPreview(
    useFrontCamera: Boolean,
    isSessionActive: Boolean,
    modifier: Modifier = Modifier,
    cameraManagerReady: (CameraManager) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraManager = remember {
        CameraManager().also { cameraManagerReady(it) }
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

    // Shut down executor when leaving composition entirely
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
