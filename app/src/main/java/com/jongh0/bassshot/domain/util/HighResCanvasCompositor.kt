package com.jongh0.bassshot.domain.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData
import kotlin.math.max

object HighResCanvasCompositor {

    /**
     * 원본 해상도(예: 3024x4032) 비트맵 위에 고화질 타임마크와 계측선을 직접 합성하여 반환
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

        // 1. 원본 사진 렌더링
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 해상도 비례 스케일 팩터 (기준 1080px 너비)
        val scale = width / 1080f

        // 2. 하단 블랙 그라데이션 음영 (텍스트 가독성 확보: 하단 28% 영역)
        val gradTop = height * 0.72f
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, gradTop, 0f, height.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(140, 10, 10, 12),
                    Color.argb(230, 10, 10, 12)
                ),
                floatArrayOf(0.0f, 0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, gradTop, width.toFloat(), height.toFloat(), gradientPaint)

        // 3. 계측 가이드라인 및 스펙 뱃지 렌더링 (측정 결과 존재 시)
        measurement?.let { m ->
            drawMeasurementHUD(canvas, width, height, scale, m)
        }

        // 4. 하단 타임마크 정보 렌더링
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

        // 캘리퍼스 주 가이드라인 (앰버 골드 + 블랙 드롭 섀도우)
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

        // 양 끝 정밀 캘리퍼스 턱(Jaw) 마커 (수평 턱)
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

        // 상단 스펙 뱃지 그리기 (스모크 글래스 캡슐)
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
        val marginB = 48f * scale

        // 큰 시간 텍스트 (예: 14:18)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 96f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val timeBounds = Rect()
        val timeText = env.timeFormatted
        timePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)

        val timeX = marginH
        val timeY = height - marginB

        canvas.drawText(timeText, timeX, timeY, timePaint)

        // 앰버 수직 구분선
        val barX = timeX + timeBounds.width() + (22f * scale)
        val barTop = timeY - timeBounds.height() + (4f * scale)
        val barBottom = timeY - (4f * scale)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5B301")
            strokeWidth = 6f * scale
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(barX, barTop, barX, barBottom, barPaint)

        // 우측 메타데이터 텍스트 (날짜, 기상/기압, 주소)
        val metaX = barX + (22f * scale)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F9FA")
            textSize = 26f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val weatherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A0A6B2")
            textSize = 24f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        var currentLineY = barTop + (22f * scale)
        if (env.dateFormatted.isNotEmpty()) {
            canvas.drawText(env.dateFormatted, metaX, currentLineY, datePaint)
            currentLineY += (30f * scale)
        }

        canvas.drawText(env.formattedWeatherLine(), metaX, currentLineY, weatherPaint)

        if (!env.addressKorean.isNullOrBlank()) {
            currentLineY += (28f * scale)
            canvas.drawText(env.addressKorean, metaX, currentLineY, weatherPaint)
        }

        // 우측 하단 정적 미니맵 (다운로드된 비트맵이 있을 시)
        env.staticMapBitmap?.let { mapBm ->
            val mapSize = 160f * scale
            val mapRight = width - marginH
            val mapLeft = mapRight - mapSize
            val mapBottomPos = height - marginB
            val mapTop = mapBottomPos - mapSize

            val roundedMap = getRoundedCornerBitmap(mapBm, 16f * scale, mapSize.toInt())
            canvas.drawBitmap(roundedMap, mapLeft, mapTop, null)

            // 미니맵 외곽선
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(45, 255, 255, 255)
                strokeWidth = 2f * scale
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                16f * scale,
                16f * scale,
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
