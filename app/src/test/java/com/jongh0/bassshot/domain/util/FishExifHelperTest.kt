package com.jongh0.bassshot.domain.util

import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData
import com.jongh0.bassshot.domain.model.NormalizedPoint
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FishExifHelperTest {

    @Test
    fun testBuildHumanReadableDescription() {
        val measurement = BassMeasurementUiModel(
            isBassDetected = true,
            isManualFallback = false,
            lengthCm = 48.5f,
            girthCm = 12.0f,
            weightG = 1420,
            snoutPoint = NormalizedPoint(0.5f, 0.2f),
            tailPoint = NormalizedPoint(0.5f, 0.8f),
            maxGirthLeft = NormalizedPoint(0.4f, 0.5f),
            maxGirthRight = NormalizedPoint(0.6f, 0.5f),
            cmPerNormalizedUnit = 100f,
            imageAspectRatio = 0.75f
        )
        val env = EnvironmentData(
            weatherDescription = "맑음",
            tempCelsius = 18.0f,
            windSpeedMps = 1.2f,
            pressureHpa = 1013.0f,
            addressKorean = "충청남도 아산시 영인면"
        )

        val description = FishExifHelper.buildHumanReadableDescription(measurement, env)
        assertTrue(description.contains("48.5cm · 1420g"))
        assertTrue(description.contains("맑음 18°C"))
        assertTrue(description.contains("1013hPa"))
        assertTrue(description.contains("충청남도 아산시 영인면"))
    }

    @Test
    fun testBuildJsonUserCommentAndParse() {
        val measurement = BassMeasurementUiModel(
            isBassDetected = true,
            isManualFallback = false,
            lengthCm = 48.5f,
            girthCm = 12.0f,
            weightG = 1420,
            snoutPoint = NormalizedPoint(0.5f, 0.2f),
            tailPoint = NormalizedPoint(0.5f, 0.8f),
            maxGirthLeft = NormalizedPoint(0.4f, 0.5f),
            maxGirthRight = NormalizedPoint(0.6f, 0.5f),
            cmPerNormalizedUnit = 100f,
            imageAspectRatio = 0.75f
        )
        val env = EnvironmentData(
            latitude = 36.8123,
            longitude = 127.0123,
            weatherDescription = "맑음",
            tempCelsius = 18.0f,
            windSpeedMps = 1.2f,
            pressureHpa = 1013.0f,
            isFromBarometerSensor = true,
            addressKorean = "충청남도 아산시 영인면 월선리 573"
        )

        val jsonStr = FishExifHelper.buildJsonUserComment(measurement, env)
        assertNotNull(jsonStr)

        // Parse with Json
        val parsed = Json.decodeFromString<FishExifRecord>(jsonStr)
        assertEquals("BassShot", parsed.app)
        assertNotNull(parsed.measurement)
        assertEquals(48.5f, parsed.measurement?.length_cm)
        assertEquals(1420, parsed.measurement?.weight_g)
        assertEquals(36.8123, parsed.environment.latitude!!, 0.0001)
        assertEquals(1013.0f, parsed.environment.pressure_hpa)
        assertTrue(parsed.environment.is_sensor_barometer)
    }
}
