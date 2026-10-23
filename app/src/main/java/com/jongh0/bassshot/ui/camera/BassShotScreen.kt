package com.jongh0.bassshot.ui.camera

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
import com.jongh0.bassshot.ui.theme.CoolGrey
import com.jongh0.bassshot.ui.theme.DarkSurface
import com.jongh0.bassshot.ui.theme.HairlineBorder
import com.jongh0.bassshot.ui.theme.LeicaAmber
import com.jongh0.bassshot.ui.theme.MatteBlack
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SmokeGlass
import com.jongh0.bassshot.ui.theme.TacticalCyan

@Composable
fun BassShotScreen(
    viewModel: BassShotViewModel,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(hasLocationPermission) {
        viewModel.refreshEnvironmentData(hasLocationPermission)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack)
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
                        text = "BASSSHOT",
                        color = LeicaAmber,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.environmentData.isFromBarometerSensor) "BARO SENSOR" else "BARO API",
                            color = if (uiState.environmentData.isFromBarometerSensor) TacticalCyan else CoolGrey,
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
                        contentDescription = "Settings",
                        tint = OpticalWhite
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SmokeGlass)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = LeicaAmber,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "AI VISION 계측 분석 중",
                                    color = OpticalWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "배스를 물에 안전하게 놓아주셔도 좋습니다",
                                    color = CoolGrey,
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

                        // 계측 캘리퍼스 HUD & 상단 스펙 뱃지 (드래그 인터랙션 지원)
                        uiState.measurement?.let { measurement ->
                            MeasurementOverlay(
                                measurement = measurement,
                                onMeasurementUpdated = { updated ->
                                    viewModel.onMeasurementUpdated(updated)
                                }
                            )
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
                        // Leica 스타일 정밀 셔터 버튼
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .border(2.dp, LeicaAmber, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(OpticalWhite)
                                .clickable { viewModel.onShutterClicked() }
                        )
                    }

                    AppState.ANALYZING -> {
                        Text(
                            text = "배스 체장 및 무게 산출 중...",
                            color = CoolGrey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AppState.RESULT_SAVED -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 다시 촬영 버튼
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onRetakeClicked() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retake",
                                    tint = OpticalWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "다시 촬영",
                                    color = OpticalWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 갤러리 저장 상태 / 보정 완료 저장 버튼
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LeicaAmber)
                                    .clickable { viewModel.saveCurrentResult() }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = MatteBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isSaving) "저장 중..." else "DCIM 저장 완료",
                                    color = MatteBlack,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 설정 다이얼로그
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                currentGloveWidthCm = 9.5f,
                currentSaveOriginal = false,
                onDismissRequest = { viewModel.dismissSettingsDialog() },
                onSaveSettings = { width, saveOrig ->
                    viewModel.updateSettings(width, saveOrig)
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
