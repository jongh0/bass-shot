package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

enum class AppState {
    CAMERA_PREVIEW,  // 실시간 카메라 프리뷰 및 Timemark 오버레이 표시 상태
    ANALYZING,       // 셔터 직후 비트맵 고정 및 Gemini Flash 분석 진행 상태
    RESULT_SAVED     // AI 계측 완료, 캘리퍼스 미세조정 및 DCIM/BassShot 저장 완료 상태
}

enum class CameraMode {
    NORMAL,       // 일반 촬영: 타임마크 오버레이 자동 합성 & 즉시 연속 저장 (프리뷰 유지)
    MEASUREMENT   // AI 계측: 촬영 후 Gemini AI 분석 및 캘리퍼스 미세조정 화면 진입
}

data class BassShotUiState(
    val appState: AppState = AppState.CAMERA_PREVIEW,
    val cameraMode: CameraMode = CameraMode.NORMAL,
    val environmentData: EnvironmentData = EnvironmentData(),
    val capturedOriginalBitmap: Bitmap? = null,
    val measurement: BassMeasurementUiModel? = null,
    val isManualMeasurementEnabled: Boolean = false,
    val isImportedPhoto: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val currentGloveWidthCm: Float = 9.5f,
    val currentSaveOriginal: Boolean = true,
    val savedPhotoUriString: String? = null,
    val errorMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val updateDownloadUrl: String? = null,
    val updateVersionName: String? = null,
    val savedPhotos: List<SavedPhotoItem> = emptyList(),
    val isPhotoViewerOpen: Boolean = false,
    val initialViewerPhotoIndex: Int = 0,
    val sourceViewerIndex: Int? = null,
    val showIssueReportDialog: Boolean = false,
    val issueReportType: IssueReportType = IssueReportType.MEASUREMENT_INACCURATE,
    val isSubmittingIssue: Boolean = false,
    val issueSubmitProgress: String? = null,
    val lastCreatedIssueUrl: String? = null,
    val issueReportOriginalBitmap: Bitmap? = null,
    val issueReportCompositeBitmap: Bitmap? = null,
    val issueReportEnvironmentData: EnvironmentData? = null,
    val issueReportMeasurement: BassMeasurementUiModel? = null
)
