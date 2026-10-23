package com.jongh0.bassshot.domain.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData

object HighResCanvasCompositor {

    private const val COLOR_OPTICAL_WHITE = "#FFFFFF"
    private const val COLOR_TITANIUM_SILVER = "#E2E4E9"
    private const val COLOR_WARM_GRAPHITE = "#9496A1"

    /**
     * 원본 해상도 비트맵 위에 고화질 타임마크와 계측선을 직접 합성
     * @param isImportedPhoto 갤러리에서 불러온 사진인 경우 타임마크 생략 (오직 계측 결과만 합성)
     */
    fun composite(
        originalBitmap: Bitmap,
        environmentData: EnvironmentData,
        measurement: BassMeasurementUiModel?,
        isImportedPhoto: Boolean = false
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. 원본 사진 드로잉
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val scale = width / 1080f

        // 2. 계측 가이드라인 및 스펙 뱃지 드로잉 (배스 감지 시에만 합성)
        if (measurement != null && measurement.isBassDetected) {
            drawMeasurementHUD(canvas, width, height, scale, measurement)
        }

        // 3. 4단 타임마크 정보 드로잉 (불러온 사진이 아닌 직접 촬영본일 때만 합성)
        if (!isImportedPhoto) {
            drawTimemarkHUD(canvas, width, height, scale, environmentData)
        }

        return output
    }

    private fun drawMeasurementHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        measurement: BassMeasurementUiModel
    ) {
        val snoutX = measurement.snoutPoint.x * width
        val snoutY = measurement.snoutPoint.y * height
        val tailX = measurement.tailPoint.x * width
        val tailY = measurement.tailPoint.y * height

        // 주 캘리퍼스 가이드라인 (크리스프 옵티컬 화이트 + 외곽 그림자)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 0, 0, 0)
            strokeWidth = 6f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(24f * scale, 14f * scale), 0f)
        }
        val mainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 2.6f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(24f * scale, 14f * scale), 0f)
        }

        canvas.drawLine(snoutX, snoutY, tailX, tailY, shadowPaint)
        canvas.drawLine(snoutX, snoutY, tailX, tailY, mainLinePaint)

        // Snout & Tail 캘리퍼스 턱(Jaw)
        val jawHalfWidth = 28f * scale
        val jawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val jawShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 0, 0, 0)
            strokeWidth = 6.5f * scale
            style = Paint.Style.STROKE
        }

        // Snout jaw
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawShadow)
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawPaint)

        // Tail jaw
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawShadow)
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawPaint)

        // 좌상(TopLeft) 큼직하고 살짝 투명한 스펙 뱃지 그리기 (길이, 무게, 체고 1줄씩)
        val line1 = "길이  ${measurement.lengthCm} cm"
        val line2 = "무게  ${measurement.weightG} g"
        val line3 = "체고  ${measurement.girthCm} cm"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(5f * scale, 0f, 2f * scale, Color.argb(200, 0, 0, 0))
        }

        val b1 = Rect()
        val b2 = Rect()
        val b3 = Rect()
        textPaint.getTextBounds(line1, 0, line1.length, b1)
        textPaint.getTextBounds(line2, 0, line2.length, b2)
        textPaint.getTextBounds(line3, 0, line3.length, b3)

        val maxTextW = maxOf(b1.width(), b2.width(), b3.width())
        val lineHeight = 36f * scale
        val badgePaddingH = 28f * scale
        val badgePaddingV = 20f * scale
        val badgeWidth = maxTextW + badgePaddingH * 2
        val badgeHeight = (lineHeight * 2) + b1.height() + (badgePaddingV * 2)

        // 좌측 상단 모서리에 배치
        val badgeLeft = 44f * scale
        val badgeTop = 44f * scale
        val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

        // 살짝 투명한 배경 (약 65% 불투명도)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 16, 17, 21)
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            strokeWidth = 1.5f * scale
            style = Paint.Style.STROKE
        }

        canvas.drawRoundRect(badgeRect, 16f * scale, 16f * scale, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 16f * scale, 16f * scale, badgeBorderPaint)

        val tY1 = badgeTop + badgePaddingV + b1.height()
        canvas.drawText(line1, badgeLeft + badgePaddingH, tY1, textPaint)
        canvas.drawText(line2, badgeLeft + badgePaddingH, tY1 + lineHeight, textPaint)
        canvas.drawText(line3, badgeLeft + badgePaddingH, tY1 + lineHeight * 2, textPaint)
    }

    private fun drawTimemarkHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        env: EnvironmentData
    ) {
        val marginH = 44f * scale
        val marginB = 44f * scale

        // 0. 우측 상단 날씨 3줄 정보 뱃지 (날씨/기온, 바람, 기압)
        val wLine1 = env.formattedWeatherLine1()
        val wLine2 = env.formattedWeatherLine2()
        val wLine3 = env.formattedWeatherLine3()

        val wPaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 24f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(5f * scale, 0f, 2f * scale, Color.argb(200, 0, 0, 0))
        }
        val wPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_TITANIUM_SILVER)
            textSize = 22f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(5f * scale, 0f, 2f * scale, Color.argb(200, 0, 0, 0))
        }

        val wb1 = Rect()
        val wb2 = Rect()
        val wb3 = Rect()
        wPaintBold.getTextBounds(wLine1, 0, wLine1.length, wb1)
        wPaintNormal.getTextBounds(wLine2, 0, wLine2.length, wb2)
        wPaintNormal.getTextBounds(wLine3, 0, wLine3.length, wb3)

        val maxWWidth = maxOf(wb1.width(), wb2.width(), wb3.width())
        val wLineHeight = 30f * scale
        val wPadH = 24f * scale
        val wPadV = 16f * scale
        val wBoxW = maxWWidth + wPadH * 2
        val wBoxH = (wLineHeight * 2) + wb1.height() + wPadV * 2

        val wBoxRight = width - (44f * scale)
        val wBoxLeft = wBoxRight - wBoxW
        val wBoxTop = 44f * scale
        val wBoxBottom = wBoxTop + wBoxH

        val wBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 16, 17, 21)
            style = Paint.Style.FILL
        }
        val wBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            strokeWidth = 1.5f * scale
            style = Paint.Style.STROKE
        }

        canvas.drawRoundRect(RectF(wBoxLeft, wBoxTop, wBoxRight, wBoxBottom), 14f * scale, 14f * scale, wBgPaint)
        canvas.drawRoundRect(RectF(wBoxLeft, wBoxTop, wBoxRight, wBoxBottom), 14f * scale, 14f * scale, wBorderPaint)

        val wtY1 = wBoxTop + wPadV + wb1.height()
        canvas.drawText(wLine1, wBoxRight - wPadH, wtY1, wPaintBold)
        canvas.drawText(wLine2, wBoxRight - wPadH, wtY1 + wLineHeight, wPaintNormal)
        canvas.drawText(wLine3, wBoxRight - wPadH, wtY1 + wLineHeight * 2, wPaintNormal)

        // 우측 미니 지도 크기 및 텍스트 허용 너비 계산
        val hasMap = env.staticMapBitmap != null
        val mapSize = if (hasMap) 220f * scale else 0f
        val maxTextWidth = if (hasMap) {
            width - (marginH * 2) - mapSize - (24f * scale)
        } else {
            width - (marginH * 2)
        }

        // 3. 주소 전용 라인 (길어지면 자동으로 2줄 래핑)
        val rawAddress = env.formattedAddressOnly()
        val addressPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_TITANIUM_SILVER)
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(8f * scale, 0f, 3f * scale, Color.argb(220, 0, 0, 0))
        }

        val addressLayout = android.text.StaticLayout.Builder.obtain(
            rawAddress, 0, rawAddress.length, addressPaint, maxTextWidth.toInt().coerceAtLeast(100)
        )
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        val addressHeight = addressLayout.height.toFloat()
        val addressTopY = height - marginB - addressHeight

        canvas.save()
        canvas.translate(marginH, addressTopY)
        addressLayout.draw(canvas)
        canvas.restore()

        // 2. 날짜 및 요일 라인 (예: 2026년 10월 21일 (금요일))
        val dateAndDayText = env.formattedDateAndDayLine()
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 32f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(8f * scale, 0f, 3f * scale, Color.argb(220, 0, 0, 0))
        }
        val line2Y = addressTopY - (16f * scale)
        if (dateAndDayText.isNotEmpty()) {
            canvas.drawText(dateAndDayText, marginH, line2Y, datePaint)
        }

        // 1. 시간 텍스트 (Tall Condensed 스타일)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 108f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.03f
            setShadowLayer(10f * scale, 0f, 4f * scale, Color.argb(220, 0, 0, 0))
        }
        val line1Y = line2Y - (42f * scale)
        canvas.drawText(env.timeFormatted, marginH, line1Y, timePaint)

        // 5. 우측 하단 미니 정적 지도
        env.staticMapBitmap?.let { mapBm ->
            val mapRight = width - marginH
            val mapLeft = mapRight - mapSize
            val mapBottomPos = height - marginB
            val mapTop = mapBottomPos - mapSize

            val roundedMap = getRoundedCornerBitmap(mapBm, 20f * scale, mapSize.toInt())

            // 지도 외곽 그림자
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 0, 0, 0)
                setShadowLayer(16f * scale, 0f, 6f * scale, Color.argb(160, 0, 0, 0))
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                20f * scale,
                20f * scale,
                shadowPaint
            )

            // 지도 비트맵
            canvas.drawBitmap(roundedMap, mapLeft, mapTop, null)

            // 흰색 얇은 테두리
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 3f * scale
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                20f * scale,
                20f * scale,
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
