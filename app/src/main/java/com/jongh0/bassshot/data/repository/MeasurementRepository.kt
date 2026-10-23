package com.jongh0.bassshot.data.repository

import android.graphics.Bitmap
import com.jongh0.bassshot.data.remote.GeminiAnalysisDataSource
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator

class MeasurementRepository(
    private val geminiAnalysisDataSource: GeminiAnalysisDataSource = GeminiAnalysisDataSource()
) {

    /**
     * 리사이즈된 비트맵과 기준 장갑 폭을 Gemini API로 전송하고 계측 결과를 UI 모델로 매핑
     * 실패 또는 미검출 시 사용자 조작이 가능한 수동 계측 모드로 자동 전환
     */
    suspend fun measureBass(
        resizedBitmap: Bitmap,
        referenceGloveWidthCm: Float,
        imageAspectRatio: Float = 0.75f
    ): BassMeasurementUiModel {
        val result = geminiAnalysisDataSource.analyzeBassImage(resizedBitmap, referenceGloveWidthCm)
        val dto = result.getOrNull()

        return if (dto != null && dto.isBassDetected) {
            FishMeasurementCalculator.fromDto(dto, imageAspectRatio)
        } else {
            FishMeasurementCalculator.createManualFallback(imageAspectRatio)
        }
    }
}
