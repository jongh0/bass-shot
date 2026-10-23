package com.jongh0.bassshot.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ImportedPhotoResult(
    val bitmap: Bitmap,
    val capturedDateFormatted: String? = null,
    val capturedDayOfWeekFormatted: String? = null,
    val capturedTimeFormatted: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

object BitmapProcessor {

    /**
     * CameraX ImageProxy로부터 회전 및 3:4 세로 비율이 보정된 고해상도 비트맵 생성
     */
    fun processImageProxy(imageProxy: ImageProxy): Bitmap {
        val rawBitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // 1. 센서 방향 회전 보정
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else {
            rawBitmap
        }

        // 2. 세로 방향 보장 (실시간 카메라 뷰파인더 전용: 가로 센서일 경우 90도 회전)
        val portraitBitmap = if (rotatedBitmap.width > rotatedBitmap.height) {
            val matrix = Matrix().apply { postRotate(90f) }
            val fixed = Bitmap.createBitmap(
                rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height, matrix, true
            )
            if (fixed != rotatedBitmap) rotatedBitmap.recycle()
            fixed
        } else {
            rotatedBitmap
        }

        // 3. 3:4 세로 종횡비 중앙 크롭 (프리뷰와 결과물 100% 일치)
        val cropped = cropToAspectRatio(portraitBitmap, 3f / 4f)

        // 4. 초고해상도 OOM 방어: 장축이 4032px 초과 시 4032px로 스케일링
        val maxDim = max(cropped.width, cropped.height)
        return if (maxDim > 4032) {
            val scale = 4032f / maxDim
            val scaled = Bitmap.createScaledBitmap(
                cropped,
                (cropped.width * scale).roundToInt(),
                (cropped.height * scale).roundToInt(),
                true
            )
            if (scaled != cropped) cropped.recycle()
            scaled
        } else {
            cropped
        }
    }

    /**
     * 갤러리에서 불러온 외부 사진의 비트맵 디코딩, EXIF 방향 보정 및 메타데이터(일시, 위치) 추출
     * (중요: 사용자가 찍은 배스/손이 잘리지 않도록 비파괴 원본 비율을 100% 보존합니다)
     */
    fun decodeUriWithExif(context: Context, uri: Uri): ImportedPhotoResult {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        var dateFormatted: String? = null
        var dayOfWeekFormatted: String? = null
        var timeFormatted: String? = null
        var latitude: Double? = null
        var longitude: Double? = null

        // 1. EXIF 메타데이터 파싱
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    latitude = latLong[0]
                    longitude = latLong[1]
                }

                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (!dateStr.isNullOrBlank()) {
                    val exifParser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    val dateObj = exifParser.parse(dateStr)
                    if (dateObj != null) {
                        dateFormatted = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(dateObj)
                        dayOfWeekFormatted = SimpleDateFormat("EEEE", Locale.KOREA).format(dateObj)
                        timeFormatted = SimpleDateFormat("HH:mm", Locale.KOREA).format(dateObj)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 비트맵 크기 사전 측정 (OOM 방어)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var sampleSize = 1
        val maxSide = max(options.outWidth, options.outHeight)
        while (maxSide / sampleSize > 4096) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("사진을 읽어올 수 없습니다.")

        // 3. EXIF 정규 회전각만 보정 (임의 회전이나 크롭 금지)
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val correctlyOrientedBitmap = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val fixed = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (fixed != rawBitmap) rawBitmap.recycle()
            fixed
        } else {
            rawBitmap
        }

        // 4. 초고해상도 OOM 방어 스케일링 (비율 유지, 크롭 없음)
        val maxDim = max(correctlyOrientedBitmap.width, correctlyOrientedBitmap.height)
        val finalBitmap = if (maxDim > 4032) {
            val scale = 4032f / maxDim
            val scaled = Bitmap.createScaledBitmap(
                correctlyOrientedBitmap,
                (correctlyOrientedBitmap.width * scale).roundToInt(),
                (correctlyOrientedBitmap.height * scale).roundToInt(),
                true
            )
            if (scaled != correctlyOrientedBitmap) correctlyOrientedBitmap.recycle()
            scaled
        } else {
            correctlyOrientedBitmap
        }

        return ImportedPhotoResult(
            bitmap = finalBitmap,
            capturedDateFormatted = dateFormatted,
            capturedDayOfWeekFormatted = dayOfWeekFormatted,
            capturedTimeFormatted = timeFormatted,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Gemini Flash API 전송 속도 최적화를 위한 1024px 다운스케일링
     */
    fun scaleForGemini(source: Bitmap, targetLongEdge: Int = 1024): Bitmap {
        val longEdge = max(source.width, source.height)
        if (longEdge <= targetLongEdge) return source

        val scale = targetLongEdge.toFloat() / longEdge
        val targetWidth = (source.width * scale).roundToInt()
        val targetHeight = (source.height * scale).roundToInt()

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * 비트맵을 지정한 세로 종횡비(targetAspect = width / height)로 중앙 크롭
     */
    private fun cropToAspectRatio(source: Bitmap, targetAspect: Float): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val currentAspect = srcWidth.toFloat() / srcHeight.toFloat()

        if (kotlin.math.abs(currentAspect - targetAspect) < 0.01f) {
            return source
        }

        val cropWidth: Int
        val cropHeight: Int

        if (currentAspect > targetAspect) {
            // 가로가 더 넓음 -> 좌우를 잘라냄
            cropHeight = srcHeight
            cropWidth = (srcHeight * targetAspect).roundToInt()
        } else {
            // 세로가 더 길음 -> 상하를 잘라냄
            cropWidth = srcWidth
            cropHeight = (srcWidth / targetAspect).roundToInt()
        }

        val startX = max(0, (srcWidth - cropWidth) / 2)
        val startY = max(0, (srcHeight - cropHeight) / 2)

        val cropped = Bitmap.createBitmap(
            source,
            startX,
            startY,
            min(cropWidth, srcWidth - startX),
            min(cropHeight, srcHeight - startY)
        )
        if (cropped != source) {
            source.recycle()
        }
        return cropped
    }
}
