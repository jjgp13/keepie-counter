package com.keepiecounter.ui.camera

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.keepiecounter.detection.CameraFrame
import com.keepiecounter.detection.FrameAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX lifecycle: binds Preview + ImageAnalysis to a LifecycleOwner.
 *
 * Frame dispatch:
 *  - ImageAnalysis delivers frames on a single-thread executor.
 *  - Each frame is wrapped in a CameraFrame and sent to all registered FrameAnalyzers.
 *  - The ImageProxy is closed only after ALL analyzers complete (or fail), preventing
 *    ML Kit from reading a closed image buffer.
 *
 * Not a Hilt-managed class — instantiated in the Composable layer because it requires
 * a PreviewView and LifecycleOwner, which are UI-layer concerns.
 */
class CameraManager {

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analyzers = mutableListOf<FrameAnalyzer>()

    @Volatile
    var isAnalysisEnabled: Boolean = false

    fun addAnalyzer(analyzer: FrameAnalyzer) {
        analyzers.add(analyzer)
    }

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(previewView.context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val lensFacing = if (useFrontCamera) CameraSelector.LENS_FACING_FRONT
                             else CameraSelector.LENS_FACING_BACK

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Check the device actually has the requested camera
            if (!provider.hasCamera(cameraSelector)) return@addListener

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (!isAnalysisEnabled || analyzers.isEmpty()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                        val frame = CameraFrame(
                            image = inputImage,
                            timestamp = System.currentTimeMillis(),
                            rotationDegrees = rotation,
                            width = imageProxy.width,
                            height = imageProxy.height,
                            lensFacing = lensFacing
                        )

                        // Dispatch to all analyzers and close proxy when ALL are done
                        val tasks = analyzers.map { it.analyze(frame) }
                        Tasks.whenAllComplete(tasks).addOnCompleteListener {
                            imageProxy.close()
                        }
                    }
                }

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        }, androidx.core.content.ContextCompat.getMainExecutor(previewView.context))
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    fun shutdown() {
        unbind()
        analysisExecutor.shutdown()
    }
}
