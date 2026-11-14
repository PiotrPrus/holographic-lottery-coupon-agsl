package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.particleemitter.CanvasEmitterConfig
import dev.piotrprus.particleemitter.CanvasParticleEmitter
import dev.piotrprus.particleemitter.ParticleShape
import kotlin.math.roundToInt

val PrintArea = Color(0xFFEFE7D6)
val FlatFoil = Color(0xFF9A9CA2)
private val Graphite = Color(0xFF3A3D44)

enum class FoilLook { None, Flat, Holo }

enum class DustGate {
    Off,

    /** Emit whenever the finger is down. */
    Always,

    /** Gate on "how much of the tool still covers foil" — reads single digits on virgin foil. */
    Coverage,

    /** Gate on swept area: newly cleared / could-have-cleared. */
    Swept,
}

/** The emitter config as it grows on the dust slide. */
enum class DustRecipe {
    /** Only rate + position: the library defaults. A confetti party. */
    Defaults,

    /** + metal colours and grit-sized particles. */
    Metal,

    /** + thrown out in every direction, short-lived. */
    Spray,

    /** + gravity. The real thing. */
    Full,
}

/** How the scratch path is drawn on the foil. */
enum class Erase {
    /** Not wired yet: the finger does nothing. */
    None,

    /** Drawn with a dark colour — we painted a line instead of scratching. */
    Paint,

    /** BlendMode.Clear. */
    Clear,
}

/** Blade size on stage: the app's 26 × 4 dp, scaled up so the back row can see it. */
val StageBladeLength = 44.dp
val StageBladeThickness = 7.dp

/**
 * One scratch panel: the prize printed on the ticket, the foil on top, optional dust.
 *
 * [mirrors] receive the same pointer input as [scratch], so two panels side by side compare two
 * configurations under exactly the same stroke. Nothing here ever scratches on its own — the
 * speaker drives every stroke.
 *
 * States in [framePoints] skip `change.historical` and keep at most one point per frame
 * ([FRAME_MS]), like an Android app that only reads `change.position`. A desktop mouse never
 * batches historical points, so this rebuilds the "one point per frame" case by time.
 *
 * [offscreen] = false skips `CompositingStrategy.Offscreen`, so `BlendMode.Clear` punches
 * through to the window. [sheenInside] = false draws the shader as a separate overlay outside
 * the scratched layer, so the bare ticket keeps shimmering.
 */
@Composable
fun ScratchPanel(
    scratch: ScratchState,
    tilt: () -> Tilt,
    modifier: Modifier = Modifier,
    mirrors: List<ScratchState> = emptyList(),
    framePoints: List<ScratchState> = emptyList(),
    look: FoilLook = FoilLook.Holo,
    layers: Int = HOLO_ALL_LAYERS,
    oilSlick: Boolean = false,
    offscreen: Boolean = true,
    erase: Erase = Erase.Clear,
    sheenInside: Boolean = true,
    dust: DustGate = DustGate.Off,
    dustRecipe: DustRecipe = DustRecipe.Full,
    clipDust: Boolean = false,
    showGrid: Boolean = false,
    scratchable: Boolean = true,
    cornerRadius: Dp = 22.dp,
    bladeLength: Dp = StageBladeLength,
    bladeThickness: Dp = StageBladeThickness,
    prizeScale: Float = 1f,
    sound: Boolean = false,
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(cornerRadius)
    val all = remember(scratch, mirrors) { listOf(scratch) + mirrors }
    val bladeLengthPx = with(density) { bladeLength.toPx() }
    val bladeThicknessPx = with(density) { bladeThickness.toPx() }

    // Outer box is unclipped so dust can spill past the panel edge.
    Box(modifier = modifier.onSizeChanged { size ->
        all.forEach { it.setBounds(size.width, size.height, bladeLengthPx, bladeThicknessPx) }
    }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(3.dp, shape)
                .clip(shape)
                .background(PrintArea),
        ) {
            Prize(scale = prizeScale)

            if (look != FoilLook.None) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (scratchable && erase != Erase.None) {
                                Modifier.scratchInput(all, framePoints, bladeLengthPx, bladeThicknessPx)
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (offscreen) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            else Modifier,
                        )
                        .drawWithCache {
                            onDrawWithContent {
                                if (look == FoilLook.Holo && sheenInside) {
                                    drawRect(holoFoilBrush(size, tilt(), layers, oilSlick))
                                } else {
                                    drawRect(FlatFoil)
                                }
                                drawContent()
                                drawScratches(scratch, erase)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "SCRATCH HERE",
                        color = Color.Black.copy(alpha = 0.22f),
                        fontSize = (20 * prizeScale).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                    )
                }

                if (look == FoilLook.Holo && !sheenInside) {
                    // The sheen as its own layer: it knows nothing about the scratches below.
                    Canvas(Modifier.matchParentSize().graphicsLayer { alpha = 0.75f }) {
                        drawRect(holoFoilBrush(size, tilt(), layers, oilSlick))
                    }
                }
            }

            if (showGrid) GridOverlay(scratch, Modifier.matchParentSize())
        }

        if (dust != DustGate.Off) {
            ScratchDust(
                scratch = scratch,
                gate = dust,
                recipe = dustRecipe,
                sound = sound,
                modifier = Modifier.matchParentSize().then(if (clipDust) Modifier.clip(shape) else Modifier),
            )
        }
    }
}

private fun DrawScope.drawScratches(scratch: ScratchState, erase: Erase) {
    if (erase == Erase.None) return
    @Suppress("UNUSED_EXPRESSION")
    scratch.revision // draw-phase read: invalidates when the path grows
    val color = if (erase == Erase.Paint) Graphite else Color.Transparent
    val blendMode = if (erase == Erase.Paint) BlendMode.SrcOver else BlendMode.Clear
    if (scratch.config.tool == Tool.RoundBrush) {
        drawPath(
            path = scratch.strokePath,
            color = color,
            blendMode = blendMode,
            style = Stroke(width = scratch.baseHalfLength * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    } else {
        drawPath(path = scratch.path, color = color, blendMode = blendMode)
    }
}

/** One frame at 60 fps. */
private const val FRAME_MS = 16L

private fun Modifier.scratchInput(
    states: List<ScratchState>,
    framePoints: List<ScratchState>,
    bladeLengthPx: Float,
    bladeThicknessPx: Float,
): Modifier = pointerInput(states, framePoints) {
    val everyPoint = states - framePoints.toSet()
    val oncePerFrame = states - everyPoint.toSet()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        states.forEach {
            it.setBounds(size.width, size.height, bladeLengthPx, bladeThicknessPx)
            it.start(down.position)
        }
        down.consume()
        var lastFrame = down.uptimeMillis
        var last = down.position
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    change.historical.forEach { h -> everyPoint.forEach { it.extend(h.position) } }
                    everyPoint.forEach { it.extend(change.position) }
                    if (change.uptimeMillis - lastFrame >= FRAME_MS) {
                        oncePerFrame.forEach { it.extend(change.position) }
                        lastFrame = change.uptimeMillis
                    }
                    last = change.position
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
        oncePerFrame.forEach { it.extend(last) }
        states.forEach { it.endStroke() }
    }
}

@Composable
internal fun Prize(scale: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "YOU WON",
                color = Color.Black,
                fontSize = (22 * scale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
            )
            Text("$100", color = Color.Black, fontSize = (96 * scale).sp, fontWeight = FontWeight.Black)
        }
    }
}

/** The occupancy grid made visible: every cleared 10 px cell tinted cyan. */
@Composable
private fun GridOverlay(scratch: ScratchState, modifier: Modifier) {
    Canvas(modifier) {
        @Suppress("UNUSED_EXPRESSION")
        scratch.revision
        val cell = CELL_PX
        val lineColor = Color(0x2200E5FF)
        var x = 0f
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += cell
        }
        var y = 0f
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += cell
        }
        val grid = scratch.cleared
        val fill = Color(0x8800B8D4)
        for (r in 0 until scratch.rows) {
            for (c in 0 until scratch.cols) {
                if (grid[r * scratch.cols + c]) {
                    drawRect(fill, topLeft = Offset(c * cell + 1f, r * cell + 1f), size = Size(cell - 2f, cell - 2f))
                }
            }
        }
    }
}

private val DustColors = listOf(
    Color(0xFF8A8E94),
    Color(0xFF6E727A),
    Color(0xFFA8ACB2),
    Color(0xFF55595F),
    Color(0xFFBFC3C8),
)

private val GritSizes = listOf(DpSize(2.5.dp, 2.5.dp), DpSize(3.5.dp, 3.5.dp), DpSize(5.dp, 5.dp))

private val PartyColors = listOf(Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFE040FB))
private val PartySizes = listOf(DpSize(10.dp, 10.dp), DpSize(14.dp, 14.dp))

private const val MaxParticlesPerSecond = 320

/** Desktop copy of `holo/ScratchDust.kt`; gate and recipe switchable for the talk. */
@Composable
fun ScratchDust(
    scratch: ScratchState,
    gate: DustGate,
    modifier: Modifier = Modifier,
    recipe: DustRecipe = DustRecipe.Full,
    sound: Boolean = false,
) {
    val density = LocalDensity.current
    val source = scratch.dust
    val center = if (source == null) DpOffset.Zero
    else with(density) { DpOffset(source.position.x.toDp(), source.position.y.toDp()) }

    val strength = when {
        source == null -> 0f
        gate == DustGate.Always -> 1f
        gate == DustGate.Coverage -> source.naiveCoverage
        gate == DustGate.Swept -> source.freshness
        else -> 0f
    }
    val rate = if (strength < 0.05f) 0 else (MaxParticlesPerSecond * strength).roundToInt()

    // The library has no defaults for these, so every recipe starts from the same base.
    fun base(
        colors: List<Color>,
        sizes: List<DpSize>,
        spread: IntRange = IntRange(-30, 30),
        force: IntRange = IntRange(20, 60),
        gravity: Float = 0f,
        lifespan: IntRange = 1200..2000,
    ) = CanvasEmitterConfig(
        particlePerSecond = rate,
        emitterCenter = center,
        startRegionShape = CanvasEmitterConfig.Shape.RECT,
        startRegionSize = DpSize(40.dp, 14.dp),
        particleShapes = listOf(ParticleShape.Circle),
        lifespanRange = lifespan,
        fadeOutTime = 250..600,
        scaleTime = 300..700,
        colors = colors,
        particleSizes = sizes,
        spread = spread,
        initialForce = force,
        rotationRange = IntRange(-40, 40),
        startScaleRange = IntRange(1, 1),
        targetScaleRange = IntRange(0, 1),
        gravityStrength = gravity,
        gravityAngle = 0,
    )

    val config = when (recipe) {
        DustRecipe.Defaults -> base(PartyColors, PartySizes)
        DustRecipe.Metal -> base(DustColors, GritSizes)
        DustRecipe.Spray -> base(DustColors, GritSizes, spread = IntRange(-180, 180), force = IntRange(30, 190), lifespan = 380..900)
        DustRecipe.Full -> base(DustColors, GritSizes, spread = IntRange(-180, 180), force = IntRange(30, 190), gravity = 1300f, lifespan = 380..900)
    }
    // The sound follows the dust: it plays only while particles are coming out.
    // The sound follows the dust; off until the talk introduces it.
    if (sound) ScratchSound(level = if (rate == 0) 0f else strength, position = source?.position)
    CanvasParticleEmitter(modifier = modifier, config = config)
}
