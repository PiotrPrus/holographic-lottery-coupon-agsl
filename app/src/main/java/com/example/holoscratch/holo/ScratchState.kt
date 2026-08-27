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
import kotlin.random.Random

/** Occupancy-grid resolution, in px. Fine enough for dust decisions, coarse enough to stay cheap. */
private const val CellPx = 10f

/** How fast the smoothed freshness follows the raw per-event ratio. */
private const val FreshnessSmoothing = 0.35f

/** The blade's width wanders between these fractions of its nominal length. */
private const val WidthJitterMin = 0.70f
private const val WidthJitterMax = 1.20f

/** How far the band's centre line can wander sideways, as a fraction of the nominal half-length. */
private const val OffsetJitter = 0.18f

/**
 * Distance travelled before a new width/offset target is drawn. Jitter is resampled per unit of
 * travel rather than per pointer event: events arrive every few px, and rerolling that often
 * produces high-frequency serration rather than the coarse raggedness of a real scrape.
 */
private const val JitterResamplePx = 9f

/** How quickly the current width/offset chase their target. Below 1 so edges wander, not jump. */
private const val JitterFollow = 0.35f

/**
 * Where dust should currently be thrown from.
 *
 * [freshness] is how much foil the blade is actually removing right now, 0..1: the area newly
 * cleared by this movement divided by the area this movement *could* have cleared had it all been
 * virgin foil. Scratching untouched foil gives ~1 at any speed; dragging back over already-bare
 * ticket gives 0, so no dust is emitted there.
 */
data class DustSource(val position: Offset, val freshness: Float)

/**
 * Accumulates the finger strokes that have been scratched off the foil.
 *
 * The strokes live in a single [Path] that is later filled with
 * [androidx.compose.ui.graphics.BlendMode.Clear], so every point the blade passed over erases the
 * foil above the prize.
 *
 * [revision] is a draw-phase invalidation signal: mutating a Path in place does not notify
 * Compose, so each edit bumps this counter and the draw lambda reads it.
 *
 * Alongside the path, a coarse boolean grid records which cells have been cleared. It is the
 * only way to answer "is there still foil under the blade?" cheaply — hit-testing the stroke
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

  /**
   * The scratching tool is a thin rectangle — a coin edge or a fingernail — not a round brush.
   * Its long axis stays perpendicular to the direction of travel, so dragging sweeps a band and
   * a tap leaves a straight-edged scrape rather than a dot.
   */
  private var baseHalfLength = 0f
  private var halfThickness = 0f

  // Live, jittered geometry: nobody drags a coin at a perfectly constant width down a dead
  // straight line, so the band's width and centre wander as the stroke advances.
  private var halfLength = 0f
  private var sideOffset = 0f
  private var prevHalfLength = 0f
  private var prevSideOffset = 0f
  private var targetHalfLength = 0f
  private var targetSideOffset = 0f
  private var sinceResample = 0f

  /**
   * Smoothed [DustSource.freshness]. The raw per-event ratio is noisy because a single move
   * covers only a few grid cells, so it is averaged; the lag also reads naturally, since dust
   * does not stop dead the instant the blade crosses onto bare ticket.
   */
  private var freshness = 0f

  private var cols = 0
  private var rows = 0
  private var cleared = BooleanArray(0)

  /** Called when the gesture starts, since that is where the panel's pixel size is known. */
  fun setBounds(widthPx: Int, heightPx: Int, bladeLengthPx: Float, bladeThicknessPx: Float) {
    baseHalfLength = bladeLengthPx / 2f
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
    resetJitter()
    dust = DustSource(position, 0f)
  }

  fun extend(position: Offset) {
    if (last == Offset.Unspecified) {
      start(position)
      return
    }
    val moved = sqrt((position - last).getDistanceSquared())

    // Skip sub-pixel jitter in the path; the blade is wide enough to cover the gaps. Dust is
    // still re-evaluated, so holding still stops emission instead of running forever.
    val instant =
      if (moved < 2f) {
        0f
      } else {
        val dirX = (position.x - last.x) / moved
        val dirY = (position.y - last.y) / moved
        advanceJitter(moved)
        if (pendingPress) stampPress(dirX, dirY)

        // Clear along the band's actual (offset) centre line, at its actual width.
        val nx = -dirY
        val ny = dirX
        val from = Offset(last.x + nx * prevSideOffset, last.y + ny * prevSideOffset)
        val to = Offset(position.x + nx * sideOffset, position.y + ny * sideOffset)
        val newlyCleared = clearAlong(from, to, (prevHalfLength + halfLength) / 2f)

        addSweep(last, position, dirX, dirY)
        last = position
        revision++

        // A blade of this length sweeping this distance clears at most a length x d band.
        ratio(newlyCleared, (prevHalfLength + halfLength) * moved / (CellPx * CellPx))
      }

    freshness += (instant - freshness) * FreshnessSmoothing
    dust = DustSource(position, freshness)
  }

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

  // ── blade jitter ────────────────────────────────────────────────────────────────────────

  private fun resetJitter() {
    halfLength = baseHalfLength * randomIn(WidthJitterMin, WidthJitterMax)
    sideOffset = baseHalfLength * randomIn(-OffsetJitter, OffsetJitter)
    targetHalfLength = halfLength
    targetSideOffset = sideOffset
    prevHalfLength = halfLength
    prevSideOffset = sideOffset
    sinceResample = 0f
  }

  private fun advanceJitter(moved: Float) {
    sinceResample += moved
    if (sinceResample >= JitterResamplePx) {
      sinceResample = 0f
      targetHalfLength = baseHalfLength * randomIn(WidthJitterMin, WidthJitterMax)
      targetSideOffset = baseHalfLength * randomIn(-OffsetJitter, OffsetJitter)
    }
    prevHalfLength = halfLength
    prevSideOffset = sideOffset
    halfLength += (targetHalfLength - halfLength) * JitterFollow
    sideOffset += (targetSideOffset - sideOffset) * JitterFollow
  }

  private fun randomIn(from: Float, to: Float): Float = from + Random.nextFloat() * (to - from)

  // ── geometry ────────────────────────────────────────────────────────────────────────────

  /** Stamps the deferred press blade once [dirX]/[dirY] is known. */
  private fun stampPress(dirX: Float, dirY: Float) {
    pendingPress = false
    addBlade(last, dirX, dirY)
    clearAlong(last, last, halfLength)
    revision++
  }

  /**
   * Adds the band swept by the blade travelling [from]..[to], plus the blade itself at the
   * leading end so the scrape terminates in a straight edge.
   *
   * The band is a trapezoid, not a rectangle: it starts at the previous width/offset and ends at
   * the current ones, so a wandering blade leaves continuous, ragged-edged geometry with no gaps
   * between consecutive segments.
   */
  private fun addSweep(from: Offset, to: Offset, dirX: Float, dirY: Float) {
    val nx = -dirY
    val ny = dirX

    val fx = from.x + nx * prevSideOffset
    val fy = from.y + ny * prevSideOffset
    val tx = to.x + nx * sideOffset
    val ty = to.y + ny * sideOffset

    path.moveTo(fx + nx * prevHalfLength, fy + ny * prevHalfLength)
    path.lineTo(tx + nx * halfLength, ty + ny * halfLength)
    path.lineTo(tx - nx * halfLength, ty - ny * halfLength)
    path.lineTo(fx - nx * prevHalfLength, fy - ny * prevHalfLength)
    path.close()

    addBlade(to, dirX, dirY)
  }

  /** One flat rectangle: [halfLength] across the direction of travel, [halfThickness] along it. */
  private fun addBlade(center: Offset, dirX: Float, dirY: Float) {
    val nx = -dirY
    val ny = dirX
    val cx = center.x + nx * sideOffset
    val cy = center.y + ny * sideOffset
    val lx = nx * halfLength
    val ly = ny * halfLength
    val tx = dirX * halfThickness
    val ty = dirY * halfThickness

    path.moveTo(cx + lx + tx, cy + ly + ty)
    path.lineTo(cx - lx + tx, cy - ly + ty)
    path.lineTo(cx - lx - tx, cy - ly - ty)
    path.lineTo(cx + lx - tx, cy + ly - ty)
    path.close()
  }

  // ── occupancy grid ──────────────────────────────────────────────────────────────────────

  private fun ratio(newlyCleared: Int, expectedCells: Float): Float =
    if (expectedCells <= 0f) 0f else (newlyCleared / expectedCells).coerceIn(0f, 1f)

  /**
   * Marks every cell within [radius] of the segment [from]..[to] as cleared, returning how many
   * of them were still foil. Approximates the swept band; it differs only at the flat ends, which
   * is far below the resolution the dust metric needs.
   */
  private fun clearAlong(from: Offset, to: Offset, radius: Float): Int {
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
