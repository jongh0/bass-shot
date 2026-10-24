package com.jongh0.bassshot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.ui.theme.MutedSilver
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.OpticalWhiteGlow
import com.jongh0.bassshot.ui.theme.SubtleTitanium
import com.jongh0.bassshot.ui.theme.TitaniumSilver
import com.jongh0.bassshot.ui.theme.WarmGraphite
import kotlin.math.hypot

private enum class DragTarget {
    NONE, SNOUT, TAIL
}

@Composable
fun MeasurementOverlay(
    measurement: BassMeasurementUiModel,
    onMeasurementUpdated: (newModel: BassMeasurementUiModel) -> Unit,
    hasWeatherAbove: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var activeTarget by remember { mutableStateOf(DragTarget.NONE) }
    val currentMeasurement by rememberUpdatedState(measurement)
    val currentOnUpdated by rememberUpdatedState(onMeasurementUpdated)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val touchRadiusPx = with(density) { 64.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val initialModel = currentMeasurement
                        var curSnout = initialModel.snoutPoint
                        var curTail = initialModel.tailPoint

                        val snoutPx = Offset(curSnout.x * widthPx, curSnout.y * heightPx)
                        val tailPx = Offset(curTail.x * widthPx, curTail.y * heightPx)

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
                                            curSnout = NormalizedPoint(
                                                (curSnout.x + dxRatio).coerceIn(0.01f, 0.99f),
                                                (curSnout.y + dyRatio).coerceIn(0.01f, 0.99f)
                                            )
                                            val updated = FishMeasurementCalculator.recalculateOnDrag(
                                                current = currentMeasurement,
                                                newSnout = curSnout
                                            )
                                            currentOnUpdated(updated)
                                        }
                                        DragTarget.TAIL -> {
                                            curTail = NormalizedPoint(
                                                (curTail.x + dxRatio).coerceIn(0.01f, 0.99f),
                                                (curTail.y + dyRatio).coerceIn(0.01f, 0.99f)
                                            )
                                            val updated = FishMeasurementCalculator.recalculateOnDrag(
                                                current = currentMeasurement,
                                                newTail = curTail
                                            )
                                            currentOnUpdated(updated)
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

            val dashOn = with(density) { 5.dp.toPx() }
            val dashOff = with(density) { 8.5.dp.toPx() }
            val mainDash = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f)

            // 1. 주 캘리퍼스 가이드라인 (배스를 가리지 않는 가볍고 섬세한 점선)
            drawLine(
                color = Color(0x8C000000),
                start = snoutPx,
                end = tailPx,
                strokeWidth = with(density) { 3.dp.toPx() },
                pathEffect = mainDash
            )
            drawLine(
                color = OpticalWhite,
                start = snoutPx,
                end = tailPx,
                strokeWidth = with(density) { 1.4.dp.toPx() },
                pathEffect = mainDash
            )

            // 2. 정밀 캘리퍼스 턱 (Snout / Tail Jaw)
            val jawHalfWidth = 26.dp.toPx()
            drawLine(
                color = Color(0xC0000000),
                start = Offset(snoutPx.x - jawHalfWidth, snoutPx.y),
                end = Offset(snoutPx.x + jawHalfWidth, snoutPx.y),
                strokeWidth = 5.5f
            )
            drawLine(
                color = OpticalWhite,
                start = Offset(snoutPx.x - jawHalfWidth, snoutPx.y),
                end = Offset(snoutPx.x + jawHalfWidth, snoutPx.y),
                strokeWidth = 2.6f
            )
            drawLine(
                color = Color(0xC0000000),
                start = Offset(tailPx.x - jawHalfWidth, tailPx.y),
                end = Offset(tailPx.x + jawHalfWidth, tailPx.y),
                strokeWidth = 5.5f
            )
            drawLine(
                color = OpticalWhite,
                start = Offset(tailPx.x - jawHalfWidth, tailPx.y),
                end = Offset(tailPx.x + jawHalfWidth, tailPx.y),
                strokeWidth = 2.6f
            )

            // 3. 고감도 정밀 레티클 핸들 (주둥이 & 꼬리 - 중앙부는 완전히 투명하여 피사체가 그대로 보임)
            val handleRadius = 18.dp.toPx()
            drawPrecisionHandle(snoutPx, OpticalWhite, activeTarget == DragTarget.SNOUT, handleRadius)
            drawPrecisionHandle(tailPx, OpticalWhite, activeTarget == DragTarget.TAIL, handleRadius)
        }

        // 4. 좌상단(TopStart) 스펙 텍스트 (날씨 3줄 아래 일정한 고정 위치에 항상 배치)
        val isDragging = activeTarget != DragTarget.NONE
        val textShadow = Shadow(
            color = Color(0xD9000000),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )
        // 날씨, 바람, 기압 정보 유무나 불러온 사진 여부와 상관없이 항상 날씨 3줄 아래의 동일한 고정 위치 유지
        val weatherBlockHeight = with(density) { (48.sp).toDp() } + 14.dp
        val topPadding = weatherBlockHeight + 24.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topPadding, start = 12.dp)
                .alpha(if (isDragging) 0.35f else 1.0f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 4줄: 길이 (날짜와 동일한 OpticalWhite 적용)
            Text(
                text = "길이 ${measurement.lengthCm}cm",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite
            )

            // 5줄: 무게 (날짜와 동일한 OpticalWhite 적용)
            Text(
                text = "무게 ${measurement.weightG}g",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    shadow = textShadow
                ),
                color = OpticalWhite
            )
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
    val strokeWidth = if (isActive) 3.2f else 2.4f

    if (isActive) {
        // 활성화 시 외곽 글로우 링 (중앙 및 내부는 비어있어 피사체 확인 가능)
        drawCircle(
            color = OpticalWhiteGlow,
            radius = radius * 1.7f,
            center = center,
            style = Stroke(width = 3.5f)
        )
    }

    // 1. 외곽 블랙 드롭 섀도우 링 (밝거나 어두운 배경 어디서나 링 테두리가 선명히 보이도록)
    drawCircle(
        color = Color(0xC0000000),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth + 2.5f)
    )

    // 2. 메인 테두리 링 (중앙부는 fill 없이 100% 투명하여 주둥이/꼬리 끝점이 그대로 보임!)
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // 3. 링 바깥쪽으로 뻗은 십자 조준선 (링 내부는 뚫려있어 가리지 않고, 바깥에서 중심 위치를 가리킴)
    val tickLen = radius * 0.45f
    val tickStroke = if (isActive) 2.2f else 1.6f
    val shadowTickStroke = tickStroke + 2.5f
    val shadowColor = Color(0xC0000000)

    // 십자선 그림자
    drawLine(shadowColor, Offset(center.x, center.y - radius - tickLen), Offset(center.x, center.y - radius), shadowTickStroke)
    drawLine(shadowColor, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius + tickLen), shadowTickStroke)
    drawLine(shadowColor, Offset(center.x - radius - tickLen, center.y), Offset(center.x - radius, center.y), shadowTickStroke)
    drawLine(shadowColor, Offset(center.x + radius, center.y), Offset(center.x + radius + tickLen, center.y), shadowTickStroke)

    // 십자선 본체
    drawLine(color, Offset(center.x, center.y - radius - tickLen), Offset(center.x, center.y - radius), tickStroke)
    drawLine(color, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius + tickLen), tickStroke)
    drawLine(color, Offset(center.x - radius - tickLen, center.y), Offset(center.x - radius, center.y), tickStroke)
    drawLine(color, Offset(center.x + radius, center.y), Offset(center.x + radius + tickLen, center.y), tickStroke)
}
