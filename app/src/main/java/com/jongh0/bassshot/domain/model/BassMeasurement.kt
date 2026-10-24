package com.jongh0.bassshot.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NormalizedPoint(
    val x: Float = 0.5f,
    val y: Float = 0.5f
) {
    fun clamped(minX: Float = 0.03f, maxX: Float = 0.97f, minY: Float = 0.03f, maxY: Float = 0.97f): NormalizedPoint {
        val normX = if (x > 1.0f) x / 1000f else x
        val normY = if (y > 1.0f) y / 1000f else y
        return NormalizedPoint(
            x = normX.coerceIn(minX, maxX),
            y = normY.coerceIn(minY, maxY)
        )
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
