package com.example.holoscratch.holo

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.holoscratch.R
import kotlin.random.Random

/** The recording, cut into ~120 ms grains with the leading silence removed. */
private val GrainResources =
  listOf(
    R.raw.scratch_grain_00, R.raw.scratch_grain_01, R.raw.scratch_grain_02, R.raw.scratch_grain_03,
    R.raw.scratch_grain_04, R.raw.scratch_grain_05, R.raw.scratch_grain_06, R.raw.scratch_grain_07,
    R.raw.scratch_grain_08, R.raw.scratch_grain_09, R.raw.scratch_grain_10, R.raw.scratch_grain_11,
    R.raw.scratch_grain_12, R.raw.scratch_grain_13, R.raw.scratch_grain_14, R.raw.scratch_grain_15,
  )

/** Finger travel on fresh foil between two scrape sounds. Faster scratching = denser sound. */
private val GrainStep = 14.dp

/** Below this dust strength the finger is on bare ticket: no sound. */
private const val MinLevel = 0.05f

/** Never start grains closer than this, so bursts stay crisp instead of turning into noise. */
private const val MinIntervalMs = 45L

/**
 * The scratching sound. One short grain of a recorded foil scrape plays for every [GrainStep] of
 * finger travel while dust comes out, so the sound follows the movement: a quick swipe gives a
 * burst, a slow drag a few separate scrapes, and the first fresh move sounds immediately.
 *
 * [level] is the dust strength (0..1) and sets the volume; [position] is the fingertip, null when
 * no finger is down. Grains are loaded up front, because loading on first use is a visible delay.
 */
@Composable
fun ScratchSound(level: Float, position: Offset?) {
  val context = LocalContext.current
  val stepPx = with(LocalDensity.current) { GrainStep.toPx() }
  val player = remember {
    GrainPlayer(
      SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .build(),
    ).apply { load { pool -> GrainResources.map { pool.load(context, it, 1) } } }
  }
  DisposableEffect(player) { onDispose { player.release() } }
  SideEffect { player.update(level, position, stepPx) }
}

private class GrainPlayer(private val pool: SoundPool) {
  private var ids: List<Int> = emptyList()
  private var last: Offset? = null
  private var travel = Float.MAX_VALUE
  private var lastIndex = -1
  private var lastPlayMs = 0L

  fun load(block: (SoundPool) -> List<Int>) {
    ids = block(pool)
  }

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
    val now = SystemClock.uptimeMillis()
    if (now - lastPlayMs < MinIntervalMs) return
    travel = 0f
    lastPlayMs = now
    play(level)
  }

  private fun play(level: Float) {
    if (ids.isEmpty()) return
    var index = Random.nextInt(ids.size)
    if (index == lastIndex && ids.size > 1) index = (index + 1) % ids.size
    lastIndex = index
    val volume = 0.4f + 0.6f * level.coerceIn(0f, 1f)
    // A small random pitch shift keeps repeated grains from sounding like a loop.
    pool.play(ids[index], volume, volume, 1, 0, 0.92f + Random.nextFloat() * 0.16f)
  }

  fun release() = pool.release()
}
