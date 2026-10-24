package com.jongh0.bassshot.domain.util

import android.content.Context
import com.jongh0.bassshot.domain.model.IssueReportInput
import com.jongh0.bassshot.domain.model.IssueReportType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IssueReportFormatter {

    fun buildIssueTitle(input: IssueReportInput): String {
        val summary = input.userComment.lineSequence().firstOrNull()?.trim() ?: "내용 없음"
        val truncated = if (summary.length > 40) summary.take(40) + "..." else summary
        return "${input.reportType.titlePrefix} - $truncated"
    }

    fun buildIssueBody(
        context: Context,
        input: IssueReportInput,
        originalImageUrl: String?,
        compositeImageUrl: String?
    ): String {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(Date())

        return buildString {
            // 1. 문제 설명
            append("## 📌 문제 설명\n")
            append(input.userComment.trim())
            append("\n\n")

            // 2. 실측값 vs AI 계측값 비교 (계측 관련 제보일 때)
            if (input.reportType != IssueReportType.GENERAL) {
                append("## 📏 계측값 비교 및 AI 분석 결과\n")
                if (input.actualLengthCm != null || input.actualWeightG != null) {
                    append("### 🎯 사용자 실측값\n")
                    if (input.actualLengthCm != null) {
                        append("- **실제 전장**: ${input.actualLengthCm} cm\n")
                    }
                    if (input.actualWeightG != null) {
                        append("- **실제 무게**: ${input.actualWeightG} g\n")
                    }
                    append("\n")
                }

                val m = input.measurement
                append("### 🤖 AI 계측 결과\n")
                if (m != null) {
                    append("- **배스 검출 여부 (is_bass_detected)**: `${m.isBassDetected}`\n")
                    append("- **수동 직접 계측 모드 여부**: `${m.isManualFallback}`\n")
                    append("- **AI 추정 전장**: `${m.lengthCm} cm`\n")
                    append("- **AI 추정 체고**: `${m.girthCm} cm`\n")
                    append("- **AI 예측 무게**: `${m.weightG} g`\n")
                    append("- **랜드마크 상대 좌표**:\n")
                    append("  - 주둥이(Snout): `(${String.format(Locale.US, "%.3f", m.snoutPoint.x)}, ${String.format(Locale.US, "%.3f", m.snoutPoint.y)})`\n")
                    append("  - 꼬리(Tail): `(${String.format(Locale.US, "%.3f", m.tailPoint.x)}, ${String.format(Locale.US, "%.3f", m.tailPoint.y)})`\n")
                    append("  - 최대 체고(좌/우): `(${String.format(Locale.US, "%.3f", m.maxGirthLeft.x)}, ${String.format(Locale.US, "%.3f", m.maxGirthLeft.y)})` ~ `(${String.format(Locale.US, "%.3f", m.maxGirthRight.x)}, ${String.format(Locale.US, "%.3f", m.maxGirthRight.y)})`\n")
                } else {
                    append("- AI 계측 데이터 없음 (미인식 또는 분석 에러)\n")
                }
                append("\n")
            }

            // 3. 이미지 첨부
            val hasOrig = !originalImageUrl.isNullOrBlank()
            val hasComp = !compositeImageUrl.isNullOrBlank()

            if (hasOrig && hasComp) {
                append("## 📸 이미지 첨부\n")
                append("| 원본 이미지 (Original) | 계측/타임마크 합성본 (Composite) |\n")
                append("| :---: | :---: |\n")
                append("| ![Original]($originalImageUrl) | ![Composite]($compositeImageUrl) |\n\n")
            } else if (hasComp) {
                append("## 📸 이미지 첨부\n")
                append("![제보 사진]($compositeImageUrl)\n\n")
            } else if (hasOrig) {
                append("## 📸 이미지 첨부\n")
                append("![제보 사진]($originalImageUrl)\n\n")
            }

            // 4. 기기 및 환경 정보
            append("## 📱 기기 및 환경 정보\n")
            append("```\n")
            append(AppLogger.getDeviceInfo(context))
            append("\n제보 등록 일시: $nowStr\n")

            input.environmentData?.let { env ->
                append("\n--- 촬영 환경 (EXIF / 센서) ---\n")
                if (env.dateFormatted.isNotBlank() && env.timeFormatted.isNotBlank()) {
                    append("촬영 일시: ${env.dateFormatted} (${env.dayOfWeekFormatted}) ${env.timeFormatted}\n")
                }
                if (!env.addressKorean.isNullOrBlank()) append("위치: ${env.addressKorean}\n")
                if (env.latitude != null && env.longitude != null) append("좌표: ${env.latitude}, ${env.longitude}\n")
                if (env.tempCelsius != null) append("기온: ${env.tempCelsius}°C\n")
                if (env.weatherDescription != null) append("날씨: ${env.weatherDescription}\n")
                if (env.pressureHpa != null) append("기압: ${env.pressureHpa} hPa (내장 센서: ${env.isFromBarometerSensor})\n")
                if (env.windSpeedMps != null) append("풍속: ${env.windSpeedMps} m/s\n")
            }
            append("```\n\n")

            // 5. 최근 앱 로그
            if (input.includeLogs) {
                append("## 💥 최근 앱 실행 로그\n")
                append("<details><summary>최근 로그 펼치기 (클릭)</summary>\n\n")
                append("```shell\n")
                append(AppLogger.getRecentLogsText(80))
                append("\n```\n")
                append("</details>\n")
            }
        }
    }
}
