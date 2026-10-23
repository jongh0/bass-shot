package com.jongh0.bassshot.data.local

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

class SensorPressureDataSource(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val isSensorAvailable: Boolean = pressureSensor != null

    /**
     * 기기 내장 기압 센서 스트림
     * 대기압 정상 범위(850 ~ 1100 hPa) 내부 값만 유효하게 방출
     */
    val pressureHpaFlow: Flow<Float?> = callbackFlow {
        if (sensorManager == null || pressureSensor == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.isEmpty()) return
                val rawPressure = event.values[0]
                // 엣지 케이스 #15: 비정상 스파이크 필터링 (지구 해수면~산악 지대 대기압 범위: 850hPa ~ 1100hPa)
                if (rawPressure in 850.0f..1100.0f) {
                    val rounded = (rawPressure * 10f).roundToInt() / 10f
                    trySend(rounded)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op
            }
        }

        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
