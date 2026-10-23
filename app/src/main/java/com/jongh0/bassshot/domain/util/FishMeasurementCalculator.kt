package com.jongh0.bassshot.domain.util

import com.jongh0.bassshot.domain.model.BassMeasurementDto
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object FishMeasurementCalculator {

    /**
     * 어류학 체장-체고 기반 무게 산출 공식: W = 0.045 * L * (G^2)
     */
    fun calculateWeightGrams(lengthCm: Float, girthCm: Float): Int {
        if (lengthCm <= 0f || girthCm <= 0f) return 0
        val weight = 0.045f * lengthCm * girthCm.pow(2)
        return weight.roundToInt().coerceAtLeast(10)
    }

    /**
     * 이미지 종횡비(width / height)를 반영한 두 정규화 좌표 간 실제 비례 거리 계산
     */
    fun calculateNormalizedDistance(
        p1: NormalizedPoint,
        p2: NormalizedPoint,
        aspectRatio: Float = 0.75f
    ): Float {
        val dx = (p2.x - p1.x) * aspectRatio
        val dy = (p2.y - p1.y)
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * AI 응답 DTO를 UI 모델로 변환 (비정상 범위 및 미검출 폴백 처리 포함)
     */
    fun fromDto(dto: BassMeasurementDto, aspectRatio: Float = 0.75f): BassMeasurementUiModel {
        val validDetection = dto.isBassDetected && dto.lengthCm in 8.0f..95.0f && dto.girthCm in 2.0f..60.0f
        if (!validDetection) {
            return createManualFallback(aspectRatio)
        }

        val snout = dto.snoutPoint.clamped()
        val tail = dto.tailPoint.clamped()
        val girthL = dto.maxGirthLeft.clamped()
        val girthR = dto.maxGirthRight.clamped()

        val initialLengthDist = calculateNormalizedDistance(snout, tail, aspectRatio).coerceAtLeast(0.05f)
        val cmPerUnit = (dto.lengthCm / initialLengthDist).coerceIn(15.0f, 200.0f)

        val roundedLength = (dto.lengthCm * 10f).roundToInt() / 10f
        val roundedGirth = (dto.girthCm * 10f).roundToInt() / 10f
        val calculatedWeight = calculateWeightGrams(roundedLength, roundedGirth)

        return BassMeasurementUiModel(
            isBassDetected = true,
            isManualFallback = false,
            lengthCm = roundedLength,
            girthCm = roundedGirth,
            weightG = calculatedWeight,
            snoutPoint = snout,
            tailPoint = tail,
            maxGirthLeft = girthL,
            maxGirthRight = girthR,
            cmPerNormalizedUnit = cmPerUnit,
            imageAspectRatio = aspectRatio
        )
    }

    /**
     * 오프라인 또는 AI 배스 미검출 시 즉시 사용할 수 있는 수동 계측 폴백 모델
     */
    fun createManualFallback(aspectRatio: Float = 0.75f): BassMeasurementUiModel {
        val snout = NormalizedPoint(0.50f, 0.22f)
        val tail = NormalizedPoint(0.50f, 0.78f)
        val girthL = NormalizedPoint(0.38f, 0.46f)
        val girthR = NormalizedPoint(0.62f, 0.46f)
        val defaultLength = 32.0f
        val defaultGirth = 8.5f
        val dist = calculateNormalizedDistance(snout, tail, aspectRatio)
        val cmPerUnit = defaultLength / dist

        return BassMeasurementUiModel(
            isBassDetected = false,
            isManualFallback = true,
            lengthCm = defaultLength,
            girthCm = defaultGirth,
            weightG = calculateWeightGrams(defaultLength, defaultGirth),
            snoutPoint = snout,
            tailPoint = tail,
            maxGirthLeft = girthL,
            maxGirthRight = girthR,
            cmPerNormalizedUnit = cmPerUnit,
            imageAspectRatio = aspectRatio
        )
    }

    /**
     * 사용자가 주둥이/꼬리/체고 핸들을 드래그했을 때 실시간으로 길이, 체고, 무게 재계산
     */
    fun recalculateOnDrag(
        current: BassMeasurementUiModel,
        newSnout: NormalizedPoint = current.snoutPoint,
        newTail: NormalizedPoint = current.tailPoint,
        newGirthLeft: NormalizedPoint = current.maxGirthLeft,
        newGirthRight: NormalizedPoint = current.maxGirthRight
    ): BassMeasurementUiModel {
        val clampedSnout = newSnout.clamped()
        val clampedTail = newTail.clamped()
        val clampedGirthL = newGirthLeft.clamped()
        val clampedGirthR = newGirthRight.clamped()

        val lengthDist = calculateNormalizedDistance(clampedSnout, clampedTail, current.imageAspectRatio)
            .coerceAtLeast(0.05f)
        val girthDist = calculateNormalizedDistance(clampedGirthL, clampedGirthR, current.imageAspectRatio)
            .coerceAtLeast(0.02f)

        val newLengthCm = ((lengthDist * current.cmPerNormalizedUnit) * 10f).roundToInt() / 10f
        val newGirthCm = ((girthDist * current.cmPerNormalizedUnit) * 10f).roundToInt() / 10f
        val newWeightG = calculateWeightGrams(newLengthCm, newGirthCm)

        return current.copy(
            lengthCm = newLengthCm,
            girthCm = newGirthCm,
            weightG = newWeightG,
            snoutPoint = clampedSnout,
            tailPoint = clampedTail,
            maxGirthLeft = clampedGirthL,
            maxGirthRight = clampedGirthR
        )
    }
}
