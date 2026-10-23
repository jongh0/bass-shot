package com.jongh0.bassshot.ui.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.AppState
import com.jongh0.bassshot.ui.components.MeasurementOverlay
import com.jongh0.bassshot.ui.components.PhotoViewerDialog
import com.jongh0.bassshot.ui.components.SettingsDialog
import com.jongh0.bassshot.ui.components.TimemarkOverlay
import com.jongh0.bassshot.ui.components.UpdateDialog
import coil.compose.AsyncImage
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SmokeGlass
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite

@Composable
fun BassShotScreen(
    viewModel: BassShotViewModel,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 갤러리 기촬영 사진 선택 런처 (Jetpack Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.processImportedPhoto(uri)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        viewModel.refreshEnvironmentData(hasLocationPermission)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 상단 HUD 컨트롤 바 (상태바/노치 안전 패딩 적용)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BASS SHOT",
                    color = OpticalWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 1.8.sp
                )

                val isCameraPreview = uiState.appState == AppState.CAMERA_PREVIEW
                IconButton(
                    onClick = { viewModel.openSettingsDialog() },
                    enabled = isCameraPreview,
                    modifier = Modifier
                        .size(36.dp)
                        .alpha(if (isCameraPreview) 1.0f else 0.35f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = MutedSilver
                    )
                }
            }

            // 2. 중앙 뷰파인더 컨테이너 (불러온 사진의 경우 원본 사진 종횡비에 맞추어 캘리퍼스 1:1 매칭)
            val containerAspectRatio = if (uiState.isImportedPhoto && uiState.capturedOriginalBitmap != null) {
                val bmp = uiState.capturedOriginalBitmap!!
                (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.4f, 1.8f)
            } else {
                3f / 4f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(containerAspectRatio)
                    .background(Color.Black)
            ) {
                when (uiState.appState) {
                    AppState.CAMERA_PREVIEW -> {
                        // 실시간 카메라 프리뷰
                        CameraPreviewContent(
                            cameraController = viewModel.cameraController,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 3x3 촬영 가이드라인 격자 (Timemarker 스타일)
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val gridColor = Color(0x26FFFFFF) // 15% opacity white
                            val stroke = 1.dp.toPx()

                            // 세로선 2개
                            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), stroke)
                            drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), stroke)

                            // 가로선 2개
                            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), stroke)
                            drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), stroke)
                        }

                        // 실시간 타임마크 및 날씨 오버레이
                        TimemarkOverlay(
                            environmentData = uiState.environmentData,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppState.ANALYZING -> {
                        // 캡처 정지 화면
                        uiState.capturedOriginalBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // 실시간 타임마크 및 날씨 오버레이 (갤러리에서 불러온 사진이 아닐 때만 표시)
                        if (!uiState.isImportedPhoto) {
                            TimemarkOverlay(
                                environmentData = uiState.environmentData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // AI 분석 중 로딩 HUD
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x88000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SmokeGlass)
                                    .border(1.dp, SubtleTitanium, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 24.dp, vertical = 20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = OpticalWhite,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "AI 배스 계측 분석 중",
                                    color = MutedSilver,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    AppState.RESULT_SAVED -> {
                        // 캡처 이미지
                        uiState.capturedOriginalBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Measured Bass",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 계측 캘리퍼스 HUD & 상단 스펙 뱃지 (실제 배스가 검출되었거나 직접 수동 계측 모드를 켰을 때만 표시)
                        val showMeasurement = uiState.measurement != null && 
                            (uiState.measurement!!.isBassDetected || uiState.isManualMeasurementEnabled)

                        if (showMeasurement) {
                            uiState.measurement?.let { measurement ->
                                MeasurementOverlay(
                                    measurement = measurement,
                                    hasWeatherAbove = !uiState.isImportedPhoto,
                                    onMeasurementUpdated = { updated ->
                                        viewModel.onMeasurementUpdated(updated)
                                    }
                                )
                            }
                        }

                        // 타임마크 및 날씨 오버레이 (갤러리에서 불러온 사진이 아닐 때만 표시)
                        if (!uiState.isImportedPhoto) {
                            TimemarkOverlay(
                                environmentData = uiState.environmentData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // 3. 하단 액션 컨트롤 바 (검정색 여백 내 수직 중앙 정렬)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.appState) {
                    AppState.CAMERA_PREVIEW -> {
                        val latestSavedPhoto = uiState.savedPhotos.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 좌측 이전 이미지 뷰어 버튼 (최신 저장 사진 썸네일 + "사진" 레이블)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.openPhotoViewer(0) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (latestSavedPhoto != null) {
                                        AsyncImage(
                                            model = latestSavedPhoto.uri,
                                            contentDescription = "최근 저장 사진",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "저장된 사진 없음",
                                            tint = TitaniumSilver,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "사진",
                                    color = MutedSilver,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 2. 중앙 럭셔리 셔터 버튼
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(2.dp, OpticalWhite, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(OpticalWhite)
                                    .clickable { viewModel.onShutterClicked() }
                            )

                            // 3. 우측 갤러리 불러오기 버튼 ("불러오기" 레이블)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "갤러리 사진 불러오기",
                                        tint = TitaniumSilver,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "불러오기",
                                    color = MutedSilver,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    AppState.ANALYZING -> {
                        val latestSavedPhoto = uiState.savedPhotos.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .alpha(0.35f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 좌측 이전 이미지 버튼 (딤처리 비활성)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (latestSavedPhoto != null) {
                                        AsyncImage(
                                            model = latestSavedPhoto.uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = TitaniumSilver,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "사진",
                                    color = MutedSilver,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 2. 중앙 셔터 버튼 (딤처리 비활성)
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(2.dp, OpticalWhite, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(OpticalWhite)
                            )

                            // 3. 우측 갤러리 불러오기 버튼 (딤처리 비활성)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = TitaniumSilver,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "불러오기",
                                    color = MutedSilver,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    AppState.RESULT_SAVED -> {
                        val isBassDetected = uiState.measurement?.isBassDetected == true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 배스 미검출 시 상단 수동 직접 계측 진입 배너 (2층 구조 분리로 하단 버튼 찌그러짐 방지)
                            if (!isBassDetected && !uiState.isManualMeasurementEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.enableManualMeasurement() }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "물고기/손 미인식",
                                        color = WarmGraphite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "직접 계측",
                                            tint = TitaniumSilver,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "직접 계측",
                                            color = TitaniumSilver,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // 하단 메인 2개 버튼: 50:50 균등 분할로 폰 화면 너비에 상관없이 절대 찌그러지지 않음
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 다시 촬영 버튼
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onRetakeClicked() }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "다시 촬영",
                                        tint = MutedSilver,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "다시 촬영",
                                        color = MutedSilver,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // 수동 갤러리 저장 버튼
                                val isAlreadySaved = uiState.savedPhotoUriString != null
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TitaniumSilver)
                                        .clickable(enabled = !uiState.isSaving) { viewModel.saveCurrentResult() }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "저장",
                                        tint = ObsidianBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            uiState.isSaving -> "저장 중..."
                                            isAlreadySaved -> "저장 완료"
                                            else -> "저장"
                                        },
                                        color = ObsidianBlack,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 설정 다이얼로그
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                currentGloveWidthCm = uiState.currentGloveWidthCm,
                currentSaveOriginal = uiState.currentSaveOriginal,
                currentShutterSound = uiState.isShutterSoundEnabled,
                onDismissRequest = { viewModel.dismissSettingsDialog() },
                onSaveSettings = { width, saveOrig, shutterSound ->
                    viewModel.updateSettings(width, saveOrig, shutterSound)
                }
            )
        }

        // 인앱 업데이트 다이얼로그
        if (uiState.showUpdateDialog && uiState.updateVersionName != null) {
            UpdateDialog(
                versionName = uiState.updateVersionName!!,
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                onConfirmUpdate = { viewModel.confirmAppUpdate() }
            )
        }

        // 사진 뷰어 다이얼로그 (DCIM/BassShot 앱 저장 사진 전용 스와이프 뷰어)
        if (uiState.isPhotoViewerOpen) {
            PhotoViewerDialog(
                photos = uiState.savedPhotos,
                initialIndex = uiState.initialViewerPhotoIndex,
                onDismissRequest = { viewModel.closePhotoViewer() },
                onDeletePhoto = { viewModel.deletePhoto(it) }
            )
        }
    }
}
