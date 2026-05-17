package com.keepiecounter.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepiecounter.data.repository.SessionRepository
import com.keepiecounter.detection.ball.BallTracker
import com.keepiecounter.detection.counter.KeepieCounter
import com.keepiecounter.detection.pose.KickDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _sessionSaved = MutableStateFlow(false)
    val sessionSaved: StateFlow<Boolean> = _sessionSaved.asStateFlow()

    val ballTracker = BallTracker()
    val kickDetector = KickDetector()
    val keepieCounter = KeepieCounter()

    private var sessionStartTime = 0L

    init {
        ballTracker.onBallMovingUp = { timestamp ->
            keepieCounter.onBallMovingUp(timestamp)
        }
        kickDetector.onKickDetected = { result ->
            keepieCounter.onKickDetected(result)
        }
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
        _sessionSaved.value = false
        ballTracker.reset()
        kickDetector.reset()
        keepieCounter.reset()
        sessionStartTime = System.currentTimeMillis()
        _isSessionActive.value = true
    }

    fun stopSession() {
        _isSessionActive.value = false
        val duration = System.currentTimeMillis() - sessionStartTime
        val finalCount = _count.value

        if (finalCount > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                sessionRepository.saveSession(finalCount, duration)
                _sessionSaved.value = true
            }
        }
    }
}
