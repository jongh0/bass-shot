package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.ui.theme.ChampagneGold
import com.jongh0.bassshot.ui.theme.ChampagneGoldGlow
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import kotlin.math.hypot

private enum class DragTarget {
    NONE, SNOUT, TAIL
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
        // 넉넉한 64dp 터치 감지 반경으로 주둥이/꼬리 원판을 손쉽게 드래그
        val touchRadiusPx = with(density) { 64.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(measurement) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val snoutPx = Offset(measurement.snoutPoint.x * widthPx, measurement.snoutPoint.y * heightPx)
                        val tailPx = Offset(measurement.tailPoint.x * widthPx, measurement.tailPoint.y * heightPx)

                        val dSnout = hypot(down.position.x - snoutPx.x, down.position.y - snoutPx.y)
                        val dTail = hypot(down.position.x - tailPx.x, down.position.y - tailPx.y)

                        val minD = minOf(dSnout, dTail)
                        val target = when {
                            minD > touchRadiusPx -> DragTarget.NONE
                            minD == dSnout -> DragTarget.SNOUT
                            else -> DragTarget.TAIL
                        }

                        if (target != DragTarget.NONE) {
                            down.consume()
                            activeTarget = target

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                if (change.positionChanged()) {
                                    change.consume()
                                    val dxRatio = (change.position.x - change.previousPosition.x) / widthPx
                                    val dyRatio = (change.position.y - change.previousPosition.y) / heightPx

                                    when (target) {
                                        DragTarget.SNOUT -> {
                                            val newP = NormalizedPoint(
                                                (measurement.snoutPoint.x + dxRatio).coerceIn(0.02f, 0.98f),
                                                (measurement.snoutPoint.y + dyRatio).coerceIn(0.02f, 0.98f)
                                            )
                                            val updated = FishMeasurementCalculator.recalculateOnDrag(
                                                current = measurement,
                                                newSnout = newP
                                            )
                                            onMeasurementUpdated(updated)
                                        }
                                        DragTarget.TAIL -> {
                                            val newP = NormalizedPoint(
                                                (measurement.tailPoint.x + dxRatio).coerceIn(0.02f, 0.98f),
                                                (measurement.tailPoint.y + dyRatio).coerceIn(0.02f, 0.98f)
                                            )
                                            val updated = FishMeasurementCalculator.recalculateOnDrag(
                                                current = measurement,
                                                newTail = newP
                                            )
                                            onMeasurementUpdated(updated)
                                        }
                                        DragTarget.NONE -> {}
                                    }
                                }
                            }
                            activeTarget = DragTarget.NONE
                        }
                    }
                }
        ) {
            val snoutPx = Offset(measurement.snoutPoint.x * widthPx, measurement.snoutPoint.y * heightPx)
            val tailPx = Offset(measurement.tailPoint.x * widthPx, measurement.tailPoint.y * heightPx)

            val mainDash = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)

            // 1. 주 캘리퍼스 가이드라인 (주둥이 ~ 꼬리)
            drawLine(
                color = Color(0xB3000000),
                start = snoutPx,
                end = tailPx,
                strokeWidth = 4.5f,
                pathEffect = mainDash
            )
            drawLine(
                color = ChampagneGold,
                start = snoutPx,
                end = tailPx,
                strokeWidth = 2.2f,
                pathEffect = mainDash
            )

            // 2. 정밀 캘리퍼스 턱 (Snout / Tail Jaw)
            val jawHalfWidth = 24.dp.toPx()
            drawLine(
                color = Color(0xB3000000),
                start = Offset(snoutPx.x - jawHalfWidth, snoutPx.y),
                end = Offset(snoutPx.x + jawHalfWidth, snoutPx.y),
                strokeWidth = 5f
            )
            drawLine(
                color = ChampagneGold,
                start = Offset(snoutPx.x - jawHalfWidth, snoutPx.y),
                end = Offset(snoutPx.x + jawHalfWidth, snoutPx.y),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xB3000000),
                start = Offset(tailPx.x - jawHalfWidth, tailPx.y),
                end = Offset(tailPx.x + jawHalfWidth, tailPx.y),
                strokeWidth = 5f
            )
            drawLine(
                color = ChampagneGold,
                start = Offset(tailPx.x - jawHalfWidth, tailPx.y),
                end = Offset(tailPx.x + jawHalfWidth, tailPx.y),
                strokeWidth = 2.5f
            )

            // 3. 고감도 정밀 조준 핸들 (주둥이 & 꼬리 2개 노드로 직관적 제어)
            val handleRadius = 16.dp.toPx()
            drawPrecisionHandle(snoutPx, ChampagneGold, activeTarget == DragTarget.SNOUT, handleRadius)
            drawPrecisionHandle(tailPx, ChampagneGold, activeTarget == DragTarget.TAIL, handleRadius)
        }

        // 4. 좌상(TopStart)으로 이동된 큼직하고 살짝 투명한 스펙 뱃지 (체고 생략)
        val isDragging = activeTarget != DragTarget.NONE
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .alpha(if (isDragging) 0.35f else 1.0f)
                .background(Color(0x9918191E), RoundedCornerShape(12.dp)) // 살짝 투명한 배경
                .border(1.dp, SubtleTitanium, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                // 길이
                Text(
                    text = "${measurement.lengthCm}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold,
                    lineHeight = 22.sp
                )
                Text(
                    text = "cm",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSilver,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )

                Text(
                    text = "  ·  ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x80FFFFFF),
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // 무게
                Text(
                    text = "${measurement.weightG}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold,
                    lineHeight = 22.sp
                )
                Text(
                    text = "g",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSilver,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawPrecisionHandle(
    center: Offset,
    color: Color,
    isActive: Boolean,
    baseRadius: Float
) {
    val radius = if (isActive) baseRadius * 1.25f else baseRadius

    if (isActive) {
        drawCircle(
            color = ChampagneGoldGlow,
            radius = radius * 2.2f,
            center = center
        )
    }

    // 어두운 반투명 원판
    drawCircle(
        color = Color(0xD918191E),
        radius = radius,
        center = center
    )

    // 정밀 테두리 링
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = if (isActive) 2.8f else 1.8f)
    )

    // 중앙 조준점
    drawCircle(
        color = color,
        radius = 3.2f,
        center = center
    )

    // 십자 미세 눈금 (카메라 레티클)
    val tickLen = radius * 0.45f
    val tickStroke = 1.2f
    drawLine(color, Offset(center.x, center.y - radius - tickLen), Offset(center.x, center.y - radius), tickStroke)
    drawLine(color, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius + tickLen), tickStroke)
    drawLine(color, Offset(center.x - radius - tickLen, center.y), Offset(center.x - radius, center.y), tickStroke)
    drawLine(color, Offset(center.x + radius, center.y), Offset(center.x + radius + tickLen, center.y), tickStroke)
}
