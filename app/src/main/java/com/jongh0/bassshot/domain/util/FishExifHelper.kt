package com.jongh0.bassshot.domain.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.jongh0.bassshot.domain.model.BassMeasurementUiModel
import com.jongh0.bassshot.domain.model.EnvironmentData
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class FishExifRecord(
    val app: String = "BassShot",
    val version: String = "1.2.0",
    val timestamp: String,
    val measurement: MeasurementExifData?,
    val environment: EnvironmentExifData
)

@Serializable
data class MeasurementExifData(
    val is_bass_detected: Boolean,
    val length_cm: Float,
    val weight_g: Int,
    val girth_cm: Float,
    val snout_point: PointExifData,
    val tail_point: PointExifData,
    val is_manual: Boolean
)

@Serializable
data class PointExifData(
    val x: Float,
    val y: Float
)

@Serializable
data class EnvironmentExifData(
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val weather: String?,
    val temp_celsius: Float?,
    val wind_speed_mps: Float?,
    val pressure_hpa: Float?,
    val is_sensor_barometer: Boolean
)

object FishExifHelper {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)

    /**
     * 구글 포토 상세 정보 패널 최상단에 표시될 자연스러운 한글 요약
     */
    fun buildHumanReadableDescription(
        measurement: BassMeasurementUiModel?,
        env: EnvironmentData
    ): String {
        return buildString {
            if (measurement != null && measurement.isBassDetected) {
                append("[배스 계측] ")
                append("${measurement.lengthCm}cm · ${measurement.weightG}g")
                append(" | ")
            }
            append(env.formattedWeatherOnly())
            if (!env.addressKorean.isNullOrBlank()) {
                append(" | ")
                append(env.addressKorean)
            }
        }.trim()
    }

    /**
     * 서버 자동 파싱 및 AI 디버깅용 구조화된 무손실 JSON 문자열
     */
    fun buildJsonUserComment(
        measurement: BassMeasurementUiModel?,
        env: EnvironmentData
    ): String {
        val now = Date()
        val record = FishExifRecord(
            timestamp = isoDateFormat.format(now),
            measurement = measurement?.takeIf { it.isBassDetected }?.let { m ->
                MeasurementExifData(
                    is_bass_detected = true,
                    length_cm = m.lengthCm,
                    weight_g = m.weightG,
                    girth_cm = m.girthCm,
                    snout_point = PointExifData(m.snoutPoint.x, m.snoutPoint.y),
                    tail_point = PointExifData(m.tailPoint.x, m.tailPoint.y),
                    is_manual = m.isManualFallback
                )
            },
            environment = EnvironmentExifData(
                latitude = env.latitude,
                longitude = env.longitude,
                address = env.addressKorean,
                weather = env.weatherDescription,
                temp_celsius = env.tempCelsius,
                wind_speed_mps = env.windSpeedMps,
                pressure_hpa = env.pressureHpa,
                is_sensor_barometer = env.isFromBarometerSensor
            )
        )
        return json.encodeToString(record)
    }

    /**
     * JPEG 파일에 표준 EXIF 메타데이터 주입
     */
    fun applyExifMetadata(
        file: File,
        measurement: BassMeasurementUiModel?,
        env: EnvironmentData
    ) {
        try {
            val exif = ExifInterface(file)
            val nowStr = exifDateFormat.format(Date())

            // 1. 구글 포토 / 갤러리 UI 표시용 설명
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, buildHumanReadableDescription(measurement, env))

            // 2. 조과 서버 파싱 & 디버깅용 구조화 JSON
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, buildJsonUserComment(measurement, env))

            // 3. 앱 및 촬영 시각 메타데이터
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "BassShot AI Measurement Camera")
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, nowStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, nowStr)

            // 4. 구글 포토 지도 핀 자동 인식을 위한 GPS 위경도
            if (env.latitude != null && env.longitude != null) {
                exif.setLatLong(env.latitude, env.longitude)
            }

            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 주어진 사진 파일/URI의 EXIF 메타데이터에 이미 배스 계측 데이터가 주입되어 있는지 확인
     */
    fun hasMeasurementData(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val desc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                if (desc != null && desc.contains("[배스 계측]")) {
                    return@use true
                }
                val comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                if (comment != null && (comment.contains("\"is_bass_detected\":true") || comment.contains("\"is_bass_detected\": true"))) {
                    return@use true
                }
                false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
