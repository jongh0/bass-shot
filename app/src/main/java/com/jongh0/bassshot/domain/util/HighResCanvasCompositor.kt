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

        // 화면(Compose 390dp 기준)과 1:1 완벽하게 동일한 비례/크기/배치 스케일링 계수
        val scale = width / 390f

        val hasMeasurement = measurement != null && measurement.isBassDetected

        // 2. 계측 가이드라인 및 스펙 뱃지 드로잉 (배스 감지 시에만 합성)
        if (hasMeasurement) {
            drawMeasurementHUD(canvas, width, height, scale, measurement!!)
        }

        // 3. 4단 타임마크 정보 드로잉 (불러온 사진이 아닌 직접 촬영본일 때만 합성)
        if (!isImportedPhoto) {
            drawTimemarkHUD(canvas, width, height, scale, environmentData, hasMeasurement)
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
            strokeWidth = 5f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(16f * scale, 10f * scale), 0f)
        }
        val mainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 2.4f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(16f * scale, 10f * scale), 0f)
        }

        canvas.drawLine(snoutX, snoutY, tailX, tailY, shadowPaint)
        canvas.drawLine(snoutX, snoutY, tailX, tailY, mainLinePaint)

        // Snout & Tail 캘리퍼스 턱(Jaw)
        val jawHalfWidth = 26f * scale
        val jawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 2.6f * scale
            style = Paint.Style.STROKE
        }
        val jawShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 0, 0, 0)
            strokeWidth = 5.5f * scale
            style = Paint.Style.STROKE
        }

        // Snout jaw
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawShadow)
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawPaint)

        // Tail jaw
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawShadow)
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawPaint)

        // 좌상단(TopLeft) 스펙 텍스트 (화면과 100% 동일한 비례, 폰트 크기, 드롭 섀도우)
        val badgeLeft = 12f * scale
        val badgeTop = 10f * scale
        val lineHeight = 20f * scale

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_WARM_GRAPHITE)
            textSize = 11f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 15f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_TITANIUM_SILVER)
            textSize = 11f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }

        val colValX = badgeLeft + (28f * scale)

        // 1줄: 길이
        val y1 = badgeTop + (15f * scale)
        canvas.drawText("길이", badgeLeft, y1, labelPaint)
        canvas.drawText("${measurement.lengthCm}", colValX, y1, valPaint)
        val u1X = colValX + valPaint.measureText("${measurement.lengthCm}") + (3f * scale)
        canvas.drawText("cm", u1X, y1, unitPaint)

        // 2줄: 무게
        val y2 = y1 + lineHeight
        canvas.drawText("무게", badgeLeft, y2, labelPaint)
        canvas.drawText("${measurement.weightG}", colValX, y2, valPaint)
        val u2X = colValX + valPaint.measureText("${measurement.weightG}") + (3f * scale)
        canvas.drawText("g", u2X, y2, unitPaint)

        // 3줄: 체고
        val y3 = y2 + lineHeight
        canvas.drawText("체고", badgeLeft, y3, labelPaint)
        canvas.drawText("${measurement.girthCm}", colValX, y3, valPaint)
        val u3X = colValX + valPaint.measureText("${measurement.girthCm}") + (3f * scale)
        canvas.drawText("cm", u3X, y3, unitPaint)
    }

    private fun drawTimemarkHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        env: EnvironmentData,
        hasMeasurement: Boolean = false
    ) {
        val marginH = 16f * scale
        val marginB = 14f * scale

        // 0. 좌측 상단 날씨 3줄 정보 (배경 박스 없이 드롭 섀도우로 좌상단에 밀착 배치)
        val wLine1 = env.formattedWeatherLine1()
        val wLine2 = env.formattedWeatherLine2()
        val wLine3 = env.formattedWeatherLine3()

        val wLeft = if (hasMeasurement) 96f * scale else 12f * scale
        val wTop = 10f * scale
        val wLineHeight = 20f * scale

        val wPaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 11.5f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }
        val wPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_TITANIUM_SILVER)
            textSize = 11f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }

        val wy1 = wTop + (15f * scale)
        canvas.drawText(wLine1, wLeft, wy1, wPaintBold)
        canvas.drawText(wLine2, wLeft, wy1 + wLineHeight, wPaintNormal)
        canvas.drawText(wLine3, wLeft, wy1 + wLineHeight * 2, wPaintNormal)

        // 우측 미니 지도 크기 및 텍스트 허용 너비 계산
        val hasMap = env.staticMapBitmap != null
        val mapSize = if (hasMap) 92f * scale else 0f
        val maxTextWidth = if (hasMap) {
            width - (marginH * 2) - mapSize - (16f * scale)
        } else {
            width - (marginH * 2)
        }

        // 3. 주소 전용 라인 (길어지면 자동으로 2줄 래핑)
        val rawAddress = env.formattedAddressOnly()
        val addressPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_TITANIUM_SILVER)
            textSize = 11.5f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
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
            textSize = 13f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }
        val line2Y = addressTopY - (6f * scale)
        if (dateAndDayText.isNotEmpty()) {
            canvas.drawText(dateAndDayText, marginH, line2Y, datePaint)
        }

        // 1. 시간 텍스트 (Tall Condensed 42sp 스타일)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 42f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.03f
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.argb(220, 0, 0, 0))
        }
        val line1Y = line2Y - (datePaint.textSize + 4f * scale)
        canvas.drawText(env.timeFormatted, marginH, line1Y, timePaint)

        // 5. 우측 하단 미니 정적 지도
        env.staticMapBitmap?.let { mapBm ->
            val mapRight = width - marginH
            val mapLeft = mapRight - mapSize
            val mapBottomPos = height - marginB
            val mapTop = mapBottomPos - mapSize

            val roundedMap = getRoundedCornerBitmap(mapBm, 8f * scale, mapSize.toInt())

            // 지도 외곽 그림자
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 0, 0, 0)
                setShadowLayer(8f * scale, 0f, 3f * scale, Color.argb(160, 0, 0, 0))
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                8f * scale,
                8f * scale,
                shadowPaint
            )

            // 지도 비트맵
            canvas.drawBitmap(roundedMap, mapLeft, mapTop, null)

            // 흰색 얇은 테두리
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 1f * scale
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                8f * scale,
                8f * scale,
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
