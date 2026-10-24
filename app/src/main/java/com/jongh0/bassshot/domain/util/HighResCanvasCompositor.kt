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
    private const val COLOR_MUTED_SILVER = "#C4C7D0"

    // 화면(MeasurementOverlay, TimemarkOverlay)의 textShadow(blurRadius=6px, offset=0,2px)와
    // 1:1로 시각적 매칭되는 은은하고 부드러운 텍스트 그림자 스케일 (화면과 동일한 얇고 자연스러운 음영)
    private const val TEXT_SHADOW_RADIUS_DP = 2.0f
    private const val TEXT_SHADOW_OFFSET_Y_DP = 0.67f
    private val TEXT_SHADOW_COLOR = Color.argb(135, 0, 0, 0)

    /**
     * 원본 해상도 비트맵 위에 고화질 타임마크와 계측선을 직접 합성
     * @param containerWidthDp 화면(Compose)에서 이미지가 표시된 컨테이너의 실제 dp 폭 (예: 360f, 390f, 411f)
     * @param fontScale 단말기 시스템 글꼴 크기 배율
     * @param isImportedPhoto 갤러리에서 불러온 사진인 경우 타임마크 생략 (오직 계측 결과만 합성)
     */
    fun composite(
        originalBitmap: Bitmap,
        environmentData: EnvironmentData,
        measurement: BassMeasurementUiModel?,
        isImportedPhoto: Boolean = false,
        containerWidthDp: Float = 360f,
        fontScale: Float = 1.0f
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. 원본 사진 드로잉
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 화면(Compose)과 1:1 완벽하게 동일한 비례/크기/배치 스케일링 계수
        // (단말기의 실제 containerWidthDp 및 시스템 fontScale 반영)
        val validContainerWidth = if (containerWidthDp > 100f) containerWidthDp else 360f
        val scaleDp = width / validContainerWidth
        val scaleFont = scaleDp * fontScale

        val hasMeasurement = measurement != null && (measurement.isBassDetected || measurement.isManualFallback)
        val hasWeather = !isImportedPhoto

        // 2. 계측 가이드라인 및 스펙 뱃지 드로잉 (배스 감지 또는 직접 계측 시 합성)
        if (hasMeasurement) {
            drawMeasurementHUD(canvas, width, height, scaleDp, scaleFont, measurement!!, hasWeather)
        }

        // 3. 4단 타임마크 정보 드로잉 (불러온 사진이 아닌 직접 촬영본일 때만 합성)
        if (hasWeather) {
            drawTimemarkHUD(canvas, width, height, scaleDp, scaleFont, environmentData)
        }

        return output
    }

    private fun drawMeasurementHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scaleDp: Float,
        scaleFont: Float,
        measurement: BassMeasurementUiModel,
        hasWeatherAbove: Boolean = true
    ) {
        val snoutX = measurement.snoutPoint.x * width
        val snoutY = measurement.snoutPoint.y * height
        val tailX = measurement.tailPoint.x * width
        val tailY = measurement.tailPoint.y * height

        // 주 캘리퍼스 가이드라인 (배스를 과도하게 가리지 않는 가볍고 섬세한 점선)
        val dashOn = 5f * scaleDp
        val dashOff = 8.5f * scaleDp
        val lineDash = DashPathEffect(floatArrayOf(dashOn, dashOff), 0f)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 0, 0, 0)
            strokeWidth = 3f * scaleDp
            style = Paint.Style.STROKE
            pathEffect = lineDash
        }
        val mainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 1.4f * scaleDp
            style = Paint.Style.STROKE
            pathEffect = lineDash
        }

        canvas.drawLine(snoutX, snoutY, tailX, tailY, shadowPaint)
        canvas.drawLine(snoutX, snoutY, tailX, tailY, mainLinePaint)

        // Snout & Tail 캘리퍼스 턱(Jaw)
        val jawHalfWidth = 26f * scaleDp
        val jawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            strokeWidth = 2.6f * scaleDp
            style = Paint.Style.STROKE
        }
        val jawShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 0, 0, 0)
            strokeWidth = 5.5f * scaleDp
            style = Paint.Style.STROKE
        }

        // Snout jaw
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawShadow)
        canvas.drawLine(snoutX - jawHalfWidth, snoutY, snoutX + jawHalfWidth, snoutY, jawPaint)

        // Tail jaw
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawShadow)
        canvas.drawLine(tailX - jawHalfWidth, tailY, tailX + jawHalfWidth, tailY, jawPaint)

        // 좌상단(TopLeft) 스펙 텍스트 (날씨 3줄 아래 살짝 간격을 띄워서 배치, 11.5sp, Normal, MutedSilver)
        val badgeLeft = 12f * scaleDp
        val lineHeight = (16f * scaleFont) + (2f * scaleDp)
        val wTop = 10f * scaleDp

        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 11.5f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(
                TEXT_SHADOW_RADIUS_DP * scaleDp,
                0f,
                TEXT_SHADOW_OFFSET_Y_DP * scaleDp,
                TEXT_SHADOW_COLOR
            )
        }

        // 날씨, 바람, 기압 정보 유무나 불러온 사진 여부와 상관없이 항상 계측 화면과 동일한 시각적 간격(13dp 엣지 갭)으로 기압 아래에 합성
        val y1 = wTop + (13f * scaleFont) + (lineHeight * 3) + (11f * scaleDp)
        canvas.drawText("길이 ${measurement.lengthCm}cm", badgeLeft, y1, specPaint)
        canvas.drawText("무게 ${measurement.weightG}g", badgeLeft, y1 + lineHeight, specPaint)
    }

    private fun drawTimemarkHUD(
        canvas: Canvas,
        width: Int,
        height: Int,
        scaleDp: Float,
        scaleFont: Float,
        env: EnvironmentData
    ) {
        val marginH = 16f * scaleDp
        val marginB = 14f * scaleDp

        // 0. 좌측 상단 날씨 3줄 정보 (날짜와 동일한 11.5sp, Normal, OpticalWhite 적용)
        val wLine1 = env.formattedWeatherLine1()
        val wLine2 = env.formattedWeatherLine2()
        val wLine3 = env.formattedWeatherLine3()

        val wLeft = 12f * scaleDp
        val wTop = 10f * scaleDp
        val lineHeight = (16f * scaleFont) + (2f * scaleDp)

        val wPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 11.5f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            setShadowLayer(
                TEXT_SHADOW_RADIUS_DP * scaleDp,
                0f,
                TEXT_SHADOW_OFFSET_Y_DP * scaleDp,
                TEXT_SHADOW_COLOR
            )
        }

        val wy1 = wTop + (13f * scaleFont)
        canvas.drawText(wLine1, wLeft, wy1, wPaint)
        canvas.drawText(wLine2, wLeft, wy1 + lineHeight, wPaint)
        canvas.drawText(wLine3, wLeft, wy1 + lineHeight * 2, wPaint)

        // 우측 미니 지도 크기 및 텍스트 허용 너비 계산
        val hasMap = env.staticMapBitmap != null
        val mapSize = if (hasMap) 88f * scaleDp else 0f
        val maxTextWidth = if (hasMap) {
            width - (marginH * 2) - mapSize - (12f * scaleDp)
        } else {
            width - (marginH * 2)
        }

        // 3. 주소 전용 라인 (길어지면 자동으로 2줄 래핑)
        val rawAddress = env.formattedAddressOnly()
        val addressPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_MUTED_SILVER)
            textSize = 11.5f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(
                TEXT_SHADOW_RADIUS_DP * scaleDp,
                0f,
                TEXT_SHADOW_OFFSET_Y_DP * scaleDp,
                TEXT_SHADOW_COLOR
            )
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
            textSize = 13f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(
                TEXT_SHADOW_RADIUS_DP * scaleDp,
                0f,
                TEXT_SHADOW_OFFSET_Y_DP * scaleDp,
                TEXT_SHADOW_COLOR
            )
        }
        val line2Y = addressTopY - (4f * scaleDp)
        if (dateAndDayText.isNotEmpty()) {
            canvas.drawText(dateAndDayText, marginH, line2Y, datePaint)
        }

        // 1. 시간 텍스트 (Tall Condensed 38sp 스타일, 날짜와 동일한 COLOR_OPTICAL_WHITE 적용)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_OPTICAL_WHITE)
            textSize = 38f * scaleFont
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = -0.03f
            setShadowLayer(
                TEXT_SHADOW_RADIUS_DP * scaleDp,
                0f,
                TEXT_SHADOW_OFFSET_Y_DP * scaleDp,
                TEXT_SHADOW_COLOR
            )
        }
        val line1Y = if (dateAndDayText.isNotEmpty()) {
            line2Y - (datePaint.textSize + 3f * scaleDp)
        } else {
            addressTopY - (3f * scaleDp)
        }
        canvas.drawText(env.timeFormatted, marginH, line1Y, timePaint)

        // 5. 우측 하단 미니 정적 지도
        env.staticMapBitmap?.let { mapBm ->
            val mapRight = width - marginH
            val mapLeft = mapRight - mapSize
            val mapBottomPos = height - marginB
            val mapTop = mapBottomPos - mapSize

            val roundedMap = getRoundedCornerBitmap(mapBm, 10f * scaleDp, mapSize.toInt())

            // 지도 외곽 그림자
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, 0, 0, 0)
                setShadowLayer(8f * scaleDp, 0f, 3f * scaleDp, Color.argb(160, 0, 0, 0))
            }
            canvas.drawRoundRect(
                RectF(mapLeft, mapTop, mapRight, mapBottomPos),
                10f * scaleDp,
                10f * scaleDp,
                shadowPaint
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
