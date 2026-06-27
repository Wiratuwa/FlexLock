package com.example.fitnessrepcounter.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Data class holding raw sensor readings.
 * @param x acceleration along x-axis (m/s²)
 * @param y acceleration along y-axis (m/s²)
 * @param z acceleration along z-axis (m/s²)
 * @param timestamp nanosecond timestamp from SensorEvent
 */
data class SensorData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
    val sensorType: Int,
)

/**
 * Repository that wraps Android SensorManager into reactive Flows.
 * Registers accelerometer + gyroscope at SENSOR_DELAY_GAME (~20ms interval).
 * Automatically unregisters on flow cancellation to prevent battery drain.
 */
class SensorRepository(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    /** Flow of accelerometer readings */
    val accelerometerFlow: Flow<SensorData> = createSensorFlow(Sensor.TYPE_ACCELEROMETER)

    /** Flow of gyroscope readings */
    val gyroscopeFlow: Flow<SensorData> = createSensorFlow(Sensor.TYPE_GYROSCOPE)

    /** Check if required sensors are available */
    fun hasAccelerometer(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    fun hasGyroscope(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

    /**
     * Creates a callbackFlow that bridges the SensorEventListener callback API
     * into a coroutine-friendly Flow.
     */
    private fun createSensorFlow(sensorType: Int): Flow<SensorData> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
            ?: run {
                close()
                return@callbackFlow
            }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    SensorData(
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        timestamp = event.timestamp,
                        sensorType = sensorType,
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used for rep counting
            }
        }

        // SENSOR_DELAY_GAME = ~20ms interval, good balance of responsiveness vs battery
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
