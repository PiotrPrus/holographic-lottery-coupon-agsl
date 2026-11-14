package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The coin edge, as in the app: 26 dp across the direction of travel, 4 dp along it. */
private val BladeLength: Dp = 26.dp
private val BladeThickness: Dp = 4.dp

/**
 * Port of the app's `holo/HoloFoilPanel.kt`: a rectangle filled with the holo foil shader,
 * scratchable with the coin blade if a [scratch] state is given.
 *
 * Same compositing trick: shader + [content] render into an offscreen layer, and the scratch
 * path is filled with BlendMode.Clear inside it, so foil and sheen come off together.
 */
@Composable
fun HoloFoilPanel(
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
    scratch: ScratchState? = null,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .then(if (scratch == null) Modifier else Modifier.coinScratch(scratch))
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawRect(holoFoilBrush(size, tilt()))
                drawContent()
                if (scratch != null) {
                    @Suppress("UNUSED_EXPRESSION")
                    scratch.revision // draw-phase read: invalidates when the path grows
                    drawPath(path = scratch.path, color = Color.Transparent, blendMode = BlendMode.Clear)
                }
            },
    ) {
        content()
    }
}

/** The app's gesture loop: press, replay coalesced points, release. */
fun Modifier.coinScratch(scratch: ScratchState): Modifier = pointerInput(scratch) {
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
