package com.jongh0.bassshot.ui.camera

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.AppState
import com.jongh0.bassshot.domain.model.CameraMode
import com.jongh0.bassshot.domain.model.IssueReportType
import com.jongh0.bassshot.ui.components.IssueReportDialog
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
import com.jongh0.bassshot.ui.theme.SafetyOrange
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

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var containerWidthDp by remember { mutableFloatStateOf(360f) }
    val fontScale = LocalDensity.current.fontScale

    // 셔터 클릭 시 이미지 영역 전체가 살짝 투명한 흰색으로 번쩍이는 플래시 시각 피드백 (찰칵 셔터음 길이 약 150~160ms와 1:1 매칭)
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        viewModel.shutterFlashEvent.collect {
            flashAlpha.snapTo(0.70f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )
        }
    }

    val isResultSaved = uiState.appState == AppState.RESULT_SAVED
    val isAlreadySaved = uiState.savedPhotoUriString != null
    val isOtherOverlayOpen = uiState.isPhotoViewerOpen || uiState.showSettingsDialog || uiState.showUpdateDialog || uiState.showIssueReportDialog

    // 계측 화면(RESULT_SAVED)에서 시스템 뒤로가기 버튼 / 제스처 처리
    // 카메라 화면에서는 BackHandler가 비활성화되어 자연스럽게 앱이 종료됨
    BackHandler(enabled = isResultSaved && !isOtherOverlayOpen) {
        if (showDiscardConfirmDialog) {
            showDiscardConfirmDialog = false
        } else if (isAlreadySaved) {
            viewModel.onReturnFromMeasurement()
        } else {
            showDiscardConfirmDialog = true
        }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.appState == AppState.RESULT_SAVED) {
                        IconButton(
                            onClick = {
                                val reportType = if (uiState.measurement?.isBassDetected == true) {
                                    IssueReportType.MEASUREMENT_INACCURATE
                                } else {
                                    IssueReportType.MEASUREMENT_FAILED
                                }
                                viewModel.openIssueReportDialog(reportType)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "계측 오류 제보",
                                tint = SafetyOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

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
            }

            // 2. 중앙 뷰파인더 컨테이너 (불러온 사진의 경우 원본 사진 종횡비에 맞추어 캘리퍼스 1:1 매칭)
            val containerAspectRatio = if (uiState.isImportedPhoto && uiState.capturedOriginalBitmap != null) {
                val bmp = uiState.capturedOriginalBitmap!!
                (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.4f, 1.8f)
            } else {
                3f / 4f
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(containerAspectRatio)
                    .background(Color.Black)
            ) {
                containerWidthDp = maxWidth.value
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

                        // 타임마크 및 날씨 오버레이 (갤러리에서 불러온 사진이 아닐 때만 표시)
                        if (!uiState.isImportedPhoto) {
                            TimemarkOverlay(
                                environmentData = uiState.environmentData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 계측 캘리퍼스 HUD & 우상단 스펙 카드 (배스 검출 또는 수동 직접 계측 모드 시)
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
                    }
                }

                // 셔터 플래시(번쩍임) 시각 피드백 오버레이: 촬영 순간 이미지 영역 전체를 살짝 투명한 흰색으로 깜빡임
                if (flashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha.value))
                    )
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
                        val isNormalMode = uiState.cameraMode == CameraMode.NORMAL

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 모드 선택 캡슐 바 (일반 촬영 vs AI 계측)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SmokeGlass)
                                    .border(1.dp, SubtleTitanium, RoundedCornerShape(20.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // [일반 촬영] 탭
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isNormalMode) OpticalWhite else Color.Transparent)
                                        .clickable { viewModel.setCameraMode(CameraMode.NORMAL) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "일반 촬영",
                                        color = if (isNormalMode) ObsidianBlack else WarmGraphite,
                                        fontWeight = if (isNormalMode) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }

                                // [AI 계측] 탭
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (!isNormalMode) OpticalWhite else Color.Transparent)
                                        .clickable { viewModel.setCameraMode(CameraMode.MEASUREMENT) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AI 계측",
                                        color = if (!isNormalMode) ObsidianBlack else WarmGraphite,
                                        fontWeight = if (!isNormalMode) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 셔터 및 액션 컨트롤 행 (이전 사진 - 셔터 - 갤러리 불러오기)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. 좌측 이전 이미지 뷰어 버튼 (최신 저장 사진 썸네일)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.openPhotoViewer(0) },
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
                                        .clickable { viewModel.onShutterClicked(containerWidthDp, fontScale) }
                                )

                                // 3. 우측 갤러리 불러오기 버튼
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.5.dp, SubtleTitanium, RoundedCornerShape(10.dp))
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "갤러리 사진 불러오기",
                                        tint = TitaniumSilver,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "물고기/손 미인식",
                                        color = WarmGraphite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 미인식 제보 버튼
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { viewModel.openIssueReportDialog(IssueReportType.MEASUREMENT_FAILED) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BugReport,
                                                contentDescription = "미인식 제보",
                                                tint = SafetyOrange,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "오류 제보",
                                                color = SafetyOrange,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // 직접 계측 버튼
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { viewModel.enableManualMeasurement() }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "직접 계측",
                                                tint = TitaniumSilver,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "직접 계측",
                                                color = TitaniumSilver,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
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
                                // 다시 촬영 / 뷰어로 돌아가기 버튼
                                val returnButtonText = when {
                                    uiState.sourceViewerIndex != null -> "뷰어로 돌아가기"
                                    uiState.isImportedPhoto -> "돌아가기"
                                    else -> "다시 촬영"
                                }
                                val returnButtonIcon = if (uiState.isImportedPhoto) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.Default.Refresh
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isAlreadySaved) {
                                                viewModel.onReturnFromMeasurement()
                                            } else {
                                                showDiscardConfirmDialog = true
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = returnButtonIcon,
                                        contentDescription = returnButtonText,
                                        tint = MutedSilver,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = returnButtonText,
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
                                        .clickable(enabled = !uiState.isSaving) { viewModel.saveCurrentResult(containerWidthDp, fontScale) }
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
                onDismissRequest = { viewModel.dismissSettingsDialog() },
                onSaveSettings = { width, saveOrig ->
                    viewModel.updateSettings(width, saveOrig)
                },
                onReportGeneralIssue = {
                    viewModel.openIssueReportDialog(IssueReportType.GENERAL)
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

        // GitHub 이슈 제보 다이얼로그
        if (uiState.showIssueReportDialog) {
            IssueReportDialog(
                reportType = uiState.issueReportType,
                hasValidToken = viewModel.hasValidGitHubToken(),
                isSubmitting = uiState.isSubmittingIssue,
                submitProgress = uiState.issueSubmitProgress,
                createdIssueUrl = uiState.lastCreatedIssueUrl,
                errorMessage = uiState.errorMessage,
                hasMeasurement = (uiState.issueReportMeasurement ?: uiState.measurement) != null,
                hasOriginalPhoto = uiState.issueReportOriginalBitmap != null,
                hasCompositePhoto = uiState.issueReportCompositeBitmap != null,
                hasEnvironmentData = uiState.issueReportEnvironmentData != null,
                onDismissRequest = { viewModel.dismissIssueReportDialog() },
                onSubmit = { comment, actualLength, actualWeight, includeLogs ->
                    viewModel.submitIssueReport(
                        comment = comment,
                        actualLength = actualLength,
                        actualWeight = actualWeight,
                        includeLogs = includeLogs,
                        containerWidthDp = containerWidthDp,
                        fontScale = fontScale
                    )
                }
            )
        }

        // 사진 뷰어 다이얼로그 (DCIM/BassShot 앱 저장 사진 전용 스와이프 뷰어)
        if (uiState.isPhotoViewerOpen) {
            PhotoViewerDialog(
                photos = uiState.savedPhotos,
                initialIndex = uiState.initialViewerPhotoIndex,
                onDismissRequest = { viewModel.closePhotoViewer() },
                onDeletePhoto = { viewModel.deletePhoto(it) },
                onMeasurePhoto = { photo, index -> viewModel.measurePhotoFromViewer(photo, index) },
                onReportIssue = { photo -> viewModel.openIssueReportForSavedPhoto(photo) }
            )
        }

        // 사진 미저장 시 뒤로가기/다시촬영/뷰어복귀 확인 다이얼로그
        if (showDiscardConfirmDialog) {
            val isFromViewer = uiState.sourceViewerIndex != null
            val isImported = uiState.isImportedPhoto
            val dialogTitle = when {
                isFromViewer || isImported -> "계측을 취소하고 돌아갈까요?"
                else -> "사진을 저장하지 않고 나갈까요?"
            }
            val dialogText = when {
                isFromViewer -> "지금 뷰어로 돌아가면 현재 계측 결과가 저장되지 않습니다."
                isImported -> "지금 돌아가면 현재 계측 결과가 저장되지 않습니다."
                else -> "지금 카메라 화면으로 돌아가면 현재 촬영 및 계측 결과가 저장되지 않습니다."
            }
            val confirmButtonText = if (isFromViewer || isImported) "돌아가기" else "나가기"

            AlertDialog(
                onDismissRequest = { showDiscardConfirmDialog = false },
                title = {
                    Text(
                        text = dialogTitle,
                        color = OpticalWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = dialogText,
                        color = MutedSilver,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardConfirmDialog = false
                            viewModel.onReturnFromMeasurement()
                        }
                    ) {
                        Text(confirmButtonText, color = Color(0xFFFF5C5C), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardConfirmDialog = false }) {
                        Text("취소", color = WarmGraphite)
                    }
                },
                containerColor = AnodizedCharcoal,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
