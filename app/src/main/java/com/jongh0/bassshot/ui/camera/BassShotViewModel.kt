package com.jongh0.bassshot.ui.camera

import android.app.Application
import android.graphics.Bitmap
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
import com.jongh0.bassshot.domain.util.HighResCanvasCompositor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class BassShotViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val environmentRepository = EnvironmentRepository(application, userPreferencesRepository)
    private val measurementRepository = MeasurementRepository()
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
        checkForAppUpdates()
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

                            // 2. 촬영 완료 즉시 화면 정지 (배스를 바로 방생할 수 있도록)
                            _uiState.update { current ->
                                current.copy(
                                    appState = AppState.ANALYZING,
                                    capturedOriginalBitmap = fullResBitmap,
                                    isAnalyzing = true,
                                    errorMessage = null
                                )
                            }

                            // 3. Gemini API 전송용 1024px 다운스케일 생성
                            val resizedForAi = BitmapProcessor.scaleForGemini(fullResBitmap, 1024)

                            // 4. 저장된 장갑 폭 설정 조회
                            val prefs = userPreferencesRepository.userPreferencesFlow.firstOrNull()
                            val gloveWidth = prefs?.referenceGloveWidthCm ?: 9.5f

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
     * 사용자가 캘리퍼스 가이드라인을 드래그하여 수정한 계측값 반영
     */
    fun onMeasurementUpdated(updated: BassMeasurementUiModel) {
        _uiState.update { it.copy(measurement = updated) }
    }

    /**
     * 현재 화면의 최종 결과(타임마크 + 최신 계측선)를 고화질 Canvas로 합성하여 DCIM/BassShot에 저장
     */
    fun saveCurrentResult() {
        val original = _uiState.value.capturedOriginalBitmap ?: return
        val env = _uiState.value.environmentData
        val measure = _uiState.value.measurement

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isSaving = true) }

            // 원본 해상도 Canvas 직접 합성
            val compositeBitmap = HighResCanvasCompositor.composite(original, env, measure)

            val prefs = userPreferencesRepository.userPreferencesFlow.firstOrNull()
            val saveOriginal = prefs?.saveOriginalPhoto ?: false

            val result = mediaStoreRepository.saveCompositeAndOriginalPhotos(
                compositeBitmap = compositeBitmap,
                originalBitmap = original,
                saveOriginalPhoto = saveOriginal
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
     * 다시 촬영 (프리뷰 상태로 리셋)
     */
    fun onRetakeClicked() {
        _uiState.update { current ->
            current.copy(
                appState = AppState.CAMERA_PREVIEW,
                capturedOriginalBitmap = null,
                measurement = null,
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
}
