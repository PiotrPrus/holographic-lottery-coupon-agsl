package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.math.sin

/**
 * Device orientation relative to gravity, normalised to roughly [-1, 1].
 * Same contract as `sensor/DeviceTilt.kt` in the Android app.
 */
data class Tilt(val roll: Float, val pitch: Float) {
    companion object {
        val Zero = Tilt(0f, 0f)

        /** Pose used for offscreen renders, so PNGs show the sheen mid-panel. */
        val Render = Tilt(0.25f, -0.15f)
    }
}

/**
 * Desktop stand-in for the gravity sensor: the mouse position over the demo area becomes
 * roll/pitch, and the value is smoothed with the same exponential moving average the Android
 * sensor code uses. When the mouse sits still for a moment the card starts a gentle wobble,
 * so the effect keeps moving while the speaker talks.
 *
 * While a button is pressed the tilt freezes: dragging only scratches, like on the phone.
 */
@Stable
class MouseTilt internal constructor(initial: Tilt) {
    var value by mutableStateOf(initial)
        internal set

    internal var target = initial
    internal var pressed = false
    internal var lastMoveNanos = 0L
    internal var wobbling = true
}

private const val Smoothing = 0.12f
private const val IdleBeforeWobbleNanos = 1_800_000_000L

@Composable
fun rememberMouseTilt(): MouseTilt {
    val inspection = LocalInspectionMode.current
    val tilt = remember { MouseTilt(if (inspection) Tilt.Render else Tilt.Zero) }
    if (!inspection) {
        LaunchedEffect(tilt) {
            var start = 0L
            while (true) {
                withFrameNanos { now ->
                    if (start == 0L) start = now
                    val idle = System.nanoTime() - tilt.lastMoveNanos > IdleBeforeWobbleNanos
                    if (idle && !tilt.pressed) {
                        tilt.wobbling = true
                        val t = (now - start) / 1_000_000_000f
                        tilt.target = Tilt(
                            roll = sin(t * 0.9f) * 0.55f,
                            pitch = sin(t * 0.63f + 1f) * 0.45f,
                        )
                    }
                    val cur = tilt.value
                    val tgt = tilt.target
                    tilt.value = Tilt(
                        roll = cur.roll + (tgt.roll - cur.roll) * Smoothing,
                        pitch = cur.pitch + (tgt.pitch - cur.pitch) * Smoothing,
                    )
                }
            }
        }
    }
    return tilt
}

/**
 * Area whose hover position drives [tilt]. Observes pointer events on the Initial pass without
 * consuming them, so scratch gestures on children keep working.
 */
@Composable
fun TiltArea(
    tilt: MouseTilt,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.pointerInput(tilt) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    tilt.pressed = change.pressed
                    if (change.pressed || event.type == PointerEventType.Exit) continue
                    val w = size.width.coerceAtLeast(1)
                    val h = size.height.coerceAtLeast(1)
                    val nx = (change.position.x / w * 2f - 1f).coerceIn(-1f, 1f)
                    val ny = (change.position.y / h * 2f - 1f).coerceIn(-1f, 1f)
                    // The virtual lamp follows the cursor: right = roll+, up = pitch+.
                    tilt.target = Tilt(roll = nx, pitch = -ny)
                    tilt.lastMoveNanos = System.nanoTime()
                    tilt.wobbling = false
                }
            }
        },
        content = content,
    )
}
