package com.jongh0.bassshot.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bass_shot_preferences")

data class UserPreferences(
    val referenceGloveWidthCm: Float = 9.5f,
    val saveOriginalPhoto: Boolean = true,
    val shutterSoundEnabled: Boolean = true,
    val cachedLatitude: Double? = null,
    val cachedLongitude: Double? = null,
    val cachedAddress: String? = null,
    val cachedPressureHpa: Float? = null,
    val cachedTempCelsius: Float? = null,
    val cachedWeatherDesc: String? = null
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_GLOVE_WIDTH = floatPreferencesKey("reference_glove_width_cm")
        val KEY_SAVE_ORIGINAL = booleanPreferencesKey("save_original_photo")
        val KEY_SHUTTER_SOUND = booleanPreferencesKey("shutter_sound_enabled")
        val KEY_CACHED_LAT = doublePreferencesKey("cached_lat")
        val KEY_CACHED_LON = doublePreferencesKey("cached_lon")
        val KEY_CACHED_ADDRESS = stringPreferencesKey("cached_address")
        val KEY_CACHED_PRESSURE = floatPreferencesKey("cached_pressure")
        val KEY_CACHED_TEMP = floatPreferencesKey("cached_temp")
        val KEY_CACHED_WEATHER_DESC = stringPreferencesKey("cached_weather_desc")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val gloveWidth = preferences[PreferencesKeys.KEY_GLOVE_WIDTH] ?: 9.5f
            val saveOriginal = preferences[PreferencesKeys.KEY_SAVE_ORIGINAL] ?: true
            val shutterSound = preferences[PreferencesKeys.KEY_SHUTTER_SOUND] ?: true
            val lat = preferences[PreferencesKeys.KEY_CACHED_LAT]
            val lon = preferences[PreferencesKeys.KEY_CACHED_LON]
            val address = preferences[PreferencesKeys.KEY_CACHED_ADDRESS]
            val pressure = preferences[PreferencesKeys.KEY_CACHED_PRESSURE]
            val temp = preferences[PreferencesKeys.KEY_CACHED_TEMP]
            val desc = preferences[PreferencesKeys.KEY_CACHED_WEATHER_DESC]

            UserPreferences(
                referenceGloveWidthCm = gloveWidth.coerceIn(5.0f, 20.0f),
                saveOriginalPhoto = saveOriginal,
                shutterSoundEnabled = shutterSound,
                cachedLatitude = lat,
                cachedLongitude = lon,
                cachedAddress = address,
                cachedPressureHpa = pressure,
                cachedTempCelsius = temp,
                cachedWeatherDesc = desc
            )
        }

    suspend fun updateReferenceGloveWidth(widthCm: Float) {
        val validated = widthCm.coerceIn(5.0f, 20.0f)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_GLOVE_WIDTH] = validated
        }
    }

    suspend fun updateSaveOriginalPhoto(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SAVE_ORIGINAL] = save
        }
    }

    suspend fun updateShutterSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SHUTTER_SOUND] = enabled
        }
    }

    suspend fun saveCachedEnvironment(
        lat: Double,
        lon: Double,
        address: String?,
        pressureHpa: Float?,
        tempCelsius: Float?,
        weatherDesc: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CACHED_LAT] = lat
            preferences[PreferencesKeys.KEY_CACHED_LON] = lon
            if (address != null) preferences[PreferencesKeys.KEY_CACHED_ADDRESS] = address
            if (pressureHpa != null) preferences[PreferencesKeys.KEY_CACHED_PRESSURE] = pressureHpa
            if (tempCelsius != null) preferences[PreferencesKeys.KEY_CACHED_TEMP] = tempCelsius
            if (weatherDesc != null) preferences[PreferencesKeys.KEY_CACHED_WEATHER_DESC] = weatherDesc
        }
    }
}
