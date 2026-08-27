package com.example.holoscratch.holo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import dev.piotrprus.particleemitter.CanvasEmitterConfig
import dev.piotrprus.particleemitter.CanvasParticleEmitter
import dev.piotrprus.particleemitter.ParticleShape

/** Colours sampled from the foil so the dust looks like the metal it came off. */
private val DustColors =
  listOf(
    Color(0xFF8A8E94),
    Color(0xFF6E727A),
    Color(0xFFA8ACB2),
    Color(0xFF55595F),
    Color(0xFFBFC3C8),
  )

/** Emission rate at which the fingertip is removing untouched foil. */
private const val MaxParticlesPerSecond = 260

/**
 * Aluminium dust thrown off under the fingertip while scratching.
 *
 * Overlaid *above* the foil and outside its clip, so flecks can spill onto the ticket instead of
 * being cut off at the panel edge. Only this composable reads [ScratchState.emitPoint], so the
 * per-frame position updates recompose the emitter config and nothing else.
 *
 * The emitter runs continuously; the rate is scaled by [DustSource.freshness] — how much foil the
 * finger is actually removing right now — so dragging back over an already-bare area emits nothing,
 * and crossing the ragged boundary of a scratch tapers off naturally. Dropping the rate to zero
 * (rather than removing the emitter) lets already-airborne flecks finish falling.
 */
@Composable
fun ScratchDust(scratch: ScratchState, modifier: Modifier = Modifier) {
  val density = LocalDensity.current
  val source = scratch.dust

  val center =
    if (source == null) {
      DpOffset.Zero
    } else {
      with(density) { DpOffset(source.position.x.toDp(), source.position.y.toDp()) }
    }

  // Below this the finger is essentially riding on bare ticket; emitting stray flecks there is
  // what made the dust look like it came from the finger rather than from the foil.
  val rate =
    if (source == null || source.freshness < 0.05f) 0
    else (MaxParticlesPerSecond * source.freshness).roundToInt()

  CanvasParticleEmitter(
    modifier = modifier,
    config =
      CanvasEmitterConfig(
        particlePerSecond = rate,
        emitterCenter = center,
        // Born around the rim of the fingertip contact patch rather than a single point,
        // so the dust looks pushed out by the edge of the finger.
        startRegionShape = CanvasEmitterConfig.Shape.OVAL,
        startRegionSize = DpSize(36.dp, 36.dp),
        particleShapes = listOf(ParticleShape.Circle),
        lifespanRange = 380..900,
        fadeOutTime = 250..600,
        scaleTime = 300..700,
        colors = DustColors,
        // Grit, not confetti.
        particleSizes = listOf(DpSize(1.5.dp, 1.5.dp), DpSize(2.dp, 2.dp), DpSize(3.dp, 3.dp)),
        // Flicked outward in every direction, then pulled down.
        spread = IntRange(-180, 180),
        initialForce = IntRange(20, 130),
        rotationRange = IntRange(-40, 40),
        startScaleRange = IntRange(1, 1),
        targetScaleRange = IntRange(0, 1),
        gravityStrength = 900f,
        gravityAngle = 0, // down
      ),
  )
}
