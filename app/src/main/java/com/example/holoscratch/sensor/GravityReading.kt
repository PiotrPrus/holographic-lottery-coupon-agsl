package com.example.holoscratch.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * One raw gravity sample, straight from `event.values`, in m/s².
 * [roll] and [pitch] use the same formula as [rememberDeviceTilt], but without smoothing.
 */
data class GravityReading(val x: Float, val y: Float, val z: Float) {
  val roll: Float
    get() = (x / SensorManager.STANDARD_GRAVITY).coerceIn(-1f, 1f)

  val pitch: Float
    get() = ((z / SensorManager.STANDARD_GRAVITY - 0.62f) * 2.2f).coerceIn(-1f, 1f)

  companion object {
    val Zero = GravityReading(0f, 0f, 0f)
  }
}

/**
 * Reads the gravity vector and exposes every sample unchanged, for the live sensor check screen.
 * Same sensor choice as [rememberDeviceTilt]: TYPE_GRAVITY, falling back to the accelerometer.
 * Registers only while the lifecycle is RESUMED.
 */
@Composable
fun rememberGravityReading(): State<GravityReading> {
  val context = LocalContext.current
  val reading = remember { mutableStateOf(GravityReading.Zero) }

  LifecycleResumeEffect(context) {
    val sensorManager = context.getSystemService(SensorManager::class.java)
    val sensor =
      sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          reading.value = GravityReading(event.values[0], event.values[1], event.values[2])
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    if (sensor != null) {
      sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }
    onPauseOrDispose { sensorManager.unregisterListener(listener) }
  }

  return reading
}
