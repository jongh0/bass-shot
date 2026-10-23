package com.jongh0.bassshot.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jongh0.bassshot.BuildConfig
import com.jongh0.bassshot.data.local.SensorPressureDataSource
import com.jongh0.bassshot.data.local.UserPreferencesRepository
import com.jongh0.bassshot.data.remote.StaticMapDownloader
import com.jongh0.bassshot.data.remote.WeatherApiService
import com.jongh0.bassshot.domain.model.EnvironmentData
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class EnvironmentRepository(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(context),
    private val sensorPressureDataSource: SensorPressureDataSource = SensorPressureDataSource(context),
    private val staticMapDownloader: StaticMapDownloader = StaticMapDownloader()
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val weatherApiService: WeatherApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WeatherApiService::class.java)
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.KOREA)

    /**
     * 현재 시간 기준 기본 데이터 생성
     */
    fun createCurrentTimeData(previous: EnvironmentData = EnvironmentData()): EnvironmentData {
        val now = Date()
        return previous.copy(
            timeFormatted = timeFormat.format(now),
            dateFormatted = dateFormat.format(now),
            dayOfWeekFormatted = dayOfWeekFormat.format(now)
        )
    }

    /**
     * 전체 환경 데이터(위치, 기압, 날씨, 미니맵, 한글 주소) 비동기 통합 수집
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchFreshEnvironmentData(hasLocationPermission: Boolean): EnvironmentData = coroutineScope {
        val now = Date()
        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)
        val dayOfWeekStr = dayOfWeekFormat.format(now)

        if (!hasLocationPermission) {
            // 위치 권한이 없는 경우: 내장 기압계만 확인 후 반환
            val sensorPressure = readSensorPressure()
            return@coroutineScope EnvironmentData(
                timeFormatted = timeStr,
                dateFormatted = dateStr,
                dayOfWeekFormatted = dayOfWeekStr,
                pressureHpa = sensorPressure,
                isFromBarometerSensor = sensorPressure != null
            )
        }

        // 1. 위치 획득 (능동 획득 4초 타임아웃)
        val location = withTimeoutOrNull(4000L) {
            getDeviceLocation()
        }

        if (location == null) {
            // 위치 획득 실패 시 캐시된 정보 사용
            val cached = userPreferencesRepository.userPreferencesFlow.firstOrNull()
            val sensorPressure = readSensorPressure() ?: cached?.cachedPressureHpa
            return@coroutineScope EnvironmentData(
                timeFormatted = timeStr,
                dateFormatted = dateStr,
                dayOfWeekFormatted = dayOfWeekStr,
                tempCelsius = cached?.cachedTempCelsius,
                weatherDescription = cached?.cachedWeatherDesc,
                pressureHpa = sensorPressure,
                isFromBarometerSensor = readSensorPressure() != null,
                addressKorean = cached?.cachedAddress,
                latitude = cached?.cachedLatitude,
                longitude = cached?.cachedLongitude
            )
        }

        val lat = location.latitude
        val lon = location.longitude

        // 2. 비동기 병렬 수집: 주소 변환, 날씨 API, 내장 기압 센서, 정적 미니맵
        val addressDeferred = async(Dispatchers.IO) { reverseGeocode(lat, lon) }
        val weatherDeferred = async(Dispatchers.IO) { fetchWeather(lat, lon) }
        val sensorPressureDeferred = async(Dispatchers.IO) { readSensorPressure() }
        val mapDeferred = async(Dispatchers.IO) { staticMapDownloader.downloadStaticMap(lat, lon) }

        val address = addressDeferred.await()
        val weather = weatherDeferred.await()
        val sensorPressure = sensorPressureDeferred.await()
        val mapBitmap = mapDeferred.await()

        // 3. 기압 결정: 내장 센서 최우선 -> 날씨 API 해면기압 -> 지상기압 순
        val finalPressure = sensorPressure
            ?: weather?.main?.seaLevelPressure
            ?: weather?.main?.pressure

        val weatherDesc = weather?.weather?.firstOrNull()?.description
        val temp = weather?.main?.temp
        val windSpeed = weather?.wind?.speed

        // 4. 차후 오프라인 복원을 위해 캐시에 저장
        userPreferencesRepository.saveCachedEnvironment(
            lat = lat,
            lon = lon,
            address = address,
            pressureHpa = finalPressure,
            tempCelsius = temp,
            weatherDesc = weatherDesc
        )

        EnvironmentData(
            timeFormatted = timeStr,
            dateFormatted = dateStr,
            dayOfWeekFormatted = dayOfWeekStr,
            tempCelsius = temp,
            weatherDescription = weatherDesc,
            windSpeedMps = windSpeed,
            pressureHpa = finalPressure,
            isFromBarometerSensor = sensorPressure != null,
            addressKorean = address,
            staticMapBitmap = mapBitmap,
            latitude = lat,
            longitude = lon
        )
    }

    private suspend fun readSensorPressure(): Float? {
        return withTimeoutOrNull(800L) {
            sensorPressureDataSource.pressureHpaFlow.firstOrNull { it != null }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    if (cont.isActive) cont.resume(lastLoc)
                } else {
                    val cancelToken = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancelToken.token
                    ).addOnSuccessListener { currentLoc ->
                        if (cont.isActive) cont.resume(currentLoc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                    cont.invokeOnCancellation { cancelToken.cancel() }
                }
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    /**
     * 위/경도를 한국어 지번/도로명 주소로 변환
     * Android 13(API 33)+ 비동기 API 분기 & "대한민국 " 접두사 제거
     */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val raw = addresses.firstOrNull()?.getAddressLine(0)
                            continuation.resume(cleanAddress(raw))
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                cleanAddress(addresses?.firstOrNull()?.getAddressLine(0))
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanAddress(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.removePrefix("대한민국").trim()
    }

    private suspend fun fetchWeather(lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.WEATHER_API_KEY
        if (apiKey.isBlank()) return@withContext null
        try {
            weatherApiService.getCurrentWeather(lat, lon, apiKey)
        } catch (e: Exception) {
            null
        }
    }
}
