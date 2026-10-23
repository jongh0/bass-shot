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
import com.jongh0.bassshot.ui.components.SettingsDialog
import com.jongh0.bassshot.ui.components.TimemarkOverlay
import com.jongh0.bassshot.ui.components.UpdateDialog
import com.jongh0.bassshot.ui.theme.AnodizedCharcoal
import com.jongh0.bassshot.ui.theme.ChampagneGold
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.ObsidianBlack
import com.jongh0.bassshot.ui.theme.SmokeGlass
import com.jongh0.bassshot.ui.theme.SoftSage
import com.jongh0.bassshot.ui.theme.SubtleTitanium
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BASS SHOT",
                        color = ChampagneGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AnodizedCharcoal)
                            .border(1.dp, SubtleTitanium, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.environmentData.isFromBarometerSensor) "내장 기압계" else "기상청 기압",
                            color = if (uiState.environmentData.isFromBarometerSensor) SoftSage else WarmGraphite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.openSettingsDialog() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = MutedSilver
                    )
                }
            }

            // 2. 중앙 3:4 고정 뷰파인더 컨테이너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
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

                        // 실시간 타임마크 오버레이
                        TimemarkOverlay(
                            environmentData = uiState.environmentData,
                            modifier = Modifier.align(Alignment.BottomStart)
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
                        // 실시간 타임마크
                        TimemarkOverlay(
                            environmentData = uiState.environmentData,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
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
                                    color = ChampagneGold,
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "배스를 물에 놓아주셔도 좋습니다",
                                    color = WarmGraphite,
                                    fontSize = 11.sp
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
                                    onMeasurementUpdated = { updated ->
                                        viewModel.onMeasurementUpdated(updated)
                                    }
                                )
                            }
                        }

                        // 하단 타임마크 오버레이
                        TimemarkOverlay(
                            environmentData = uiState.environmentData,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }

            // 3. 하단 액션 컨트롤 바 (내비게이션바 제스처 패딩 적용)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.appState) {
                    AppState.CAMERA_PREVIEW -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 좌측 갤러리 불러오기 버튼 (48dp 원형 차콜 버튼 + 샴페인 골드 테두리)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AnodizedCharcoal)
                                    .border(1.dp, SubtleTitanium, CircleShape)
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
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 2. 중앙 럭셔리 셔터 버튼
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(2.dp, ChampagneGold, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(MutedSilver)
                                    .clickable { viewModel.onShutterClicked() }
                            )

                            // 3. 우측 균형용 투명 스페이서
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }

                    AppState.ANALYZING -> {
                        Text(
                            text = "배스 길이 및 무게 분석 중...",
                            color = WarmGraphite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AppState.RESULT_SAVED -> {
                        val isBassDetected = uiState.measurement?.isBassDetected == true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!isBassDetected && !uiState.isManualMeasurementEnabled) {
                                Text(
                                    text = "물고기/손이 감지되지 않았습니다 (타임마크만 저장됨)",
                                    color = WarmGraphite,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 다시 촬영 버튼
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AnodizedCharcoal)
                                        .border(1.dp, SubtleTitanium, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onRetakeClicked() }
                                        .padding(horizontal = 14.dp, vertical = 11.dp)
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // 물고기 미검출 시 직접 수동 계측 모드로 진입 버튼
                                if (!isBassDetected && !uiState.isManualMeasurementEnabled) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AnodizedCharcoal)
                                            .border(1.dp, ChampagneGold, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.enableManualMeasurement() }
                                            .padding(horizontal = 14.dp, vertical = 11.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "직접 계측",
                                            tint = ChampagneGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "직접 계측",
                                            color = ChampagneGold,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // 갤러리 저장 상태 / 보정 완료 저장 버튼
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ChampagneGold)
                                        .clickable { viewModel.saveCurrentResult() }
                                        .padding(horizontal = 18.dp, vertical = 11.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "저장",
                                        tint = ObsidianBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isSaving) "저장 중..." else "갤러리 저장됨",
                                        color = ObsidianBlack,
                                        fontSize = 13.sp,
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
    }
}
