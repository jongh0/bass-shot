package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

/**
 * 실시간 카메라 프리뷰 및 최종 캡처본에 오버레이될 환경 데이터
 */
data class EnvironmentData(
    val timeFormatted: String = "--:--", // e.g. "13:26"
    val dateFormatted: String = "", // e.g. "2026년10월23일"
    val dayOfWeekFormatted: String = "", // e.g. "금요일"
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
     * 주소 및 기상 정보 한 줄/두 줄 조합
     */
    fun formattedAddressAndWeatherLine(): String {
        val parts = mutableListOf<String>()

        if (!addressKorean.isNullOrBlank()) {
            parts.add(addressKorean)
        }

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
            parts.add("${pressureHpa.toInt()}hPa")
        }

        return if (parts.isEmpty()) "위치 정보 수신 중" else parts.joinToString("  |  ")
    }
}
