package dev.piotrprus.holoscratch.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Finger travel on fresh foil between two scrape sounds. Faster scratching = denser sound. */
private val GrainStep = 14.dp

/** Below this dust strength the finger is on bare ticket: no sound. */
private const val MinLevel = 0.05f

/** Never start grains closer than this, so bursts stay crisp instead of turning into noise. */
private const val MinIntervalMs = 45.0

/** One short buzz per grain: the finger feels the same rhythm it hears. */
private const val VibrateMs = 12

/**
 * Port of the app's `holo/ScratchSound.kt`. One ~120 ms grain of a recorded foil scrape plays
 * per [GrainStep] of travel while fresh foil comes off; volume follows freshness, pitch varies
 * ±8 %. Android's SoundPool becomes Web Audio (holo.js), which can only start after a tap, so the
 * very first touch on the page unlocks it.
 *
 * Each grain also sends a short navigator.vibrate pulse. iOS has no vibrate API, so there it is
 * sound only.
 */
@Composable
fun ScratchSound(scratch: ScratchState, vibration: Boolean = true) {
    val stepPx = with(LocalDensity.current) { GrainStep.toPx() }
    LaunchedEffect(scratch) {
        audioWant()
        val player = GrainPlayer(vibration)
        snapshotFlow { scratch.dust }.collect { d -> player.update(d?.freshness ?: 0f, d?.position, stepPx) }
    }
}

private class GrainPlayer(private val vibration: Boolean) {
    private var last: Offset? = null
    private var travel = Float.MAX_VALUE
    private var lastPlayMs = 0.0

    fun update(level: Float, position: Offset?, stepPx: Float) {
        if (position == null) {
            last = null
            travel = Float.MAX_VALUE // the next stroke sounds on its first fresh move
            return
        }
        val previous = last
        last = position
        if (previous != null) travel += (position - previous).getDistance()
        if (level < MinLevel || travel < stepPx) return
        val now = nowMs()
        if (now - lastPlayMs < MinIntervalMs) return
        travel = 0f
        lastPlayMs = now
        val volume = 0.4f + 0.6f * level.coerceIn(0f, 1f)
        // A small random pitch shift keeps repeated grains from sounding like a loop.
        audioPlay(volume.toDouble(), (0.92f + Random.nextFloat() * 0.16f).toDouble())
        if (vibration) vibrate(VibrateMs)
    }
}
