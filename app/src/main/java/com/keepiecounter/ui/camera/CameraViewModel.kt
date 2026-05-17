package com.keepiecounter.ui.camera

import androidx.lifecycle.ViewModel
import com.keepiecounter.detection.ball.BallTracker
import com.keepiecounter.detection.counter.KeepieCounter
import com.keepiecounter.detection.pose.KickDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    val ballTracker = BallTracker()
    val kickDetector = KickDetector()
    val keepieCounter = KeepieCounter()

    init {
        // Wire detection pipeline: BallTracker → KeepieCounter
        ballTracker.onBallMovingUp = { timestamp ->
            keepieCounter.onBallMovingUp(timestamp)
        }

        // Wire detection pipeline: KickDetector → KeepieCounter
        kickDetector.onKickDetected = { result ->
            keepieCounter.onKickDetected(result)
        }

        // Wire counter → UI state
        keepieCounter.onCountChanged = { newCount ->
            _count.value = newCount
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun startSession() {
        _count.value = 0
        ballTracker.reset()
        kickDetector.reset()
        keepieCounter.reset()
        _isSessionActive.value = true
    }

    fun stopSession() {
        _isSessionActive.value = false
    }
}
