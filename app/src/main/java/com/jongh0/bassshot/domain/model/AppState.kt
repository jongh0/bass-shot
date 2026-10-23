package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

enum class AppState {
    CAMERA_PREVIEW,  // 실시간 카메라 프리뷰 및 Timemark 오버레이 표시 상태
    ANALYZING,       // 셔터 직후 비트맵 고정 및 Gemini Flash 분석 진행 상태
    RESULT_SAVED     // AI 계측 완료, 캘리퍼스 미세조정 및 DCIM/BassShot 저장 완료 상태
}

data class BassShotUiState(
    val appState: AppState = AppState.CAMERA_PREVIEW,
    val environmentData: EnvironmentData = EnvironmentData(),
    val capturedOriginalBitmap: Bitmap? = null,
    val measurement: BassMeasurementUiModel? = null,
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val savedPhotoUriString: String? = null,
    val errorMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val updateDownloadUrl: String? = null,
    val updateVersionName: String? = null
)
