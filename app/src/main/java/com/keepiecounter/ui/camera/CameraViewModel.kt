package com.keepiecounter.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepiecounter.data.repository.SessionRepository
import com.keepiecounter.detection.ball.BallTracker
import com.keepiecounter.detection.counter.KeepieCounter
import com.keepiecounter.detection.pose.KickDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isBallDetected = MutableStateFlow(false)
    val isBallDetected: StateFlow<Boolean> = _isBallDetected.asStateFlow()

    private val _isPoseDetected = MutableStateFlow(false)
    val isPoseDetected: StateFlow<Boolean> = _isPoseDetected.asStateFlow()

    // Event flow for haptic feedback — replay=0 so no stale vibrations
    private val _countEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val countEvent: SharedFlow<Unit> = _countEvent.asSharedFlow()

    val ballTracker = BallTracker()
    val kickDetector = KickDetector()
    val keepieCounter = KeepieCounter()

    private var sessionStartTime = 0L
    private var timerJob: Job? = null
    private var detectionTimeoutJob: Job? = null

    @Volatile
    private var lastBallSeenAt = 0L
    @Volatile
    private var lastPoseSeenAt = 0L

    companion object {
        const val DETECTION_TIMEOUT_MS = 800L
    }

    init {
        ballTracker.onBallMovingUp = { timestamp ->
            lastBallSeenAt = System.currentTimeMillis()
            keepieCounter.onBallMovingUp(timestamp)
        }
        kickDetector.onKickDetected = { result ->
            lastPoseSeenAt = System.currentTimeMillis()
            keepieCounter.onKickDetected(result)
        }
        keepieCounter.onCountChanged = { newCount ->
            _count.value = newCount
            _countEvent.tryEmit(Unit)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun startSession() {
        timerJob?.cancel()
        detectionTimeoutJob?.cancel()

        _count.value = 0
        _elapsedSeconds.value = 0L
        _sessionSaved.value = false
        _isBallDetected.value = false
        _isPoseDetected.value = false
        lastBallSeenAt = 0L
        lastPoseSeenAt = 0L
        ballTracker.reset()
        kickDetector.reset()
        keepieCounter.reset()
        sessionStartTime = System.currentTimeMillis()
        _isSessionActive.value = true

        // Timer ticks every second
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value = (System.currentTimeMillis() - sessionStartTime) / 1000
            }
        }

        // Periodically check detection recency to clear stale indicators
        detectionTimeoutJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val now = System.currentTimeMillis()
                _isBallDetected.value = lastBallSeenAt > 0 && (now - lastBallSeenAt) < DETECTION_TIMEOUT_MS
                _isPoseDetected.value = lastPoseSeenAt > 0 && (now - lastPoseSeenAt) < DETECTION_TIMEOUT_MS
            }
        }
    }

    fun stopSession() {
        _isSessionActive.value = false
        timerJob?.cancel()
        detectionTimeoutJob?.cancel()
        _isBallDetected.value = false
        _isPoseDetected.value = false
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
