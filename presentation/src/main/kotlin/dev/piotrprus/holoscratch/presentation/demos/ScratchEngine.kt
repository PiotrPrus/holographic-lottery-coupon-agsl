package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

/**
 * Desktop port of `holo/ScratchState.kt`, with switches for every intermediate version the
 * talk walks through. With the defaults it behaves exactly like the app.
 */
enum class Tool {
    /** Step 1: a stroked path with round caps — a marker, not a coin. */
    RoundBrush,

    /** Step 7: filled rectangles, but the blade never turns. */
    FlatBlade,

    /** Step 9: the blade's long axis stays perpendicular to the drag. */
    TurningBlade,
}

enum class Jitter {
    Off,

    /** New random width on every pointer event: serrated, saw-blade edges. */
    PerEvent,

    /** New target every ~9 px of travel, chased gradually: a ragged scrape. */
    PerDistance,
}

data class ScratchConfig(
    /**
     * Round brush by default: the talk shows a circle until the coin chapter introduces the
     * blade. Coin slides and the finale use [Coin].
     */
    val tool: Tool = Tool.RoundBrush,
    val jitter: Jitter = Jitter.PerDistance,
    /** Join consecutive segments as trapezoids. False = independent rectangles, which gap. */
    val trapezoids: Boolean = true,
    /** Wait for the first move before stamping the press. False = the "tab" bug. */
    val deferPress: Boolean = true,
) {
    companion object {
        /** The finished tool from the app: a turning blade with a wobbling width. */
        val Coin = ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.PerDistance)
    }
}

/** See the app's DustSource: [freshness] is the swept-area metric, [naiveCoverage] the first, wrong one. */
data class DustSource(val position: Offset, val freshness: Float, val naiveCoverage: Float)

const val CELL_PX = 10f
private const val FreshnessSmoothing = 0.35f
private const val WidthJitterMin = 0.70f
private const val WidthJitterMax = 1.20f
private const val OffsetJitter = 0.18f
private const val JitterResamplePx = 9f
private const val JitterFollow = 0.35f

/** Travel needed before the first blade is placed, so its direction is measured, not guessed. */
private const val MinDirectionPx = 8f

/** How quickly the blade turns toward a new direction of travel (eases per-event noise). */
private const val DirectionFollow = 0.5f

@Stable
class ScratchState(config: ScratchConfig = ScratchConfig()) {
    var config by mutableStateOf(config)

    /** Filled geometry for the blade tools. */
    internal val path = Path()

    /** Centre-line polyline for [Tool.RoundBrush], drawn as a stroke. */
    internal val strokePath = Path()

    var revision by mutableIntStateOf(0)
        private set

    var dust by mutableStateOf<DustSource?>(null)
        private set

    /** Last swept-area reading, kept after the finger lifts so meters can show it. */
    var lastFreshness by mutableFloatStateOf(0f)
        private set
    var lastNaive by mutableFloatStateOf(0f)
        private set

    private var last = Offset.Unspecified
    private var pendingPress = false

    /** Smoothed blade direction (unit vector), valid once the press has been placed. */
    private var bladeDirX = 0f
    private var bladeDirY = 1f

    internal var baseHalfLength = 0f
        private set
    private var halfThickness = 0f
    private var halfLength = 0f
    private var sideOffset = 0f
    private var prevHalfLength = 0f
    private var prevSideOffset = 0f
    private var targetHalfLength = 0f
    private var targetSideOffset = 0f
    private var sinceResample = 0f
    private var freshness = 0f

    internal var cols = 0
        private set
    internal var rows = 0
        private set
    internal var cleared = BooleanArray(0)
        private set

    fun setBounds(widthPx: Int, heightPx: Int, bladeLengthPx: Float, bladeThicknessPx: Float) {
        baseHalfLength = bladeLengthPx / 2f
        halfThickness = bladeThicknessPx / 2f
        val newCols = max(1, ceil(widthPx / CELL_PX).toInt())
        val newRows = max(1, ceil(heightPx / CELL_PX).toInt())
        if (newCols != cols || newRows != rows) {
            cols = newCols
            rows = newRows
            cleared = BooleanArray(cols * rows)
        }
    }

    fun start(position: Offset) {
        last = position
        freshness = 0f
        resetJitter()
        dust = DustSource(position, 0f, 0f)
        if (config.tool == Tool.RoundBrush) {
            strokePath.moveTo(position.x, position.y)
            strokePath.lineTo(position.x + 0.01f, position.y)
            clearAlong(position, position, baseHalfLength)
            revision++
            pendingPress = false
        } else if (config.deferPress) {
            pendingPress = true
        } else {
            // The bug: no direction yet, so the blade lands with a default orientation.
            pendingPress = false
            addBlade(position, dirX = 0f, dirY = 1f)
            clearAlong(position, position, halfLength)
            revision++
        }
    }

    fun extend(position: Offset) {
        if (last == Offset.Unspecified) {
            start(position)
            return
        }
        val moved = sqrt((position - last).getDistanceSquared())
        if (pendingPress && moved < MinDirectionPx) {
            // Direction not known yet: keep the press point as the anchor, draw nothing.
            freshness += (0f - freshness) * FreshnessSmoothing
            lastFreshness = freshness
            dust = DustSource(position, freshness, lastNaive)
            return
        }
        val instant = if (moved < 2f) {
            0f
        } else {
            var dirX = (position.x - last.x) / moved
            var dirY = (position.y - last.y) / moved
            if (config.tool == Tool.FlatBlade) {
                // Never turns: always as if dragging straight down.
                dirX = 0f
                dirY = 1f
            } else if (!pendingPress) {
                val x = bladeDirX + (dirX - bladeDirX) * DirectionFollow
                val y = bladeDirY + (dirY - bladeDirY) * DirectionFollow
                val len = sqrt(x * x + y * y)
                if (len > 1e-3f) {
                    dirX = x / len
                    dirY = y / len
                }
            }
            bladeDirX = dirX
            bladeDirY = dirY
            advanceJitter(moved)
            if (pendingPress) stampPress(dirX, dirY)

            naive(position)
            val nx = -dirY
            val ny = dirX
            val from = Offset(last.x + nx * prevSideOffset, last.y + ny * prevSideOffset)
            val to = Offset(position.x + nx * sideOffset, position.y + ny * sideOffset)
            val radius = if (config.tool == Tool.RoundBrush) baseHalfLength else (prevHalfLength + halfLength) / 2f
            val newlyCleared = clearAlong(from, to, radius)

            if (config.tool == Tool.RoundBrush) {
                strokePath.lineTo(position.x, position.y)
            } else {
                addSweep(last, position, dirX, dirY)
            }
            last = position
            revision++
            ratio(newlyCleared, 2f * radius * moved / (CELL_PX * CELL_PX))
        }
        freshness += (instant - freshness) * FreshnessSmoothing
        lastFreshness = freshness
        dust = DustSource(position, freshness, lastNaive)
    }

    fun endStroke() {
        // A press that never moved far enough is a tap: no direction, so no scratch.
        pendingPress = false
        last = Offset.Unspecified
        freshness = 0f
        dust = null
    }

    fun reset() {
        path.reset()
        strokePath.reset()
        last = Offset.Unspecified
        pendingPress = false
        freshness = 0f
        lastFreshness = 0f
        lastNaive = 0f
        dust = null
        cleared.fill(false)
        revision++
    }

    // ── blade jitter ───────────────────────────────────────────────────────

    private fun resetJitter() {
        if (config.jitter == Jitter.Off) {
            halfLength = baseHalfLength
            sideOffset = 0f
        } else {
            halfLength = baseHalfLength * randomIn(WidthJitterMin, WidthJitterMax)
            sideOffset = baseHalfLength * randomIn(-OffsetJitter, OffsetJitter)
        }
        targetHalfLength = halfLength
        targetSideOffset = sideOffset
        prevHalfLength = halfLength
        prevSideOffset = sideOffset
        sinceResample = 0f
    }

    private fun advanceJitter(moved: Float) {
        prevHalfLength = halfLength
        prevSideOffset = sideOffset
        when (config.jitter) {
            Jitter.Off -> Unit
            Jitter.PerEvent -> {
                halfLength = baseHalfLength * randomIn(WidthJitterMin, WidthJitterMax)
                sideOffset = baseHalfLength * randomIn(-OffsetJitter, OffsetJitter)
            }
            Jitter.PerDistance -> {
                sinceResample += moved
                if (sinceResample >= JitterResamplePx) {
                    sinceResample = 0f
                    targetHalfLength = baseHalfLength * randomIn(WidthJitterMin, WidthJitterMax)
                    targetSideOffset = baseHalfLength * randomIn(-OffsetJitter, OffsetJitter)
                }
                halfLength += (targetHalfLength - halfLength) * JitterFollow
                sideOffset += (targetSideOffset - sideOffset) * JitterFollow
            }
        }
    }

    private fun randomIn(from: Float, to: Float): Float = from + Random.nextFloat() * (to - from)

    // ── geometry ───────────────────────────────────────────────────────────

    private fun stampPress(dirX: Float, dirY: Float) {
        pendingPress = false
        addBlade(last, dirX, dirY)
        clearAlong(last, last, halfLength)
        revision++
    }

    private fun addSweep(from: Offset, to: Offset, dirX: Float, dirY: Float) {
        val nx = -dirY
        val ny = dirX
        // Independent rectangles use the current width at both ends, so a jump leaves a step.
        val startHalf = if (config.trapezoids) prevHalfLength else halfLength
        val startOffset = if (config.trapezoids) prevSideOffset else sideOffset

        val fx = from.x + nx * startOffset
        val fy = from.y + ny * startOffset
        val tx = to.x + nx * sideOffset
        val ty = to.y + ny * sideOffset

        if (config.trapezoids) {
            addQuad(
                fx + nx * startHalf, fy + ny * startHalf,
                tx + nx * halfLength, ty + ny * halfLength,
                tx - nx * halfLength, ty - ny * halfLength,
                fx - nx * startHalf, fy - ny * startHalf,
            )
        } else {
            // Shorter than the step so neighbouring rectangles do not overlap and hide the gaps.
            val gx = (tx - fx) * 0.25f
            val gy = (ty - fy) * 0.25f
            addQuad(
                fx + gx + nx * halfLength, fy + gy + ny * halfLength,
                tx + nx * halfLength, ty + ny * halfLength,
                tx - nx * halfLength, ty - ny * halfLength,
                fx + gx - nx * halfLength, fy + gy - ny * halfLength,
            )
        }
        if (config.trapezoids) addBlade(to, dirX, dirY)
    }

    private fun addBlade(center: Offset, dirX: Float, dirY: Float) {
        val nx = -dirY
        val ny = dirX
        val cx = center.x + nx * sideOffset
        val cy = center.y + ny * sideOffset
        val lx = nx * halfLength
        val ly = ny * halfLength
        val tx = dirX * halfThickness
        val ty = dirY * halfThickness
        addQuad(
            cx + lx + tx, cy + ly + ty,
            cx - lx + tx, cy - ly + ty,
            cx - lx - tx, cy - ly - ty,
            cx + lx - tx, cy + ly - ty,
        )
    }

    /**
     * Adds one quad with a consistent (clockwise) winding. The path uses the non-zero fill rule,
     * so an overlapping quad wound the other way would cancel out and leave an unscratched stripe.
     */
    private fun addQuad(
        x0: Float, y0: Float, x1: Float, y1: Float,
        x2: Float, y2: Float, x3: Float, y3: Float,
    ) {
        val area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0) +
            (x2 - x0) * (y3 - y0) - (y2 - y0) * (x3 - x0)
        path.moveTo(x0, y0)
        if (area >= 0f) {
            path.lineTo(x1, y1)
            path.lineTo(x2, y2)
            path.lineTo(x3, y3)
        } else {
            path.lineTo(x3, y3)
            path.lineTo(x2, y2)
            path.lineTo(x1, y1)
        }
        path.close()
    }

    // ── occupancy grid ─────────────────────────────────────────────────────

    private fun ratio(newlyCleared: Int, expectedCells: Float): Float =
        if (expectedCells <= 0f) 0f else (newlyCleared / expectedCells).coerceIn(0f, 1f)

    /** The first metric: what fraction of the tool's footprint is still foil. */
    private fun naive(at: Offset) {
        if (cleared.isEmpty()) return
        val r = baseHalfLength
        var total = 0
        var foil = 0
        forCellsNear(at, at, r) { i ->
            total++
            if (!cleared[i]) foil++
        }
        lastNaive = if (total == 0) 0f else foil.toFloat() / total
    }

    private fun clearAlong(from: Offset, to: Offset, radius: Float): Int {
        if (cleared.isEmpty() || radius <= 0f) return 0
        var newlyCleared = 0
        forCellsNear(from, to, radius) { i ->
            if (!cleared[i]) {
                cleared[i] = true
                newlyCleared++
            }
        }
        return newlyCleared
    }

    private inline fun forCellsNear(from: Offset, to: Offset, radius: Float, action: (Int) -> Unit) {
        val c0 = ((min(from.x, to.x) - radius) / CELL_PX).toInt().coerceIn(0, cols - 1)
        val c1 = ((max(from.x, to.x) + radius) / CELL_PX).toInt().coerceIn(0, cols - 1)
        val r0 = ((min(from.y, to.y) - radius) / CELL_PX).toInt().coerceIn(0, rows - 1)
        val r1 = ((max(from.y, to.y) + radius) / CELL_PX).toInt().coerceIn(0, rows - 1)
        val rSq = radius * radius
        for (r in r0..r1) {
            for (c in c0..c1) {
                val px = (c + 0.5f) * CELL_PX
                val py = (r + 0.5f) * CELL_PX
                if (distanceToSegmentSquared(px, py, from, to) <= rSq) action(r * cols + c)
            }
        }
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
