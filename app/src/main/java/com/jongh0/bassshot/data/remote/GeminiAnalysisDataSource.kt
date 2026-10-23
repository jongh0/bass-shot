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
                사진 속 낚시인이 잡은 배스(큰입배스) 또는 물고기가 있습니다.
                사람의 손(맨손 또는 낚시 장갑) 또는 기준 물체가 함께 찍혀 있을 수 있습니다.
                
                [중요: 랜드마크 키포인트 지정 엄격 규칙]
                1. snout_point(주둥이 끝): 
                   - 배스의 아가미나 가슴, 눈 근처가 절대 아닙니다! 
                   - 배스 주둥이 맨 끝 입술 끝점(Snout tip / Top-lip endpoint)을 정확히 지정해야 합니다.
                   - 세로 또는 가로로 놓여있는 배스 신체 부위 중 입술 끝 좌표를 찍으세요.
                2. tail_point(꼬리 끝):
                   - 꼬리지느러미의 가장 외곽 끝점(지느러미 끝)을 정확히 찍으세요.
                3. max_girth_left, max_girth_right(최대 체고 좌우 끝):
                   - 배스 몸통에서 등지느러미와 배 사이의 가장 두꺼운 단면 좌우 외곽 윤곽선 끝점을 지정하세요.
                
                [배스 검출 여부 (is_bass_detected)]
                - 사진 속에 배스(물고기)가 명확히 보이면 반드시 is_bass_detected: true 로 설정하십시오.
                - 손이나 장갑이 가려져 있거나 잘 보이지 않더라도, 물고기가 확실히 존재하면 is_bass_detected: true 로 설정하고 주둥이와 꼬리 좌표를 도출하십시오.
                - 배스나 물고기가 전혀 없는 책상, 일반 사물, 풍경 등일 때만 is_bass_detected: false 로 설정하십시오.
                
                [길이 산출 교차 검증 (Cross-Check)]
                - 배스 입을 쥐고 있는 주먹(장갑)의 상하 높이(너클 전체 폭) 실제 크기는 약 ${referenceHandWidthCm}cm 입니다.
                - 사진 속 배스의 전체 길이(snout_point ~ tail_point)가 쥐고 있는 주먹 높이의 몇 배인지 먼저 시각적으로 비교하십시오:
                  * 주먹 높이의 약 2배 ~ 2.3배 수준이라면 전장은 약 20cm ~ 23cm (30cm 미만의 어린 잔씨알 배스) 입니다.
                  * 주먹 높이의 약 3배 이상이어야 30cm 이상의 배스입니다.
                
                [수치 산출]
                - length_cm: 주먹 높이와 배스 전장 비율을 계산한 실제 길이 (소수점 첫째 자리까지)
                - girth_cm: 배스 최대 체고 폭 (20cm대 배스는 통상 4.5~6.5cm 수준)
                - weight_g: 어류학 공식 W = 0.045 * length_cm * (girth_cm ^ 2) 로 계산하여 정수 반올림
                - 모든 좌표 x, y는 전체 이미지 가로/세로 대비 상대 비율 (0.0 ~ 1.0)
            """.trimIndent()

            runCatching {
                withTimeout(20_000L) {
                    val responseText = try {
                        createModel("gemini-3.8-flash").generateContent(
                            content {
                                image(resizedBitmap)
                                text(prompt)
                            }
                        ).text
                    } catch (e: Exception) {
                        createModel("gemini-flash-latest").generateContent(
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
