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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongh0.bassshot.R
import com.jongh0.bassshot.domain.model.BassLengthTier
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.NormalizedPoint
import com.jongh0.bassshot.domain.model.lengthTier
import com.jongh0.bassshot.domain.util.FishMeasurementCalculator
import com.jongh0.bassshot.ui.theme.ElectricCyan
import com.jongh0.bassshot.ui.theme.ElectricCyanGlow
import com.jongh0.bassshot.ui.theme.FrostedGlassBg
import com.jongh0.bassshot.ui.theme.LunkerGold
import com.jongh0.bassshot.ui.theme.NeonLime
import com.jongh0.bassshot.ui.theme.OpticalWhite
import com.jongh0.bassshot.ui.theme.PillBadgeBg
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

    // 계측 HUD 오버레이는 시스템 글꼴 배율(fontScale) 설정과 무관하게 1.0f로 고정하여 레이아웃 깨짐 원천 방지
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1.0f
        )
    ) {
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

            // 1-2. 정밀 캘리퍼스 브래킷: Snout & Tail (배스 각도에 맞춰 360° 직교 자동 회전)
            val jawHalfWidth = with(density) { 28.dp.toPx() }
            val lipLength = with(density) { 14.dp.toPx() }
            val bracketStroke = with(density) { 2.4.dp.toPx() }
            val shadowStroke = with(density) { 4.5.dp.toPx() }

            val dx = tailPx.x - snoutPx.x
            val dy = tailPx.y - snoutPx.y
            val lineDist = hypot(dx, dy)
            val lineUnitVector = if (lineDist > 0.001f) {
                Offset(dx / lineDist, dy / lineDist)
            } else {
                Offset(0f, 1f)
            }

            // 주둥이 브래킷 (배스 계측선에 수직 정렬, 가로 촬영 시 세로 브래킷으로 자동 전환)
            drawCaliperBracket(
                center = snoutPx,
                lineUnitVector = lineUnitVector,
                halfWidth = jawHalfWidth,
                lipLength = lipLength,
                isSnout = true,
                color = ElectricCyan,
                strokeWidth = bracketStroke,
                shadowWidth = shadowStroke,
                isActive = activeTarget == DragTarget.SNOUT
            )

            // 꼬리 브래킷 (배스 계측선에 수직 정렬)
            drawCaliperBracket(
                center = tailPx,
                lineUnitVector = lineUnitVector,
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

        // 3. 우상단(TopEnd) 계측 결과 카드 (직사각형 모서리 + 우상단 끝 밀착)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(120.dp)
                .background(FrostedGlassBg)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .alpha(if (isDragging) 0.45f else 1.0f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 3-1. 헤더: 줄자(좌) & 물고기(우)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ruler),
                        contentDescription = null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_fish),
                        contentDescription = null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 3-2. 길이 섹션 (한 줄: "길이" ... "26.5 cm", 1/2짜, 3짜, 4짜, 5짜 이상 티어별 컬러)
                val lengthColor = when (measurement.lengthTier) {
                    BassLengthTier.TIER_UNDER_30 -> TitaniumSilver
                    BassLengthTier.TIER_30S -> NeonLime
                    BassLengthTier.TIER_40S -> ElectricCyan
                    BassLengthTier.TIER_50_PLUS -> LunkerGold
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "길이",
                        color = lengthColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            lineHeight = 16.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "${measurement.lengthCm}",
                            color = lengthColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                lineHeight = 16.sp
                            )
                        )
                        Text(
                            text = "cm",
                            color = lengthColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 1.dp),
                            style = TextStyle(
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                // 3-3. 무게 섹션 (한 줄: "무게" ... "606 g")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "무게",
                        color = OpticalWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(
                            lineHeight = 16.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "${measurement.weightG}",
                            color = OpticalWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                lineHeight = 16.sp
                            )
                        )
                        Text(
                            text = "g",
                            color = OpticalWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            style = TextStyle(
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * 캘리퍼스 브래킷 드로잉 (배스 계측선 벡터에 맞춰 360° 직교 자동 회전)
 * - 세로 배스: 가로 ㄷ자 브래킷 (┌──+──┐ / └──+──┘)
 * - 가로 배스: 세로 ㄷ자 브래킷 (├──+ / +──┤)
 * - 사선 배스: 배스 각도에 맞춰 수직 정렬
 */
private fun DrawScope.drawCaliperBracket(
    center: Offset,
    lineUnitVector: Offset,
    halfWidth: Float,
    lipLength: Float,
    isSnout: Boolean,
    color: Color,
    strokeWidth: Float,
    shadowWidth: Float,
    isActive: Boolean
) {
    val ux = lineUnitVector.x
    val uy = lineUnitVector.y
    // 수직 직교 단위 벡터 (Perpendicular Unit Vector)
    val vx = -uy
    val vy = ux

    // 드래그 활성 시 글로우 아우라
    if (isActive) {
        drawCircle(
            color = ElectricCyanGlow,
            radius = halfWidth + 8f,
            center = center
        )
    }

    // 캘리퍼스 메인 날개 끝점 (계측선과 직교하는 양 끝)
    val p1x = center.x - halfWidth * vx
    val p1y = center.y - halfWidth * vy
    val p2x = center.x + halfWidth * vx
    val p2y = center.y + halfWidth * vy

    // 캘리퍼스 립(측면 날개)은 배스 몸통 쪽을 향하도록 (Snout은 +u, Tail은 -u)
    val lipDir = if (isSnout) 1f else -1f
    val lip1x = p1x + lipDir * lipLength * ux
    val lip1y = p1y + lipDir * lipLength * uy
    val lip2x = p2x + lipDir * lipLength * ux
    val lip2y = p2y + lipDir * lipLength * uy

    val path = Path().apply {
        moveTo(lip1x, lip1y)
        lineTo(p1x, p1y)
        lineTo(p2x, p2y)
        lineTo(lip2x, lip2y)
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

    // 3. 중심 크로스헤어 '+' 틱 (계측선 및 직교선에 정렬)
    val tickHalf = 5f
    // 직교 바 방향 틱
    val barTickStart = Offset(center.x - tickHalf * vx, center.y - tickHalf * vy)
    val barTickEnd = Offset(center.x + tickHalf * vx, center.y + tickHalf * vy)
    drawLine(Color(0xCC000000), barTickStart, barTickEnd, shadowWidth)
    drawLine(color, barTickStart, barTickEnd, strokeWidth)

    // 계측선 방향 틱
    val lineTickStart = Offset(center.x - tickHalf * ux, center.y - tickHalf * uy)
    val lineTickEnd = Offset(center.x + tickHalf * ux, center.y + tickHalf * uy)
    drawLine(Color(0xCC000000), lineTickStart, lineTickEnd, shadowWidth)
    drawLine(color, lineTickStart, lineTickEnd, strokeWidth)
}
