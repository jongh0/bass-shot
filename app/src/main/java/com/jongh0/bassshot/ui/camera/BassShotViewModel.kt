package com.jongh0.bassshot.ui.camera

import android.app.Application
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jongh0.bassshot.data.local.UserPreferencesRepository
import com.jongh0.bassshot.data.repository.EnvironmentRepository
import com.jongh0.bassshot.data.repository.MeasurementRepository
import com.jongh0.bassshot.data.repository.MediaStoreRepository
import com.jongh0.bassshot.data.repository.UpdateRepository
import com.jongh0.bassshot.domain.model.AppState
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.BassShotUiState
import com.jongh0.bassshot.domain.util.BitmapProcessor
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.domain.util.HighResCanvasCompositor
import com.jongh0.bassshot.domain.util.ShutterSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class BassShotViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val measurementRepository = MeasurementRepository()
    private val environmentRepository = EnvironmentRepository(application)
    private val mediaStoreRepository = MediaStoreRepository(application)
    private val updateRepository = UpdateRepository(application)

    private val _uiState = MutableStateFlow(BassShotUiState())
    val uiState: StateFlow<BassShotUiState> = _uiState.asStateFlow()

    val cameraController = LifecycleCameraController(application).apply {
        imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }

    private val isCapturing = AtomicBoolean(false)

    init {
        startRealtimeClock()
        observeUserPreferences()
        checkForAppUpdates()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        isShutterSoundEnabled = prefs.shutterSoundEnabled,
                        currentGloveWidthCm = prefs.referenceGloveWidthCm,
                        currentSaveOriginal = prefs.saveOriginalPhoto
                    )
                }
            }
        }
    }

    /**
     * 1초마다 시계 갱신
     */
    private fun startRealtimeClock() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { current ->
                    current.copy(
                        environmentData = environmentRepository.createCurrentTimeData(current.environmentData)
                    )
                }
                delay(1000L)
            }
        }
    }

    /**
     * 위치 권한 상태에 따라 환경 정보(기압, 날씨, 주소, 지도) 갱신
     */
    fun refreshEnvironmentData(hasLocationPermission: Boolean) {
        viewModelScope.launch {
            val freshData = environmentRepository.fetchFreshEnvironmentData(hasLocationPermission)
            _uiState.update { it.copy(environmentData = freshData) }
        }
    }

    /**
     * 셔터 클릭: 즉시 프리뷰 고정 -> 리사이즈 후 Gemini 분석 -> 저장 대기
     */
    fun onShutterClicked() {
        if (_uiState.value.appState != AppState.CAMERA_PREVIEW || isCapturing.getAndSet(true)) {
            return
        }

        // 절제된 부드러운 셔터음 재생 (설정에서 켜져 있을 때만)
        if (_uiState.value.isShutterSoundEnabled) {
            ShutterSoundPlayer.playGentleShutterSound()
        }

        val executor = ContextCompat.getMainExecutor(getApplication())
        cameraController.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
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
                                    isAnalyzing = false
                                )
                            }

                            // 6. 기본 계측 결과로 즉시 갤러리 저장 실행
                            saveCurrentResult()
                        } catch (e: Exception) {
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

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    isCapturing.set(false)
                    _uiState.update { current ->
                        current.copy(errorMessage = "촬영에 실패했습니다: ${exception.localizedMessage}")
                    }
                }
            }
        )
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
                        isAnalyzing = false
                    )
                }

                // 5. 기본 결과로 DCIM 저장
                saveCurrentResult()
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
        _uiState.update { it.copy(measurement = updated) }
    }

    /**
     * 현재 화면의 최종 결과(타임마크 + 최신 계측선 + EXIF 헤더)를 고화질 Canvas로 합성하여 DCIM/BassShot에 저장
     */
    fun saveCurrentResult() {
        val original = _uiState.value.capturedOriginalBitmap ?: return
        val env = _uiState.value.environmentData
        val measure = _uiState.value.measurement
        val isImported = _uiState.value.isImportedPhoto

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isSaving = true) }

            // 원본 해상도 Canvas 직접 합성 (불러온 사진일 경우 타임마크 생략)
            val compositeBitmap = HighResCanvasCompositor.composite(
                originalBitmap = original,
                environmentData = env,
                measurement = measure,
                isImportedPhoto = isImported
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

            _uiState.update { current ->
                current.copy(
                    isSaving = false,
                    savedPhotoUriString = result.getOrNull()
                )
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
                measurement = fallback.copy(isBassDetected = true, isManualFallback = true)
            )
        }
        saveCurrentResult()
    }

    /**
     * 다시 촬영 (프리뷰 상태로 리셋)
     */
    fun onRetakeClicked() {
        _uiState.update { current ->
            current.copy(
                appState = AppState.CAMERA_PREVIEW,
                capturedOriginalBitmap = null,
                measurement = null,
                isManualMeasurementEnabled = false,
                isImportedPhoto = false,
                isAnalyzing = false,
                isSaving = false,
                savedPhotoUriString = null,
                errorMessage = null
            )
        }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun updateSettings(gloveWidthCm: Float, saveOriginal: Boolean, shutterSound: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateReferenceGloveWidth(gloveWidthCm)
            userPreferencesRepository.updateSaveOriginalPhoto(saveOriginal)
            userPreferencesRepository.updateShutterSound(shutterSound)
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
}
