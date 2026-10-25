package com.jongh0.bassshot.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NormalizedPoint(
    val x: Float = 0.5f,
    val y: Float = 0.5f
) {
    /**
     * AI 모델이 반환하는 좌표 스케일(0.0~1.0 비율, 0~10.0 스케일, 0~100% 백분율, 0~1000 프로밀/픽셀)을
     * 지능적으로 판별하여 표준 0.0~1.0 비율로 정규화하고 안전 마진을 적용합니다.
     */
    fun clamped(minX: Float = 0.03f, maxX: Float = 0.97f, minY: Float = 0.03f, maxY: Float = 0.97f): NormalizedPoint {
        val normX = normalizeCoordinate(x)
        val normY = normalizeCoordinate(y)
        return NormalizedPoint(
            x = normX.coerceIn(minX, maxX),
            y = normY.coerceIn(minY, maxY)
        )
    }

    companion object {
        fun normalizeCoordinate(v: Float): Float {
            return when {
                v < 0f -> 0f
                v > 100.0f -> v / 1000f        // 0~1000 프로밀(permille) 또는 1024px 스케일 (예: 954.0 -> 0.954)
                v > 10.0f -> v / 100f          // 0~100 백분율(percentage) 스케일 (예: 95.4 -> 0.954)
                v > 1.0f -> v / 10f            // 0~10 스케일 (예: 9.54 -> 0.954)
                else -> v                      // 0.0 ~ 1.0 표준 정규화 비율
            }
        }
    }
}

@Serializable
data class BassMeasurementDto(
    @SerialName("is_bass_detected") val isBassDetected: Boolean = false,
    @SerialName("length_cm") val lengthCm: Float = 0f,
    @SerialName("girth_cm") val girthCm: Float = 0f,
    @SerialName("weight_g") val weightG: Int = 0,
    @SerialName("snout_point") val snoutPoint: NormalizedPoint = NormalizedPoint(0.5f, 0.25f),
    @SerialName("tail_point") val tailPoint: NormalizedPoint = NormalizedPoint(0.5f, 0.75f),
    @SerialName("max_girth_left") val maxGirthLeft: NormalizedPoint = NormalizedPoint(0.40f, 0.48f),
    @SerialName("max_girth_right") val maxGirthRight: NormalizedPoint = NormalizedPoint(0.60f, 0.48f)
)

data class BassMeasurementUiModel(
    val isBassDetected: Boolean,
    val isManualFallback: Boolean,
    val lengthCm: Float,
    val girthCm: Float,
    val weightG: Int,
    val snoutPoint: NormalizedPoint,
    val tailPoint: NormalizedPoint,
    val maxGirthLeft: NormalizedPoint,
    val maxGirthRight: NormalizedPoint,
    val cmPerNormalizedUnit: Float,
    val imageAspectRatio: Float = 0.75f // width / height (3:4 ratio = 0.75)
)

/**
 * 배스 체장 구간 분류 (1/2짜, 3짜, 4짜, 5짜 이상)
 */
enum class BassLengthTier {
    TIER_UNDER_30, // 1/2짜 (< 30.0cm): 짜치 (TitaniumSilver)
    TIER_30S,      // 3짜 (30.0cm ~ 39.9cm): 준수한 배스 (NeonLime)
    TIER_40S,      // 4짜 (40.0cm ~ 49.9cm): 대형 배스 (ElectricCyan)
    TIER_50_PLUS;  // 5짜 이상 (>= 50.0cm): 런커 / 대물 (LunkerGold)

    companion object {
        fun fromLength(lengthCm: Float): BassLengthTier = when {
            lengthCm < 30.0f -> TIER_UNDER_30
            lengthCm < 40.0f -> TIER_30S
            lengthCm < 50.0f -> TIER_40S
            else -> TIER_50_PLUS
        }
    }
}

val BassMeasurementUiModel.lengthTier: BassLengthTier
    get() = BassLengthTier.fromLength(lengthCm)
