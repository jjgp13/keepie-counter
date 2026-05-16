package com.keepiecounter.ui.camera

import androidx.lifecycle.ViewModel
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

    fun onPermissionGranted() {
        _hasCameraPermission.value = true
    }

    fun startSession() {
        _count.value = 0
        _isSessionActive.value = true
        // Camera pipeline and detectors will be started in Phase 2-5
    }

    fun stopSession() {
        _isSessionActive.value = false
        // Session will be saved in Phase 7
    }
}
