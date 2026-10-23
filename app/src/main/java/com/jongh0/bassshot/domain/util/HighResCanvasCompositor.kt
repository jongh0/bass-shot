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
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData

object HighResCanvasCompositor {

    /**
     * 원본 해상도 비트맵 위에 Timemarker 스타일의 고화질 타임마크와 계측선을 직접 합성
     */
    fun composite(
        originalBitmap: Bitmap,
        environmentData: EnvironmentData,
        measurement: BassMeasurementUiModel?
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. 원본 사진 드로잉
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val scale = width / 1080f

        // 2. 계측 가이드라인 및 스펙 뱃지 드로잉 (결과 존재 시)
        measurement?.let { m ->
            drawMeasurementHUD(canvas, width, height, scale, m)
        }

        // 3. Timemarker 스타일 하단 정보 드로잉 (글자체 드롭 섀도우 적용)
        drawTimemarkHUD(canvas, width, height, scale, environmentData)

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

        // 주 캘리퍼스 가이드라인 (앰버 골드 + 외곽 그림자)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 0, 0, 0)
            strokeWidth = 5.5f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(24f * scale, 14f * scale), 0f)
        }
        val mainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5B301") // Leica Amber
            strokeWidth = 2.5f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(24f * scale, 14f * scale), 0f)
        }

        canvas.drawLine(snoutX, snoutY, tailX, tailY, shadowPaint)
        canvas.drawLine(snoutX, snoutY, tailX, tailY, mainLinePaint)

        // 양 끝 정밀 캘리퍼스 턱(Jaw)
        val jawHalfWidth = 28f * scale
        val jawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5B301")
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val jawShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 0, 0, 0)
            strokeWidth = 6f * scale
            style = Paint.Style.STROKE
        }

        // Snout jaw
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawShadow)
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawPaint)

        // Tail jaw
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawShadow)
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawPaint)

        // 체고(Girth) 가이드라인 (시안 색상 점선)
        val girthLeftX = measurement.maxGirthLeft.x * width
        val girthLeftY = measurement.maxGirthLeft.y * height
        val girthRightX = measurement.maxGirthRight.x * width
        val girthRightY = measurement.maxGirthRight.y * height

        val girthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF") // Tactical Cyan
            strokeWidth = 2f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(14f * scale, 10f * scale), 0f)
        }
        canvas.drawLine(girthLeftX, girthLeftY, girthRightX, girthRightY, girthPaint)

        // 상단 스펙 뱃지 그리기
        val badgeText = "${measurement.lengthCm} cm  |  ${measurement.weightG} g  (체고 ${measurement.girthCm} cm)"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(badgeText, 0, badgeText.length, textBounds)
        val badgePaddingH = 32f * scale
        val badgePaddingV = 16f * scale
        val badgeWidth = textBounds.width() + badgePaddingH * 2
        val badgeHeight = textBounds.height() + badgePaddingV * 2
        val badgeLeft = (width - badgeWidth) / 2f
        val badgeTop = 50f * scale
        val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 18, 19, 22)
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1.5f * scale
            style = Paint.Style.STROKE
        }

        canvas.drawRoundRect(badgeRect, 12f * scale, 12f * scale, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 12f * scale, 12f * scale, badgeBorderPaint)

        val textY = badgeTop + badgePaddingV + textBounds.height() - (textBounds.bottom / 2f)
        canvas.drawText(badgeText, width / 2f, textY, textPaint)
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

        // 1. 큰 시간 텍스트 (Tall Condensed 스타일)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 125f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.04f
            setShadowLayer(10f * scale, 0f, 4f * scale, Color.argb(220, 0, 0, 0))
        }
        val timeBounds = Rect()
        val timeText = env.timeFormatted
        timePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)

        // 하단 주소 높이 고려하여 시간 Y 위치 결정
        val addressText = env.formattedAddressAndWeatherLine()
        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 28f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(8f * scale, 0f, 3f * scale, Color.argb(220, 0, 0, 0))
        }
        val addressBounds = Rect()
        addressPaint.getTextBounds(addressText, 0, addressText.length, addressBounds)

        val addressY = height - marginB
        val timeY = addressY - addressBounds.height() - (28f * scale)
        val timeX = marginH

        canvas.drawText(timeText, timeX, timeY, timePaint)

        // 2. 옐로우 얇은 수직 분할 바
        val barX = timeX + timeBounds.width() + (18f * scale)
        val barTop = timeY - timeBounds.height() + (8f * scale)
        val barBottom = timeY - (4f * scale)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5B301")
            strokeWidth = 4.5f * scale
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(200, 0, 0, 0))
        }
        canvas.drawLine(barX, barTop, barX, barBottom, barPaint)

        // 3. 2층 날짜/요일 (1열: 2026년10월23일 / 2열: 금요일)
        val metaX = barX + (18f * scale)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 30f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(8f * scale, 0f, 3f * scale, Color.argb(220, 0, 0, 0))
        }

        val dateY = barTop + (30f * scale)
        if (env.dateFormatted.isNotEmpty()) {
            canvas.drawText(env.dateFormatted, metaX, dateY, datePaint)
        }
        if (env.dayOfWeekFormatted.isNotEmpty()) {
            val dayOfWeekY = dateY + (34f * scale)
            canvas.drawText(env.dayOfWeekFormatted, metaX, dayOfWeekY, datePaint)
        }

        // 4. 하단 주소 및 날씨 정보 (시간 아래쪽으로 자연스럽게 표시)
        canvas.drawText(addressText, timeX, addressY, addressPaint)

        // 5. 우측 하단 시원하게 확대된 미니 정적 지도 (240px * scale, 약 92dp 크기)
        env.staticMapBitmap?.let { mapBm ->
            val mapSize = 230f * scale
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
