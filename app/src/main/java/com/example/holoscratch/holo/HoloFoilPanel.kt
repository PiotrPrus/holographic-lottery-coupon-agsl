package com.example.holoscratch.holo

import android.graphics.RuntimeShader
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.holoscratch.sensor.Tilt

/**
 * The scratching tool: a coin edge or a fingernail. [BladeLength] is how wide the scrape is
 * across the direction of travel, [BladeThickness] how thin it is along it.
 */
private val BladeLength: Dp = 26.dp
private val BladeThickness: Dp = 8.dp

/**
 * A rectangle filled with the [HOLO_FOIL_SHADER], scratchable if a [scratch] state is given.
 *
 * [tilt] is read inside the draw phase only, so sensor updates redraw without recomposing.
 * The tilt itself is never driven by touch — dragging only removes foil.
 *
 * Compositing: the whole panel (shader + [content]) renders into an offscreen layer via
 * [CompositingStrategy.Offscreen]. Only then does [BlendMode.Clear] punch real holes through it
 * rather than painting black. Because the shader lives inside that same layer, an erased
 * region loses the metal AND its holographic sheen together — exactly like real foil coming
 * off — while every remaining pixel keeps reacting to tilt.
 */
@Composable
fun HoloFoilPanel(
  tilt: () -> Tilt,
  modifier: Modifier = Modifier,
  scratch: ScratchState? = null,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier =
      modifier
        .then(
          if (scratch == null) Modifier
          else
            Modifier.pointerInput(scratch) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                scratch.setBounds(size.width, size.height, BladeLength.toPx(), BladeThickness.toPx())
                scratch.start(down.position)
                down.consume()
                do {
                  val event = awaitPointerEvent()
                  event.changes.forEach { change ->
                    if (change.pressed) {
                      // Replay coalesced points so fast swipes don't leave gaps.
                      change.historical.forEach { scratch.extend(it.position) }
                      scratch.extend(change.position)
                      change.consume()
                    }
                  }
                } while (event.changes.any { it.pressed })
                scratch.endStroke()
              }
            }
        )
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
          val shader = RuntimeShader(HOLO_FOIL_SHADER)
          val brush = ShaderBrush(shader)
          shader.setFloatUniform("uResolution", size.width, size.height)
          onDrawWithContent {
            val t = tilt()
            shader.setFloatUniform("uTilt", t.roll, t.pitch)
            drawRect(brush)
            drawContent()
            if (scratch != null) {
              @Suppress("UNUSED_EXPRESSION")
              scratch.revision // draw-phase read: invalidates when the path grows
              // Filled, not stroked: the path already holds the rectangles the blade swept out.
              drawPath(path = scratch.path, color = Color.Transparent, blendMode = BlendMode.Clear)
            }
          }
        }
  ) {
    content()
  }
}
