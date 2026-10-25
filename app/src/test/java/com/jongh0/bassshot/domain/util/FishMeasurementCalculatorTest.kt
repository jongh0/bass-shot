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

    @Test
    fun testNormalizedPoint_scaleNormalizations() {
        // 0.0~1.0 표준 비율
        assertEquals(0.954f, NormalizedPoint(0.5f, 0.954f).clamped().y, 0.001f)
        // 0~10 스케일 (9.54 -> 0.954)
        assertEquals(0.954f, NormalizedPoint(0.5f, 9.54f).clamped().y, 0.001f)
        // 0~100% 백분율 (95.4 -> 0.954)
        assertEquals(0.954f, NormalizedPoint(0.5f, 95.4f).clamped().y, 0.001f)
        // 0~1000 프로밀/픽셀 (954.0 -> 0.954)
        assertEquals(0.954f, NormalizedPoint(0.5f, 954.0f).clamped().y, 0.001f)
    }

    @Test
    fun testFromDto_guardsAgainstExplodedCmPerUnit() {
        // AI가 주둥이와 꼬리를 거의 같은 위치(거리 0.01)로 반환하더라도 cmPerNormalizedUnit이 75를 넘지 않음
        val dto = BassMeasurementDto(
            isBassDetected = true,
            lengthCm = 35.5f,
            girthCm = 9.0f,
            weightG = 600,
            snoutPoint = NormalizedPoint(0.5f, 0.20f),
            tailPoint = NormalizedPoint(0.5f, 0.21f)
        )
        val uiModel = FishMeasurementCalculator.fromDto(dto, 0.75f)
        assertTrue("cmPerNormalizedUnit should be guarded <= 75.0", uiModel.cmPerNormalizedUnit <= 75.0f)
    }

    @Test
    fun testRecalculateOnDrag_neverExceedsRealisticLength() {
        val dto = BassMeasurementDto(
            isBassDetected = true,
            lengthCm = 35.5f,
            girthCm = 9.0f,
            weightG = 600,
            snoutPoint = NormalizedPoint(0.5f, 0.2f),
            tailPoint = NormalizedPoint(0.5f, 0.8f)
        )
        val initial = FishMeasurementCalculator.fromDto(dto, 0.75f)

        // 극단적인 화면 모서리(0,0 -> 1,1) 드래그 시에도 최대 85cm로 제한
        val dragged = FishMeasurementCalculator.recalculateOnDrag(
            current = initial,
            newSnout = NormalizedPoint(0.01f, 0.01f),
            newTail = NormalizedPoint(0.99f, 0.99f)
        )
        assertTrue("Length should never exceed 85cm", dragged.lengthCm <= 85.0f)
        assertTrue("Weight should be realistic", dragged.weightG <= 10000)
    }
}
