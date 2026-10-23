package com.jongh0.bassshot.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class WeatherResponseDto(
    @SerialName("main") val main: MainDto? = null,
    @SerialName("weather") val weather: List<WeatherDto> = emptyList(),
    @SerialName("wind") val wind: WindDto? = null,
    @SerialName("name") val cityName: String? = null
)

@Serializable
data class MainDto(
    @SerialName("temp") val temp: Float? = null,
    @SerialName("pressure") val pressure: Float? = null,
    @SerialName("sea_level") val seaLevelPressure: Float? = null
)

@Serializable
data class WeatherDto(
    @SerialName("id") val id: Int,
    @SerialName("main") val main: String,
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)

@Serializable
data class WindDto(
    @SerialName("speed") val speed: Float? = null
)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr"
    ): WeatherResponseDto
}
