package com.jongh0.bassshot.ui.camera

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jongh0.bassshot.data.local.UserPreferencesRepository
import com.jongh0.bassshot.data.repository.EnvironmentRepository
import com.jongh0.bassshot.data.repository.IssueReportRepository
import com.jongh0.bassshot.data.repository.MeasurementRepository
import com.jongh0.bassshot.data.repository.MediaStoreRepository
import com.jongh0.bassshot.data.repository.UpdateRepository
import com.jongh0.bassshot.domain.model.AppState
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.BassShotUiState
import com.jongh0.bassshot.domain.model.CameraMode
import com.jongh0.bassshot.domain.model.IssueReportInput
import com.jongh0.bassshot.domain.model.IssueReportType
import com.jongh0.bassshot.domain.model.SavedPhotoItem
import com.jongh0.bassshot.domain.util.AppLogger
import com.jongh0.bassshot.domain.util.BitmapProcessor
import com.jongh0.bassshot.domain.util.FishExifHelper
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.domain.util.HighResCanvasCompositor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class BassShotViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val measurementRepository = MeasurementRepository()
    private val environmentRepository = EnvironmentRepository(application)
    private val mediaStoreRepository = MediaStoreRepository(application)
    private val updateRepository = UpdateRepository(application)
    private val issueReportRepository = IssueReportRepository(application)

    private val _uiState = MutableStateFlow(BassShotUiState())
    val uiState: StateFlow<BassShotUiState> = _uiState.asStateFlow()

    private val _shutterFlashEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shutterFlashEvent: SharedFlow<Unit> = _shutterFlashEvent.asSharedFlow()

    val cameraController = LifecycleCameraController(application).apply {
        imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }

    private val isCapturing = AtomicBoolean(false)

    init {
        startRealtimeClock()
        observeUserPreferences()
        checkForAppUpdates()
        loadSavedPhotos()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                val savedMode = runCatching { CameraMode.valueOf(prefs.cameraMode) }.getOrDefault(CameraMode.NORMAL)
                _uiState.update { current ->
                    current.copy(
                        currentGloveWidthCm = prefs.referenceGloveWidthCm,
                        currentSaveOriginal = prefs.saveOriginalPhoto,
                        cameraMode = savedMode
                    )
                }
            }
        }
    }

    /**
     * 촬영 모드 변경 (일반 촬영 / AI 계측) 및 사용자 설정 저장
     */
    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.cameraMode == mode) return
        AppLogger.i("ViewModel", "Switching camera mode to: $mode")
        _uiState.update { it.copy(cameraMode = mode) }
        viewModelScope.launch {
            userPreferencesRepository.updateCameraMode(mode.name)
        }
    }

    private fun checkHasLocationPermission(): Boolean {
        val app = getApplication<Application>()
        return ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 1초마다 시계 갱신 (프리뷰 상태일 때만 현재 시간으로 갱신하여 결과 확인 중인 사진 시간 보존)
     */
    private fun startRealtimeClock() {
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.appState == AppState.CAMERA_PREVIEW) {
                    _uiState.update { current ->
                        current.copy(
                            environmentData = environmentRepository.createCurrentTimeData(current.environmentData)
                        )
                    }
                }
                delay(1000L)
            }
        }
    }

    /**
     * 위치 권한 상태에 따라 환경 정보(기압, 날씨, 주소, 지도) 갱신
     * (카메라 프리뷰 상태일 때만 최신 정보로 갱신하여 결과 확인 중인 사진의 메타데이터 보존)
     */
    fun refreshEnvironmentData(hasLocationPermission: Boolean = checkHasLocationPermission()) {
        if (_uiState.value.appState != AppState.CAMERA_PREVIEW) return

        viewModelScope.launch {
            val freshData = environmentRepository.fetchFreshEnvironmentData(hasLocationPermission)
            if (_uiState.value.appState == AppState.CAMERA_PREVIEW) {
                _uiState.update { it.copy(environmentData = freshData) }
            }
        }
    }

    /**
     * 셔터 클릭: 촬영 모드(일반 촬영 vs AI 계측)에 따른 분기 처리
     * @param containerWidthDp 화면(Compose)에서 이미지가 표시된 컨테이너의 실제 dp 폭
     * @param fontScale 단말기 시스템 글꼴 크기 배율
     */
    fun onShutterClicked(containerWidthDp: Float = 360f, fontScale: Float = 1.0f) {
        if (_uiState.value.appState != AppState.CAMERA_PREVIEW || isCapturing.getAndSet(true)) {
            return
        }

        // 촬영 시각 피드백 (뷰파인더 흰색 플래시/번쩍임 애니메이션 이벤트 트리거)
        _shutterFlashEvent.tryEmit(Unit)

        val currentMode = _uiState.value.cameraMode
        AppLogger.i("ViewModel", "Shutter clicked in mode: $currentMode")
        val executor = ContextCompat.getMainExecutor(getApplication())
        cameraController.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    AppLogger.d("ViewModel", "Image capture success (format: ${image.format}, ${image.width}x${image.height})")
                    if (currentMode == CameraMode.NORMAL) {
                        handleNormalCaptureSuccess(image, containerWidthDp, fontScale)
                    } else {
                        handleMeasurementCaptureSuccess(image)
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    isCapturing.set(false)
                    AppLogger.e("ViewModel", "Camera takePicture failed: ${exception.message}", exception)
                    _uiState.update { current ->
                        current.copy(errorMessage = "촬영에 실패했습니다: ${exception.localizedMessage}")
                    }
                }
            }
        )
    }

    /**
     * 일반 촬영 모드: 화면 멈춤 없이 백그라운드에서 타임마크 고해상도 합성 및 갤러리 자동 저장
     * (사용자는 대기 없이 계속 다음 사진 연속 촬영 가능)
     */
    private fun handleNormalCaptureSuccess(
        image: ImageProxy,
        containerWidthDp: Float,
        fontScale: Float
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. 센서 방향 및 3:4 비율이 보정된 고해상도 원본 비트맵 획득
                val fullResBitmap = image.use { proxy ->
                    BitmapProcessor.processImageProxy(proxy)
                }

                // 2. 하드웨어 캡처 플래그 즉시 해제 -> 다음 촬영 바로 가능
                isCapturing.set(false)

                // 3. 고해상도 타임마크 합성 (계측선 없이 오직 환경 타임마크 정보만)
                val env = _uiState.value.environmentData
                val compositeBitmap = HighResCanvasCompositor.composite(
                    originalBitmap = fullResBitmap,
                    environmentData = env,
                    measurement = null,
                    isImportedPhoto = false,
                    containerWidthDp = containerWidthDp,
                    fontScale = fontScale,
                    context = getApplication()
                )

                // 4. 갤러리(DCIM/BassShot) 자동 저장
                val prefs = userPreferencesRepository.userPreferencesFlow.firstOrNull()
                val saveOriginal = prefs?.saveOriginalPhoto ?: true

                val result = mediaStoreRepository.saveCompositeAndOriginalPhotos(
                    compositeBitmap = compositeBitmap,
                    originalBitmap = fullResBitmap,
                    saveOriginalPhoto = saveOriginal,
                    environmentData = env,
                    measurement = null
                )

                val savedUri = result.getOrNull()
                if (savedUri != null) {
                    loadSavedPhotos()
                }
            } catch (e: Exception) {
                isCapturing.set(false)
                AppLogger.e("ViewModel", "Normal capture background processing failed: ${e.message}", e)
                _uiState.update { current ->
                    current.copy(errorMessage = "사진 저장 중 오류가 발생했습니다: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * AI 계측 촬영 모드: 프리뷰 정지 후 Gemini 분석 진행 -> 계측 캘리퍼스 미세조정 화면 진입
     */
    private fun handleMeasurementCaptureSuccess(image: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. 센서 방향 및 3:4 비율이 보정된 고해상도 원본 비트맵 획득
                val fullResBitmap = image.use { proxy ->
                    BitmapProcessor.processImageProxy(proxy)
                }

                // 2. 촬영 완료 즉시 화면 정지
                _uiState.update { current ->
                    current.copy(
                        appState = AppState.ANALYZING,
                        capturedOriginalBitmap = fullResBitmap,
                        isImportedPhoto = false,
                        isAnalyzing = true,
                        errorMessage = null
                    )
                }

                // 3. Gemini API 전송용 1024px 다운스케일 생성
                val resizedForAi = BitmapProcessor.scaleForGemini(fullResBitmap, 1024)

                // 4. 저장된 장갑 폭 설정 조회
                val gloveWidth = _uiState.value.currentGloveWidthCm

                // 5. Gemini AI 분석 요청
                val measurementResult = measurementRepository.measureBass(
                    resizedBitmap = resizedForAi,
                    referenceGloveWidthCm = gloveWidth,
                    imageAspectRatio = 3f / 4f
                )

                _uiState.update { current ->
                    current.copy(
                        appState = AppState.RESULT_SAVED,
                        measurement = measurementResult,
                        isAnalyzing = false,
                        savedPhotoUriString = null
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("ViewModel", "Measurement capture/analysis failed: ${e.message}", e)
                _uiState.update { current ->
                    current.copy(
                        isAnalyzing = false,
                        errorMessage = "분석 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            } finally {
                isCapturing.set(false)
            }
        }
    }

    /**
     * 사진 뷰어에서 현재 보고 있는 사진을 즉시 AI 계측 모드로 전환하여 분석
     */
    fun measurePhotoFromViewer(photoItem: SavedPhotoItem, index: Int = 0) {
        _uiState.update { it.copy(sourceViewerIndex = index) }
        closePhotoViewer()
        processImportedPhoto(photoItem.uri)
    }

    /**
     * 기촬영된 갤러리 사진 불러오기 후 AI 계측 분석 진행 (비파괴 원본 비율 보존)
     */
    fun processImportedPhoto(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _uiState.update { current ->
                    current.copy(
                        appState = AppState.ANALYZING,
                        isImportedPhoto = true,
                        isAnalyzing = true,
                        errorMessage = null
                    )
                }

                // 1. 갤러리 비트맵 로드 및 EXIF 메타데이터 추출 (크롭 없이 원본 비율 보존)
                val imported = BitmapProcessor.decodeUriWithExif(getApplication(), uri)
                val fullResBitmap = imported.bitmap
                val photoAspect = fullResBitmap.width.toFloat() / fullResBitmap.height.toFloat()

                // 2. 원본 사진의 일시/위치가 있으면 환경 데이터에 반영
                if (imported.capturedDateFormatted != null && imported.capturedTimeFormatted != null) {
                    _uiState.update { current ->
                        current.copy(
                            environmentData = current.environmentData.copy(
                                dateFormatted = imported.capturedDateFormatted,
                                dayOfWeekFormatted = imported.capturedDayOfWeekFormatted ?: current.environmentData.dayOfWeekFormatted,
                                timeFormatted = imported.capturedTimeFormatted,
                                latitude = imported.latitude ?: current.environmentData.latitude,
                                longitude = imported.longitude ?: current.environmentData.longitude
                            )
                        )
                    }
                }

                _uiState.update { it.copy(capturedOriginalBitmap = fullResBitmap) }

                // 3. Gemini 전송용 1024px 스케일링
                val resizedForAi = BitmapProcessor.scaleForGemini(fullResBitmap, 1024)
                val gloveWidth = _uiState.value.currentGloveWidthCm

                // 4. Gemini AI 분석 실행 (사진의 실제 종횡비 반영)
                val measurementResult = measurementRepository.measureBass(
                    resizedBitmap = resizedForAi,
                    referenceGloveWidthCm = gloveWidth,
                    imageAspectRatio = photoAspect
                )

                _uiState.update { current ->
                    current.copy(
                        appState = AppState.RESULT_SAVED,
                        measurement = measurementResult,
                        isAnalyzing = false,
                        savedPhotoUriString = null
                    )
                }
                // 사용자가 직접 '저장' 버튼을 누를 때 저장되도록 자동 저장 제거
            } catch (e: Exception) {
                val photoAspect = _uiState.value.capturedOriginalBitmap?.let { 
                    it.width.toFloat() / it.height.toFloat() 
                } ?: (3f / 4f)
                _uiState.update { current ->
                    current.copy(
                        appState = AppState.RESULT_SAVED,
                        isAnalyzing = false,
                        measurement = FishMeasurementCalculator.createManualFallback(photoAspect),
                        errorMessage = "AI 분석에 실패했습니다. 직접 계측을 이용해주세요: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * 사용자가 캘리퍼스 가이드라인을 드래그하여 수정한 계측값 반영
     */
    fun onMeasurementUpdated(updated: BassMeasurementUiModel) {
        _uiState.update { 
            it.copy(
                measurement = updated,
                savedPhotoUriString = null // 계측값 변경 시 다시 '저장' 버튼을 누를 수 있도록 저장 상태 리셋
            ) 
        }
    }

    /**
     * 현재 화면의 최종 결과(타임마크 + 최신 계측선 + EXIF 헤더)를 고화질 Canvas로 합성하여 DCIM/BassShot에 저장
     */
    fun saveCurrentResult(
        containerWidthDp: Float = 360f,
        fontScale: Float = 1.0f
    ) {
        val original = _uiState.value.capturedOriginalBitmap ?: return
        val env = _uiState.value.environmentData
        val measure = _uiState.value.measurement
        val isImported = _uiState.value.isImportedPhoto

        viewModelScope.launch(Dispatchers.Default) {
            AppLogger.i("ViewModel", "saveCurrentResult started (isImported: $isImported, containerWidthDp: $containerWidthDp)")
            _uiState.update { it.copy(isSaving = true) }

            // 원본 해상도 Canvas 직접 합성 (화면과 1:1 동일한 scaleDp 및 fontScale 반영)
            val compositeBitmap = HighResCanvasCompositor.composite(
                originalBitmap = original,
                environmentData = env,
                measurement = measure,
                isImportedPhoto = isImported,
                containerWidthDp = containerWidthDp,
                fontScale = fontScale,
                context = getApplication()
            )

            val prefs = userPreferencesRepository.userPreferencesFlow.firstOrNull()
            val userSaveOriginalPref = prefs?.saveOriginalPhoto ?: true

            // 기촬영된 갤러리 사진을 불러온 경우, 원본이 이미 갤러리에 있으므로 중복 백업 방지
            val saveOriginal = userSaveOriginalPref && !isImported

            val result = mediaStoreRepository.saveCompositeAndOriginalPhotos(
                compositeBitmap = compositeBitmap,
                originalBitmap = original,
                saveOriginalPhoto = saveOriginal,
                environmentData = env,
                measurement = measure
            )

            val savedUri = result.getOrNull()
            _uiState.update { current ->
                current.copy(
                    isSaving = false,
                    savedPhotoUriString = savedUri
                )
            }

            if (savedUri != null) {
                loadSavedPhotos()
            }

            withContext(Dispatchers.Main) {
                if (savedUri != null) {
                    android.widget.Toast.makeText(getApplication(), "갤러리에 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                    android.widget.Toast.makeText(getApplication(), "저장에 실패했습니다: $err", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 사용자가 수동 계측 모드를 활성화한 경우
     */
    fun enableManualMeasurement() {
        val aspect = _uiState.value.capturedOriginalBitmap?.let {
            it.width.toFloat() / it.height.toFloat()
        } ?: (3f / 4f)
        _uiState.update { current ->
            val fallback = current.measurement ?: FishMeasurementCalculator.createManualFallback(aspect)
            current.copy(
                isManualMeasurementEnabled = true,
                measurement = fallback.copy(isBassDetected = true, isManualFallback = true),
                savedPhotoUriString = null
            )
        }
    }

    /**
     * 계측 화면에서 이전 화면(뷰어 또는 카메라 프리뷰)으로 복귀
     */
    fun onReturnFromMeasurement() {
        val viewerIndex = _uiState.value.sourceViewerIndex
        val isSaved = _uiState.value.savedPhotoUriString != null

        _uiState.update { current ->
            current.copy(
                appState = AppState.CAMERA_PREVIEW,
                capturedOriginalBitmap = null,
                measurement = null,
                isManualMeasurementEnabled = false,
                isImportedPhoto = false,
                sourceViewerIndex = null,
                isAnalyzing = false,
                isSaving = false,
                savedPhotoUriString = null,
                errorMessage = null
            )
        }
        refreshEnvironmentData()

        if (viewerIndex != null) {
            // 뷰어에서 온 경우: 저장을 마쳤으면 최신 저장 사진(0번)으로, 저장하지 않고 복귀했으면 원래 보던 인덱스로 복귀
            val targetIndex = if (isSaved) 0 else viewerIndex
            openPhotoViewer(targetIndex)
        }
    }

    /**
     * 다시 촬영 (프리뷰 상태로 리셋)
     */
    fun onRetakeClicked() {
        onReturnFromMeasurement()
    }

    fun openSettingsDialog() {
        if (_uiState.value.appState != AppState.CAMERA_PREVIEW) return
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    /**
     * DCIM/BassShot 에 저장된 사진 목록 조회
     */
    fun loadSavedPhotos() {
        viewModelScope.launch {
            val photos = mediaStoreRepository.queryBassShotPhotos()
            _uiState.update { it.copy(savedPhotos = photos) }
        }
    }

    /**
     * 사진 뷰어 열기
     */
    fun openPhotoViewer(startIndex: Int = 0) {
        if (_uiState.value.savedPhotos.isEmpty()) {
            loadSavedPhotos()
        }
        _uiState.update {
            it.copy(
                isPhotoViewerOpen = true,
                initialViewerPhotoIndex = startIndex.coerceAtLeast(0)
            )
        }
    }

    /**
     * 사진 뷰어 닫기
     */
    fun closePhotoViewer() {
        _uiState.update { it.copy(isPhotoViewerOpen = false) }
    }

    /**
     * 사진 뷰어에서 사진 삭제
     */
    fun deletePhoto(item: SavedPhotoItem) {
        viewModelScope.launch {
            val result = mediaStoreRepository.deletePhoto(item.uri)
            if (result.isSuccess) {
                val updatedList = _uiState.value.savedPhotos.filter { it.id != item.id }
                _uiState.update {
                    it.copy(
                        savedPhotos = updatedList,
                        isPhotoViewerOpen = updatedList.isNotEmpty()
                    )
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "사진이 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "사진 삭제에 실패했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateSettings(gloveWidthCm: Float, saveOriginal: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateReferenceGloveWidth(gloveWidthCm)
            userPreferencesRepository.updateSaveOriginalPhoto(saveOriginal)
            _uiState.update { it.copy(showSettingsDialog = false) }
        }
    }

    private fun checkForAppUpdates() {
        viewModelScope.launch {
            val latestRelease = updateRepository.checkForUpdates()
            if (latestRelease != null) {
                val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    _uiState.update { current ->
                        current.copy(
                            showUpdateDialog = true,
                            updateDownloadUrl = apkAsset.downloadUrl,
                            updateVersionName = latestRelease.tagName
                        )
                    }
                }
            }
        }
    }

    fun confirmAppUpdate() {
        val url = _uiState.value.updateDownloadUrl ?: return
        val version = _uiState.value.updateVersionName ?: "latest"
        updateRepository.startDownloadAndInstall(url, version)
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun hasValidGitHubToken(): Boolean = issueReportRepository.hasValidToken()

    /**
     * GitHub 이슈 제보 다이얼로그 열기 (계측 결과 화면 / 일반 피드백)
     */
    fun openIssueReportDialog(type: IssueReportType = IssueReportType.MEASUREMENT_INACCURATE) {
        val currentState = _uiState.value
        val isGeneral = (type == IssueReportType.GENERAL)
        _uiState.update {
            it.copy(
                showIssueReportDialog = true,
                issueReportType = type,
                // 일반 피드백일 때는 촬영 사진이나 계측 데이터를 자동으로 첨부하지 않고 기기정보/로그만 전송
                issueReportOriginalBitmap = if (isGeneral) null else currentState.capturedOriginalBitmap,
                issueReportCompositeBitmap = null,
                issueReportEnvironmentData = if (isGeneral) null else currentState.environmentData,
                issueReportMeasurement = if (isGeneral) null else currentState.measurement,
                lastCreatedIssueUrl = null,
                issueSubmitProgress = null
            )
        }
    }

    /**
     * 사진 뷰어에서 현재 사진을 이슈 제보용으로 열기
     */
    fun openIssueReportForSavedPhoto(item: SavedPhotoItem) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. 현재 사진 비트맵 디코딩
                val currentPhotoBitmap = BitmapProcessor.decodeUriWithExif(getApplication(), item.uri).bitmap

                // 2. 짝꿍 무보정 원본(_ORIG) 사진이 갤러리에 있는지 조회
                val origUri = mediaStoreRepository.findOriginalPhotoUri(item.displayName)
                val origBitmap = if (origUri != null) {
                    runCatching { BitmapProcessor.decodeUriWithExif(getApplication(), origUri).bitmap }.getOrNull()
                } else null

                // 3. 사진의 EXIF로부터 당시 촬영 환경 및 계측 데이터 복원
                val photoEnv = FishExifHelper.extractEnvironmentData(getApplication(), item.uri)
                val photoMeasurement = FishExifHelper.extractMeasurementData(getApplication(), item.uri)

                _uiState.update {
                    it.copy(
                        showIssueReportDialog = true,
                        issueReportType = IssueReportType.SAVED_PHOTO,
                        // 원본이 실제로 있으면 원본+합성본 2장, 없으면 해당 사진 1장만 전달
                        issueReportOriginalBitmap = origBitmap,
                        issueReportCompositeBitmap = currentPhotoBitmap,
                        issueReportEnvironmentData = photoEnv,
                        issueReportMeasurement = photoMeasurement,
                        lastCreatedIssueUrl = null,
                        issueSubmitProgress = null
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("ViewModel", "Failed to decode photo for issue report: ${e.message}", e)
            }
        }
    }

    fun dismissIssueReportDialog() {
        _uiState.update {
            it.copy(
                showIssueReportDialog = false,
                isSubmittingIssue = false,
                issueSubmitProgress = null,
                lastCreatedIssueUrl = null,
                issueReportOriginalBitmap = null,
                issueReportCompositeBitmap = null,
                issueReportEnvironmentData = null,
                issueReportMeasurement = null
            )
        }
    }

    /**
     * 이슈 리포트 전송 실행
     */
    fun submitIssueReport(
        comment: String,
        actualLength: Float?,
        actualWeight: Int?,
        includeLogs: Boolean,
        containerWidthDp: Float = 360f,
        fontScale: Float = 1.0f
    ) {
        val currentState = _uiState.value
        val reportType = currentState.issueReportType
        val env = currentState.issueReportEnvironmentData ?: currentState.environmentData
        val measurement = currentState.issueReportMeasurement ?: currentState.measurement

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isSubmittingIssue = true,
                    issueSubmitProgress = "이미지 준비 중...",
                    errorMessage = null
                )
            }

            try {
                // 비트맵 결정:
                // 1) 뷰어에서 지정된 비트맵이 있는 경우:
                //    - 짝꿍 원본이 있으면 orig, 없으면 null! 합성본 자리에 해당 사진 1장!
                // 2) 계측 화면에서 방금 촬영한 경우:
                //    - originalBitmap은 무보정 원본!
                //    - compositeBitmap은 계측/타임마크를 합성한 고해상도 비트맵!
                val originalBitmap: Bitmap?
                val compositeBitmap: Bitmap?

                if (currentState.issueReportCompositeBitmap != null) {
                    originalBitmap = currentState.issueReportOriginalBitmap
                    compositeBitmap = currentState.issueReportCompositeBitmap
                } else {
                    originalBitmap = currentState.issueReportOriginalBitmap ?: currentState.capturedOriginalBitmap
                    compositeBitmap = if (originalBitmap != null) {
                        HighResCanvasCompositor.composite(
                            originalBitmap = originalBitmap,
                            environmentData = env,
                            measurement = measurement,
                            isImportedPhoto = currentState.isImportedPhoto,
                            containerWidthDp = containerWidthDp,
                            fontScale = fontScale,
                            context = getApplication()
                        )
                    } else null
                }

                val input = IssueReportInput(
                    reportType = reportType,
                    userComment = comment,
                    actualLengthCm = actualLength,
                    actualWeightG = actualWeight,
                    originalBitmap = originalBitmap,
                    compositeBitmap = compositeBitmap,
                    measurement = measurement,
                    environmentData = env,
                    includeLogs = includeLogs
                )

                val result = issueReportRepository.submitReport(input) { progress ->
                    _uiState.update { it.copy(issueSubmitProgress = progress) }
                }

                if (result.isSuccess) {
                    val issueResponse = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            isSubmittingIssue = false,
                            issueSubmitProgress = null,
                            lastCreatedIssueUrl = issueResponse.htmlUrl
                        )
                    }
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                    _uiState.update {
                        it.copy(
                            isSubmittingIssue = false,
                            issueSubmitProgress = null,
                            errorMessage = "이슈 등록 실패: $err"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingIssue = false,
                        issueSubmitProgress = null,
                        errorMessage = "이슈 전송 중 오류 발생: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
