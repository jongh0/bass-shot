package com.jongh0.bassshot.domain.util

import com.jongh0.bassshot.domain.model.BassMeasurementDto
import com.jongh0.bassshot.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FishMeasurementCalculatorTest {

    @Test
    fun testWeightFormula_standardBass() {
        // L = 32.2cm, G = 8.0cm -> W = 0.208 * 32.2 * 64 = 428.7 -> 429g
        val userBassWeight = FishMeasurementCalculator.calculateWeightGrams(32.2f, 8.0f)
        assertEquals(429, userBassWeight)

        // L = 40cm, G = 10cm -> W = 0.208 * 40 * 100 = 832g
        val weight = FishMeasurementCalculator.calculateWeightGrams(40f, 10f)
        assertEquals(832, weight)

        // L = 50cm, G = 14cm (런커 배스) -> W = 0.208 * 50 * 196 = 2038g
        val lunkerWeight = FishMeasurementCalculator.calculateWeightGrams(50f, 14f)
        assertEquals(2038, lunkerWeight)
    }

    @Test
    fun testFromDto_validResponse() {
        val dto = BassMeasurementDto(
            isBassDetected = true,
            lengthCm = 35.0f,
            girthCm = 9.0f,
            weightG = 590,
            snoutPoint = NormalizedPoint(0.5f, 0.2f),
            tailPoint = NormalizedPoint(0.5f, 0.8f)
        )
        val uiModel = FishMeasurementCalculator.fromDto(dto, 0.75f)

        assertTrue(uiModel.isBassDetected)
        assertFalse(uiModel.isManualFallback)
        assertEquals(35.0f, uiModel.lengthCm, 0.1f)
        assertEquals(9.0f, uiModel.girthCm, 0.1f)
        assertEquals(590, uiModel.weightG)
    }

    @Test
    fun testRecalculateOnDrag_increasesLengthWhenTailMovedDown() {
        val dto = BassMeasurementDto(
            isBassDetected = true,
            lengthCm = 30.0f,
            girthCm = 8.0f,
            weightG = 86,
            snoutPoint = NormalizedPoint(0.5f, 0.2f),
            tailPoint = NormalizedPoint(0.5f, 0.7f)
        )
        val initial = FishMeasurementCalculator.fromDto(dto, 0.75f)

        // 꼬리를 더 아래로 드래그 (0.7 -> 0.85)
        val updated = FishMeasurementCalculator.recalculateOnDrag(
            current = initial,
            newTail = NormalizedPoint(0.5f, 0.85f)
        )

        assertTrue(updated.lengthCm > initial.lengthCm)
        assertTrue(updated.weightG > initial.weightG)
    }
}
