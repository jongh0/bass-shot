package com.jongh0.bassshot.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModeTest {

    @Test
    fun testDefaultCameraMode_isNormal() {
        val uiState = BassShotUiState()
        assertEquals(CameraMode.NORMAL, uiState.cameraMode)
        assertEquals(AppState.CAMERA_PREVIEW, uiState.appState)
    }

    @Test
    fun testCameraModeSwitching() {
        var uiState = BassShotUiState()
        assertEquals(CameraMode.NORMAL, uiState.cameraMode)

        uiState = uiState.copy(cameraMode = CameraMode.MEASUREMENT)
        assertEquals(CameraMode.MEASUREMENT, uiState.cameraMode)

        uiState = uiState.copy(cameraMode = CameraMode.NORMAL)
        assertEquals(CameraMode.NORMAL, uiState.cameraMode)
    }

    @Test
    fun testCameraMode_enumValues() {
        val modes = CameraMode.values()
        assertEquals(2, modes.size)
        assertTrue(modes.contains(CameraMode.NORMAL))
        assertTrue(modes.contains(CameraMode.MEASUREMENT))
    }
}
