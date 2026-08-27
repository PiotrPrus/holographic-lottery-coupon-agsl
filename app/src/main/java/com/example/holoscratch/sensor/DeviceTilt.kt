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
 * Device orientation relative to gravity, normalised to roughly [-1, 1].
 *
 * roll  — rotation around the long (Y) axis: right edge down = positive.
 * pitch — rotation around the short (X) axis: 0 ≈ held at a comfortable ~50° angle,
 *          negative = more upright, positive = laid flat on its back.
 */
data class Tilt(val roll: Float, val pitch: Float) {
  companion object {
    val Zero = Tilt(0f, 0f)
  }
}

/**
 * Reads the gravity vector and exposes it as a smoothed [Tilt].
 * Registers only while the lifecycle is RESUMED. Uses TYPE_GRAVITY (fused, already low-pass
 * filtered) and falls back to the raw accelerometer on devices without it.
 */
@Composable
fun rememberDeviceTilt(smoothing: Float = 0.18f): State<Tilt> {
  val context = LocalContext.current
  val tilt = remember { mutableStateOf(Tilt.Zero) }

  LifecycleResumeEffect(context) {
    val sensorManager = context.getSystemService(SensorManager::class.java)
    val sensor =
      sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var roll = tilt.value.roll
    var pitch = tilt.value.pitch

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val g = SensorManager.STANDARD_GRAVITY
          val targetRoll = (event.values[0] / g).coerceIn(-1f, 1f)
          // values[2] is ~0 when upright and ~1 when flat. Re-centre around a natural hold angle.
          val targetPitch = ((event.values[2] / g - 0.62f) * 2.2f).coerceIn(-1f, 1f)

          roll += (targetRoll - roll) * smoothing
          pitch += (targetPitch - pitch) * smoothing
          tilt.value = Tilt(roll, pitch)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    if (sensor != null) {
      sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }
    onPauseOrDispose { sensorManager.unregisterListener(listener) }
  }

  return tilt
}
