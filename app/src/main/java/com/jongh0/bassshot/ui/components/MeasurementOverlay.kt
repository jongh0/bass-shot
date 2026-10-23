package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import com.jongh0.bassshot.ui.theme.DarkSurface
import com.jongh0.bassshot.ui.theme.HairlineBorder
import com.jongh0.bassshot.ui.theme.LeicaAmber
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.SmokeGlass
import com.jongh0.bassshot.ui.theme.TacticalCyan
import kotlin.math.hypot

private enum class DragTarget {
    NONE, SNOUT, TAIL, GIRTH_LEFT, GIRTH_RIGHT
}

@Composable
fun MeasurementOverlay(
    measurement: BassMeasurementUiModel,
    onMeasurementUpdated: (newModel: BassMeasurementUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var activeTarget by remember { mutableStateOf(DragTarget.NONE) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val touchRadiusPx = with(density) { 44.dp.toPx() }

        // 인터랙티브 캘리퍼스 드래그 영역
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(measurement) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val snoutPx = Offset(measurement.snoutPoint.x * widthPx, measurement.snoutPoint.y * heightPx)
                            val tailPx = Offset(measurement.tailPoint.x * widthPx, measurement.tailPoint.y * heightPx)
                            val girthLPx = Offset(measurement.maxGirthLeft.x * widthPx, measurement.maxGirthLeft.y * heightPx)
                            val girthRPx = Offset(measurement.maxGirthRight.x * widthPx, measurement.maxGirthRight.y * heightPx)

                            val dSnout = hypot(offset.x - snoutPx.x, offset.y - snoutPx.y)
                            val dTail = hypot(offset.x - tailPx.x, offset.y - tailPx.y)
                            val dGirthL = hypot(offset.x - girthLPx.x, offset.y - girthLPx.y)
                            val dGirthR = hypot(offset.x - girthRPx.x, offset.y - girthRPx.y)

                            val minD = minOf(dSnout, dTail, dGirthL, dGirthR)
                            activeTarget = when {
                                minD > touchRadiusPx -> DragTarget.NONE
                                minD == dSnout -> DragTarget.SNOUT
                                minD == dTail -> DragTarget.TAIL
                                minD == dGirthL -> DragTarget.GIRTH_LEFT
                                else -> DragTarget.GIRTH_RIGHT
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeTarget == DragTarget.NONE) return@detectDragGestures

                            val dxRatio = dragAmount.x / widthPx
                            val dyRatio = dragAmount.y / heightPx

                            when (activeTarget) {
                                DragTarget.SNOUT -> {
                                    val newP = NormalizedPoint(
                                        measurement.snoutPoint.x + dxRatio,
                                        measurement.snoutPoint.y + dyRatio
                                    )
                                    val updated = com.jongh0.bassshot.domain.util.FishMeasurementCalculator.recalculateOnDrag(
                                        current = measurement,
                                        newSnout = newP
                                    )
                                    onMeasurementUpdated(updated)
                                }
                                DragTarget.TAIL -> {
                                    val newP = NormalizedPoint(
                                        measurement.tailPoint.x + dxRatio,
                                        measurement.tailPoint.y + dyRatio
                                    )
                                    val updated = com.jongh0.bassshot.domain.util.FishMeasurementCalculator.recalculateOnDrag(
                                        current = measurement,
                                        newTail = newP
                                    )
                                    onMeasurementUpdated(updated)
                                }
                                DragTarget.GIRTH_LEFT -> {
                                    val newP = NormalizedPoint(
                                        measurement.maxGirthLeft.x + dxRatio,
                                        measurement.maxGirthLeft.y + dyRatio
                                    )
                                    val updated = com.jongh0.bassshot.domain.util.FishMeasurementCalculator.recalculateOnDrag(
                                        current = measurement,
                                        newGirthLeft = newP
                                    )
                                    onMeasurementUpdated(updated)
                                }
                                DragTarget.GIRTH_RIGHT -> {
                                    val newP = NormalizedPoint(
                                        measurement.maxGirthRight.x + dxRatio,
                                        measurement.maxGirthRight.y + dyRatio
                                    )
                                    val updated = com.jongh0.bassshot.domain.util.FishMeasurementCalculator.recalculateOnDrag(
                                        current = measurement,
                                        newGirthRight = newP
                                    )
                                    onMeasurementUpdated(updated)
                                }
                                DragTarget.NONE -> {}
                            }
                        },
                        onDragEnd = { activeTarget = DragTarget.NONE },
                        onDragCancel = { activeTarget = DragTarget.NONE }
                    )
                }
        ) {
            val snoutPx = Offset(measurement.snoutPoint.x * widthPx, measurement.snoutPoint.y * heightPx)
            val tailPx = Offset(measurement.tailPoint.x * widthPx, measurement.tailPoint.y * heightPx)
            val girthLPx = Offset(measurement.maxGirthLeft.x * widthPx, measurement.maxGirthLeft.y * heightPx)
            val girthRPx = Offset(measurement.maxGirthRight.x * widthPx, measurement.maxGirthRight.y * heightPx)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)

            // 1. 주 계측 점선 (주둥이 ~ 꼬리)
            // 그림자
            drawLine(
                color = Color(0x99000000),
                start = snoutPx,
                end = tailPx,
                strokeWidth = 4.5f,
                pathEffect = dashEffect
            )
            // 메인 선
            drawLine(
                color = LeicaAmber,
                start = snoutPx,
                end = tailPx,
                strokeWidth = 2f,
                pathEffect = dashEffect
            )

            // 2. 체고 점선 (좌 ~ 우)
            drawLine(
                color = TacticalCyan,
                start = girthLPx,
                end = girthRPx,
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            // 3. 캘리퍼스 턱(Jaw) 수평 바
            val jawHalfWidth = 24.dp.toPx()
            drawLine(
                color = LeicaAmber,
                start = Offset(snoutPx.x - jawHalfWidth, snoutPx.y),
                end = Offset(snoutPx.x + jawHalfWidth, snoutPx.y),
                strokeWidth = 2.5f
            )
            drawLine(
                color = LeicaAmber,
                start = Offset(tailPx.x - jawHalfWidth, tailPx.y),
                end = Offset(tailPx.x + jawHalfWidth, tailPx.y),
                strokeWidth = 2.5f
            )

            // 4. 드래그 타겟 십자선/노드 렌더링
            val handleRadius = 14.dp.toPx()
            drawHandle(snoutPx, LeicaAmber, activeTarget == DragTarget.SNOUT, handleRadius)
            drawHandle(tailPx, LeicaAmber, activeTarget == DragTarget.TAIL, handleRadius)
            drawHandle(girthLPx, TacticalCyan, activeTarget == DragTarget.GIRTH_LEFT, handleRadius * 0.85f)
            drawHandle(girthRPx, TacticalCyan, activeTarget == DragTarget.GIRTH_RIGHT, handleRadius * 0.85f)
        }

        // 상단 스펙 뱃지
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(SmokeGlass, RoundedCornerShape(20.dp))
                .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            val fallbackNote = if (measurement.isManualFallback) " [수동 모드]" else ""
            Text(
                text = "${measurement.lengthCm} cm  |  ${measurement.weightG} g  (체고 ${measurement.girthCm} cm)$fallbackNote",
                style = com.jongh0.bassshot.ui.theme.Typography.headlineMedium.copy(fontSize = 15.sp),
                color = OpticalWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    center: Offset,
    color: Color,
    isActive: Boolean,
    baseRadius: Float
) {
    val radius = if (isActive) baseRadius * 1.3f else baseRadius

    // 외곽 링
    drawCircle(
        color = Color(0xCC121316),
        radius = radius,
        center = center
    )
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = if (isActive) 3f else 1.8f)
    )
    // 중앙 작은 점
    drawCircle(
        color = color,
        radius = 3.5f,
        center = center
    )
}
