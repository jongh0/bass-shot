package com.jongh0.bassshot.domain.model

import android.graphics.Bitmap

/**
 * 실시간 카메라 프리뷰 및 최종 캡처본에 오버레이될 환경 데이터
 */
data class EnvironmentData(
    val timeFormatted: String = "--:--", // e.g. "21:24"
    val dateFormatted: String = "", // e.g. "2026년 10월 23일"
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
     * 날짜 및 요일 한 줄 조합: "2026년 10월 21일 (금요일)"
     */
    fun formattedDateAndDayLine(): String {
        return if (dateFormatted.isNotEmpty() && dayOfWeekFormatted.isNotEmpty()) {
            "$dateFormatted ($dayOfWeekFormatted)"
        } else if (dateFormatted.isNotEmpty()) {
            dateFormatted
        } else {
            ""
        }
    }

    /**
     * 나노 바나나 스타일 깔끔한 날짜 조합: "2026. 10. 24 (토)"
     */
    fun formattedDateAndDayClean(): String {
        val cleanDate = dateFormatted
            .replace("년 ", ". ")
            .replace("월 ", ". ")
            .replace("일", "")
            .trim()
        val shortDay = dayOfWeekFormatted.replace("요일", "").trim()
        return if (cleanDate.isNotEmpty() && shortDay.isNotEmpty()) {
            "$cleanDate ($shortDay)"
        } else if (cleanDate.isNotEmpty()) {
            cleanDate
        } else {
            formattedDateAndDayLine()
        }
    }

    /**
     * 주소 전용 라인 (긴 주소도 잘리지 않도록 독립 줄로 제공)
     */
    fun formattedAddressOnly(): String {
        return addressKorean?.takeIf { it.isNotBlank() } ?: "위치 확인 중..."
    }

    /**
     * 우상단 날씨 3줄 표시 전용: 1줄 날씨/기온
     */
    fun formattedWeatherLine1(): String {
        val rawDesc = weatherDescription?.takeIf { it.isNotBlank() } ?: "날씨 확인 중"
        // OpenWeatherMap의 기술적 번역('온흐림'=overcast clouds, '튼구름'=broken clouds)을 친숙한 일상 표현으로 정제
        val desc = when (rawDesc) {
            "온흐림" -> "흐림"
            "튼구름" -> "구름많음"
            else -> rawDesc
        }
        val temp = if (tempCelsius != null) " ${tempCelsius.toInt()}°C" else ""
        return "$desc$temp"
    }

    /**
     * 우상단 날씨 3줄 표시 전용: 2줄 바람
     */
    fun formattedWeatherLine2(): String {
        return if (windSpeedMps != null) "바람 ${String.format(java.util.Locale.US, "%.1f", windSpeedMps)}m/s" else "바람 -- m/s"
    }

    /**
     * 우상단 날씨 3줄 표시 전용: 3줄 기압 (절대 잘리지 않도록 단독 라인)
     */
    fun formattedWeatherLine3(): String {
        return if (pressureHpa != null) "기압 ${Math.round(pressureHpa)}hPa" else "기압 -- hPa"
    }

    /**
     * 날씨, 기온, 바람, 기압 전용 라인: "맑음 18°C  ·  바람 1.2m/s  ·  1013hPa"
     */
    fun formattedWeatherOnly(): String {
        val parts = mutableListOf<String>()

        val weatherPart = buildString {
            if (!weatherDescription.isNullOrBlank()) {
                val desc = when (weatherDescription) {
                    "온흐림" -> "흐림"
                    "튼구름" -> "구름많음"
                    else -> weatherDescription
                }
                append(desc)
                append(" ")
            }
            if (tempCelsius != null) {
                append("${tempCelsius.toInt()}°C")
            }
        }.trim()
        if (weatherPart.isNotEmpty()) parts.add(weatherPart)

        if (windSpeedMps != null) {
            parts.add("바람 ${String.format(java.util.Locale.US, "%.1f", windSpeedMps)}m/s")
        }

        if (pressureHpa != null) {
            parts.add("${pressureHpa.toInt()}hPa")
        }

        return if (parts.isEmpty()) "기상 정보 수신 중" else parts.joinToString("  ·  ")
    }

    /**
     * 하위 호환용 전체 조합 라인
     */
    fun formattedAddressAndWeatherLine(): String {
        val addr = addressKorean?.takeIf { it.isNotBlank() }
        val weather = formattedWeatherOnly()
        return if (addr != null) "$addr  |  $weather" else weather
    }
}
