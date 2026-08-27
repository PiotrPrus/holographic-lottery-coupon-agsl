package com.example.holoscratch.holo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Accumulates the finger strokes that have been scratched off the foil.
 *
 * The strokes live in a single [Path] that is later stroked with [androidx.compose.ui.graphics.BlendMode.Clear],
 * so every point the finger touched erases the foil above the prize.
 *
 * [revision] is a draw-phase invalidation signal: mutating a Path in place does not notify
 * Compose, so each edit bumps this counter and the draw lambda reads it.
 */
@Stable
class ScratchState {
  internal val path = Path()

  internal var revision by mutableIntStateOf(0)
    private set

  private var last = Offset.Unspecified

  fun start(position: Offset) {
    path.moveTo(position.x, position.y)
    // Zero-length segment so a plain tap still leaves a round dot.
    path.lineTo(position.x, position.y)
    last = position
    revision++
  }

  fun extend(position: Offset) {
    if (last == Offset.Unspecified) {
      start(position)
      return
    }
    // Skip sub-pixel jitter; the round cap covers the gaps.
    if ((position - last).getDistanceSquared() < 4f) return
    path.lineTo(position.x, position.y)
    last = position
    revision++
  }

  fun endStroke() {
    last = Offset.Unspecified
  }

  fun reset() {
    path.reset()
    last = Offset.Unspecified
    revision = 0
  }
}
