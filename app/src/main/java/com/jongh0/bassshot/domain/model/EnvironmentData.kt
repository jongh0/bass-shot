package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

/**
 * 실시간 카메라 프리뷰 및 최종 캡처본에 오버레이될 환경 데이터
 */
data class EnvironmentData(
    val timeFormatted: String = "--:--", // e.g. "14:18"
    val dateFormatted: String = "", // e.g. "2026년 10월 23일 금요일"
    val tempCelsius: Float? = null, // e.g. 18.0f
    val weatherDescription: String? = null, // e.g. "맑음"
    val windSpeedMps: Float? = null, // e.g. 2.1f
    val pressureHpa: Float? = null, // e.g. 1015f
    val isFromBarometerSensor: Boolean = false, // 내장 센서 측정 여부
    val addressKorean: String? = null, // e.g. "충청남도 아산시 영인면 월선리 573"
    val staticMapBitmap: Bitmap? = null, // 구글 정적 지도 미니 맵 비트맵
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    /**
     * 날씨/기온/바람/기압 한 줄 포맷 텍스트
     * 오프라인 또는 부분 결측치 발생 시 자연스럽게 존재하는 값만 조합
     */
    fun formattedWeatherLine(): String {
        val parts = mutableListOf<String>()

        val weatherPart = buildString {
            if (!weatherDescription.isNullOrBlank()) {
                append(weatherDescription)
                append(" ")
            }
            if (tempCelsius != null) {
                append("${tempCelsius.toInt()}°C")
            }
        }.trim()
        if (weatherPart.isNotEmpty()) parts.add(weatherPart)

        if (windSpeedMps != null) {
            parts.add("바람 ${String.format("%.1f", windSpeedMps)}m/s")
        }

        if (pressureHpa != null) {
            val sensorTag = if (isFromBarometerSensor) "" else ""
            parts.add("${pressureHpa.toInt()}hPa$sensorTag")
        }

        return if (parts.isEmpty()) "기압 측정 대기 중" else parts.joinToString("  |  ")
    }
}
