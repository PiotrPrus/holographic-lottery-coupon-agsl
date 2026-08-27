package com.example.holoscratch.holo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Occupancy-grid resolution, in px. Fine enough for dust decisions, coarse enough to stay cheap. */
private const val CellPx = 10f

/** How fast the smoothed freshness follows the raw per-event ratio. */
private const val FreshnessSmoothing = 0.35f

/**
 * Where dust should currently be thrown from.
 *
 * [freshness] is how much foil the finger is actually removing right now, 0..1: the area newly
 * cleared by this movement divided by the area this movement *could* have cleared had it all been
 * virgin foil. Scratching untouched foil gives ~1 at any speed; dragging back over already-bare
 * ticket gives 0, so no dust is emitted there.
 */
data class DustSource(val position: Offset, val freshness: Float)

/**
 * Accumulates the finger strokes that have been scratched off the foil.
 *
 * The strokes live in a single [Path] that is later stroked with
 * [androidx.compose.ui.graphics.BlendMode.Clear], so every point the finger touched erases the
 * foil above the prize.
 *
 * [revision] is a draw-phase invalidation signal: mutating a Path in place does not notify
 * Compose, so each edit bumps this counter and the draw lambda reads it.
 *
 * Alongside the path, a coarse boolean grid records which cells have been cleared. It is the
 * only way to answer "is there still foil under the finger?" cheaply — hit-testing the stroke
 * Path itself would mean building a Region every frame.
 */
@Stable
class ScratchState {
  internal val path = Path()

  internal var revision by mutableIntStateOf(0)
    private set

  /** Non-null only while a finger is down; drives the dust emitter. */
  var dust by mutableStateOf<DustSource?>(null)
    private set

  private var last = Offset.Unspecified

  private var cols = 0
  private var rows = 0
  private var cleared = BooleanArray(0)

  /** Erase radius in px, set from the stroke width the panel draws with. */
  private var radius = 0f

  /**
   * Smoothed [DustSource.freshness]. The raw per-event ratio is noisy because a single move
   * covers only a few grid cells, so it is averaged; the lag also reads naturally, since dust
   * does not stop dead the instant the finger crosses onto bare ticket.
   */
  private var freshness = 0f

  /** Called when the gesture starts, since that is where the panel's pixel size is known. */
  fun setBounds(widthPx: Int, heightPx: Int, strokeWidthPx: Float) {
    radius = strokeWidthPx / 2f
    val newCols = max(1, ceil(widthPx / CellPx).toInt())
    val newRows = max(1, ceil(heightPx / CellPx).toInt())
    if (newCols != cols || newRows != rows) {
      cols = newCols
      rows = newRows
      cleared = BooleanArray(cols * rows)
    }
  }

  fun start(position: Offset) {
    path.moveTo(position.x, position.y)
    // Zero-length segment so a plain tap still leaves a round dot.
    path.lineTo(position.x, position.y)
    val newlyCleared = clearAlong(position, position)
    // A press lands the whole fingertip disc at once.
    val expected = (PI * radius * radius / (CellPx * CellPx)).toFloat()
    freshness = ratio(newlyCleared, expected)
    last = position
    revision++
    dust = DustSource(position, freshness)
  }

  fun extend(position: Offset) {
    if (last == Offset.Unspecified) {
      start(position)
      return
    }
    val moved = sqrt((position - last).getDistanceSquared())

    // Skip sub-pixel jitter in the path; the round cap covers the gaps. Dust is still
    // re-evaluated, so holding still stops emission instead of running forever.
    val instant =
      if (moved < 2f) {
        0f
      } else {
        val newlyCleared = clearAlong(last, position)
        path.lineTo(position.x, position.y)
        last = position
        revision++
        // Sweeping a disc of this radius over this distance can clear at most a 2r x d strip.
        ratio(newlyCleared, 2f * radius * moved / (CellPx * CellPx))
      }

    freshness += (instant - freshness) * FreshnessSmoothing
    dust = DustSource(position, freshness)
  }

  private fun ratio(newlyCleared: Int, expectedCells: Float): Float =
    if (expectedCells <= 0f) 0f else (newlyCleared / expectedCells).coerceIn(0f, 1f)

  fun endStroke() {
    last = Offset.Unspecified
    freshness = 0f
    dust = null
  }

  fun reset() {
    path.reset()
    last = Offset.Unspecified
    freshness = 0f
    dust = null
    cleared.fill(false)
    revision = 0
  }

  /** Marks every cell within [radius] of the segment [from]..[to] as cleared, returning how
   * many of them were still foil. */
  private fun clearAlong(from: Offset, to: Offset): Int {
    if (cleared.isEmpty() || radius <= 0f) return 0
    var newlyCleared = 0
    val minX = min(from.x, to.x) - radius
    val maxX = max(from.x, to.x) + radius
    val minY = min(from.y, to.y) - radius
    val maxY = max(from.y, to.y) + radius
    val c0 = ((minX / CellPx).toInt()).coerceIn(0, cols - 1)
    val c1 = ((maxX / CellPx).toInt()).coerceIn(0, cols - 1)
    val r0 = ((minY / CellPx).toInt()).coerceIn(0, rows - 1)
    val r1 = ((maxY / CellPx).toInt()).coerceIn(0, rows - 1)
    val rSq = radius * radius
    for (r in r0..r1) {
      for (c in c0..c1) {
        val px = (c + 0.5f) * CellPx
        val py = (r + 0.5f) * CellPx
        if (distanceToSegmentSquared(px, py, from, to) <= rSq) {
          val i = r * cols + c
          if (!cleared[i]) {
            cleared[i] = true
            newlyCleared++
          }
        }
      }
    }
    return newlyCleared
  }

  private fun distanceToSegmentSquared(px: Float, py: Float, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lenSq = abx * abx + aby * aby
    val t = if (lenSq == 0f) 0f else (((px - a.x) * abx + (py - a.y) * aby) / lenSq).coerceIn(0f, 1f)
    val dx = px - (a.x + t * abx)
    val dy = py - (a.y + t * aby)
    return dx * dx + dy * dy
  }
}
