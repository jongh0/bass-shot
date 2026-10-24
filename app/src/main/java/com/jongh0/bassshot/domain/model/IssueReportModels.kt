package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

enum class IssueReportType(val label: String, val titlePrefix: String) {
    MEASUREMENT_INACCURATE("AI 계측 부정확", "[BUG]: AI 계측 부정확"),
    MEASUREMENT_FAILED("물고기 미인식 / AI 실패", "[BUG]: 물고기 미인식 / 계측 실패"),
    SAVED_PHOTO("저장된 사진 오류", "[BUG]: 사진 뷰어 계측 오류"),
    GENERAL("일반 버그 / 피드백", "[BUG]: 일반 오류 및 피드백")
}

data class IssueReportInput(
    val reportType: IssueReportType,
    val userComment: String,
    val actualLengthCm: Float? = null,
    val actualWeightG: Int? = null,
    val originalBitmap: Bitmap? = null,
    val compositeBitmap: Bitmap? = null,
    val measurement: BassMeasurementUiModel? = null,
    val environmentData: EnvironmentData? = null,
    val includeLogs: Boolean = true
)
