package com.jongh0.bassshot.domain.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

        // 2. 세로 방향 보장 (가로 사진일 경우 90도 회전하여 세로 뷰파인더에 맞춤)
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
