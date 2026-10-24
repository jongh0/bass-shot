package com.jongh0.bassshot.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.jongh0.bassshot.R
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData

object HighResCanvasCompositor {

    private const val COLOR_OPTICAL_WHITE = "#FFFFFF"
    private const val COLOR_ELECTRIC_CYAN = "#00E5FF"
    private const val COLOR_NEON_LIME = "#A3E635"
    private const val COLOR_WARM_GRAPHITE = "#9496A1"
    private const val COLOR_MUTED_SILVER = "#C4C7D0"

    // 화면(MeasurementOverlay, TimemarkOverlay)의 textShadow와 1:1로 시각적 매칭되는 텍스트 음영
    private const val TEXT_SHADOW_RADIUS_DP = 2.0f
    private const val TEXT_SHADOW_OFFSET_Y_DP = 0.67f
    private val TEXT_SHADOW_COLOR = Color.argb(135, 0, 0, 0)

    /**
     * 원본 해상도 비트맵 위에 고화질 타임마크와 계측선을 직접 합성
     * @param context 벡터 아이콘 로딩용 Context
     * @param containerWidthDp 화면(Compose)에서 이미지가 표시된 컨테이너의 실제 dp 폭
     * @param fontScale 단말기 시스템 글꼴 크기 배율
     * @param isImportedPhoto 갤러리에서 불러온 사진인 경우 타임마크 생략 (오직 계측 결과만 합성)
     */
    fun composite(
        originalBitmap: Bitmap,
        environmentData: EnvironmentData,
        measurement: BassMeasurementUiModel?,
        isImportedPhoto: Boolean = false,
        containerWidthDp: Float = 360f,
        fontScale: Float = 1.0f,
        context: Context? = null
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. 원본 사진 드로잉
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 화면(Compose)과 1:1 완벽하게 동일한 비례/크기/배치 스케일링 계수
        // HUD 오버레이 폰트는 단말기 시스템 글꼴 크기 설정에 영향받지 않도록 1.0f 고정
        val validContainerWidth = if (containerWidthDp > 100f) containerWidthDp else 360f
        val scaleDp = width / validContainerWidth
        val scaleFont = scaleDp

        val hasMeasurement = measurement != null && (measurement.isBassDetected || measurement.isManualFallback)
        val hasWeather = !isImportedPhoto

        // 2. 계측 가이드라인 및 스펙 뱃지 드로잉 (배스 감지 또는 직접 계측 시 합성)
        if (hasMeasurement) {
            drawMeasurementHUD(canvas, width, height, scaleDp, scaleFont, measurement!!, context)
        }

        // 3. 4단 타임마크 정보 드로잉 (불러온 사진이 아닌 직접 촬영본일 때만 합성)
        if (hasWeather) {
            drawTimemarkHUD(canvas, width, height, scaleDp, scaleFont, environmentData, context)
        }

        return output
    }

    private fun drawVector(
        context: Context?,
        canvas: Canvas,
        @DrawableRes resId: Int,
        left: Float,
        top: Float,
        size: Float,
        tintColor: Int = Color.WHITE
    ) {
        if (context == null) return
        val drawable = ContextCompat.getDrawable(context, resId) ?: return
        drawable.mutate()
        drawable.setTint(tintColor)
        drawable.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
        drawable.draw(canvas)
    }

    private fun drawMeasurementHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scaleDp: Float,
        scaleFont: Float,
        measurement: BassMeasurementUiModel,
        context: Context?
    ) {
        val snoutX = measurement.snoutPoint.x * width
        val snoutY = measurement.snoutPoint.y * height
        val tailX = measurement.tailPoint.x * width
        val tailY = measurement.tailPoint.y * height

        // 1. 주 캘리퍼스 가이드라인 (일렉트릭 사이언 점선 + 드롭 섀도우)
        val dashOn = 6f * scaleDp
        val dashOff = 6f * scaleDp
        val lineDash = DashPathEffect(floatArrayOf(dashOn, dashOff), 0f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(153, 0, 0, 0)
            strokeWidth = 3.5f * scaleDp
            style = Paint.Style.STROKE
            pathEffect = lineDash
        }
        val mainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_ELECTRIC_CYAN)
            strokeWidth = 1.8f * scaleDp
            style = Paint.Style.STROKE
            pathEffect = lineDash
        }

        canvas.drawLine(snoutX, snoutY, tailX, tailY, shadowPaint)
        canvas.drawLine(snoutX, snoutY, tailX, tailY, mainLinePaint)

        // 2. Snout & Tail 캘리퍼스 브래킷: Snout (┌──+──┐) & Tail (└──+──┘)
        val jawHalfWidth = 28f * scaleDp
        val lipLength = 14f * scaleDp
        val bracketStroke = 2.4f * scaleDp
        val bracketShadow = 4.5f * scaleDp

        drawCaliperBracketOnCanvas(
            canvas = canvas,
            centerX = snoutX,
            centerY = snoutY,
            halfWidth = jawHalfWidth,
            lipLength = lipLength,
            isSnout = true,
            strokeWidth = bracketStroke,
            shadowWidth = bracketShadow
        )

        drawCaliperBracketOnCanvas(
            canvas = canvas,
            centerX = tailX,
            centerY = tailY,
            halfWidth = jawHalfWidth,
            lipLength = lipLength,
            isSnout = false,
            strokeWidth = bracketStroke,
            shadowWidth = bracketShadow
        )

        // 3. 중앙 길이 알약 캡슐 뱃지: [ + 35.2cm ]
        val midX = (snoutX + tailX) / 2f
        val midY = (snoutY + tailY) / 2f
        val badgeW = 86f * scaleDp
        val badgeH = 26f * scaleDp
        val badgeRadius = 13f * scaleDp

        val pillRect = RectF(midX - badgeW / 2f, midY - badgeH / 2f, midX + badgeW / 2f, midY + badgeH / 2f)
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(217, 8, 47, 73) // Color(0xD9082F49)
            style = Paint.Style.FILL
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_ELECTRIC_CYAN)
            strokeWidth = 1.2f * scaleDp
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(pillRect, badgeRadius, badgeRadius, pillBgPaint)
        canvas.drawRoundRect(pillRect, badgeRadius, badgeRadius, pillBorderPaint)

        // 뱃지 내부 텍스트 (+ 35.2cm)
        val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_ELECTRIC_CYAN)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val lenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val lenText = "${measurement.lengthCm}cm"
        val plusWidth = plusPaint.measureText("+ ")
        val totalTextWidth = plusWidth + lenPaint.measureText(lenText)
        val textStartX = midX - totalTextWidth / 2f
        val textBaselineY = midY + (lenPaint.textSize / 3f)

        canvas.drawText("+ ", textStartX, textBaselineY, plusPaint)
        canvas.drawText(lenText, textStartX + plusWidth, textBaselineY, lenPaint)

        // 4. 우상단(TopEnd) 계측 결과 카드 (나노 바나나 목업 디자인 일치)
        val cardWidth = 140f * scaleDp
        val cardHeight = 88f * scaleDp
        val cardMarginRight = 12f * scaleDp
        val cardTop = 10f * scaleDp
        val cardRight = width - cardMarginRight
        val cardLeft = cardRight - cardWidth
        val cardBottom = cardTop + cardHeight
        val cardRadius = 16f * scaleDp
        val cardPaddingH = 14f * scaleDp
        val cardPaddingV = 12f * scaleDp

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 0, 0, 0) // FrostedGlassBg
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255) // FrostedGlassBorder
            strokeWidth = 1f * scaleDp
            style = Paint.Style.STROKE
        }
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

        val rowGap = 8f * scaleDp
        val lineHeight = 16f * scaleFont

        // 4-1. 헤더: 줄자(좌) & 물고기(우)
        val headerY = cardTop + cardPaddingV
        drawVector(context, canvas, R.drawable.ic_ruler, cardLeft + cardPaddingH, headerY, 15f * scaleDp, Color.argb(153, 255, 255, 255))
        drawVector(context, canvas, R.drawable.ic_fish, cardRight - cardPaddingH - 16f * scaleDp, headerY, 16f * scaleDp, Color.argb(153, 255, 255, 255))

        // 4-2. 길이 섹션 (한 줄: "길이" ... "26.5 cm")
        val row2Y = headerY + lineHeight + rowGap
        val textBaselineY2 = row2Y + lineHeight - 3f * scaleDp

        val lenLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_NEON_LIME)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        canvas.drawText("길이", cardLeft + cardPaddingH, textBaselineY2, lenLabelPaint)

        val lenUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_NEON_LIME)
            textSize = 11f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val lenNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_NEON_LIME)
            textSize = 14f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val lenUnitStr = "cm"
        val lenNumStr = "${measurement.lengthCm} "
        canvas.drawText(lenUnitStr, cardRight - cardPaddingH, textBaselineY2, lenUnitPaint)
        val lenUnitW = lenUnitPaint.measureText(lenUnitStr)
        canvas.drawText(lenNumStr, cardRight - cardPaddingH - lenUnitW, textBaselineY2, lenNumPaint)

        // 4-3. 무게 섹션 (한 줄: "무게" ... "606 g")
        val row3Y = row2Y + lineHeight + rowGap
        val textBaselineY3 = row3Y + lineHeight - 3f * scaleDp

        val weightLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_WARM_GRAPHITE)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        canvas.drawText("무게", cardLeft + cardPaddingH, textBaselineY3, weightLabelPaint)

        val weightUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(204, 255, 255, 255)
            textSize = 11f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val weightNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 14f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val weightUnitStr = "g"
        val weightNumStr = "${measurement.weightG} "
        canvas.drawText(weightUnitStr, cardRight - cardPaddingH, textBaselineY3, weightUnitPaint)
        val weightUnitW = weightUnitPaint.measureText(weightUnitStr)
        canvas.drawText(weightNumStr, cardRight - cardPaddingH - weightUnitW, textBaselineY3, weightNumPaint)
    }

    private fun drawCaliperBracketOnCanvas(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        halfWidth: Float,
        lipLength: Float,
        isSnout: Boolean,
        strokeWidth: Float,
        shadowWidth: Float
    ) {
        val left = centerX - halfWidth
        val right = centerX + halfWidth
        val y = centerY
        val lipY = if (isSnout) y + lipLength else y - lipLength

        val path = Path().apply {
            moveTo(left, lipY)
            lineTo(left, y)
            lineTo(right, y)
            lineTo(right, lipY)
        }

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(204, 0, 0, 0)
            this.strokeWidth = shadowWidth
            style = Paint.Style.STROKE
        }
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_ELECTRIC_CYAN)
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
        }

        canvas.drawPath(path, shadowPaint)
        canvas.drawPath(path, mainPaint)

        // 중심 '+' 틱
        val tickHalf = 5f * (strokeWidth / 2.4f)
        canvas.drawLine(centerX - tickHalf, y, centerX + tickHalf, y, shadowPaint)
        canvas.drawLine(centerX - tickHalf, y, centerX + tickHalf, y, mainPaint)
        canvas.drawLine(centerX, y - tickHalf, centerX, y + tickHalf, shadowPaint)
        canvas.drawLine(centerX, y - tickHalf, centerX, y + tickHalf, mainPaint)
    }

    private fun drawTimemarkHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scaleDp: Float,
        scaleFont: Float,
        env: EnvironmentData,
        context: Context?
    ) {
        val marginH = 16f * scaleDp
        val marginB = 14f * scaleDp

        // 1. 좌상단 프로스티드 글래스 날씨 카드
        val wLine1 = env.formattedWeatherLine1()
        val wLine2 = env.formattedWeatherLine2()
        val wLine3 = env.formattedWeatherLine3()

        val cardLeft = 12f * scaleDp
        val cardTop = 10f * scaleDp
        val cardRadius = 16f * scaleDp
        val cardPaddingH = 14f * scaleDp
        val cardPaddingV = 12f * scaleDp
        val iconSize = 15f * scaleDp
        val iconTextGap = 8f * scaleDp
        val rowGap = 8f * scaleDp

        val wPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }

        val maxTextW = maxOf(wPaint.measureText(wLine1), wPaint.measureText(wLine2), wPaint.measureText(wLine3))
        val cardW = 140f * scaleDp
        val lineHeight = 16f * scaleFont
        val cardH = cardPaddingV * 2 + (lineHeight * 3) + (rowGap * 2)

        val weatherCardRect = RectF(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            strokeWidth = 1f * scaleDp
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(weatherCardRect, cardRadius, cardRadius, cardBgPaint)
        canvas.drawRoundRect(weatherCardRect, cardRadius, cardRadius, cardBorderPaint)

        // 3줄 행 드로잉 (아이콘을 한글 텍스트 시각적 중심과 일치하도록 오프셋 보정)
        val textX = cardLeft + cardPaddingH + iconSize + iconTextGap
        val iconX = cardLeft + cardPaddingH
        val row1Y = cardTop + cardPaddingV
        val row2Y = row1Y + lineHeight + rowGap
        val row3Y = row2Y + lineHeight + rowGap
        val iconTint = Color.argb(204, 255, 255, 255)
        val weatherIconYOffset = 1.8f * scaleDp

        drawVector(context, canvas, R.drawable.ic_weather_cloud, iconX, row1Y + weatherIconYOffset, iconSize, iconTint)
        canvas.drawText(wLine1, textX, row1Y + lineHeight - 3f * scaleDp, wPaint)

        drawVector(context, canvas, R.drawable.ic_wind, iconX, row2Y + weatherIconYOffset, iconSize, iconTint)
        canvas.drawText(wLine2, textX, row2Y + lineHeight - 3f * scaleDp, wPaint)

        drawVector(context, canvas, R.drawable.ic_barometer, iconX, row3Y + weatherIconYOffset, iconSize, iconTint)
        canvas.drawText(wLine3, textX, row3Y + lineHeight - 3f * scaleDp, wPaint)

        // 2. 우측 미니 지도 크기 및 텍스트 허용 너비 계산
        val hasMap = env.staticMapBitmap != null
        val mapSize = if (hasMap) 88f * scaleDp else 0f
        val maxTextWidth = if (hasMap) {
            width - (marginH * 2) - mapSize - (12f * scaleDp)
        } else {
            width - (marginH * 2)
        }

        // 3. 주소 전용 라인 (앞에 위치 핀 아이콘 ic_location_pin)
        val rawAddress = env.formattedAddressOnly()
        val pinIconSize = 14f * scaleDp
        val pinGap = 4f * scaleDp
        val addressIndent = pinIconSize + pinGap

        val addressPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 12f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }

        val addressLayout = android.text.StaticLayout.Builder.obtain(
            rawAddress, 0, rawAddress.length, addressPaint, (maxTextWidth - addressIndent).toInt().coerceAtLeast(100)
        )
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        val addressHeight = addressLayout.height.toFloat()
        val addressTopY = height - marginB - addressHeight

        // 핀 아이콘 드로잉 (한글 텍스트 높이 및 베이스라인과 시각적 중심 일치 보정)
        drawVector(context, canvas, R.drawable.ic_location_pin, marginH, addressTopY + 3.2f * scaleDp, pinIconSize, Color.WHITE)

        canvas.save()
        canvas.translate(marginH + addressIndent, addressTopY)
        addressLayout.draw(canvas)
        canvas.restore()

        // 4. 날짜 및 요일 라인: "2026. 10. 24 (토)"
        val dateAndDayText = env.formattedDateAndDayClean()
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 14f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val line2Y = addressTopY - (5f * scaleDp)
        if (dateAndDayText.isNotEmpty()) {
            canvas.drawText(dateAndDayText, marginH, line2Y, datePaint)
        }

        // 5. 시간 텍스트 (42sp 대형 볼드 서체)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 42f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.03f
            setShadowLayer(TEXT_SHADOW_RADIUS_DP * scaleDp, 0f, TEXT_SHADOW_OFFSET_Y_DP * scaleDp, TEXT_SHADOW_COLOR)
        }
        val line1Y = if (dateAndDayText.isNotEmpty()) {
            line2Y - (datePaint.textSize + 4f * scaleDp)
        } else {
            addressTopY - (4f * scaleDp)
        }
        canvas.drawText(env.timeFormatted, marginH, line1Y, timePaint)

        // 6. 우측 하단 미니 정적 지도
        env.staticMapBitmap?.let { mapBm ->
            val mapRight = width - marginH
            val mapLeft = mapRight - mapSize
            val mapBottomPos = height - marginB
            val mapTop = mapBottomPos - mapSize

            val roundedMap = getRoundedCornerBitmap(mapBm, 10f * scaleDp, mapSize.toInt())

            // 지도 외곽 그림자
            val shadowMapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 0, 0, 0)
                setShadowLayer(8f * scaleDp, 0f, 3f * scaleDp, Color.argb(160, 0, 0, 0))
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                10f * scaleDp,
                10f * scaleDp,
                shadowMapPaint
            )

            // 지도 비트맵
            canvas.drawBitmap(roundedMap, mapLeft, mapTop, null)

            // 흰색 얇은 테두리
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 1.5f * scaleDp
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                10f * scaleDp,
                10f * scaleDp,
                borderPaint
            )
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float, targetSize: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, targetSize, targetSize)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)

        if (scaled != bitmap && scaled != output) scaled.recycle()
        return output
    }
}
