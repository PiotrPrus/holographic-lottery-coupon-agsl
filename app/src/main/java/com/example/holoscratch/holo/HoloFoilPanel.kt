package com.example.holoscratch.holo

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import com.example.holoscratch.sensor.Tilt

/**
 * A rectangle filled with the [HOLO_FOIL_SHADER].
 *
 * [tilt] is read inside the draw phase only, so sensor updates trigger a redraw
 * without recomposing anything. Purely sensor-driven; no touch handling.
 */
@Composable
fun HoloFoilPanel(
  tilt: () -> Tilt,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier =
      modifier
        .drawWithCache {
          val shader = RuntimeShader(HOLO_FOIL_SHADER)
          val brush = ShaderBrush(shader)
          shader.setFloatUniform("uResolution", size.width, size.height)
          onDrawBehind {
            val t = tilt()
            shader.setFloatUniform("uTilt", t.roll, t.pitch)
            drawRect(brush)
          }
        }
  ) {
    content()
  }
}
