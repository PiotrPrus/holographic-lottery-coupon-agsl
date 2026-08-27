package com.example.holoscratch.holo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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

/**
 * Aluminium dust thrown off under the fingertip while scratching.
 *
 * Overlaid *above* the foil and outside its clip, so flecks can spill onto the ticket instead of
 * being cut off at the panel edge. Only this composable reads [ScratchState.emitPoint], so the
 * per-frame position updates recompose the emitter config and nothing else.
 *
 * The emitter runs continuously; emission is switched by dropping [CanvasEmitterConfig.particlePerSecond]
 * to zero when the finger lifts, which lets already-airborne flecks finish falling.
 */
@Composable
fun ScratchDust(scratch: ScratchState, modifier: Modifier = Modifier) {
  val density = LocalDensity.current
  val point = scratch.emitPoint
  val scratching = point != Offset.Unspecified

  val center =
    if (scratching) {
      with(density) { DpOffset(point.x.toDp(), point.y.toDp()) }
    } else {
      DpOffset.Zero
    }

  CanvasParticleEmitter(
    modifier = modifier,
    config =
      CanvasEmitterConfig(
        particlePerSecond = if (scratching) 220 else 0,
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
