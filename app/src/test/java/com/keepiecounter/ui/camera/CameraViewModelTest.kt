package com.keepiecounter.ui.camera

import com.keepiecounter.data.local.SessionEntity
import com.keepiecounter.data.local.SessionDao
import com.keepiecounter.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Unit tests for CameraViewModel.
 * Tests run on JVM (no Android device needed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private lateinit var viewModel: CameraViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    // Fake DAO for testing (avoids Room/Android dependency)
    private val fakeDao = object : SessionDao {
        val sessions = mutableListOf<SessionEntity>()
        override suspend fun insert(session: SessionEntity) { sessions.add(session) }
        override fun getAllSessions(): Flow<List<SessionEntity>> = flowOf(sessions.toList())
        override suspend fun getPersonalBest(): Int? = sessions.maxByOrNull { it.count }?.count
        override suspend fun getSessionCount(): Int = sessions.size
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraViewModel(SessionRepository(fakeDao))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
    fun `initial state - uses back camera`() {
        assertFalse(viewModel.isFrontCamera.value)
    }

    @Test
    fun `initial state - elapsed seconds is zero`() {
        assertEquals(0L, viewModel.elapsedSeconds.value)
    }

    @Test
    fun `initial state - detection indicators are false`() {
        assertFalse(viewModel.isBallDetected.value)
        assertFalse(viewModel.isPoseDetected.value)
    }

    @Test
    fun `startSession - activates session and resets count`() {
        viewModel.startSession()
        assertTrue(viewModel.isSessionActive.value)
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun `startSession resets detectors`() {
        viewModel.keepieCounter.onBallMovingUp(1000L)
        viewModel.keepieCounter.onKickDetected(
            com.keepiecounter.detection.pose.KickDetector.KickResult(
                true, com.keepiecounter.detection.pose.KickDetector.Foot.LEFT, 0.8f, 1050L
            )
        )
        assertEquals(1, viewModel.count.value)

        viewModel.startSession()
        assertEquals(0, viewModel.count.value)
    }

    @Test
    fun `startSession resets elapsed seconds`() {
        viewModel.startSession()
        assertEquals(0L, viewModel.elapsedSeconds.value)
    }

    @Test
    fun `stopSession - deactivates session`() {
        viewModel.startSession()
        viewModel.stopSession()
        assertFalse(viewModel.isSessionActive.value)
    }

    @Test
    fun `stopSession clears detection indicators`() {
        viewModel.startSession()
        viewModel.stopSession()
        assertFalse(viewModel.isBallDetected.value)
        assertFalse(viewModel.isPoseDetected.value)
    }

    @Test
    fun `onPermissionResult granted - sets permission flag`() {
        viewModel.onPermissionResult(true)
        assertTrue(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `onPermissionResult denied - permission stays false`() {
        viewModel.onPermissionResult(false)
        assertFalse(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `toggleCamera - switches from back to front`() {
        assertFalse(viewModel.isFrontCamera.value)
        viewModel.toggleCamera()
        assertTrue(viewModel.isFrontCamera.value)
    }

    @Test
    fun `toggleCamera twice - returns to back camera`() {
        viewModel.toggleCamera()
        viewModel.toggleCamera()
        assertFalse(viewModel.isFrontCamera.value)
    }

    @Test
    fun `startSession after stop - resets count to zero`() {
        viewModel.startSession()
        viewModel.stopSession()
        viewModel.startSession()
        assertEquals(0, viewModel.count.value)
    }
}
