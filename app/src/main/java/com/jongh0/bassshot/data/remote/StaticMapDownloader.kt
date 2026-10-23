package com.jongh0.bassshot.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jongh0.bassshot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class StaticMapDownloader(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Google Static Maps API로부터 정사각형 지도 비트맵 다운로드
     * 다크 스타일/세련된 로드맵 스타일 파라미터 적용
     */
    suspend fun downloadStaticMap(latitude: Double, longitude: Double): Bitmap? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) return@withContext null

        val url = "https://maps.googleapis.com/maps/api/staticmap?" +
            "center=$latitude,$longitude&zoom=14&size=240x240&scale=2&maptype=roadmap" +
            "&markers=color:red%7C$latitude,$longitude" +
            "&key=$apiKey"

        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
