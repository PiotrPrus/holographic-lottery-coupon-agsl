package com.example.holoscratch.holo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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

  /**
   * A press cannot know which way the blade is facing yet, so geometry is deferred until the
   * first move reveals the direction. Stamping it eagerly left a tab sticking out sideways at
   * the start of every scrape.
   */
  private var pendingPress = false

  private var cols = 0
  private var rows = 0
  private var cleared = BooleanArray(0)

  /**
   * The scratching tool is a thin rectangle — a coin edge or a fingernail — not a round brush.
   * Its long axis stays perpendicular to the direction of travel, so dragging sweeps a flat band
   * and a tap leaves a straight-edged scrape rather than a dot.
   */
  private var halfLength = 0f
  private var halfThickness = 0f

  /**
   * Smoothed [DustSource.freshness]. The raw per-event ratio is noisy because a single move
   * covers only a few grid cells, so it is averaged; the lag also reads naturally, since dust
   * does not stop dead the instant the finger crosses onto bare ticket.
   */
  private var freshness = 0f

  /** Called when the gesture starts, since that is where the panel's pixel size is known. */
  fun setBounds(widthPx: Int, heightPx: Int, bladeLengthPx: Float, bladeThicknessPx: Float) {
    halfLength = bladeLengthPx / 2f
    halfThickness = bladeThicknessPx / 2f
    val newCols = max(1, ceil(widthPx / CellPx).toInt())
    val newRows = max(1, ceil(heightPx / CellPx).toInt())
    if (newCols != cols || newRows != rows) {
      cols = newCols
      rows = newRows
      cleared = BooleanArray(cols * rows)
    }
  }

  fun start(position: Offset) {
    last = position
    pendingPress = true
    freshness = 0f
    dust = DustSource(position, 0f)
  }

  /** Stamps the deferred press blade once [dirX]/[dirY] is known. */
  private fun stampPress(dirX: Float, dirY: Float) {
    pendingPress = false
    addBlade(last, dirX, dirY)
    clearAlong(last, last)
    revision++
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
        if (pendingPress) stampPress(ux(last, position), uy(last, position))
        val newlyCleared = clearAlong(last, position)
        addSweep(last, position)
        last = position
        revision++
        // Sweeping a blade of this length over this distance clears at most a length x d band.
        ratio(newlyCleared, 2f * halfLength * moved / (CellPx * CellPx))
      }

    freshness += (instant - freshness) * FreshnessSmoothing
    dust = DustSource(position, freshness)
  }

  private fun ux(from: Offset, to: Offset): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    return if (len < 1e-3f) 0f else dx / len
  }

  private fun uy(from: Offset, to: Offset): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    return if (len < 1e-3f) 1f else dy / len
  }

  private fun ratio(newlyCleared: Int, expectedCells: Float): Float =
    if (expectedCells <= 0f) 0f else (newlyCleared / expectedCells).coerceIn(0f, 1f)

  fun endStroke() {
    // A press that never moved is a genuine tap: land the blade flat.
    if (pendingPress && last != Offset.Unspecified) stampPress(dirX = 0f, dirY = 1f)
    pendingPress = false
    last = Offset.Unspecified
    freshness = 0f
    dust = null
  }

  fun reset() {
    path.reset()
    last = Offset.Unspecified
    pendingPress = false
    freshness = 0f
    dust = null
    cleared.fill(false)
    revision = 0
  }

  /**
   * Adds the band swept by the blade travelling [from]..[to], plus the blade itself at the
   * leading end so the scrape terminates in a straight edge.
   */
  private fun addSweep(from: Offset, to: Offset) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1e-3f) return
    val ux = dx / len
    val uy = dy / len
    // Blade long axis is perpendicular to travel.
    val nx = -uy * halfLength
    val ny = ux * halfLength

    path.moveTo(from.x + nx, from.y + ny)
    path.lineTo(to.x + nx, to.y + ny)
    path.lineTo(to.x - nx, to.y - ny)
    path.lineTo(from.x - nx, from.y - ny)
    path.close()

    addBlade(to, ux, uy)
  }

  /** One flat rectangle: [halfLength] across the direction of travel, [halfThickness] along it. */
  private fun addBlade(center: Offset, dirX: Float, dirY: Float) {
    val nx = -dirY * halfLength
    val ny = dirX * halfLength
    val tx = dirX * halfThickness
    val ty = dirY * halfThickness

    path.moveTo(center.x + nx + tx, center.y + ny + ty)
    path.lineTo(center.x - nx + tx, center.y - ny + ty)
    path.lineTo(center.x - nx - tx, center.y - ny - ty)
    path.lineTo(center.x + nx - tx, center.y + ny - ty)
    path.close()
  }

  /**
   * Marks every cell within [halfLength] of the segment [from]..[to] as cleared, returning how
   * many of them were still foil. Approximates the swept band; it differs only at the flat ends,
   * which is far below the resolution the dust metric needs.
   */
  private fun clearAlong(from: Offset, to: Offset): Int {
    if (cleared.isEmpty() || halfLength <= 0f) return 0
    var newlyCleared = 0
    val minX = min(from.x, to.x) - halfLength
    val maxX = max(from.x, to.x) + halfLength
    val minY = min(from.y, to.y) - halfLength
    val maxY = max(from.y, to.y) + halfLength
    val c0 = ((minX / CellPx).toInt()).coerceIn(0, cols - 1)
    val c1 = ((maxX / CellPx).toInt()).coerceIn(0, cols - 1)
    val r0 = ((minY / CellPx).toInt()).coerceIn(0, rows - 1)
    val r1 = ((maxY / CellPx).toInt()).coerceIn(0, rows - 1)
    val rSq = halfLength * halfLength
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
