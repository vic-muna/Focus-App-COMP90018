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
     * Whether the phone is lying screen-down: gravity points out through the screen,
     * and the proximity sensor sees something right against it. Phones without a
     * gravity sensor use the accelerometer (the same thing while the phone is still);
     * phones without a proximity sensor go by gravity alone.
     */
    fun isFaceDownFlow(): Flow<Boolean> = callbackFlow {
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (gravity == null) {
            close()
            return@callbackFlow
        }

        var isScreenDown = false
        var isCovered = proximity == null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor) {
                    gravity -> isScreenDown = event.values[2] < FACE_DOWN_GRAVITY_Z
                    // Most proximity sensors only report "near" (0) or "far" (their maximum range).
                    proximity -> isCovered = event.values[0] < event.sensor.maximumRange
                }
                trySend(isScreenDown && isCovered)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_NORMAL)
        if (proximity != null) {
            sensorManager.registerListener(listener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }.distinctUntilChanged()
}
