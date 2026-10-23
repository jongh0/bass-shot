package com.jongh0.bassshot.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.jongh0.bassshot.BuildConfig
import com.jongh0.bassshot.domain.model.BassMeasurementDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class GeminiAnalysisDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val pointSchema = Schema(
        name = "point",
        description = "Normalized 2D coordinate (0.0 to 1.0)",
        type = FunctionType.OBJECT,
        properties = mapOf(
            "x" to Schema(name = "x", description = "Horizontal ratio 0.0 (left) to 1.0 (right)", type = FunctionType.NUMBER),
            "y" to Schema(name = "y", description = "Vertical ratio 0.0 (top) to 1.0 (bottom)", type = FunctionType.NUMBER)
        ),
        required = listOf("x", "y")
    )

    private val bassResponseSchema = Schema(
        name = "bass_measurement",
        description = "Bass fish measurement result based on reference hand/glove width",
        type = FunctionType.OBJECT,
        properties = mapOf(
            "is_bass_detected" to Schema(name = "is_bass_detected", description = "True if a bass fish is visible", type = FunctionType.BOOLEAN),
            "length_cm" to Schema(name = "length_cm", description = "Estimated total length from snout tip to tail tip in cm", type = FunctionType.NUMBER),
            "girth_cm" to Schema(name = "girth_cm", description = "Estimated maximum body depth/girth width in cm", type = FunctionType.NUMBER),
            "weight_g" to Schema(name = "weight_g", description = "Estimated weight in grams using W = 0.045 * L * (G^2)", type = FunctionType.INTEGER),
            "snout_point" to pointSchema,
            "tail_point" to pointSchema,
            "max_girth_left" to pointSchema,
            "max_girth_right" to pointSchema
        ),
        required = listOf(
            "is_bass_detected", "length_cm", "girth_cm", "weight_g",
            "snout_point", "tail_point", "max_girth_left", "max_girth_right"
        )
    )

    private fun createModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
                responseSchema = bassResponseSchema
            }
        )
    }

    suspend fun analyzeBassImage(resizedBitmap: Bitmap, referenceHandWidthCm: Float): Result<BassMeasurementDto> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다."))
            }

            val prompt = """
                당신은 어류 계측 전문 AI 비전 시스템입니다.
                사진 속 낚시인이 배스(큰입배스)의 아랫입술을 손(장갑 착용 포함)으로 잡고 세로로 들고 있습니다.
                
                [계측 기준]
                1. 배스 입을 쥐고 있는 주먹(장갑)의 상하 폭(너클 전체 폭) 실제 크기는 정확히 ${referenceHandWidthCm}cm 입니다.
                2. 만약 손(장갑)이 일부 가려져 있다면, 보이는 엄지손가락 폭(약 ${String.format("%.1f", referenceHandWidthCm * 0.25f)}cm)과 배스 두장(머리 길이) 대비 체형 비율을 보조 레퍼런스로 삼으세요.
                3. 이 픽셀 비율을 기준으로 배스의 주둥이 최상단 끝(snout_point)부터 꼬리지느러미 최하단 끝(tail_point)까지의 전장(length_cm)과, 배스 몸통의 가장 두꺼운 체고 구간의 좌우 끝(max_girth_left, max_girth_right) 길이(girth_cm)를 소수점 첫째 자리까지 산출하세요.
                4. 추정 무게(weight_g)는 공식 W = 0.045 * length_cm * (girth_cm ^ 2) 로 계산하여 정수로 반올림하세요.
                5. 모든 좌표(x, y)는 이미지 전체 가로/세로 대비 상대 비율(0.0 ~ 1.0)로 정확히 반환하세요.
            """.trimIndent()

            runCatching {
                withTimeout(12_000L) {
                    val responseText = try {
                        createModel("gemini-2.5-flash").generateContent(
                            content {
                                image(resizedBitmap)
                                text(prompt)
                            }
                        ).text
                    } catch (e: Exception) {
                        createModel("gemini-2.0-flash").generateContent(
                            content {
                                image(resizedBitmap)
                                text(prompt)
                            }
                        ).text
                    } ?: throw IllegalStateException("Empty response from Gemini")

                    val cleanedJson = responseText
                        .trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    json.decodeFromString<BassMeasurementDto>(cleanedJson)
                }
            }
        }
}
