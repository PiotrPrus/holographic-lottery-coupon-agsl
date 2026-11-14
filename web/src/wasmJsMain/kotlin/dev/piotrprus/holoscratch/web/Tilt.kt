package dev.piotrprus.holoscratch.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Same as the app's `sensor/DeviceTilt.kt`: roll and pitch, roughly −1..1. */
data class Tilt(val roll: Float, val pitch: Float) {
    companion object {
        val Zero = Tilt(0f, 0f)
    }
}

/** Android's SensorManager.STANDARD_GRAVITY. */
const val G = 9.80665f

private const val Smoothing = 0.18f

/** No motion event this long after start → assume a desktop browser and tilt with the pointer. */
private const val SensorGraceMs = 1500.0

enum class TiltSource { Sensor, NeedsPermission, Denied, Pointer }

/**
 * The phone's gravity vector and the tilt derived from it, for the whole app.
 *
 * holo.js turns the browser's `accelerationIncludingGravity` into Android's TYPE_GRAVITY
 * convention (see the comment there). This object applies the app's formula:
 *
 *   roll  = x / g                     clamp −1..1
 *   pitch = (z / g − 0.62) × 2.2      clamp −1..1
 *
 * smoothed by 0.18. Android smooths per sensor event (SENSOR_DELAY_GAME, ~50 Hz); here it is
 * once per animation frame (~60 Hz) on the latest sample, which feels the same.
 *
 * Without a sensor (desktop browsers) the pointer position over the page drives the same
 * roll/pitch range, so every page still does something on a laptop.
 */
object Motion {
    var x by mutableStateOf(0f)
        private set
    var y by mutableStateOf(0f)
        private set
    var z by mutableStateOf(0f)
        private set
    var tilt by mutableStateOf(Tilt.Zero)
        private set
    var source by mutableStateOf(TiltSource.Pointer)
        private set

    private var roll = 0f
    private var pitch = 0f
    private var pointerRoll = 0f
    private var pointerPitch = 0f
    private val startMs = nowMs()

    /** Called by the root modifier with the pointer position as a fraction of the window. */
    internal fun pointerAt(fx: Float, fy: Float) {
        pointerRoll = (fx * 2f - 1f).coerceIn(-1f, 1f)
        pointerPitch = (fy * 2f - 1f).coerceIn(-1f, 1f)
    }

    internal fun frame() {
        val live = motionEvents() > 0
        val state = motionState()
        source = when {
            live -> TiltSource.Sensor
            state == "needs-permission" -> TiltSource.NeedsPermission
            state == "denied" -> TiltSource.Denied
            nowMs() - startMs < SensorGraceMs -> TiltSource.Sensor // still waiting for a first event
            else -> TiltSource.Pointer
        }

        val targetRoll: Float
        val targetPitch: Float
        if (live) {
            x = motionX().toFloat()
            y = motionY().toFloat()
            z = motionZ().toFloat()
            targetRoll = (x / G).coerceIn(-1f, 1f)
            // z is ~0 when upright and ~g when flat. Re-centre around a natural hold angle.
            targetPitch = ((z / G - 0.62f) * 2.2f).coerceIn(-1f, 1f)
        } else {
            targetRoll = pointerRoll
            targetPitch = pointerPitch
        }
        roll += (targetRoll - roll) * Smoothing
        pitch += (targetPitch - pitch) * Smoothing
        tilt = Tilt(roll, pitch)
    }
}

/** Runs [Motion.frame] every frame. Installed once at the root. */
@Composable
fun MotionLoop() {
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { Motion.frame() }
    }
}

/** Feeds pointer positions to [Motion] without consuming anything, for the no-sensor fallback. */
fun Modifier.pointerTiltFallback(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            if (size.width > 0 && size.height > 0) {
                Motion.pointerAt(change.position.x / size.width, change.position.y / size.height)
            }
        }
    }
}
