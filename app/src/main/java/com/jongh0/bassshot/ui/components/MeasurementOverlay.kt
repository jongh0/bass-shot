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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.R
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.ui.theme.ElectricCyan
import com.jongh0.bassshot.ui.theme.ElectricCyanGlow
import com.jongh0.bassshot.ui.theme.FrostedGlassBg
import com.jongh0.bassshot.ui.theme.FrostedGlassBorder
import com.jongh0.bassshot.ui.theme.NeonLime
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.PillBadgeBg
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

        val snoutPx = Offset(measurement.snoutPoint.x * widthPx, measurement.snoutPoint.y * heightPx)
        val tailPx = Offset(measurement.tailPoint.x * widthPx, measurement.tailPoint.y * heightPx)
        val midX = (snoutPx.x + tailPx.x) / 2f
        val midY = (snoutPx.y + tailPx.y) / 2f

        // 1. 배스 계측 캔버스 (사이언 대시 점선 + 캘리퍼스 브래킷)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val initialModel = currentMeasurement
                        var curSnout = initialModel.snoutPoint
                        var curTail = initialModel.tailPoint

                        val sPx = Offset(curSnout.x * widthPx, curSnout.y * heightPx)
                        val tPx = Offset(curTail.x * widthPx, curTail.y * heightPx)

                        val dSnout = hypot(down.position.x - sPx.x, down.position.y - sPx.y)
                        val dTail = hypot(down.position.x - tPx.x, down.position.y - tPx.y)

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
            val dashOn = with(density) { 6.dp.toPx() }
            val dashOff = with(density) { 6.dp.toPx() }
            val mainDash = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f)

            // 1-1. 주 캘리퍼스 가이드라인 (일렉트릭 사이언 점선 + 드롭 섀도우)
            drawLine(
                color = Color(0x99000000),
                start = snoutPx,
                end = tailPx,
                strokeWidth = with(density) { 3.5.dp.toPx() },
                pathEffect = mainDash
            )
            drawLine(
                color = ElectricCyan,
                start = snoutPx,
                end = tailPx,
                strokeWidth = with(density) { 1.8.dp.toPx() },
                pathEffect = mainDash
            )

            // 1-2. 정밀 캘리퍼스 브래킷: Snout (┌──+──┐) & Tail (└──+──┘)
            val jawHalfWidth = with(density) { 28.dp.toPx() }
            val lipLength = with(density) { 14.dp.toPx() }
            val bracketStroke = with(density) { 2.4.dp.toPx() }
            val shadowStroke = with(density) { 4.5.dp.toPx() }

            // 주둥이 브래킷 (하향 ㄷ자 ┌───+───┐)
            drawCaliperBracket(
                center = snoutPx,
                halfWidth = jawHalfWidth,
                lipLength = lipLength,
                isSnout = true,
                color = ElectricCyan,
                strokeWidth = bracketStroke,
                shadowWidth = shadowStroke,
                isActive = activeTarget == DragTarget.SNOUT
            )

            // 꼬리 브래킷 (상향 ㄷ자 └───+───┘)
            drawCaliperBracket(
                center = tailPx,
                halfWidth = jawHalfWidth,
                lipLength = lipLength,
                isSnout = false,
                color = ElectricCyan,
                strokeWidth = bracketStroke,
                shadowWidth = shadowStroke,
                isActive = activeTarget == DragTarget.TAIL
            )
        }

        // 2. 중앙 길이 캡슐 뱃지 (Pill Badge: [ + 35.2cm ])
        val isDragging = activeTarget != DragTarget.NONE
        val badgeWidthEst = with(density) { 86.dp.toPx() }
        val badgeHeightEst = with(density) { 26.dp.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (midX - badgeWidthEst / 2).toInt(),
                        y = (midY - badgeHeightEst / 2).toInt()
                    )
                }
                .clip(RoundedCornerShape(13.dp))
                .background(PillBadgeBg)
                .border(1.2.dp, ElectricCyan, RoundedCornerShape(13.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "+",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${measurement.lengthCm}cm",
                    color = OpticalWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // 3. 우상단(TopEnd) 계측 결과 카드 (나노 바나나 목업 디자인 일치)
        val textShadow = Shadow(
            color = Color(0xD9000000),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
                .width(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FrostedGlassBg)
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .alpha(if (isDragging) 0.45f else 1.0f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 3-1. 헤더: 줄자(좌) & 물고기(우)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ruler),
                        contentDescription = null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_fish),
                        contentDescription = null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3-2. 길이 섹션
                Text(
                    text = "길이",
                    color = NeonLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = textShadow)
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${measurement.lengthCm}",
                        color = NeonLime,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        style = TextStyle(shadow = textShadow)
                    )
                    Text(
                        text = "cm",
                        color = NeonLime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp),
                        style = TextStyle(shadow = textShadow)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 3-3. 무게 섹션
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "무게",
                        color = WarmGraphite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(shadow = textShadow)
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "${measurement.weightG}",
                            color = OpticalWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(shadow = textShadow)
                        )
                        Text(
                            text = "g",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 1.dp),
                            style = TextStyle(shadow = textShadow)
                        )
                    }
                }

                // 3-4. 푸터: 저울(좌) & 케틀벨(우)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_scale),
                        contentDescription = null,
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_weight),
                        contentDescription = null,
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 캘리퍼스 브래킷 드로잉 (주둥이: ┌──+──┐, 꼬리: └──+──┘)
 */
private fun DrawScope.drawCaliperBracket(
    center: Offset,
    halfWidth: Float,
    lipLength: Float,
    isSnout: Boolean,
    color: Color,
    strokeWidth: Float,
    shadowWidth: Float,
    isActive: Boolean
) {
    val left = center.x - halfWidth
    val right = center.x + halfWidth
    val y = center.y
    val lipY = if (isSnout) y + lipLength else y - lipLength

    // 드래그 활성 시 글로우 아우라
    if (isActive) {
        drawCircle(
            color = ElectricCyanGlow,
            radius = halfWidth + 8f,
            center = center
        )
    }

    val path = Path().apply {
        moveTo(left, lipY)
        lineTo(left, y)
        lineTo(right, y)
        lineTo(right, lipY)
    }

    // 1. 드롭 섀도우
    drawPath(
        path = path,
        color = Color(0xCC000000),
        style = Stroke(width = shadowWidth)
    )

    // 2. 메인 브래킷
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )

    // 3. 중심 크로스헤어 '+' 틱
    val tickHalf = 5f
    // 수평 틱 섀도우 & 메인
    drawLine(Color(0xCC000000), Offset(center.x - tickHalf, y), Offset(center.x + tickHalf, y), shadowWidth)
    drawLine(color, Offset(center.x - tickHalf, y), Offset(center.x + tickHalf, y), strokeWidth)

    // 수직 틱 섀도우 & 메인
    drawLine(Color(0xCC000000), Offset(center.x, y - tickHalf), Offset(center.x, y + tickHalf), shadowWidth)
    drawLine(color, Offset(center.x, y - tickHalf), Offset(center.x, y + tickHalf), strokeWidth)
}
