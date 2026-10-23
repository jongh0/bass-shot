package com.jongh0.bassshot.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData
import com.jongh0.bassshot.domain.util.FishExifHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreRepository(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA)

    /**
     * DCIM/BassShot 폴더에 고화질 합성본 JPG 저장 및 EXIF 메타데이터 주입
     * saveOriginalPhoto 가 true일 경우 무보정 원본(_ORIG.jpg)도 함께 저장
     * @return 저장된 합성본의 Uri 문자열
     */
    suspend fun saveCompositeAndOriginalPhotos(
        compositeBitmap: Bitmap,
        originalBitmap: Bitmap?,
        saveOriginalPhoto: Boolean,
        environmentData: EnvironmentData = EnvironmentData(),
        measurement: BassMeasurementUiModel? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = dateFormat.format(Date())
            val compositeFilename = "BASS_${timestamp}.jpg"

            // 1. 합성본 저장 + EXIF 주입
            val compositeUri = saveBitmapToMediaStore(
                bitmap = compositeBitmap,
                filename = compositeFilename,
                subDirectory = "DCIM/BassShot",
                environmentData = environmentData,
                measurement = measurement
            ) ?: throw IllegalStateException("합성본 MediaStore 저장에 실패했습니다.")

            // 2. 사용자가 원본 저장을 켠 경우, 원본 사진 백업 + EXIF 주입
            if (saveOriginalPhoto && originalBitmap != null) {
                val originalFilename = "BASS_${timestamp}_ORIG.jpg"
                saveBitmapToMediaStore(
                    bitmap = originalBitmap,
                    filename = originalFilename,
                    subDirectory = "DCIM/BassShot",
                    environmentData = environmentData,
                    measurement = measurement
                )
            }

            compositeUri.toString()
        }
    }

    private fun saveBitmapToMediaStore(
        bitmap: Bitmap,
        filename: String,
        subDirectory: String,
        environmentData: EnvironmentData,
        measurement: BassMeasurementUiModel?
    ): Uri? {
        val resolver = context.contentResolver

        // 임시 파일 생성하여 비트맵 압축 및 EXIF 메타데이터 주입
        val tempFile = File.createTempFile("bass_shot_", ".jpg", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                fos.flush()
            }

            // EXIF 메타데이터 주입 (구글 포토 설명, 서버 파싱용 JSON, GPS 등)
            FishExifHelper.applyExifMetadata(tempFile, measurement, environmentData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, subDirectory)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        outputStream.flush()
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return uri
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    return null
                }
            } else {
                // Android 9 이하 레거시 저장소
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "BassShot"
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, filename)
                tempFile.copyTo(targetFile, overwrite = true)

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                }
                return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        } finally {
            tempFile.delete()
        }
    }
}
