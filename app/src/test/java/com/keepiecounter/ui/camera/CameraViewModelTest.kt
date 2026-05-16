package com.keepiecounter.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CameraViewModel.
 * Tests run on JVM (no Android device needed).
 */
class CameraViewModelTest {

    private lateinit var viewModel: CameraViewModel

    @Before
    fun setup() {
        viewModel = CameraViewModel()
    }

    @Test
    fun `initial state - count is zero`() {
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun `initial state - session is not active`() {
        assertFalse(viewModel.isSessionActive.value)
    }

    @Test
    fun `initial state - camera permission not granted`() {
        assertFalse(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `startSession - activates session and resets count`() {
        viewModel.startSession()
        assertTrue(viewModel.isSessionActive.value)
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun `stopSession - deactivates session`() {
        viewModel.startSession()
        viewModel.stopSession()
        assertFalse(viewModel.isSessionActive.value)
    }

    @Test
    fun `onPermissionGranted - sets permission flag`() {
        viewModel.onPermissionGranted()
        assertTrue(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `startSession after stop - resets count to zero`() {
        viewModel.startSession()
        // In later phases, count will increment during session
        viewModel.stopSession()
        viewModel.startSession()
        assertEquals(0, viewModel.count.value)
    }
}
