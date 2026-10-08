package com.example.focusapp.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.sqrt

// Gravity along the screen's axis when lying face-down is about -9.8 m/s². Below this
// counts as face-down - roughly within 30° of flat.
private const val FACE_DOWN_GRAVITY_Z = -8.5f

/**
 * Phone movement for the gesture shortcuts: shake to end a session (accelerometer),
 * and flip face-down to start one (gravity + proximity sensors).
 * Each flow listens only while collected.
 */
class MotionSensorDataSource(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Total acceleration in g (1 at rest), about 50 times a second. */
    fun gForceFlow(): Flow<Float> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                trySend(sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /**
     * Whether the phone is lying screen-down: gravity points out through the back of the screen.
     * Phones without a gravity sensor use the accelerometer (the same thing while the phone is still).
     */
    fun isFaceDownFlow(): Flow<Boolean> = callbackFlow {
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (gravity == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor == gravity) {
                    val isScreenDown = event.values[2] < FACE_DOWN_GRAVITY_Z
                    trySend(isScreenDown)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.distinctUntilChanged()
}
