package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Hands-free motion, as if someone picks the card up and turns it in the light: it rolls,
 * pitches and lifts on its own, with no mouse. [tilt] drives the holo shader exactly like the
 * mouse or the gravity sensor would; [lift] is 0 when the card rests and 1 when fully raised.
 */
@Stable
class HandMotion internal constructor() {
    val tilt = MouseTilt(Tilt.Render)

    var lift by mutableFloatStateOf(0.5f)
        internal set
}

@Composable
fun rememberHandMotion(): HandMotion {
    val motion = remember { HandMotion() }
    if (!LocalInspectionMode.current) {
        LaunchedEffect(motion) {
            var start = 0L
            while (true) {
                withFrameNanos { now ->
                    if (start == 0L) start = now
                    val t = (now - start) / 1_000_000_000f
                    // Slow sines at unrelated rates: the motion never quite repeats, like a hand.
                    val roll = sin(t * 0.71f) * 0.75f + sin(t * 1.37f + 0.8f) * 0.2f
                    val pitch = sin(t * 0.53f + 1.9f) * 0.6f + sin(t * 1.11f) * 0.15f
                    motion.tilt.value = Tilt(roll.coerceIn(-1f, 1f), pitch.coerceIn(-1f, 1f))
                    motion.lift = 0.5f + 0.5f * sin(t * 0.42f - 1.2f)
                }
            }
        }
    }
    return motion
}

/** Rolls, pitches and raises the content with [motion]. */
fun Modifier.handHeld(
    motion: HandMotion,
    maxDegrees: Float = 4f,
    liftDp: Float = 6f,
    liftScale: Float = 0.012f,
): Modifier = graphicsLayer {
    val t = motion.tilt.value
    val lift = motion.lift
    rotationY = t.roll * maxDegrees
    rotationX = -t.pitch * maxDegrees * 0.7f
    translationY = -lift * liftDp.dp.toPx()
    val scale = 1f + lift * liftScale
    scaleX = scale
    scaleY = scale
    cameraDistance = 14f * density
    transformOrigin = TransformOrigin.Center
}
