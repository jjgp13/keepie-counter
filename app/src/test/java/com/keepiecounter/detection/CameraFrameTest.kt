package com.keepiecounter.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFrameTest {

    @Test
    fun `CameraFrame stores all metadata correctly`() {
        val frame = CameraFrame(
            image = org.mockito.Mockito.mock(com.google.mlkit.vision.common.InputImage::class.java),
            timestamp = 1234567890L,
            rotationDegrees = 90,
            width = 640,
            height = 480,
            lensFacing = 1
        )

        assertEquals(1234567890L, frame.timestamp)
        assertEquals(90, frame.rotationDegrees)
        assertEquals(640, frame.width)
        assertEquals(480, frame.height)
        assertEquals(1, frame.lensFacing)
    }

    @Test
    fun `CameraFrame equality works with same metadata`() {
        val mockImage = org.mockito.Mockito.mock(com.google.mlkit.vision.common.InputImage::class.java)

        val frame1 = CameraFrame(mockImage, 100L, 0, 640, 480, 0)
        val frame2 = CameraFrame(mockImage, 100L, 0, 640, 480, 0)

        assertEquals(frame1, frame2)
    }
}
